/* 파일: GemClearVerification.java
 * 역할: 획득 전환 회귀 검증. 책임: 공중 미획득·착지·지상 스윕·시간·저장·복귀 검증.
 * 협력: 실제 StageRuntime, CompletionStore. 관련 사양: docs/11_GEM_CLEAR.md.
 */
package io.github.hookrecoil;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.*;
import io.github.hookrecoil.collectible.*;
import io.github.hookrecoil.input.InputFrame;
import io.github.hookrecoil.physics.PhysicsConfig;
import io.github.hookrecoil.stage.*;
import io.github.hookrecoil.save.CompletionStore;
import java.nio.file.*;

public class GemClearVerification {
    private static int assertions;
    private static void check(boolean test,String message){assertions++;if(!test)throw new AssertionError(message);}
    private static StageRuntime game(){Json j=new Json();return new StageRuntime(j.fromJson(StageDefinition.class,new FileHandle("assets/stages/prologue.json")),j.fromJson(PhysicsConfig.class,new FileHandle("assets/config/physics.json")));}
    public static void run() {
        // 신체 크기만큼 확장한 센서는 끝점만 검사하면 놓치는 고속 통과도 검출한다.
        check(GemSensor.contact(0,2,10,2,.17f,.75f,5,2.4f,.07f,.15f)>0,"High-speed gem crossing detected");
        check(GemSensor.contact(0,4,10,4,.17f,.75f,5,2.4f,.07f,.15f)<0,"Halo is not a body sensor");
        for(int mode=0;mode<4;mode++) {
            StageRuntime s=game();s.definition.clearGem.y=5;
            s.player.reset(46,4.5f);s.player.previousX=46;s.player.x=47.5f;s.player.vx=16;s.player.vy=mode==0?0:(mode==1?6:-9);
            s.player.facingRight=mode%2==0;s.player.shotPose=.3f;s.grapple.attached=mode==3?0:-1;s.grapple.flying=true;s.elapsed=12.5f;
            final int[] commits={0};s.completionSink=stage->{commits[0]++;};
            s.player.grounded=false;
            check(!s.resolveGoal(false)&&!s.clear.frozen()&&commits[0]==0,"Airborne crossing cannot collect or save");
            s.definition.clearGem.y=2.48f;s.player.reset(46,2);s.player.x=47.5f;s.player.facingRight=mode%2==0;
            s.player.shotPose=.3f;s.grapple.attached=mode==3?0:-1;s.grapple.flying=true;
            check(s.resolveGoal(false),"Valid body sweep collects");float x=s.player.x,y=s.player.y;boolean facing=s.player.facingRight;
            check(x>46&&x<47.5f,"Freeze at first contact, not end of tick");
            InputFrame held=new InputFrame();held.move=-1;held.jump=held.fire=held.hookPressed=held.hookHeld=held.restart=true;
            for(int i=0;i<180;i++)s.tick(held,1f/120);
            check(s.player.x==x&&s.player.y==2&&s.player.y==y&&s.player.facingRight==facing,"Grounded feet and facing remain frozen");
            check(s.player.vx==0&&s.player.vy==0&&s.player.jumpBuffer==0&&s.player.shotPose==0,"Motion and buffered actions cleared");
            check(s.grapple.attached<0&&!s.grapple.flying,"Rope and projectile removed");
            check(s.elapsed==12.5f&&s.clear.time==0,"Physics and presentation clocks separated");
            s.resolveGoal(true);check(s.deaths==0,"No hazards after collecting");
            s.advancePresentation(1.08);check(s.clear.frame()==5,"Ring begins on frame five at slower timing");
            s.advancePresentation(.92);check(s.complete&&commits[0]==1,"Two-second completion commits exactly once");
            s.restartAll();s.tick(new InputFrame(),1f/120);check(!s.clear.frozen()&&s.elapsed>0,"Retry restores simulation");
        }
        StageRuntime landing=game();landing.player.reset(landing.definition.clearGem.x,3);
        landing.player.grounded=false;landing.player.vy=-5;
        for(int i=0;i<120&&!landing.clear.frozen();i++) {
            landing.tick(new InputFrame(),1f/120);
            if(landing.player.y>2.0001f)check(!landing.clear.frozen(),"Falling through gem does not start animation");
        }
        check(landing.clear.frozen()&&landing.player.y==2&&landing.player.grounded,"Animation begins at ground contact without air rewind");
        StageRuntime miss=game();miss.player.reset(miss.definition.clearGem.x,2.4f);miss.player.grounded=false;
        miss.player.vx=10;miss.player.vy=2;
        for(int i=0;i<8;i++)miss.tick(new InputFrame(),1f/120);
        check(!miss.clear.frozen(),"Air touch is not queued for automatic collection elsewhere");
        StageRuntime dead=game();dead.player.reset(dead.definition.clearGem.x,2);
        dead.resolveGoal(true);check(dead.deathTimer>0&&!dead.clear.frozen(),"Same-tick death wins over gem");
        for(int fps:new int[]{30,60,144}) {
            ClearSequence seq=new ClearSequence(new int[]{200,200,200,240,240,360,240,320});seq.begin(1,2,true,7);
            for(int i=0;i<fps*2-1;i++)seq.advance(1.0/fps);
            check(seq.state==ClearSequence.State.COLLECTING,"Animation not completed early");
            seq.advance(1.0/fps);check(seq.state==ClearSequence.State.CLEARED&&seq.frame()==7,"Two seconds / final pose across FPS");
        }
        com.badlogic.gdx.Input previous=com.badlogic.gdx.Gdx.input;
        try {
            com.badlogic.gdx.Gdx.input=(com.badlogic.gdx.Input)java.lang.reflect.Proxy.newProxyInstance(
                com.badlogic.gdx.Input.class.getClassLoader(),new Class[]{com.badlogic.gdx.Input.class},
                (proxy,method,args)->method.getReturnType()==boolean.class?true:null);
            io.github.hookrecoil.input.InputRouter router=new io.github.hookrecoil.input.InputRouter();router.clear();
            router.keyDown(com.badlogic.gdx.Input.Keys.R);router.keyDown(com.badlogic.gdx.Input.Keys.ENTER);
            router.touchDown(0,0,0,com.badlogic.gdx.Input.Buttons.LEFT);
            InputFrame held=router.consume(1,0);
            check(!held.fire&&!held.restart&&!router.newRunRequested,"Held fire/restart/enter are blocked after collection");
            router.keyUp(com.badlogic.gdx.Input.Keys.R);router.keyDown(com.badlogic.gdx.Input.Keys.R);
            router.touchUp(0,0,0,com.badlogic.gdx.Input.Buttons.LEFT);router.touchDown(0,0,0,com.badlogic.gdx.Input.Buttons.LEFT);
            InputFrame fresh=router.consume(1,0);check(fresh.fire&&fresh.restart,"Fresh press restores controls after release");
        } finally {com.badlogic.gdx.Gdx.input=previous;}
        JsonValue frames=new JsonReader().parse(new FileHandle("assets/gem_pickup/atlas.json")).get("images").get("player_collect").get("frames");
        int[] expected={100,100,100,120,120,180,120,160};int index=0;
        for(JsonValue f=frames.child;f!=null;f=f.next)check(f.getInt("duration_ms")==expected[index++],"Atlas timing matches sequence");
        try {
            Path temp=Files.createTempDirectory("hookrecoil-completion-test-");
            CompletionStore store=new CompletionStore(temp);StageRuntime s=game();s.completionSink=store::commit;
            s.player.reset(s.definition.clearGem.x,2);s.elapsed=17.25f;s.resolveGoal(false);
            check(Files.exists(temp.resolve("completion.json"))&&!s.complete,"Saved before animation ends");
            CompletionStore.Record r=store.load();StageRuntime restored=game();store.restore(restored,r);
            check(restored.complete&&restored.elapsed==17.25f,"Restart goes directly to results");
            store.commit(s);Files.write(temp.resolve("completion.json"),new byte[]{1,2,3});
            check(store.load().seconds==17.25f,"Corrupt main recovers valid backup");
            Files.deleteIfExists(temp.resolve("completion.json"));Files.deleteIfExists(temp.resolve("completion.backup.json"));Files.delete(temp);
        }catch(Exception e){throw new AssertionError("Atomic completion persistence",e);}
        System.out.println("Gem clear PASS: "+assertions+" assertions.");
    }
}
