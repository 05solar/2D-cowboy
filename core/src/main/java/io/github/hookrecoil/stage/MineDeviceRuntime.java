/* 파일: MineDeviceRuntime.java
 * 역할: 광산 전용 이동체·압착기·톱·공중 재충전 장치 시뮬레이션.
 * 주요 책임: 고정 주기 갱신, 이동 발판 속도 전달, 스윕 사망 판정, 재시작 위상 복원.
 * 협력 대상: StageRuntime, StageDefinition, PlayerState, GemSensor.
 * 수명: StageRuntime이 소유하며 체크포인트 복귀·재시작 때 모든 장치 상태를 초기화한다.
 * 관련 사양: docs/assets/mine_chapter/STAGES_06_10.md.
 */
package io.github.hookrecoil.stage;

import io.github.hookrecoil.collectible.GemSensor;
import io.github.hookrecoil.physics.PhysicsConfig;
import io.github.hookrecoil.player.PlayerState;

public final class MineDeviceRuntime {
    private static final float TAU=(float)(Math.PI*2);
    private final StageDefinition stage;
    private final boolean[] recharged;
    private final String[] previousCrusherStates;
    private boolean riding;
    private float riderVx,riderVy;
    private float time;
    public boolean warning;

    public MineDeviceRuntime(StageDefinition stage) {
        this.stage=stage;
        recharged=new boolean[stage.rechargeRings.length];
        previousCrusherStates=new String[stage.mineCrushers.length];
        reset();
    }

    public void reset() {
        time=0;warning=false;riding=false;riderVx=riderVy=0;
        java.util.Arrays.fill(recharged,false);
        for(StageDefinition.MineMover m:stage.mineMovers) {
            m.x=m.fromX;m.y=m.fromY;m.vx=m.vy=0;
            if(m.platform>=0&&m.platform<stage.platforms.length) {
                stage.platforms[m.platform].x=m.fromX;stage.platforms[m.platform].y=m.fromY;
            }
            if(m.anchor>=0&&m.anchor<stage.anchors.length) {
                stage.anchors[m.anchor].x=m.fromX;stage.anchors[m.anchor].y=m.fromY;
            }
        }
        for(int i=0;i<stage.mineCrushers.length;i++) {
            StageDefinition.MineCrusher c=stage.mineCrushers[i];c.extension=0;c.state="open";previousCrusherStates[i]="open";
        }
        for(StageDefinition.MineSaw s:stage.mineSaws) {s.x=s.fromX;s.y=s.fromY;s.angle=0;}
    }

    public void tick(float dt,PlayerState player) {
        warning=false;riding=false;riderVx=riderVy=0;
        int riddenPlatform=-1;
        if(player.grounded) for(StageDefinition.MineMover m:stage.mineMovers) if(m.platform>=0) {
            StageDefinition.Block b=stage.platforms[m.platform];
            if(Math.abs(player.y-b.y-b.height)<0.025f&&player.x+0.02f>b.x&&player.x-0.02f<b.x+b.width) {riddenPlatform=m.platform;break;}
        }
        float oldTime=time;time+=dt;
        for(StageDefinition.MineMover m:stage.mineMovers) {
            float oldX=m.x,oldY=m.y;
            float q=pingPong(time,m.period,m.phase);
            m.x=m.fromX+(m.toX-m.fromX)*q;m.y=m.fromY+(m.toY-m.fromY)*q;
            m.vx=(m.x-oldX)/dt;m.vy=(m.y-oldY)/dt;
            if(m.platform>=0&&m.platform<stage.platforms.length) {
                StageDefinition.Block b=stage.platforms[m.platform];
                if(riddenPlatform==m.platform){player.x+=m.x-oldX;player.y+=m.y-oldY;riding=true;riderVx=m.vx;riderVy=m.vy;}
                b.x=m.x;b.y=m.y;
            }
            if(m.anchor>=0&&m.anchor<stage.anchors.length) {stage.anchors[m.anchor].x=m.x;stage.anchors[m.anchor].y=m.y;}
        }
        for(int i=0;i<stage.mineCrushers.length;i++) {
            StageDefinition.MineCrusher c=stage.mineCrushers[i];
            String old=previousCrusherStates[i];updateCrusher(c);
            if("warning".equals(c.state)&&!"warning".equals(old))warning=true;
            previousCrusherStates[i]=c.state;
        }
        for(StageDefinition.MineSaw s:stage.mineSaws) {
            float q=pingPong(time,s.period,s.phase);
            s.x=s.fromX+(s.toX-s.fromX)*q;s.y=s.fromY+(s.toY-s.fromY)*q;
            s.angle=(time*240)%360;
        }
    }

    /** Apply platform momentum once only when leaving the moving surface. */
    public void transferLaunchVelocity(PlayerState player) {
        if(riding&&!player.grounded){player.vx+=riderVx;player.vy+=riderVy;}
        riding=false;riderVx=riderVy=0;
    }

    public boolean recharged(int index) { return index>=0&&index<recharged.length&&recharged[index]; }

    public boolean tryRecharge(PlayerState player,PhysicsConfig config) {
        if(player.grounded)return false;
        for(int i=0;i<stage.rechargeRings.length;i++) {
            StageDefinition.RechargeRing r=stage.rechargeRings[i];
            if(!recharged[i]&&GemSensor.contact(player.previousX,player.previousY,player.x,player.y,
                config.halfWidth,config.height,r.x,r.y,r.halfWidth,r.halfHeight)>=0) {
                recharged[i]=true;player.ammo=2;return true;
            }
        }
        return false;
    }

    public boolean lethal(PlayerState player,PhysicsConfig config) {
        for(StageDefinition.MineCrusher c:stage.mineCrushers) if(c.extension>0.02f) {
            float y=c.direction.equals("up")?c.topY:c.topY-c.extension;
            float h=c.extension;
            if(GemSensor.contact(player.previousX,player.previousY,player.x,player.previousY,
                config.halfWidth,config.height,c.x+c.width*.5f,y+h*.5f,c.width*.5f,h*.5f)>=0
                ||GemSensor.contact(player.x,player.previousY,player.x,player.y,
                config.halfWidth,config.height,c.x+c.width*.5f,y+h*.5f,c.width*.5f,h*.5f)>=0)return true;
        }
        for(StageDefinition.MineSaw s:stage.mineSaws) {
            float ax=player.previousX,ay=player.previousY+config.height*.5f;
            float bx=player.x,by=player.y+config.height*.5f,dx=bx-ax,dy=by-ay;
            float denominator=dx*dx+dy*dy;
            float t=denominator<1e-8f?0:Math.max(0,Math.min(1,((s.x-ax)*dx+(s.y-ay)*dy)/denominator));
            float ex=ax+dx*t-s.x,ey=ay+dy*t-s.y;
            float radius=s.radius+(float)Math.sqrt(config.halfWidth*config.halfWidth+config.height*config.height*.25f);
            if(ex*ex+ey*ey<=radius*radius)return true;
        }
        return false;
    }

    private void updateCrusher(StageDefinition.MineCrusher c) {
        float period=Math.max(.01f,c.period),elapsed=((time/period+c.phase)%1f+1f)%1f*period;
        float open=1.4f*period/3.6f,warningTime=.45f*period/3.6f,closing=.4f*period/3.6f;
        float closed=.9f*period/3.6f,opening=.45f*period/3.6f;
        if(elapsed<open){c.state="open";c.extension=0;}
        else if(elapsed<open+warningTime){c.state="warning";c.extension=0;}
        else if(elapsed<open+warningTime+closing){c.state="closing";c.extension=c.stroke*(elapsed-open-warningTime)/closing;}
        else if(elapsed<open+warningTime+closing+closed){c.state="closed";c.extension=c.stroke;}
        else {c.state="opening";c.extension=c.stroke*(1-(elapsed-open-warningTime-closing-closed)/opening);}
    }

    private static float pingPong(float t,float period,float phase) {
        float q=((t/Math.max(.01f,period)+phase)%1f+1f)%1f;
        return .5f-.5f*(float)Math.cos(TAU*q);
    }
}
