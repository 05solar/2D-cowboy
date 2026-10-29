/*
 * 파일: CityRouteDriver.java
 * 역할: 도시 테스트 맵의 점프·반동·이동 발판 입력 경로를 재생한다.
 * 주요 책임: 현재 발판에서 다음 authored 발판을 향해 조작하고 실제 착지를 확인한다.
 * 협력 대상: CityVerification, StageRuntime, StageDefinition.
 * 관련 사양: docs/assets/city_chapter/STAGES_11_15.md. 위치 순간이동은 사용하지 않는다.
 */
package io.github.hookrecoil;

import io.github.hookrecoil.input.InputFrame;
import io.github.hookrecoil.stage.StageDefinition;
import io.github.hookrecoil.stage.StageRuntime;

final class CityRouteDriver {
    int platform=1,airTicks;
    int settleTicks;
    boolean launched;

    InputFrame next(StageRuntime stage) {
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
            if((stage.player.x-barrierX)*(targetCenter-barrierX)<0&& !"off".equals(barrier.state))return input;
        }
        boolean landed=target.active&&stage.player.grounded
            &&Math.abs(stage.player.y-target.surfaceY(stage.player.x))<.04f
            &&stage.player.x+stage.config.halfWidth>target.x&&stage.player.x-stage.config.halfWidth<target.x+target.width;
        if(landed){platform++;launched=false;airTicks=0;settleTicks=12;return input;}
        if(stage.player.grounded&&settleTicks>0){settleTicks--;return input;}
        float targetX=target.x+target.width*.5f;
        if(stage.player.grounded&&!launched) {
            StageDefinition.Block source=stage.definition.platforms[Math.max(0,platform-1)];
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
            input.move=targetX>stage.player.x?1:-1;
            if(airTicks==2&&stage.player.ammo>0){input.fire=true;input.aimX=finalPlatform&&targetX>stage.player.x?-.7f:0;input.aimY=-1;}
            airTicks++;return input;
        }
        float error=targetX-stage.player.x;
        float desiredVx=Math.max(-stage.config.groundSpeed,Math.min(stage.config.groundSpeed,error*2.2f));
        float steering=desiredVx-stage.player.vx;
        input.move=Math.abs(steering)<.45f?0:steering>0?1:-1;
        if(!stage.player.grounded&&launched) {
            if(airTicks==2&&stage.player.ammo>0){input.fire=true;input.aimX=finalPlatform&&targetX>stage.player.x?-.7f:0;input.aimY=-1;}
            airTicks++;
        }
        return input;
    }
}
