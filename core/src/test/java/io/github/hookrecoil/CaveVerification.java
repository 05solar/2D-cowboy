/* 파일: CaveVerification.java
 * 역할: 동굴 전체 경로와 진행 회귀 검증. 책임: 실제 120Hz 입력 경로·FPS 동일성·장치·저장.
 * 협력: CaveRouteDriver, StageRuntime, ProgressStore. 임시 저장 디렉터리만 사용한다.
 * 관련 사양: docs/12_CAVE_CHAPTER.md.
 */
package io.github.hookrecoil;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.*;
import io.github.hookrecoil.stage.*;
import io.github.hookrecoil.physics.PhysicsConfig;
import io.github.hookrecoil.save.ProgressStore;
import io.github.hookrecoil.input.InputFrame;
import java.nio.file.*;
public class CaveVerification {
    private static int assertions;
    private static void check(boolean condition,String message){assertions++;if(!condition)throw new AssertionError(message);}
    public static StageRuntime game(String id){Json j=new Json();return new StageRuntime(j.fromJson(StageDefinition.class,new FileHandle("assets/stages/"+id+".json")),j.fromJson(PhysicsConfig.class,new FileHandle("assets/config/physics.json")));}
    private static StageRuntime run(String id,int fps){
        StageRuntime s=game(id);CaveRouteDriver driver=new CaveRouteDriver(new JsonReader().parse(new FileHandle("assets/stages/cave_routes.json")).get(id));
        double accumulator=0;int ticks=0;
        while(ticks<18000&&!s.clear.frozen()){
            accumulator+=1.0/fps;
            while(accumulator+1e-12>=1.0/120&&!s.clear.frozen()){
                s.tick(driver.next(s),1f/120);accumulator-=1.0/120;ticks++;
                if(s.deaths>0)throw new AssertionError(id+" route "+driver.routeIndex+" phase "+driver.phase+" died x="+s.player.x+" y="+s.player.y+" ticks="+ticks);
            }
        }
        check(s.clear.frozen(),id+" route timeout "+driver.routeIndex+" x="+s.player.x+" y="+s.player.y+" phase="+driver.phase);
        check(s.checkpoint+1==s.definition.checkpoints.length,id+" checkpoints reached: "+(s.checkpoint+1)+" / "+s.definition.checkpoints.length+"; x="+s.player.x+" route="+driver.routeIndex);
        System.out.printf(java.util.Locale.ROOT,"%s %d FPS full input route: %.3fs %d shots, %d deaths, %d checkpoints%n",id,fps,s.clear.recordTime,s.shots,s.deaths,s.checkpoint+1);
        return s;
    }
    public static void main(String[] args)throws Exception{
        for(int i=1;i<=5;i++){
            String id=String.format("cave_%02d",i);StageRuntime first=run(id,30);
            for(int fps:new int[]{60,144}){StageRuntime s=run(id,fps);check(s.clear.recordTime==first.clear.recordTime&&s.clear.x==first.clear.x&&s.shots==first.shots,"FPS deterministic "+id);}
        }
        StageCatalog catalog=new Json().fromJson(StageCatalog.class,new FileHandle("assets/cave_chapter/stages.json"));
        check(catalog.stages.length==20&&catalog.find("prologue")==null,"Prologue separate");
        Path dir=Files.createTempDirectory("hook-cave-verification-");ProgressStore store=new ProgressStore(dir);store.load();
        check(store.canStart(catalog.find("cave_01")),"Implemented cave stages are available on a fresh save");
        check(store.canStart(catalog.find("mine_05")),"Implemented mine stages are available on a fresh save");
        check(!store.canStart(catalog.find("city_01")),"Unimplemented stages remain unavailable");
        StageRuntime p=game("prologue");p.player.reset(p.definition.clearGem.x,2);p.resolveGoal(false);store.begin(p);
        check(store.canStart(catalog.find("cave_01")),"Implemented stages remain startable during pending collection");
        ProgressStore restored=new ProgressStore(dir);check(restored.load()!=null,"Interrupted collection recovered");check(restored.canStart(catalog.find("cave_01")),"Prologue unlocks cave 1");
        for(int i=1;i<=5;i++){
            String id=String.format("cave_%02d",i);StageRuntime s=game(id);s.player.reset(s.definition.clearGem.x,s.definition.clearGem.y-.48f);
            s.completionSink=restored::begin;s.resultSink=x->restored.finish();s.resolveGoal(false);check(!restored.cleared(id),"No early unlock");
            s.advancePresentation(1);s.advancePresentation(1);check(restored.cleared(id),"Clear saved once");check(restored.canStart(catalog.find(id)),"Replay enabled");
        }
        check(restored.unlocked(catalog.find("mine_01"))&&restored.canStart(catalog.find("mine_01")),"Cave 5 unlocks the test-enabled mine chapter");
        check(catalog.find("mine_05").available&&catalog.find("mine_05").number==10,"Mine 6-10 test cards are enabled");
        ProgressStore reload=new ProgressStore(dir);reload.load();check(reload.cleared("cave_05"),"Disk restore");
        StageRuntime s=game("cave_04");s.player.reset(4,2);s.devices.landed(s.player,s.config);
        boolean heardWarning=false;for(int i=0;i<119;i++){s.devices.tick(1f/120);heardWarning|=s.devices.warning;}
        check(heardWarning,"Crumble warning fires before collapse");check(s.definition.platforms[1].active,"Before collapse retains collision");
        for(int i=0;i<2;i++)s.devices.tick(1f/120);check(!s.definition.platforms[1].active,"Collapse removes collision");s.respawn();check(s.definition.platforms[1].active,"Respawn resets devices");
        s.resolveGoal(true);for(int i=0;i<43;i++)s.tick(new InputFrame(),1f/120);check(s.deathTimer==0,"Failure resets in 0.36 sec");
        s=game("cave_02");s.player.reset(10,2);
        for(int i=0;i<84;i++)s.tick(new InputFrame(),1f/120);
        check(s.deaths==1,"Open pit triggers a fall death");
        check(s.player.x==s.definition.spawnX&&s.deathTimer==0,"Open pit respawns within 0.7 sec");
        s=game("cave_01");s.player.reset(10,2);
        for(int i=0;i<100;i++)s.tick(new InputFrame(),1f/120);
        check(s.deaths==0&&s.player.grounded&&Math.abs(s.player.y+1)<0.001f,"Cave 1 recovery pad remains safe");
        s=game("cave_04");s.player.reset(4,2);
        for(int i=0;i<210;i++)s.tick(new InputFrame(),1f/120);
        check(s.deaths==1,"Cave 4 crumble platform fall is lethal");
        check(s.player.x==s.definition.spawnX&&s.deathTimer==0,"Cave 4 fall returns to checkpoint");
        s=game("cave_03");s.player.previousX=8;s.player.x=16;s.player.previousY=s.player.y=-.2f;check(s.devices.lethal(s.player,s.config),"Fast swept hazard cannot tunnel");
        System.out.println("Cave verification PASS: "+assertions+" assertions; isolated save "+dir);
    }
}
