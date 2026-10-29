/*
 * 파일: StageIntroCamera.java
 * 역할: 스테이지 시작 때 카메라를 스폰 위치에서 클리어 보석까지 이동시킨 뒤 플레이어에게 돌려보낸다.
 * 주요 책임: 고정 길이 도입 연출 시계와 보간 좌표를 제공한다. 게임 물리나 입력 상태는 소유하지 않는다.
 * 협력 대상: GameplayScreen, StageDefinition.
 * 관련 사양: docs/13_STAGE_INTRO_CAMERA.md.
 */
package io.github.hookrecoil.screens;

import com.badlogic.gdx.math.MathUtils;

final class StageIntroCamera {
    private static final float OUT_SECONDS=1.8f;
    private static final float GEM_HOLD_SECONDS=.4f;
    private static final float RETURN_SECONDS=1.5f;
    private float startX,startY,gemX,gemY,time;
    private boolean active;

    void begin(float playerX,float playerY,float clearGemX,float clearGemY) {
        startX=playerX;startY=playerY;gemX=clearGemX;gemY=clearGemY;time=0;active=true;
    }

    void cancel(){active=false;time=0;}
    boolean active(){return active;}

    void advance(float delta,boolean paused) {
        if(active&&!paused) {
            time+=Math.max(0,delta);
            if(time>=OUT_SECONDS+GEM_HOLD_SECONDS+RETURN_SECONDS) {
                time=OUT_SECONDS+GEM_HOLD_SECONDS+RETURN_SECONDS;active=false;
            }
        }
    }

    float x(){return coordinate(startX,gemX);}
    float y(){return coordinate(startY,gemY);}

    private float coordinate(float start,float end) {
        if(time<=OUT_SECONDS)return MathUtils.lerp(start,end,smooth(time/OUT_SECONDS));
        if(time<=OUT_SECONDS+GEM_HOLD_SECONDS)return end;
        float t=(time-OUT_SECONDS-GEM_HOLD_SECONDS)/RETURN_SECONDS;
        return MathUtils.lerp(end,start,smooth(t));
    }

    private float smooth(float t){t=MathUtils.clamp(t,0,1);return t*t*(3-2*t);}
}
