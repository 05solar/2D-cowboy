/* 파일: StageRuntime.java
 * 역할: 스테이지별 고정 틱 조정. 책임: 사망 우선·장치·체크포인트·지상 보석 접촉·사망 대기.
 * 협력: PlayerController, RecoilSystem, GrappleSystem, DeviceRuntime, MineDeviceRuntime, CityDeviceRuntime.
 * 수명: 스테이지별 상태를 소유하며 시작/복귀 때 플레이어·장치·사망 대기를 초기화한다.
 * 관련 사양: docs/10_PROLOGUE.md, docs/12_CAVE_CHAPTER.md, docs/assets/city_chapter/DEVICES_AND_MOTION.md.
 */
package io.github.hookrecoil.stage;

import io.github.hookrecoil.physics.*;
import io.github.hookrecoil.player.*;
import io.github.hookrecoil.recoil.RecoilSystem;
import io.github.hookrecoil.grapple.GrappleSystem;
import io.github.hookrecoil.input.InputFrame;
import io.github.hookrecoil.collectible.*;

public class StageRuntime {
    public final ClearSequence clear=new ClearSequence(new int[]{200,200,200,240,240,360,240,320});
    public interface CompletionSink { void commit(StageRuntime stage) throws Exception; }
    public CompletionSink completionSink;
    public CompletionSink resultSink;
    public boolean noCheckpoint;
    public final java.util.Set<String> optionalCollected=new java.util.HashSet<>();
    public final DeviceRuntime devices;
    public final MineDeviceRuntime mineDevices;
    public final CityDeviceRuntime cityDevices;
    public boolean justCollected;
    public String saveError;
    public final StageDefinition definition;
    public final PhysicsConfig config;
    public final PlayerState player=new PlayerState();
    public final CollisionWorld collision;
    public final GrappleSystem grapple;
    private final PlayerController controller;
    private final RecoilSystem recoil;
    public int checkpoint=-1,deaths,shots;
    public float elapsed,deathTimer,noticeTimer;
    public boolean complete,justReset,deathWaiting;
    public StageRuntime(StageDefinition stage,PhysicsConfig c) {
        stage.validate(); definition=stage;config=c;cityDevices=new CityDeviceRuntime(stage);collision=new CollisionWorld(stage,c);devices=new DeviceRuntime(stage);mineDevices=new MineDeviceRuntime(stage);
        grapple=new GrappleSystem(c,stage,collision);controller=new PlayerController(c);recoil=new RecoilSystem(c);
        respawn();
    }
    public void tick(InputFrame in,float dt) {
        justReset=false;
        justCollected=false;
        if(clear.state==ClearSequence.State.COLLECTING)return;
        if (in.restart) { if(complete) restartAll(); else respawn(); return; }
        if (complete) return;
        if(deathTimer>0) {
            deathTimer=Math.max(0,deathTimer-dt);
            if(deathTimer==0)deathWaiting=true;
            return;
        }
        if(deathWaiting)return;
        elapsed+=dt;noticeTimer=Math.max(0,noticeTimer-dt);
        devices.tick(dt);
        if(definition.id.startsWith("city_")){cityDevices.tick(dt,player);devices.warning|=cityDevices.warning;}
        else {mineDevices.tick(dt,player);devices.warning|=mineDevices.warning;}
        controller.prepare(player,in,dt);
        if(definition.id.startsWith("city_"))cityDevices.transferLaunchVelocity(player);else mineDevices.transferLaunchVelocity(player);
        if(in.fire && recoil.fire(player)) shots++;
        grapple.update(player,in,dt);
        if(definition.id.startsWith("city_")) {
            cityDevices.sampleWind(player,config);
            controller.integrate(player,collision,dt,cityDevices.windX(),cityDevices.windY());
        } else controller.integrate(player,collision,dt);
        grapple.constrain(player);
        devices.landed(player,config);
        mineDevices.tryRecharge(player,config);
        if(resolveGoal(player.y < definition.deathPlaneY || player.x < -1 || player.x > definition.width+1
            || devices.lethal(player,config) || mineDevices.lethal(player,config) || cityDevices.lethal(player,config)))return;
        for(StageDefinition.Gem g:definition.optionalGems)
            if(GemSensor.contact(player.previousX,player.previousY,player.x,player.y,config.halfWidth,config.height,g.x,g.y,g.halfWidth,g.halfHeight)>=0)optionalCollected.add(g.id);
        for(int i=0;i<definition.checkpoints.length;i++) {
            StageDefinition.Point cp=definition.checkpoints[i];
            if(!noCheckpoint && i>checkpoint && player.grounded && Math.abs(player.y-cp.y)<0.1f && Math.abs(player.x-cp.x)<0.7f) { checkpoint=i;noticeTimer=2; }
        }
    }
    public boolean resolveGoal(boolean lethal) {
        if(clear.frozen())return true;
        if(lethal) {deaths++;deathTimer=0.35f;deathWaiting=false;grapple.reset();return true;}
        if(!player.grounded)return false;
        StageDefinition.Gem g=definition.clearGem;
        // 착지 틱에는 공중 궤적을 되감지 않고 착지가 끝난 위치에서만 검사한다.
        boolean sameGroundHeight=Math.abs(player.previousY-player.y)<0.0001f;
        float startX=sameGroundHeight?player.previousX:player.x;
        float t=GemSensor.contact(startX,player.y,player.x,player.y,
            config.halfWidth,config.height,g.x,g.y,g.halfWidth,g.halfHeight);
        if(t<0)return false;
        float x=startX+(player.x-startX)*t,y=player.y;
        // 수평 스윕의 최초 접점도 실제 바닥 위인지 확인한다.
        if(!collision.supports(x,y))return false;
        if(clear.begin(x,y,player.facingRight,elapsed)) {
            player.x=player.previousX=x;player.y=player.previousY=y;
            player.vx=player.vy=player.coyote=player.jumpBuffer=player.shotPose=player.shotCooldown=0;
            grapple.reset();justCollected=true;
            if(completionSink!=null)try {completionSink.commit(this);}catch(Exception e){saveError="완료 기록 저장 실패: "+e.getClass().getSimpleName();}
        }
        return true;
    }
    public void advancePresentation(double seconds) {
        boolean wasComplete=complete;
        clear.advance(seconds);complete=clear.state==ClearSequence.State.CLEARED;
        if(complete&&!wasComplete&&resultSink!=null)try{resultSink.commit(this);}catch(Exception e){saveError="완료 기록 저장 실패: "+e.getClass().getSimpleName();}
    }
    public void respawn() {
        if(clear.frozen())return;
        float x=definition.spawnX,y=definition.spawnY;
        if(!noCheckpoint&&checkpoint>=0) { x=definition.checkpoints[checkpoint].x;y=definition.checkpoints[checkpoint].y; }
        devices.reset();mineDevices.reset();cityDevices.reset();player.reset(x,y);grapple.reset();deathTimer=0;deathWaiting=false;justReset=true;
    }
    public void restartAll() {clear.reset();saveError=null;checkpoint=-1;elapsed=0;deaths=shots=0;complete=false;optionalCollected.clear();respawn();}
    public int zoneIndex() {
        int result=0;
        for(int i=0;i<definition.zones.length;i++) if(player.x>=definition.zones[i].x) result=i;
        return result;
    }
}
