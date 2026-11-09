/* 파일: PrologueRouteDriver.java
 * 역할: 프롤로그 스폰부터 클리어 보석까지 실제 입력으로 연속 재생한다.
 * 주요 책임: PrologueVerification에서 개별로 검증된 4개 구간(기본 점프·반동 점프·천장 스윙·마지막 반동 점프)을
 *          하나의 상태 기계로 이어 붙여 걷기·점프·반동·갈고리 입력을 물리 틱에 공급한다.
 * 협력 대상: FullPlaythroughDemo, StageRuntime, assets/stages/prologue.json.
 * 관련 사양: docs/10_PROLOGUE.md.
 */
package io.github.hookrecoil;

import io.github.hookrecoil.input.InputFrame;
import io.github.hookrecoil.stage.StageRuntime;

public final class PrologueRouteDriver {
    private static final int WALK1=0,AIR1=1,WALK2=2,AIR2=3,WALK3=4,
        CEIL_HOOK1=5,CEIL_HOOK2_SETUP=6,CEIL_HOOK2=7,CEIL_RECOIL1=8,CEIL_WAIT_COOLDOWN=9,
        WALK5=10;
    private int phase=WALK1,localTick;
    private boolean gotFirst,gotSecond;

    public InputFrame next(StageRuntime s) {
        InputFrame f=new InputFrame();
        switch(phase) {
            case WALK1:
                f.move=1;
                if(s.player.grounded&&s.player.x>=7.3f){f.jump=true;phase=AIR1;localTick=0;}
                break;
            case AIR1:
                f.move=1;localTick++;
                if(s.player.grounded&&localTick>5)phase=WALK2;
                break;
            case WALK2:
                f.move=1;
                if(s.player.grounded&&s.player.x>=15.4f){f.jump=true;phase=AIR2;localTick=0;}
                break;
            case AIR2:
                f.move=1;f.aimX=-0.8f;f.aimY=-0.6f;
                if(localTick==12)f.fire=true;
                localTick++;
                if(s.player.grounded&&localTick>13)phase=WALK3;
                break;
            case WALK3:
                f.move=1;
                if(s.player.grounded&&s.player.x>=25.9f){
                    f.jump=true;
                    f.aimX=29-s.player.x;f.aimY=6.6f-s.player.y-s.config.height*0.65f;
                    f.hookHeld=true;f.hookPressed=true;
                    phase=CEIL_HOOK1;
                }
                break;
            case CEIL_HOOK1:
                f.move=1;
                f.aimX=29-s.player.x;f.aimY=6.6f-s.player.y-s.config.height*0.65f;
                f.hookHeld=true;
                if(s.grapple.attached==0)gotFirst=true;
                if(gotFirst&&s.player.x>29.7f&&s.player.vy>0) {
                    f.hookHeld=false;f.aimX=-0.4f;f.aimY=-0.916515f;f.fire=true;
                    phase=CEIL_HOOK2_SETUP;
                }
                break;
            case CEIL_HOOK2_SETUP:
                f.move=1;
                f.aimX=33.5f-s.player.x;f.aimY=6.6f-s.player.y-s.config.height*0.65f;
                f.hookHeld=true;f.hookPressed=true;
                phase=CEIL_HOOK2;
                break;
            case CEIL_HOOK2:
                f.move=1;
                f.aimX=33.5f-s.player.x;f.aimY=6.6f-s.player.y-s.config.height*0.65f;
                f.hookHeld=true;
                if(s.grapple.attached==1)gotSecond=true;
                if(gotSecond&&s.player.x>34.5f&&s.player.vy>0) {
                    f.hookHeld=false;
                    phase=CEIL_RECOIL1;
                }
                break;
            case CEIL_RECOIL1:
                f.move=1;f.aimX=-0.4f;f.aimY=-0.916515f;f.fire=true;
                phase=CEIL_WAIT_COOLDOWN;
                break;
            case CEIL_WAIT_COOLDOWN:
                f.move=1;
                if(s.player.shotCooldown<=0) {
                    f.aimX=-0.8f;f.aimY=-0.6f;f.fire=true;
                    phase=WALK5;
                }
                break;
            case WALK5:
                // PrologueVerification의 route(true,34.5f)에서 확인한 대로, 두 번째 반동 사격은
                // 천장 구간~마지막 구멍(41~45, 4H)을 한 번에 건너 x=46.4 부근(클리어 보석 x=46.8
                // 바로 앞)까지 데려간다. 별도의 네 번째 점프 없이 보석까지 걸어가면 된다.
            default:
                f.move=s.player.x>s.definition.clearGem.x?-1:1;
                break;
        }
        float len=(float)Math.sqrt(f.aimX*f.aimX+f.aimY*f.aimY);
        if(len>0) {f.aimX/=len;f.aimY/=len;}
        return f;
    }
}
