/*
 * 파일: CityDeviceRuntime.java
 * 역할: 지하도시의 이동 발판·이동 링·고정 기류·전기 장벽 상태를 고정 틱으로 갱신한다.
 * 주요 책임: 이동체 동기화, 탑승 속도 1회 전달, 바람 힘 적용, 장벽 위상 및 치명 접촉 판정.
 * 협력 대상: StageRuntime, StageDefinition, CollisionWorld, PlayerController, PlayerState.
 * 수명: StageRuntime이 소유하며 시작·체크포인트 재시작에서 시계·위상·탑승 상태를 초기화한다.
 * 관련 사양: docs/assets/city_chapter/DEVICES_AND_MOTION.md, docs/assets/city_chapter/STAGES_11_15.md.
 */
package io.github.hookrecoil.stage;

import io.github.hookrecoil.collectible.GemSensor;
import io.github.hookrecoil.physics.PhysicsConfig;
import io.github.hookrecoil.player.PlayerState;

/** City chapter device clock; it does not change global movement constants. */
public final class CityDeviceRuntime {
    private static final float TAU=(float)(Math.PI*2);
    private final StageDefinition stage;
    private float time,riderVx,riderVy;
    private float windX,windY;
    private float sampleX,sampleY;
    private boolean riding;
    public boolean warning;

    public CityDeviceRuntime(StageDefinition stage) {
        this.stage=stage;
        for(StageDefinition.CityStructure structure:stage.cityStructures) {
            int count=structure.colliders==null?0:structure.colliders.length;
            structure.collisionParts=new StageDefinition.Block[count];
            for(int i=0;i<count;i++) {
                float[] rect=structure.colliders[i];StageDefinition.Block part=new StageDefinition.Block();
                part.x=structure.x+rect[0]*structure.width;
                part.y=structure.y+(1-rect[1]-rect[3])*structure.height;
                part.width=rect[2]*structure.width;part.height=rect[3]*structure.height;
                structure.collisionParts[i]=part;
            }
        }
        for(StageDefinition.CityBarrier barrier:stage.cityBarriers) {
            StageDefinition.Block collider=new StageDefinition.Block();
            collider.x=barrier.x;collider.y=barrier.y;collider.width=barrier.width;collider.height=barrier.height;
            collider.active=false;barrier.collider=collider;
        }
        reset();
    }

    public void reset() {
        time=0;riding=false;riderVx=riderVy=windX=windY=0;warning=false;
        for(StageDefinition.MineMover mover:stage.mineMovers) {
            mover.x=mover.fromX;mover.y=mover.fromY;mover.vx=mover.vy=0;
            if(mover.platform>=0&&mover.platform<stage.platforms.length) {
                StageDefinition.Block platform=stage.platforms[mover.platform];platform.x=mover.fromX;platform.y=mover.fromY;
            }
            if(mover.anchor>=0&&mover.anchor<stage.anchors.length) {
                StageDefinition.Point anchor=stage.anchors[mover.anchor];anchor.x=mover.fromX;anchor.y=mover.fromY;
            }
            if(mover.structure>=0&&mover.structure<stage.cityStructures.length)moveStructure(stage.cityStructures[mover.structure],mover.fromX,mover.fromY);
        }
        for(StageDefinition.CityBarrier barrier:stage.cityBarriers) {
            barrier.state="off";barrier.collider.active=false;
        }
    }

    public void tick(float dt,PlayerState player) {
        warning=false;riding=false;riderVx=riderVy=0;
        int ridden=-1;
        if(player.grounded)for(StageDefinition.MineMover mover:stage.mineMovers)if(mover.platform>=0) {
            StageDefinition.Block platform=stage.platforms[mover.platform];
            if(Math.abs(player.y-platform.surfaceY(player.x))<.035f&&player.x>platform.x-.02f&&player.x<platform.x+platform.width+.02f){ridden=mover.platform;break;}
        }
        time+=dt;
        for(StageDefinition.MineMover mover:stage.mineMovers) {
            float oldX=mover.x,oldY=mover.y;
            samplePosition(mover,time);
            mover.x=sampleX;mover.y=sampleY;
            mover.vx=(mover.x-oldX)/dt;mover.vy=(mover.y-oldY)/dt;
            if(mover.platform>=0&&mover.platform<stage.platforms.length) {
                StageDefinition.Block platform=stage.platforms[mover.platform];
                if(ridden==mover.platform){player.x+=mover.x-oldX;player.y+=mover.y-oldY;riding=true;riderVx=mover.vx;riderVy=mover.vy;}
                platform.x=mover.x;platform.y=mover.y;
            }
            if(mover.anchor>=0&&mover.anchor<stage.anchors.length) {
                stage.anchors[mover.anchor].x=mover.x;stage.anchors[mover.anchor].y=mover.y;
            }
            if(mover.structure>=0&&mover.structure<stage.cityStructures.length)moveStructure(stage.cityStructures[mover.structure],mover.x,mover.y);
        }
        for(StageDefinition.CityBarrier barrier:stage.cityBarriers) {
            float cycle=barrier.offSeconds+barrier.warningSeconds+barrier.onSeconds;
            float phase=((time+barrier.phase)%Math.max(.01f,cycle)+cycle)%cycle;
            String previous=barrier.state;
            if(phase<barrier.offSeconds)barrier.state="off";
            else if(phase<barrier.offSeconds+barrier.warningSeconds)barrier.state="warning";
            else barrier.state="on";
            barrier.collider.active=!"off".equals(barrier.state);
            if("warning".equals(barrier.state)&&!"warning".equals(previous))warning=true;
        }
    }

    private void moveStructure(StageDefinition.CityStructure structure,float x,float y) {
        float dx=x-structure.x,dy=y-structure.y;structure.x=x;structure.y=y;
        if(structure.collisionParts!=null)for(StageDefinition.Block part:structure.collisionParts){part.x+=dx;part.y+=dy;}
    }

    private void samplePosition(StageDefinition.MineMover mover,float elapsed) {
        if(mover.waypoints==null||mover.waypoints.length<2) {
            float angle=TAU*(elapsed/Math.max(.01f,mover.period)+mover.phase);
            float q=.5f-.5f*(float)Math.cos(angle);
            sampleX=mover.fromX+(mover.toX-mover.fromX)*q;
            sampleY=mover.fromY+(mover.toY-mover.fromY)*q;
            return;
        }
        float travel=Math.max(.01f,mover.travelSeconds),hold=Math.max(0,mover.holdSeconds);
        float segmentDuration=travel+hold,total=segmentDuration*mover.waypoints.length;
        float local=((elapsed+mover.phase)%total+total)%total;
        int index=Math.min(mover.waypoints.length-1,(int)(local/segmentDuration));
        float inSegment=local-index*segmentDuration;
        int next=(index+1)%mover.waypoints.length;
        float q=inSegment<=hold?0:(inSegment-hold)/travel;
        q=q*q*(3-2*q);
        sampleX=mover.waypoints[index][0]+(mover.waypoints[next][0]-mover.waypoints[index][0])*q;
        sampleY=mover.waypoints[index][1]+(mover.waypoints[next][1]-mover.waypoints[index][1])*q;
    }

    public void transferLaunchVelocity(PlayerState player) {
        if(riding&&!player.grounded){player.vx+=riderVx;player.vy+=riderVy;}
        riding=false;riderVx=riderVy=0;
    }

    public void sampleWind(PlayerState player,PhysicsConfig config) {
        float x=player.x,y=player.y+config.height*.5f,ax=0,ay=0;
        for(StageDefinition.CityWindZone zone:stage.cityWindZones)
            if(x>=zone.x&&x<=zone.x+zone.width&&y>=zone.y&&y<=zone.y+zone.height){ax+=zone.ax;ay+=zone.ay;}
        windX=ax;windY=ay;
    }
    public float windX(){return windX;}
    public float windY(){return windY;}

    public boolean lethal(PlayerState player,PhysicsConfig config) {
        for(StageDefinition.CityBarrier barrier:stage.cityBarriers)if("on".equals(barrier.state)) {
            if(GemSensor.contact(player.previousX,player.previousY,player.x,player.previousY,
                config.halfWidth,config.height,barrier.x+barrier.width*.5f,barrier.y+barrier.height*.5f,
                barrier.width*.5f,barrier.height*.5f)>=0
                ||GemSensor.contact(player.x,player.previousY,player.x,player.y,
                config.halfWidth,config.height,barrier.x+barrier.width*.5f,barrier.y+barrier.height*.5f,
                barrier.width*.5f,barrier.height*.5f)>=0)return true;
        }
        return false;
    }
}
