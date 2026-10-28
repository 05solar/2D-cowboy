/* 파일: MineVerification.java
 * 역할: 광산 6~10 통합 검증. 책임: 실제 데이터·이동 장치·위험·재충전·완주 경로 검사.
 * 협력 대상: MineRouteDriver, StageRuntime, ProgressStore. 파일 저장은 격리된 임시 폴더만 사용한다.
 * 관련 사양: docs/assets/mine_chapter/STAGES_06_10.md.
 */
package io.github.hookrecoil;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import io.github.hookrecoil.input.InputFrame;
import io.github.hookrecoil.physics.PhysicsConfig;
import io.github.hookrecoil.stage.*;

public final class MineVerification {
    private static int assertions;
    private static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    private static StageRuntime game(String id) {
        Json json=new Json();
        StageDefinition d=json.fromJson(StageDefinition.class,new FileHandle("assets/stages/"+id+".json"));
        PhysicsConfig c=json.fromJson(PhysicsConfig.class,new FileHandle("assets/config/physics.json"));
        return new StageRuntime(d,c);
    }
    private static StageRuntime route(String id,int fps) {
        StageRuntime s=game(id);
        MineRouteDriver driver=new MineRouteDriver(new JsonReader().parse(new FileHandle("assets/stages/mine_routes.json")).get(id));
        double accumulator=0;int ticks=0;
        while(ticks<15000&&!s.clear.frozen()) {
            accumulator+=1.0/fps;
            while(accumulator+1e-12>=1.0/120&&!s.clear.frozen()) {
                s.tick(driver.next(s),1f/120);accumulator-=1.0/120;ticks++;
                if(s.deaths>0)throw new AssertionError(id+" route "+driver.stepIndex+" died at "+s.player.x+","+s.player.y);
            }
        }
        check(s.clear.frozen(),id+" input route reaches clear gem (step="+driver.stepIndex+", pos="+s.player.x+","+s.player.y+", grounded="+s.player.grounded+")");
        check(s.checkpoint+1==s.definition.checkpoints.length,id+" route activates every checkpoint");
        System.out.printf(java.util.Locale.ROOT,"%s %d FPS route %.3fs, %d shots, %d CP%n",id,fps,s.clear.recordTime,s.shots,s.checkpoint+1);
        return s;
    }
    public static void main(String[] args) {
        StageRuntime s=game("mine_01");
        check(s.definition.mineMovers.length==2,"Mine 6 contains lift and moving ring");
        s.player.reset(5.8f,2);s.player.grounded=true;s.mineDevices.tick(.25f,s.player);
        check(s.player.y>2,"Riding lift carries player vertically");
        s.player.grounded=false;s.mineDevices.transferLaunchVelocity(s.player);
        check(s.player.vy>0,"Leaving lift transfers its velocity exactly once");
        s.respawn();check(Math.abs(s.definition.platforms[1].y-1.2f)<.001f,"Respawn restores lift start position");

        s=game("mine_04");s.player.reset(7.6f-0.12f,7.65f);s.player.grounded=false;s.player.previousX=7.1f;s.player.previousY=8.2f;s.player.ammo=0;
        check(s.mineDevices.tryRecharge(s.player,s.config)&&s.player.ammo==2,"Airborne recharge ring restores two shots");
        s.player.ammo=0;check(!s.mineDevices.tryRecharge(s.player,s.config),"Recharge ring is one use per attempt");
        s.respawn();check(!s.mineDevices.recharged(0),"Recharge ring resets at checkpoint respawn");
        s=game("mine_04");StageDefinition.MineSaw saw=s.definition.mineSaws[0];
        s.player.reset(saw.x,saw.y-s.config.height*.5f);
        check(s.mineDevices.lethal(s.player,s.config),"Saw uses circular body collision");

        s=game("mine_03");StageDefinition.MineCrusher crusher=s.definition.mineCrushers[0];
        s.mineDevices.tick(2.05f,s.player);check(crusher.extension>0,"Crusher extends through its timed states");
        s.player.reset(crusher.x+crusher.width*.5f,crusher.topY-crusher.extension+.1f);
        check(s.mineDevices.lethal(s.player,s.config),"Extended crusher contact is lethal");
        float start=s.definition.mineCrushers[0].extension;s.respawn();
        check(s.definition.mineCrushers[0].extension==0&&start>0,"Respawn resets crusher phase");

        for(int n=1;n<=5;n++) {
            String id=String.format(java.util.Locale.ROOT,"mine_%02d",n);
            StageRuntime base=route(id,30);
            for(int fps:new int[]{60,144}) {
                StageRuntime other=route(id,fps);
                check(base.clear.recordTime==other.clear.recordTime&&base.clear.x==other.clear.x,
                    id+" fixed-tick trajectory matches at "+fps+" FPS");
            }
        }
        System.out.println("Mine verification PASS: "+assertions+" assertions.");
    }
}
