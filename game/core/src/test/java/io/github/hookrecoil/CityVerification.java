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
            while(accumulator+1e-10>=1.0/120){stage.cityDevices.tick(1f/120,stage.player,stage.config.halfWidth);accumulator-=1.0/120;}
        }
        StageDefinition.MineMover mover=stage.definition.mineMovers[0];return new float[]{mover.x,mover.y};
    }

    private static boolean diagnoseRoute(String id) {
        StageRuntime stage=game(id);CityRouteDriver driver=new CityRouteDriver();int ticks=0;
        while(ticks<120000&&!stage.clear.frozen()&&stage.deaths==0) {
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
        if(ticks>=120000){
            System.out.printf("STUCK %s vx=%.2f vy=%.2f grounded=%s launched=%s transition=%s airTicks=%d settle=%d deathWait=%s dt=%.2f elapsed=%.2f ammo=%d attached=%d flying=%s hook=(%.2f,%.2f)%n",id,stage.player.vx,stage.player.vy,stage.player.grounded,driver.launched,driver.transitionEngaged,driver.airTicks,driver.settleTicks,stage.deathWaiting,stage.deathTimer,stage.elapsed,stage.player.ammo,stage.grapple.attached,stage.grapple.flying,stage.grapple.hookX,stage.grapple.hookY);
            System.out.println("  DRIVER WAIT: "+driver.blockedBy);
            System.out.println("  DRIVER SAFE windows gates="+driver.safeGateTicks+" crushers="+driver.safeCrusherTicks);
            for(int i=0;i<stage.definition.platforms.length;i++) {
                StageDefinition.Block p=stage.definition.platforms[i];
                if(stage.player.x+stage.config.halfWidth>p.x&&stage.player.x-stage.config.halfWidth<p.x+p.width
                    &&Math.abs(stage.player.y-p.surfaceY(stage.player.x))<.04f)System.out.println("  SUPPORT platform="+i+" driverTarget="+driver.platform);
            }
            for(StageDefinition.CityBarrier b:stage.definition.cityBarriers)System.out.printf("  GATE x=%.2f y=%.2f state=%s phase=%.2f%n",b.x,b.y,b.state,b.phase);
            for(StageDefinition.MineCrusher c:stage.definition.mineCrushers)System.out.printf("  PRESS x=%.2f y=%.2f state=%s ext=%.2f phase=%.2f%n",c.x,c.topY,c.state,c.extension,c.phase);
        }
        return stage.clear.frozen();
    }

    private static boolean simulateCity11FirstUpper() {
        StageRuntime stage=game("city_01");CityRouteDriver driver=new CityRouteDriver();
        StageDefinition.Block upper=stage.definition.platforms[5];int ticks=0;
        while(ticks<10000&&stage.deaths==0&&!stage.clear.frozen()) {
            stage.tick(driver.next(stage),1f/120);ticks++;
            boolean onUpper=stage.player.grounded&&Math.abs(stage.player.y-upper.surfaceY(stage.player.x))<.04f
                &&stage.player.x+stage.config.halfWidth>upper.x&&stage.player.x-stage.config.halfWidth<upper.x+upper.width;
            if(onUpper) {
                System.out.printf("CITY 11 FIRST UPPER reached by gameplay input at x=%.2f y=%.2f ticks=%d shots=%d deaths=%d%n",
                    stage.player.x,stage.player.y,ticks,stage.shots,stage.deaths);
                return true;
            }
        }
        System.out.printf("CITY 11 FIRST UPPER FAILED x=%.2f y=%.2f grounded=%s ticks=%d shots=%d deaths=%d%n",
            stage.player.x,stage.player.y,stage.player.grounded,ticks,stage.shots,stage.deaths);
        return false;
    }

    public static void main(String[] args) {
        StageCatalog catalog=catalog();
        check(simulateCity11FirstUpper(),"City 11 reaches the first upper platform from spawn using runtime jump/hook/recoil input");
        for(int i=1;i<=5;i++) {
            String id=String.format(java.util.Locale.ROOT,"city_%02d",i);
            StageCatalog.Entry meta=catalog.find(id);StageRuntime stage=game(id);
            check(meta!=null&&meta.number==10+i,"Fixed catalog ID and number: "+id);
            check(meta.artAvailable&&meta.available,"City stage is available for direct playtesting: "+id);
            check(stage.definition.checkpoints.length==(i==5?1:2),"Checkpoint count: "+id);
            boolean distinctCenters=true;
            for(int a=0;a<stage.definition.platforms.length;a++)for(int b=a+1;b<stage.definition.platforms.length;b++)
                if(Math.abs(stage.definition.platforms[a].x+stage.definition.platforms[a].width*.5f
                    -stage.definition.platforms[b].x-stage.definition.platforms[b].width*.5f)<.001f)distinctCenters=false;
            check(distinctCenters,"No platforms share the same vertical centerline: "+id);
            check(stage.definition.anchors.length>0&&stage.definition.ceilings.length>=stage.definition.anchors.length,
                "Grapple points have overhead structures: "+id);
            check(stage.definition.routeDesign!=null&&stage.definition.routeDesign.moduleCount==meta.targetModules
                &&!stage.definition.routeDesign.lengthMeasured,"V2 route targets are labeled as unmeasured: "+id);
            float expectedNarrowWidth=new float[]{2.0f,1.9f,1.8f,1.7f,1.6f}[i-1];
            check(Math.abs(stage.definition.platforms[(meta.targetModules-3)*5+2].width-expectedNarrowWidth)<.001f,
                "Authored narrow landing width tightens from city 11 to 15: "+id);
            java.util.Set<String> rowShapes=new java.util.HashSet<>();
            for(int module=0;module<stage.definition.routeDesign.moduleCount;module++) {
                StringBuilder signature=new StringBuilder();
                for(int j=0;j<5;j++) {
                    StageDefinition.Block p=stage.definition.platforms[module*5+j];
                    signature.append(Math.round(p.x+p.width*.5f)).append(',');
                }
                rowShapes.add(signature.toString());
            }
            check(rowShapes.size()>=4,"City floors use visibly distinct horizontal route shapes: "+id);
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
            StageDefinition.Block goal=stage.definition.platforms[stage.definition.platforms.length-1];
            int finishApproachIndex=(stage.definition.routeDesign.moduleCount-2)*5;
            float goalLeft=0,goalRight=stage.definition.width;
            float goalBottom=stage.definition.platforms[finishApproachIndex].surfaceY()-.35f;
            float goalTop=goal.surfaceY()+stage.config.height+2.0f;
            boolean clearApproach=true;
            for(StageDefinition.Hazard h:stage.definition.hazards)
                if(h.x+h.width>goalLeft&&h.x<goalRight&&h.y+h.height>goalBottom&&h.y<goalTop)clearApproach=false;
            for(StageDefinition.CityBarrier b:stage.definition.cityBarriers)
                if(b.x+b.width>goalLeft&&b.x<goalRight&&b.y+b.height>goalBottom&&b.y<goalTop)clearApproach=false;
            for(StageDefinition.MineCrusher c:stage.definition.mineCrushers)
                if(c.x+c.width>goalLeft&&c.x<goalRight&&c.topY>goalBottom&&c.topY-c.stroke<goalTop)clearApproach=false;
            for(StageDefinition.MineSaw s:stage.definition.mineSaws)
                if(Math.max(s.fromX,s.toX)+s.radius>goalLeft&&Math.min(s.fromX,s.toX)-s.radius<goalRight
                    &&s.fromY+s.radius>goalBottom&&s.fromY-s.radius<goalTop)clearApproach=false;
            check(clearApproach,"Last two route modules and clear gem approach are free of lethal traps: "+id);
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
        StageRuntime movingRide=game("city_01");
        StageDefinition.MineMover moving=movingRide.definition.mineMovers[0];
        StageDefinition.Block movingDeck=movingRide.definition.platforms[moving.platform];
        float riderStartX=movingDeck.x+movingDeck.width*.5f;
        movingRide.player.reset(riderStartX,movingDeck.surfaceY(riderStartX));movingRide.player.grounded=true;
        float deckStartX=movingDeck.x;
        for(int i=0;i<180;i++)movingRide.cityDevices.tick(1f/120,movingRide.player,movingRide.config.halfWidth);
        check(Math.abs(movingDeck.x-deckStartX)>.5f,"City moving platform traverses its authored horizontal route");
        check(Math.abs((movingRide.player.x-riderStartX)-(movingDeck.x-deckStartX))<.001f,
            "Player body rides the moving platform by exactly its deck displacement");
        StageRuntime barrier=game("city_03");StageDefinition.CityBarrier gate=barrier.definition.cityBarriers[0];
        barrier.cityDevices.tick(gate.offSeconds+.1f,barrier.player,barrier.config.halfWidth);check("warning".equals(gate.state)&&gate.collider.active,"Barrier warning is visible and collision-active");
        barrier.cityDevices.tick(.5f,barrier.player,barrier.config.halfWidth);check("on".equals(gate.state),"Barrier on phase follows the fixed cycle");
        check(barrier.collision.blocked(gate.x-1,gate.y+gate.height*.5f,gate.x+1,gate.y+gate.height*.5f),
            "On barrier blocks rope/projectile line queries");
        barrier.player.previousX=gate.x-.1f;barrier.player.x=gate.x+.1f;
        barrier.player.previousY=barrier.player.y=gate.y+1;
        check(barrier.cityDevices.lethal(barrier.player,barrier.config),"On barrier kills swept body contact");
        StageRuntime cityHazards=game("city_04");cityHazards.mineDevices.tickCityHazards(.4f);
        check(cityHazards.definition.mineCrushers.length>0,"City crusher phases run through the shared hazard clock");
        check(game("city_05").definition.mineSaws.length>0,"City V2 saw is registered as an independent moving hazard");
        StageRuntime restored=game("city_05");
        for(int i=0;i<240;i++){restored.cityDevices.tick(1f/120,restored.player,restored.config.halfWidth);restored.mineDevices.tickCityHazards(1f/120);}
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
            StageRuntime density=game(id);
            int expectedBarriers=i+4,expectedCrushers=(i+1)/2+2;
            check(density.definition.cityBarriers.length==expectedBarriers,
                "Electric barrier density increases across city stages: "+id);
            check(density.definition.mineCrushers.length==expectedCrushers,
                "Crusher density increases across city stages: "+id);
            float[] p30=moverPosition(30,id),p60=moverPosition(60,id),p144=moverPosition(144,id);
            check(Math.abs(p30[0]-p60[0])<.001f&&Math.abs(p60[0]-p144[0])<.001f
                &&Math.abs(p30[1]-p60[1])<.001f&&Math.abs(p60[1]-p144[1])<.001f,
                "V2 mover path is display-rate independent at fixed 120 Hz: "+id);
        }

        for(int i=4;i<=5;i++) {
            StageRuntime crumbleStage=game(String.format(java.util.Locale.ROOT,"city_%02d",i));
            int crumbleIndex=-1;
            for(int j=0;j<crumbleStage.definition.platforms.length;j++)if(crumbleStage.definition.platforms[j].crumble)crumbleIndex=j;
            check(crumbleIndex>=0,"City stage "+(i+10)+" has a breakable route platform");
            StageDefinition.Block deck=crumbleStage.definition.platforms[crumbleIndex];
            float deckX=deck.x+deck.width*.5f;
            crumbleStage.player.reset(deckX,deck.surfaceY(deckX));crumbleStage.player.grounded=true;
            crumbleStage.devices.landed(crumbleStage.player,crumbleStage.config);
            check("intact".equals(crumbleStage.devices.frame(crumbleIndex)),"Breakable platform starts intact after landing");
            crumbleStage.devices.tick(.46f);
            check("warning".equals(crumbleStage.devices.frame(crumbleIndex))&&deck.active,"Breakable platform warns before removing collision");
            crumbleStage.devices.tick(.55f);
            check("collapse".equals(crumbleStage.devices.frame(crumbleIndex))&&!deck.active,"Breakable platform removes collision after the warning");
            crumbleStage.devices.tick(.25f);
            check("gone".equals(crumbleStage.devices.frame(crumbleIndex)),"Breakable platform reaches gone state");
            crumbleStage.respawn();
            check(deck.active&&crumbleStage.devices.frame(crumbleIndex).equals("intact"),"Respawn restores breakable platform and timer");
        }
        for(int i=1;i<=5;i++) {
            boolean reachedClear=diagnoseRoute(String.format(java.util.Locale.ROOT,"city_%02d",i));
            check(reachedClear,"city_"+String.format(java.util.Locale.ROOT,"%02d",i)+" route reaches clear gem with added devices");
            System.out.println("ROUTE CLEAR city_0"+i+": clear gem reached");
        }
        StageRuntime held=game("city_01");held.resolveGoal(true);
        for(int i=0;i<60;i++)held.tick(new InputFrame(),1f/120);
        check(held.deathWaiting,"Death state remains input-gated during city runtime");
        System.out.println("City V2 verification: "+assertions+" assertions passed, including the scripted routes; this is not a human playtest.");
    }
}
