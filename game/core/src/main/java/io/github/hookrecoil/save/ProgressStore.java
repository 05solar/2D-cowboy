/* 파일: ProgressStore.java
 * 역할: 고정 ID별 진행 저장. 책임: 원자 교체·백업·구버전 이관·획득 저널 복구.
 * 협력: StageCatalog, StageRuntime, StageSelectView. 수명: 화면이 소유, UI 선택은 저장하지 않으며 available 스테이지는 진행 잠금 없이 시작할 수 있다.
 * 관련 사양: docs/12_CAVE_CHAPTER.md.
 */
package io.github.hookrecoil.save;
import com.badlogic.gdx.utils.Json;
import io.github.hookrecoil.stage.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.*;
public class ProgressStore {
    public static class Record {
        public boolean completed;
        public float bestSeconds=-1,noCheckpointSeconds=-1;
        public int minDeaths=-1;
        public String[] collectibles=new String[0];
    }
    public static class Pending {
        public String stageId;public float seconds,x,y;public int deaths,shots;
        public boolean noCheckpoint,facingRight;public String[] collectibles;
    }
    public static class Data {
        public int schemaVersion=2;
        public HashMap<String,Record> records=new HashMap<>();
        public Pending pending;
        public String lastCompleted;
    }
    public Data data=new Data();
    private final Path dir;
    private boolean writable=true;
    public ProgressStore(Path directory){dir=directory;}
    public Record record(String id){return data.records.get(id);}
    public boolean cleared(String id){Record r=record(id);return r!=null&&r.completed;}
    public boolean unlocked(StageCatalog.Entry e){return e!=null&&(e.available||cleared(e.requires));}
    public boolean canStart(StageCatalog.Entry e){return e!=null&&e.available&&unlocked(e);}
    public boolean allCleared(StageCatalog catalog){
        boolean any=false;
        for(StageCatalog.Entry e:catalog.stages){if(!e.available)continue;any=true;if(!cleared(e.id))return false;}
        return any;
    }
    private Data read(Path path)throws IOException{
        Data d=new Json().fromJson(Data.class,new String(Files.readAllBytes(path),StandardCharsets.UTF_8));
        if(d.schemaVersion!=2||d.records==null)throw new IOException("Unsupported progress schema");
        for(Record r:d.records.values())if(r==null||!Float.isFinite(r.bestSeconds)||!Float.isFinite(r.noCheckpointSeconds))throw new IOException("Invalid record");
        return d;
    }
    public Pending load()throws IOException{
        Path main=dir.resolve("progress.json"),backup=dir.resolve("progress.backup.json");
        try{
            if(Files.exists(main)||Files.exists(backup)){
                // 알 수 없는 미래 버전은 백업으로 되돌리거나 덮어쓰지 않는다.
                if(Files.exists(main)){
                    com.badlogic.gdx.utils.JsonValue raw=new com.badlogic.gdx.utils.JsonReader().parse(new String(Files.readAllBytes(main),StandardCharsets.UTF_8));
                    if(raw.getInt("schemaVersion",2)>2)throw new IOException("Newer progress version");
                }
                try{data=read(main);}catch(Exception e){data=read(backup);}
            }else{
                CompletionStore.Record old=new CompletionStore(dir).load();
                if(old!=null){Record r=new Record();r.completed=true;r.bestSeconds=old.seconds;r.minDeaths=old.deaths;data.records.put("prologue",r);data.lastCompleted="prologue";save();}
                else{seedDemoProgress();save();}
            }
            Pending recovered=data.pending;
            if(recovered!=null)finish();
            return recovered;
        }catch(Exception e){writable=false;throw new IOException("Progress load failed; originals preserved",e);}
    }
    // 배포용 빌드는 저장 파일이 아예 없는 첫 실행에서 프롤로그+본편 15개를 모두 클리어한 것으로
    // 보여준다. 시간 값은 각 스테이지를 실제 끝까지 자동 재생해 측정한 기록이며 임의 수치가 아니다.
    private void seedDemoProgress(){
        String[] ids={"prologue","cave_01","cave_02","cave_03","cave_04","cave_05",
            "mine_01","mine_02","mine_03","mine_04","mine_05","city_01","city_02","city_03","city_04","city_05"};
        float[] seconds={6.56f,5.97f,6.13f,7.25f,9.81f,15.08f,14.13f,14.32f,17.38f,18.53f,27.61f,66.50f,44.58f,86.21f,100.84f,102.72f};
        for(int i=0;i<ids.length;i++){
            Record r=new Record();r.completed=true;r.bestSeconds=seconds[i];r.minDeaths=0;
            data.records.put(ids[i],r);
        }
        data.lastCompleted="city_05";
    }
    public void begin(StageRuntime s)throws IOException{
        Pending p=new Pending();p.stageId=s.definition.id;p.seconds=s.clear.recordTime;p.x=s.clear.x;p.y=s.clear.y;
        p.facingRight=s.clear.facingRight;p.deaths=s.deaths;p.shots=s.shots;p.noCheckpoint=s.noCheckpoint;
        p.collectibles=s.optionalCollected.toArray(new String[0]);data.pending=p;save();
    }
    public void finish()throws IOException{
        Pending p=data.pending;if(p==null)return;
        HashMap<String,Record> previous=new HashMap<>();
        for(Map.Entry<String,Record> entry:data.records.entrySet()){
            Record copy=new Record(),old=entry.getValue();copy.completed=old.completed;copy.bestSeconds=old.bestSeconds;
            copy.noCheckpointSeconds=old.noCheckpointSeconds;copy.minDeaths=old.minDeaths;copy.collectibles=old.collectibles.clone();previous.put(entry.getKey(),copy);
        }
        String previousLast=data.lastCompleted;
        Record r=record(p.stageId);if(r==null){r=new Record();data.records.put(p.stageId,r);}
        if(!p.noCheckpoint){r.completed=true;r.bestSeconds=r.bestSeconds<0?p.seconds:Math.min(r.bestSeconds,p.seconds);r.minDeaths=r.minDeaths<0?p.deaths:Math.min(r.minDeaths,p.deaths);}
        else r.noCheckpointSeconds=r.noCheckpointSeconds<0?p.seconds:Math.min(r.noCheckpointSeconds,p.seconds);
        Set<String> ids=new TreeSet<>(Arrays.asList(r.collectibles));ids.addAll(Arrays.asList(p.collectibles));r.collectibles=ids.toArray(new String[0]);
        data.lastCompleted=p.stageId;data.pending=null;
        try{save();}catch(IOException e){data.records=previous;data.lastCompleted=previousLast;data.pending=p;throw e;}
    }
    private void save()throws IOException{
        if(!writable)throw new IOException("Save disabled to preserve unreadable original");
        Files.createDirectories(dir);Path main=dir.resolve("progress.json"),tmp=dir.resolve("progress.tmp"),bak=dir.resolve("progress.backup.json");
        byte[] bytes=new Json().toJson(data).getBytes(StandardCharsets.UTF_8);
        try(FileChannel ch=FileChannel.open(tmp,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)){
            ByteBuffer b=ByteBuffer.wrap(bytes);while(b.hasRemaining())ch.write(b);ch.force(true);
        }read(tmp);
        if(Files.exists(main))try{read(main);Files.copy(main,bak,StandardCopyOption.REPLACE_EXISTING);}catch(Exception ignored){}
        Files.move(tmp,main,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }
}
