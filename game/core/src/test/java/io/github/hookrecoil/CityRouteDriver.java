/*
 * 파일: CityRouteDriver.java
 * 역할: 도시 V2 테스트 맵의 점프·갈고리·반동·이동 발판 입력 경로를 재생한다.
 * 주요 책임: 현재 발판에서 다음 authored 발판을 향해 조작하고 실제 착지를 확인한다.
 * 협력 대상: CityVerification, StageRuntime, StageDefinition.
 * 관련 사양: docs/CITY_CHAPTER_V2.md. 위치 순간이동은 사용하지 않는다.
 */
package io.github.hookrecoil;

import io.github.hookrecoil.input.InputFrame;
import io.github.hookrecoil.stage.StageDefinition;
import io.github.hookrecoil.stage.StageRuntime;

final class CityRouteDriver {
    int platform=1,airTicks;
    int settleTicks;
    boolean launched;
    boolean transitionEngaged;
    String blockedBy="none";
    int safeGateTicks,safeCrusherTicks;

    private void aimRecoil(StageRuntime stage,InputFrame input,float targetX,boolean finalPlatform) {
        boolean overheadRisk=false;
        float left=Math.min(stage.player.x,targetX),right=Math.max(stage.player.x,targetX);
        for(StageDefinition.Hazard hazard:stage.definition.hazards)
            if(hazard.x+hazard.width>=left-.4f&&hazard.x<=right+.4f
                &&hazard.y>stage.player.y&&hazard.y<stage.player.y+5.0f)overheadRisk=true;
        if(overheadRisk) {
            input.aimX=targetX>stage.player.x?-1:1;
            input.aimY=-.12f;
        } else {
            input.aimX=finalPlatform&&targetX>stage.player.x?-.7f:0;
            input.aimY=-1;
        }
    }

    private void steer(StageRuntime stage,InputFrame input,float targetX) {
        float error=targetX-stage.player.x;
        float desiredVx=Math.max(-stage.config.groundSpeed,Math.min(stage.config.groundSpeed,error*2.2f));
        float steering=desiredVx-stage.player.vx;
        input.move=Math.abs(steering)<.45f?0:steering>0?1:-1;
    }

    InputFrame next(StageRuntime stage) {
        blockedBy="none";
        InputFrame input=new InputFrame();input.aimX=0;input.aimY=-1;
        if(stage.player.grounded) {
            for(int i=0;i<stage.definition.platforms.length;i++) {
                StageDefinition.Block support=stage.definition.platforms[i];
                if(support.active&&stage.player.x+stage.config.halfWidth>support.x
                    &&stage.player.x-stage.config.halfWidth<support.x+support.width
                    &&Math.abs(stage.player.y-support.surfaceY(stage.player.x))<.04f&&i!=platform-1) {
                    platform=i+1;launched=false;airTicks=0;settleTicks=12;break;
                }
            }
        }
        if(platform>=stage.definition.platforms.length) {
            input.move=stage.player.x<stage.definition.clearGem.x?1:-1;return input;
        }
        StageDefinition.Block target=stage.definition.platforms[platform];
        boolean finalPlatform=platform==stage.definition.platforms.length-1;
        float targetCenter=target.x+target.width*.5f;
        for(StageDefinition.CityBarrier barrier:stage.definition.cityBarriers) {
            float barrierX=barrier.x+barrier.width*.5f;
            float routeBottom=Math.min(stage.player.y,target.surfaceY(targetCenter))-.3f;
            // Include the recoil arc above both endpoints so dense gates are
            // timed before launch, not only after the airborne crossing begins.
            float routeTop=Math.max(stage.player.y,target.surfaceY(targetCenter))+stage.config.height+5.0f;
            boolean crossesBarrierHeight=barrier.y+barrier.height>routeBottom&&barrier.y<routeTop;
            if(crossesBarrierHeight&&(stage.player.x-barrierX)*(targetCenter-barrierX)<0) {
                float cycle=barrier.offSeconds+barrier.warningSeconds+barrier.onSeconds;
                float local=((stage.elapsed+barrier.phase)%Math.max(.01f,cycle)+cycle)%cycle;
                if(!"off".equals(barrier.state)||local>barrier.offSeconds-1.5f){blockedBy="barrier "+barrier.id+" local="+local;return input;}
                safeGateTicks++;
            }
        }
        if(stage.player.grounded)for(StageDefinition.MineCrusher crusher:stage.definition.mineCrushers) {
            float left=Math.min(stage.player.x,targetCenter)-.8f,right=Math.max(stage.player.x,targetCenter)+.8f;
            float targetY=target.surfaceY(targetCenter);
            float pathTop=Math.max(stage.player.y,targetY)+stage.config.height+1.0f;
            float pathBottom=Math.min(stage.player.y,targetY)-.8f;
            boolean overlapsPath=crusher.topY>=pathBottom&&crusher.topY-crusher.extension<=pathTop;
            if(overlapsPath&&crusher.x+crusher.width>=left&&crusher.x<=right) {
                float period=Math.max(.01f,crusher.period);
                float local=((stage.elapsed/period+crusher.phase)%1f+1f)%1f*period;
                float openWindow=1.4f*period/3.6f;
                // Reserve enough of the open interval for the whole crossing, not just launch.
                if(!"open".equals(crusher.state)||local>openWindow-1.8f){blockedBy="crusher "+crusher.id+" local="+local;return input;}
                safeCrusherTicks++;
            }
        }
        boolean landed=target.active&&stage.player.grounded
            &&Math.abs(stage.player.y-target.surfaceY(stage.player.x))<.04f
            &&stage.player.x+stage.config.halfWidth>target.x&&stage.player.x-stage.config.halfWidth<target.x+target.width;
        if(landed){platform++;launched=false;transitionEngaged=false;airTicks=0;settleTicks=12;return input;}
        StageDefinition.Block source=stage.definition.platforms[Math.max(0,platform-1)];
        boolean verticalTransition=target.surfaceY(targetCenter)-source.surfaceY(source.x+source.width*.5f)>2.0f;
        if(verticalTransition) {
            float top=target.surfaceY(targetCenter);
            if(stage.player.grounded&&!transitionEngaged) {
                int best=-1;float bestDistance=stage.config.hookRange;
                for(int i=0;i<stage.definition.anchors.length;i++) {
                    StageDefinition.Point anchor=stage.definition.anchors[i];
                    if(anchor.y<top-1.5f||Math.abs(anchor.x-targetCenter)>1.5f)continue;
                    float dx=anchor.x-stage.player.x,dy=anchor.y-stage.player.y-stage.config.height*.65f;
                    float distance=(float)Math.sqrt(dx*dx+dy*dy);
                    if(distance<bestDistance){best=i;bestDistance=distance;}
                }
                if(best>=0) {
                    StageDefinition.Point anchor=stage.definition.anchors[best];float dx=anchor.x-stage.player.x;
                    float dy=anchor.y-stage.player.y-stage.config.height*.65f,len=(float)Math.sqrt(dx*dx+dy*dy);
                    input.aimX=dx/len;input.aimY=dy/len;input.hookPressed=input.hookHeld=true;
                    input.move=targetCenter>stage.player.x?1:-1;transitionEngaged=true;airTicks=0;return input;
                }
            }
            if(transitionEngaged&&(stage.grapple.attached>=0||stage.grapple.flying)) {
                input.hookHeld=stage.player.y<top-.45f;
                if(stage.grapple.attached>=0&&"city_05".equals(stage.definition.id)&&platform>=40) {
                    boolean targetOnPlayerSide=(targetCenter-stage.grapple.hookX)*(stage.player.x-stage.grapple.hookX)>=0;
                    input.move=(targetCenter>stage.player.x?1:-1)*(targetOnPlayerSide?1:-1);
                } else input.move=stage.grapple.attached>=0
                    ?(stage.player.x>=stage.grapple.hookX?1:-1):(targetCenter>stage.player.x?1:-1);
                if(stage.grapple.attached>=0&&stage.player.ammo>0&&stage.player.shotCooldown<=0
                    &&stage.player.y<top-1.0f&&airTicks%36==18) {input.fire=true;input.aimX=0;input.aimY=-1;}
                airTicks++;
                return input;
            }
            if(transitionEngaged&&!stage.player.grounded) {
                // If the rope releases before the ledge, continue the jump/recoil approach instead of steering forever.
                transitionEngaged=false;launched=true;airTicks=0;
            }
            if(transitionEngaged&&!stage.player.grounded) {
                input.move=targetCenter>stage.player.x?1:-1;airTicks++;return input;
            }
            if(stage.player.grounded&&transitionEngaged)transitionEngaged=false;
        }
        if(stage.player.grounded&&settleTicks>0){settleTicks--;return input;}
        float targetX=target.x+target.width*.5f;
        if(stage.player.grounded&&!launched) {
            float sourceCenter=source.x+source.width*.5f;
            boolean towardRight=targetX>sourceCenter;
            float targetNear=towardRight?target.x-stage.config.halfWidth-.15f:target.x+target.width+stage.config.halfWidth+.15f;
            float sourceEdge=towardRight?source.x+source.width-stage.config.halfWidth-.08f:source.x+stage.config.halfWidth+.08f;
            float launchX=towardRight?Math.min(targetNear,sourceEdge):Math.max(targetNear,sourceEdge);
            if(Math.abs(launchX-stage.player.x)>.1f) {
                input.move=launchX>stage.player.x?1:-1;return input;
            }
            input.move=platform>1&&!finalPlatform?(towardRight?1:-1):0;input.jump=true;launched=true;airTicks=0;return input;
        }
        if(!stage.player.grounded&&launched&&stage.player.y<target.surfaceY(targetX)-.02f) {
            steer(stage,input,targetX);
            if(airTicks==2&&stage.player.ammo>0){input.fire=true;aimRecoil(stage,input,targetX,finalPlatform);}
            airTicks++;return input;
        }
        steer(stage,input,targetX);
        if(!stage.player.grounded&&launched) {
            if(airTicks==2&&stage.player.ammo>0){input.fire=true;aimRecoil(stage,input,targetX,finalPlatform);}
            airTicks++;
        }
        return input;
    }
}
