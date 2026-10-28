/*
 * 파일: CityVerification.java
 * 역할: 지하도시 11~15 초기 데이터와 장치 핵심 규칙을 자동 검증한다.
 * 주요 책임: ID·체크포인트·천장 링·복합 콜라이더·기류·장벽 위상·고정 틱 이동 일관성을 검사한다.
 * 협력 대상: StageCatalog, StageRuntime, CityDeviceRuntime, CollisionWorld.
 * 관련 사양: docs/assets/city_chapter/STAGES_11_15.md, docs/assets/city_chapter/DEVICES_AND_MOTION.md.
 */
package io.github.hookrecoil;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import io.github.hookrecoil.input.InputFrame;
import io.github.hookrecoil.physics.PhysicsConfig;
import io.github.hookrecoil.stage.*;

public final class CityVerification {
    private static int assertions;
    private static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    private static Json json(){return new Json();}
    private static StageCatalog catalog(){return json().fromJson(StageCatalog.class,new FileHandle("assets/cave_chapter/stages.json"));}
    private static StageRuntime game(String id){return new StageRuntime(json().fromJson(StageDefinition.class,new FileHandle("assets/stages/"+id+".json")),json().fromJson(PhysicsConfig.class,new FileHandle("assets/config/physics.json")));}

    private static float moverX(int fps) {
        StageRuntime stage=game("city_02");double accumulator=0;int frames=fps*2;
        for(int frame=0;frame<frames;frame++) {
            accumulator+=1.0/fps;
            while(accumulator+1e-10>=1.0/120){stage.cityDevices.tick(1f/120,stage.player);accumulator-=1.0/120;}
        }
        return stage.definition.platforms[5].x;
    }

    public static void main(String[] args) {
        StageCatalog catalog=catalog();
        for(int i=1;i<=5;i++) {
            String id=String.format(java.util.Locale.ROOT,"city_%02d",i);
            StageCatalog.Entry meta=catalog.find(id);StageRuntime stage=game(id);
            check(meta!=null&&meta.number==10+i,"Fixed catalog ID and number: "+id);
            check(meta.artAvailable&&!meta.available,"Art is ready while unverified maps remain unavailable: "+id);
            check(stage.definition.checkpoints.length==(i==5?1:2),"Checkpoint count: "+id);
            check(stage.definition.anchors.length>0&&stage.definition.ceilings.length>=stage.definition.anchors.length,
                "Grapple points have overhead structures: "+id);
            check(stage.collision.supports(stage.definition.clearGem.x,stage.definition.platforms[stage.definition.platforms.length-1].surfaceY()),
                "Clear gem platform has real support: "+id);
        }
        StageRuntime wind=game("city_02");wind.player.reset(3.5f,9);
        wind.cityDevices.sampleWind(wind.player,wind.config);check(wind.cityDevices.windY()==8,"Updraft is a fixed authored acceleration");
        StageRuntime complex=game("city_04");
        check(complex.definition.cityStructures.length==2&&complex.definition.cityStructures[0].collisionParts.length==2,
            "L block uses separate union colliders");
        check(complex.definition.mineMovers[1].waypoints.length==4,"Right-angle mover uses waypoint path data");
        StageRuntime barrier=game("city_03");StageDefinition.CityBarrier gate=barrier.definition.cityBarriers[0];
        barrier.cityDevices.tick(1.5f,barrier.player);check("warning".equals(gate.state)&&gate.collider.active,"Barrier warning is visible and collision-active");
        barrier.cityDevices.tick(.5f,barrier.player);check("on".equals(gate.state),"Barrier on phase follows the fixed cycle");
        check(barrier.collision.blocked(gate.x-1,gate.y+gate.height*.5f,gate.x+1,gate.y+gate.height*.5f),
            "On barrier blocks rope/projectile line queries");
        barrier.player.previousX=gate.x-.1f;barrier.player.x=gate.x+.1f;
        barrier.player.previousY=barrier.player.y=gate.y+1;
        check(barrier.cityDevices.lethal(barrier.player,barrier.config),"On barrier kills swept body contact");
        float x30=moverX(30),x60=moverX(60),x144=moverX(144);
        check(Math.abs(x30-x60)<.001f&&Math.abs(x60-x144)<.001f,"Mover path is display-rate independent at fixed 120 Hz");
        StageRuntime held=game("city_01");held.resolveGoal(true);
        for(int i=0;i<60;i++)held.tick(new InputFrame(),1f/120);
        check(held.deathWaiting,"Death state remains input-gated during city runtime");
        System.out.println("City verification PASS: "+assertions+" assertions; city maps remain unavailable pending route playtests.");
    }
}
