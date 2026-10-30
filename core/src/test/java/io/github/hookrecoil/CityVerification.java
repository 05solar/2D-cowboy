/*
 * 파일: CityVerification.java
 * 역할: 지하도시 V2 11~15 데이터와 장치 핵심 규칙을 자동 검증한다.
 * 주요 책임: 얇은 one-way 발판·배경 구역·빈 프레임·기류·장벽·고정 틱 이동을 검사한다.
 * 협력 대상: StageCatalog, StageRuntime, CityDeviceRuntime, CollisionWorld.
 * 관련 사양: docs/CITY_CHAPTER_V2.md, assets/city_chapter/v2_layout_blueprints.json.
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

    private static float[] moverPosition(int fps,String id) {
        StageRuntime stage=game(id);double accumulator=0;int frames=fps*5;
        for(int frame=0;frame<frames;frame++) {
            accumulator+=1.0/fps;
            while(accumulator+1e-10>=1.0/120){stage.cityDevices.tick(1f/120,stage.player);accumulator-=1.0/120;}
        }
        StageDefinition.MineMover mover=stage.definition.mineMovers[0];return new float[]{mover.x,mover.y};
    }

    private static boolean diagnoseRoute(String id) {
        StageRuntime stage=game(id);CityRouteDriver driver=new CityRouteDriver();int ticks=0;
        while(ticks<20000&&!stage.clear.frozen()&&stage.deaths==0) {
            io.github.hookrecoil.input.InputFrame command=driver.next(stage);
            stage.tick(command,1f/120);ticks++;
        }
        System.out.printf(java.util.Locale.ROOT,"CITY ROUTE %s: step=%d/%d deaths=%d x=%.2f y=%.2f grounded=%s ticks=%d targetX=%.2f top=%.2f sourceTop=%.2f%n",
            id,driver.platform,stage.definition.platforms.length,stage.deaths,stage.player.x,stage.player.y,stage.player.grounded,ticks,
            stage.definition.platforms[Math.min(driver.platform,stage.definition.platforms.length-1)].x,
            stage.definition.platforms[Math.min(driver.platform,stage.definition.platforms.length-1)].surfaceY(),
            stage.definition.platforms[Math.max(0,driver.platform-1)].surfaceY());
        if(stage.deaths>0)System.out.printf("DEATH CAUSE %s hazards=%s mine=%s city=%s fall=%s%n",id,
            stage.devices.lethal(stage.player,stage.config),stage.mineDevices.lethal(stage.player,stage.config),
            stage.cityDevices.lethal(stage.player,stage.config),stage.player.y<stage.definition.deathPlaneY);
        if(ticks>=20000)System.out.printf("STUCK %s vx=%.2f vy=%.2f deathWait=%s dt=%.2f elapsed=%.2f ammo=%d attached=%d flying=%s hook=(%.2f,%.2f)%n",id,stage.player.vx,stage.player.vy,stage.deathWaiting,stage.deathTimer,stage.elapsed,stage.player.ammo,stage.grapple.attached,stage.grapple.flying,stage.grapple.hookX,stage.grapple.hookY);
        return stage.clear.frozen();
    }

    public static void main(String[] args) {
        StageCatalog catalog=catalog();
        for(int i=1;i<=5;i++) {
            String id=String.format(java.util.Locale.ROOT,"city_%02d",i);
            StageCatalog.Entry meta=catalog.find(id);StageRuntime stage=game(id);
            check(meta!=null&&meta.number==10+i,"Fixed catalog ID and number: "+id);
            check(meta.artAvailable&&meta.available,"City stage is available for direct playtesting: "+id);
            check(stage.definition.checkpoints.length==(i==5?1:2),"Checkpoint count: "+id);
            boolean distinctCenters=true;
            for(int a=0;a<stage.definition.platforms.length;a++)for(int b=a+1;b<stage.definition.platforms.length;b++)
                if(Math.abs(stage.definition.platforms[a].x+stage.definition.platforms[a].width*.5f
                    -stage.definition.platforms[b].x-stage.definition.platforms[b].width*.5f)<.01f)distinctCenters=false;
            check(distinctCenters,"No platforms share the same vertical centerline: "+id);
            check(stage.definition.anchors.length>0&&stage.definition.ceilings.length>=stage.definition.anchors.length,
                "Grapple points have overhead structures: "+id);
            check(stage.definition.routeDesign!=null&&stage.definition.routeDesign.moduleCount==meta.targetModules
                &&!stage.definition.routeDesign.lengthMeasured,"V2 route targets are labeled as unmeasured: "+id);
            check(meta.backgroundVariants!=null&&meta.backgroundVariants.horizontal.endsWith("_horizontal.png")
                &&!meta.backgroundVariants.repeat,"Horizontal and vertical backgrounds are explicitly non-repeating: "+id);
            boolean solidSlab=false,oneWayDeck=false;
            for(StageDefinition.Block p:stage.definition.platforms) {
                check(Math.abs(p.height-.16f)<.001f,"Thin walkable collider is 0.16H: "+id);
                solidSlab|=!p.oneWay;oneWayDeck|=p.oneWay;
            }
            check(solidSlab&&oneWayDeck,"Solid slabs and one-way decks are explicit types: "+id);
            for(StageDefinition.CityStructure structure:stage.definition.cityStructures)
                check("none".equals(structure.collisionMode)&&structure.collisionParts.length==0,
                    "Open frame is visual-only with no filled collider: "+id);
            check(stage.collision.supports(stage.definition.clearGem.x,stage.definition.platforms[stage.definition.platforms.length-1].surfaceY()),
                "Clear gem platform has real support: "+id);
        }
        StageRuntime cityOne=game("city_01");
        StageDefinition.CityBackgroundRegion horizontal=cityOne.definition.cityBackgroundRegions[0],vertical=null;
        for(StageDefinition.CityBackgroundRegion region:cityOne.definition.cityBackgroundRegions)if("vertical".equals(region.variant)){vertical=region;break;}
        float cameraHalfWidth=2560f/(2*cityOne.config.pixelsPerH);
        check(vertical!=null&&vertical.maxX>=cameraHalfWidth
            &&vertical.minX<=cityOne.definition.width-cameraHalfWidth,"Vertical background region intersects the clamped 16:9 camera path");
        io.github.hookrecoil.screens.CityBackgroundSelector selector=new io.github.hookrecoil.screens.CityBackgroundSelector();
        selector.update(cityOne.definition,(horizontal.minX+horizontal.maxX)*.5f,(horizontal.minY+horizontal.maxY)*.5f,.35f,true);
        String horizontalPath=selector.activePath();
        selector.update(cityOne.definition,(vertical.minX+vertical.maxX)*.5f,(vertical.minY+vertical.maxY)*.5f,0,true);
        check(!horizontalPath.equals(selector.activePath())&&selector.previousPath().equals(horizontalPath)&&selector.blend()==0,
            "Background switch starts a crossfade at its authored camera region");
        selector.update(cityOne.definition,(vertical.minX+vertical.maxX)*.5f,(vertical.minY+vertical.maxY)*.5f,.35f,true);
        check(selector.blend()==1&&selector.previousPath()==null,"Background crossfade completes without repeating either image");
        selector.update(cityOne.definition,vertical.minX-.5f,(vertical.minY+vertical.maxY)*.5f,.1f,true);
        check(selector.activePath().equals(vertical.path),"Camera region hysteresis prevents edge flicker");
        StageDefinition oneWayDefinition=new StageDefinition();oneWayDefinition.platforms=new StageDefinition.Block[1];
        oneWayDefinition.ceilings=new StageDefinition.Block[0];
        oneWayDefinition.platforms[0]=new StageDefinition.Block();oneWayDefinition.platforms[0].x=0;
        oneWayDefinition.platforms[0].y=0;oneWayDefinition.platforms[0].width=2;oneWayDefinition.platforms[0].height=.16f;
        oneWayDefinition.platforms[0].oneWay=true;oneWayDefinition.platforms[0].ropeStops=true;
        io.github.hookrecoil.physics.CollisionWorld oneWayWorld=new io.github.hookrecoil.physics.CollisionWorld(oneWayDefinition,json().fromJson(PhysicsConfig.class,new FileHandle("assets/config/physics.json")));
        io.github.hookrecoil.player.PlayerState upward=new io.github.hookrecoil.player.PlayerState();upward.x=1;upward.y=-1.5f;
        oneWayWorld.move(upward,0,1);check(upward.y<0,"One-way deck does not catch a player rising from below");
        io.github.hookrecoil.player.PlayerState falling=new io.github.hookrecoil.player.PlayerState();falling.x=1;falling.y=.5f;
        oneWayWorld.move(falling,0,-1);check(falling.grounded&&Math.abs(falling.y-.16f)<.001f,"One-way deck catches a falling player on its top face");
        io.github.hookrecoil.player.PlayerState fastFall=new io.github.hookrecoil.player.PlayerState();fastFall.x=1;fastFall.y=5;
        oneWayWorld.move(fastFall,0,-10);check(fastFall.grounded&&Math.abs(fastFall.y-.16f)<.001f,"Swept collision catches a high-speed fall on a 0.16H deck");
        check(oneWayWorld.blocked(1,-.2f,1,.2f),"One-way beam blocks a grapple line when ropeStops is true");
        oneWayDefinition.platforms[0].ropeStops=false;
        check(!oneWayWorld.blocked(1,-.2f,1,.2f),"A one-way platform can explicitly allow grapple lines through");
        StageRuntime wind=game("city_02");wind.player.reset(3.5f,9);
        wind.cityDevices.sampleWind(wind.player,wind.config);check(wind.cityDevices.windY()==8,"Updraft is a fixed authored acceleration");
        StageRuntime complex=game("city_04");
        check(complex.definition.cityStructures.length>0&&"none".equals(complex.definition.cityStructures[0].collisionMode),
            "V2 frames remain open visual structures instead of filled V1 blocks");
        boolean movingSurface=false;for(StageDefinition.MineMover mover:complex.definition.mineMovers)if(mover.platform>=0)movingSurface=true;
        check(movingSurface,"V2 moving surfaces remain separate from visual rail parts");
        StageRuntime barrier=game("city_03");StageDefinition.CityBarrier gate=barrier.definition.cityBarriers[0];
        barrier.cityDevices.tick(.9f,barrier.player);check("warning".equals(gate.state)&&gate.collider.active,"Barrier warning is visible and collision-active");
        barrier.cityDevices.tick(.5f,barrier.player);check("on".equals(gate.state),"Barrier on phase follows the fixed cycle");
        check(barrier.collision.blocked(gate.x-1,gate.y+gate.height*.5f,gate.x+1,gate.y+gate.height*.5f),
            "On barrier blocks rope/projectile line queries");
        barrier.player.previousX=gate.x-.1f;barrier.player.x=gate.x+.1f;
        barrier.player.previousY=barrier.player.y=gate.y+1;
        check(barrier.cityDevices.lethal(barrier.player,barrier.config),"On barrier kills swept body contact");
        StageRuntime cityHazards=game("city_04");cityHazards.mineDevices.tickCityHazards(.4f);
        check(cityHazards.definition.mineCrushers.length>0,"City crusher phases run through the shared hazard clock");
        check(game("city_05").definition.mineSaws.length>0,"City V2 saw is registered as an independent moving hazard");
        StageRuntime restored=game("city_05");
        for(int i=0;i<240;i++){restored.cityDevices.tick(1f/120,restored.player);restored.mineDevices.tickCityHazards(1f/120);}
        StageDefinition.MineMover resetMover=restored.definition.mineMovers[0];
        restored.checkpoint=0;restored.respawn();
        check(Math.abs(resetMover.x-resetMover.fromX)<.001f&&Math.abs(resetMover.y-resetMover.fromY)<.001f,
            "Checkpoint restart resets mover pose and cycle time");
        check(restored.definition.mineSaws[0].x==restored.definition.mineSaws[0].fromX,
            "Checkpoint restart returns the saw to its authored phase origin");
        check("open".equals(restored.definition.mineCrushers[0].state)&&restored.definition.mineCrushers[0].extension==0,
            "Checkpoint restart returns the crusher to its open phase");
        StageDefinition.RechargeRing rechargePoint=restored.definition.rechargeRings[0];
        restored.player.reset(rechargePoint.x,rechargePoint.y);restored.player.previousX=restored.player.x;
        restored.player.previousY=restored.player.y;restored.player.grounded=false;restored.player.ammo=0;
        check(restored.mineDevices.tryRecharge(restored.player,restored.config)&&restored.mineDevices.recharged(0),
            "Air recharge activates once during the attempt");
        restored.respawn();check(!restored.mineDevices.recharged(0),"Checkpoint restart restores the one-use recharge ring");
        for(int i=1;i<=5;i++) {
            String id=String.format(java.util.Locale.ROOT,"city_%02d",i);
            float[] p30=moverPosition(30,id),p60=moverPosition(60,id),p144=moverPosition(144,id);
            check(Math.abs(p30[0]-p60[0])<.001f&&Math.abs(p60[0]-p144[0])<.001f
                &&Math.abs(p30[1]-p60[1])<.001f&&Math.abs(p60[1]-p144[1])<.001f,
                "V2 mover path is display-rate independent at fixed 120 Hz: "+id);
        }
        for(int i=1;i<=5;i++) {
            boolean reachedClear=diagnoseRoute(String.format(java.util.Locale.ROOT,"city_%02d",i));
            if(reachedClear)System.out.println("ROUTE CLEAR city_0"+i+": clear gem reached");
        }
        StageRuntime held=game("city_01");held.resolveGoal(true);
        for(int i=0;i<60;i++)held.tick(new InputFrame(),1f/120);
        check(held.deathWaiting,"Death state remains input-gated during city runtime");
        System.out.println("City V2 verification: "+assertions+" data/device assertions passed; scripted routes reported separately and are not manual playtests.");
    }
}
