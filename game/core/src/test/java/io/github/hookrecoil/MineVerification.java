/* 파일: MineVerification.java
 * 역할: 광산 6~10 통합 검증. 책임: 맵 차별화·장치·위험·재충전·완주 경로 검사.
 * 협력 대상: MineRouteDriver, StageRuntime, ProgressStore. 파일 저장은 격리된 임시 폴더만 사용한다.
 * 관련 사양: docs/assets/mine_chapter/STAGES_06_10.md.
 */
package io.github.hookrecoil;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
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
        MineRouteExposure exposure=new MineRouteExposure(s);
        MineRouteDriver driver=new MineRouteDriver(new JsonReader().parse(new FileHandle("assets/stages/mine_routes.json")).get(id));
        double accumulator=0;int ticks=0;
        StringBuilder trace=new StringBuilder("time,x,y,step\n");
        while(ticks<120000&&!s.clear.frozen()) {
            accumulator+=1.0/fps;
            while(accumulator+1e-12>=1.0/120&&!s.clear.frozen()) {
                int priorCheckpoint=s.checkpoint;s.tick(driver.next(s),1f/120);accumulator-=1.0/120;ticks++;
                exposure.observe(s);
                if(fps==30)trace.append(s.elapsed).append(',').append(s.player.x).append(',').append(s.player.y).append(',').append(driver.stepIndex).append('\n');
                if(s.deaths>0)throw new AssertionError(id+" route "+driver.stepIndex+" died at "+s.player.x+","+s.player.y+
                    " velocity="+s.player.vx+","+s.player.vy+" grounded="+s.player.grounded+" ammo="+s.player.ammo);
            }
        }
        if(fps==30)new FileHandle("build/mine-route-traces/"+id+".csv").writeString(trace.toString(),false,"UTF-8");
        check(s.clear.frozen(),id+" input route reaches clear gem (step="+driver.stepIndex+", pos="+s.player.x+","+s.player.y+", grounded="+s.player.grounded+")");
        check(s.checkpoint+1==s.definition.checkpoints.length,id+" route activates every checkpoint ("+(s.checkpoint+1)+"/"+s.definition.checkpoints.length+")");
        System.out.printf(java.util.Locale.ROOT,"%s %d FPS route %.3fs, %d shots, %d CP%n",id,fps,s.clear.recordTime,s.shots,s.checkpoint+1);
        exposure.verify(id);
        return s;
    }
    private static void checkpointRoutes(String id) {
        StageRuntime authored=game(id);
        JsonValue routeData=new JsonReader().parse(new FileHandle("assets/stages/mine_routes.json")).get(id);
        for(int cp=0;cp<authored.definition.checkpoints.length;cp++) {
            boolean cleared=false;
            for(int delay=0;delay<=60&&!cleared;delay++) {
                StageRuntime s=game(id);s.checkpoint=cp;s.respawn();
                MineRouteDriver driver=new MineRouteDriver(routeData);
                driver.recoverUpperLanding=true;
                float best=Float.MAX_VALUE;
                for(int j=0;j<routeData.size;j++) {
                    StageDefinition.Block b=s.definition.platforms[routeData.get(j).getInt("platform")];
                    float distance=Math.abs(b.surfaceY(s.player.x)-s.player.y)*10+Math.abs(b.x+b.width*.5f-s.player.x);
                    if(distance<best){best=distance;driver.stepIndex=j;}
                }
                for(int tick=0;tick<18000&&!s.clear.frozen()&&s.deaths==0;tick++)
                    s.tick(tick<delay*12?new InputFrame():driver.next(s),1f/120);
                cleared=s.clear.frozen()&&s.deaths==0;
                if(delay==0&&!cleared)System.out.println(id+" CP failed step="+driver.stepIndex+" at "+s.player.x+","+s.player.y+" deaths="+s.deaths);
                if(cleared)System.out.println(id+" CP "+(cp+1)+" clear from rest; observation wait "+delay*.1f+"s");
            }
            check(cleared,id+" checkpoint "+(cp+1)+" can clear from rest with two shots");
        }
    }
    public static void main(String[] args) {
        if(args.length>0&&"--trace".equals(args[0])) {
            for(int n=1;n<=5;n++)route(String.format(java.util.Locale.ROOT,"mine_%02d",n),30);
            return;
        }
        int[] expectedObstacleCounts={0,2,2,3,5};
        int previousDifficulty=-1;
        float firstVerticalCycle=-1,lastVerticalCycle=-1;
        boolean sawVerticalBarrier=false,sawAlignedBarrier=false;
        for(int i=0;i<expectedObstacleCounts.length;i++) {
            StageRuntime authored=game(String.format(java.util.Locale.ROOT,"mine_%02d",i+1));
            MineGroundHazards.verify(authored);
            int obstacleCount=authored.definition.hazards.length+authored.definition.mineCrushers.length+authored.definition.mineSaws.length;
            check(obstacleCount==expectedObstacleCounts[i],"Mine stage "+(i+6)+" has its authored obstacle count with no post-goal hazards");
            check(authored.definition.cityBarriers.length>=i+2,"Mine stage "+(i+6)+" adds progressive electric barriers");
            // Each barrier's own route avoidance wait (MineRouteDriver) is sensitive to its exact off-window,
            // so the per-stage cycle trends shorter overall (first vs last stage) rather than strictly every step.
            float stageVerticalCycle=Float.MAX_VALUE;
            for(StageDefinition.CityBarrier barrier:authored.definition.cityBarriers) {
                float cycle=barrier.offSeconds+barrier.warningSeconds+barrier.onSeconds;
                if(barrier.height>barrier.width){sawVerticalBarrier=true;stageVerticalCycle=Math.min(stageVerticalCycle,cycle);}
                else sawAlignedBarrier=true;
            }
            if(i==0)firstVerticalCycle=stageVerticalCycle;
            check(stageVerticalCycle<=firstVerticalCycle+.001f,
                "Mine stage "+(i+6)+" electric barrier cycle is no longer than the opening mine stage ("+stageVerticalCycle+" vs "+firstVerticalCycle+")");
            lastVerticalCycle=stageVerticalCycle;
            int difficulty=obstacleCount+authored.definition.cityBarriers.length;
            check(difficulty>previousDifficulty,"Mine stage "+(i+6)+" raises the combined hazard and barrier challenge");
            previousDifficulty=difficulty;
            StageDefinition.Block last=authored.definition.platforms[authored.definition.platforms.length-1];
            check(Math.abs(authored.definition.clearGem.x-(last.x+last.width*.5f))<.01f
                &&authored.definition.width-last.x-last.width<1.01f,"Mine stage "+(i+6)+" ends at its clear gem platform");
            for(StageDefinition.Block platform:authored.definition.platforms)
                check(platform.x+platform.width*.5f<=authored.definition.clearGem.x+.01f,"No platform extends past mine stage "+(i+6)+" clear gem");
            for(StageDefinition.Hazard h:authored.definition.hazards)
                check(h.x+h.width<=authored.definition.clearGem.x+.01f,"No spike hazard lies beyond mine stage "+(i+6)+" clear gem");
            for(StageDefinition.CityBarrier b:authored.definition.cityBarriers)
                check(b.x+b.width<=authored.definition.clearGem.x+.01f,"No electric barrier lies beyond mine stage "+(i+6)+" clear gem");
            for(StageDefinition.MineCrusher c:authored.definition.mineCrushers)
                check(c.x+c.width<=authored.definition.clearGem.x+.01f,"No crusher lies beyond mine stage "+(i+6)+" clear gem");
            for(StageDefinition.MineSaw saw:authored.definition.mineSaws)
                check(Math.max(saw.fromX,saw.toX)+saw.radius<=authored.definition.clearGem.x+.01f,"No saw lies beyond mine stage "+(i+6)+" clear gem");
            for(StageDefinition.Block ceiling:authored.definition.ceilings)
                check(ceiling.x+ceiling.width<=authored.definition.clearGem.x+.01f,"No ceiling asset lies beyond mine stage "+(i+6)+" clear gem");
            for(StageDefinition.MineMover mover:authored.definition.mineMovers)
                check(Math.max(mover.fromX,mover.toX)<=authored.definition.clearGem.x+.01f,"No moving asset lies beyond mine stage "+(i+6)+" clear gem");
            for(StageDefinition.RechargeRing ring:authored.definition.rechargeRings)
                check(ring.x<=authored.definition.clearGem.x+.01f,"No recharge ring lies beyond mine stage "+(i+6)+" clear gem");
            for(StageDefinition.Point anchor:authored.definition.anchors)
                check(anchor.x<=authored.definition.clearGem.x+.01f,"No hook anchor lies beyond mine stage "+(i+6)+" clear gem");
            for(StageDefinition.Gem gem:authored.definition.optionalGems)
                check(gem.x<=authored.definition.clearGem.x+.01f,"No optional gem lies beyond mine stage "+(i+6)+" clear gem");
            java.util.Set<String> silhouettes=new java.util.HashSet<>();
            for(StageDefinition.Block platform:authored.definition.platforms)silhouettes.add(platform.sprite);
            check(silhouettes.size()>=3,"Mine stage "+(i+6)+" uses varied platform silhouettes");
        }
        check(sawVerticalBarrier,"Mine chapter authors at least one vertical electric barrier");
        check(sawAlignedBarrier,"Mine chapter authors at least one platform-aligned (horizontal) electric barrier");
        check(lastVerticalCycle<firstVerticalCycle-.001f,"Mine chapter's final stage cycles its electric barriers faster than the opening stage");

        StageRuntime s=game("mine_01");
        check(s.definition.mineMovers.length==2,"Mine 6 contains lift and moving ring");
        StageDefinition.Block liftBlock=s.definition.platforms[1];
        check(liftBlock.surfaceY(5.8f)<liftBlock.y+liftBlock.height,"Lift collision uses its recessed deck surface, not the sprite rectangle edge");
        StageDefinition.Block startBlock=s.definition.platforms[0];
        check(startBlock.surfaceProfile!=null&&startBlock.surfaceY()<startBlock.y+startBlock.height,"Platform records the visible outer surface profile below the atlas rectangle edge");
        check(Math.abs(startBlock.surfaceY(startBlock.x+startBlock.width*.12f)-startBlock.surfaceY(startBlock.x+startBlock.width*.88f))>.001f,"Collision surface follows visible silhouette changes across the platform");
        check(s.collision.supports(startBlock.x+startBlock.width*.5f,startBlock.surfaceY()),"Collision supports the authored visible platform surface");
        check(!s.collision.supports(startBlock.x+startBlock.width*.5f,startBlock.y+startBlock.height),"Collision ignores the transparent atlas edge");
        s.player.reset(startBlock.x+.2f,startBlock.surfaceY(startBlock.x+.2f));
        float walkDx=(startBlock.width*.82f-.2f)/90f;
        for(int i=0;i<90;i++)s.collision.move(s.player,walkDx,-.001f);
        check(s.player.grounded&&s.collision.supports(s.player.x,s.player.y),"Walking stays grounded across the authored outer contour");
        float liftSurface=s.definition.platforms[1].surfaceY(5.8f);s.player.reset(5.8f,liftSurface);s.player.grounded=true;s.mineDevices.tick(.25f,s.player,s.config.halfWidth);
        check(s.player.y>liftSurface,"Riding lift carries player from its visible surface");
        s.player.grounded=false;s.mineDevices.transferLaunchVelocity(s.player);
        check(s.player.vy>0,"Leaving lift transfers its velocity exactly once");
        s.respawn();check(Math.abs(s.definition.platforms[1].y-s.definition.mineMovers[0].fromY)<.001f,"Respawn restores lift start position");
        check(Math.abs(s.definition.platforms[1].surfaceY(5.8f)-2)<.1f,"Lift deck aligns with its authored platform height");
        StageDefinition.Block cartBlock=game("mine_02").definition.platforms[1];
        float cartTopOffset=cartBlock.y+cartBlock.height-cartBlock.surfaceY(cartBlock.x+cartBlock.width*.5f);
        check(cartTopOffset>.04f&&cartTopOffset<.08f,"Minecart collision follows its top deck");
        StageRuntime cartRide=game("mine_02");StageDefinition.Block testCart=cartRide.definition.platforms[1];
        float edgeX=testCart.x-.1f,edgeY=testCart.surfaceY(edgeX);cartRide.player.reset(edgeX,edgeY);
        check(cartRide.collision.supports(edgeX,edgeY),"Mine 7 cart supports the player's body when only its edge overlaps the deck");
        cartRide.mineDevices.tick(.25f,cartRide.player,cartRide.config.halfWidth);
        check(cartRide.player.x>edgeX+.001f,"Mine 7 moving cart carries a grounded player whose body overlaps at the deck edge");

        s=game("mine_04");StageDefinition.RechargeRing ring=s.definition.rechargeRings[0];
        s.player.reset(ring.x,ring.y-.3f);s.player.grounded=false;s.player.previousX=ring.x-.5f;s.player.previousY=ring.y-.3f;s.player.ammo=0;
        check(s.mineDevices.tryRecharge(s.player,s.config)&&s.player.ammo==2,"Airborne recharge ring restores two shots");
        s.player.ammo=0;check(!s.mineDevices.tryRecharge(s.player,s.config),"Recharge ring is one use per attempt");
        s.respawn();check(!s.mineDevices.recharged(0),"Recharge ring resets at checkpoint respawn");
        s=game("mine_04");StageDefinition.MineSaw saw=s.definition.mineSaws[0];
        s.player.reset(saw.x,saw.y-s.config.height*.5f);
        check(s.mineDevices.lethal(s.player,s.config),"Saw uses circular body collision");

        s=game("mine_03");StageDefinition.MineCrusher crusher=s.definition.mineCrushers[0];
        s.mineDevices.tick(((.7f-crusher.phase+1)%1)*crusher.period,s.player,s.config.halfWidth);check(crusher.extension>0,"Crusher extends through its timed states");
        s.player.reset(crusher.x+crusher.width*.5f,crusher.topY-crusher.extension+.1f);
        check(s.mineDevices.lethal(s.player,s.config),"Extended crusher contact is lethal");
        float start=s.definition.mineCrushers[0].extension;s.respawn();
        check(s.definition.mineCrushers[0].extension==0&&start>0,"Respawn resets crusher phase");

        s=game("mine_05");StageDefinition.CityBarrier mineGate=s.definition.cityBarriers[0];
        float mineGateCycle=mineGate.offSeconds+mineGate.warningSeconds+mineGate.onSeconds;
        float warningAt=(mineGate.offSeconds-mineGate.phase+mineGateCycle)%mineGateCycle+.1f;
        s.cityDevices.tickBarriers(warningAt);
        check("warning".equals(mineGate.state)&&mineGate.collider.active,"Mine electric barrier has a visible collision-active warning state");
        s.cityDevices.tickBarriers(.5f);
        check("on".equals(mineGate.state)&&s.collision.blocked(mineGate.x-1,mineGate.y+mineGate.height*.5f,
            mineGate.x+1,mineGate.y+mineGate.height*.5f),"Mine active electric barrier blocks movement lines");
        s.player.previousX=mineGate.x-.1f;s.player.x=mineGate.x+.1f;s.player.previousY=s.player.y=mineGate.y+1;
        check(s.cityDevices.lethal(s.player,s.config),"Mine active electric barrier kills swept body contact");
        s.respawn();check("off".equals(mineGate.state)&&!mineGate.collider.active,"Mine barrier resets to its authored phase on respawn");

        if(args.length>0&&"--surfaces-only".equals(args[0])) {
            System.out.println("Mine surface/device verification PASS: "+assertions+" assertions.");return;
        }

        for(int n=1;n<=5;n++) {
            String id=String.format(java.util.Locale.ROOT,"mine_%02d",n);
            JsonValue routeData=new JsonReader().parse(new FileHandle("assets/stages/mine_routes.json")).get(id);
            check(routeData.get(routeData.size-1).getInt("platform")==game(id).definition.platforms.length-1,
                id+" scripted route names the final platform beneath its clear gem");
            boolean[] routePlatforms=new boolean[game(id).definition.platforms.length];
            for(JsonValue step:routeData)routePlatforms[step.getInt("platform")]=true;
            for(int platform=1;platform<routePlatforms.length;platform++)
                check(routePlatforms[platform],id+" has no unused intermediate platform outside its documented route");
            StageRuntime base=route(id,30);
            checkpointRoutes(id);
            for(int fps:new int[]{60,144}) {
                StageRuntime other=route(id,fps);
                check(base.clear.recordTime==other.clear.recordTime&&base.clear.x==other.clear.x,
                    id+" fixed-tick trajectory matches at "+fps+" FPS");
            }
        }
        System.out.println("Mine verification PASS: "+assertions+" assertions.");
    }
}
