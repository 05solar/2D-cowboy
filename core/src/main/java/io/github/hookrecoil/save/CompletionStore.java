/* 파일: CompletionStore.java
 * 역할: 프롤로그 완료 기록 저장. 책임: 임시 파일 fsync·원자 교체·백업·결과 화면 복귀.
 * 협력: GameplayScreen, StageRuntime. 관련 사양: docs/11_GEM_CLEAR.md.
 */
package io.github.hookrecoil.save;

import com.badlogic.gdx.utils.Json;
import io.github.hookrecoil.stage.StageRuntime;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

public class CompletionStore {
    public static class Record {
        public int schemaVersion=1,deaths,shots;
        public String stageId="prologue",gemId,unlockedStageId="stage_01";
        public boolean completed=true,facingRight;
        public float seconds,x,y;
    }
    private final Path directory;
    public CompletionStore(Path directory) {this.directory=directory;}
    public Record load() throws IOException {
        Path main=directory.resolve("completion.json"),backup=directory.resolve("completion.backup.json");
        if(!Files.exists(main)&&!Files.exists(backup))return null;
        try{return read(main);}catch(Exception first){
            try{return read(backup);}catch(Exception second){throw new IOException("Completion save cannot be read",first);}
        }
    }
    private Record read(Path path) throws IOException {
        Record r=new Json().fromJson(Record.class,new String(Files.readAllBytes(path),StandardCharsets.UTF_8));
        if(r.schemaVersion!=1||!r.completed||!"prologue".equals(r.stageId)||r.gemId==null
            ||!Float.isFinite(r.seconds)||r.seconds<0||!Float.isFinite(r.x)||!Float.isFinite(r.y))throw new IOException("Invalid completion record");
        return r;
    }
    public void commit(StageRuntime s) throws IOException {
        Files.createDirectories(directory);
        Record r=new Record();r.gemId=s.definition.clearGem.id;r.seconds=s.clear.recordTime;
        r.x=s.clear.x;r.y=s.clear.y;r.facingRight=s.clear.facingRight;r.deaths=s.deaths;r.shots=s.shots;
        Path main=directory.resolve("completion.json"),temp=directory.resolve("completion.tmp"),backup=directory.resolve("completion.backup.json");
        byte[] bytes=new Json().toJson(r).getBytes(StandardCharsets.UTF_8);
        try(FileChannel channel=FileChannel.open(temp,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)) {
            ByteBuffer buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())channel.write(buffer);channel.force(true);
        }
        read(temp);
        if(Files.exists(main)) {try {read(main);Files.copy(main,backup,StandardCopyOption.REPLACE_EXISTING);}catch(IOException ignored){/* 정상 백업 보존 */}}
        // 원자 교체 미지원이면 기존 파일을 보존하고 호출자에게 실패를 보고한다.
        Files.move(temp,main,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }
    public void restore(StageRuntime s,Record r) {
        if(!s.definition.clearGem.id.equals(r.gemId))return;
        s.player.reset(r.x,r.y);s.player.facingRight=r.facingRight;s.elapsed=r.seconds;s.deaths=r.deaths;s.shots=r.shots;
        s.clear.begin(r.x,r.y,r.facingRight,r.seconds);s.advancePresentation(s.clear.durationSeconds());s.grapple.reset();
    }
}
