/*
 * 파일: StageIntroCamera.java
 * 역할: 스테이지 시작 때 스폰에서 전체 맵을 보여주고 보석을 비춘 뒤 스폰으로 돌아온다.
 * 주요 책임: 줌·카메라 위치 단계 계산과 연출 시간 상태를 소유하고 초기화/해제한다.
 * 협력 대상: GameplayScreen, StageDefinition.
 * 관련 사양: docs/13_STAGE_INTRO_CAMERA.md, docs/CITY_CHAPTER_V2.md.
 */
package io.github.hookrecoil.screens;

import com.badlogic.gdx.math.MathUtils;
import io.github.hookrecoil.stage.StageDefinition;

final class StageIntroCamera {
    private static final float VIEW_WIDTH=2560f,VIEW_HEIGHT=1440f;
    private static final float ZOOM_OUT_SECONDS=1.4f,OVERVIEW_HOLD_SECONDS=.8f;
    private static final float GEM_APPROACH_SECONDS=1.4f,GEM_HOLD_SECONDS=.45f,RETURN_SECONDS=1.6f;
    private float spawnX,spawnY,gemX,gemY,overviewX,overviewY,overviewZoom=1,time;
    private boolean active;

    void begin(StageDefinition stage,float pixelsPerH) {
        time=0;active=false;
        float worldWidth=stage.width*pixelsPerH,worldHeight=stage.height*pixelsPerH;
        float maxX=Math.max(VIEW_WIDTH*.5f,worldWidth-VIEW_WIDTH*.5f);
        float maxY=Math.max(VIEW_HEIGHT*.5f,worldHeight-VIEW_HEIGHT*.5f);
        spawnX=MathUtils.clamp(stage.spawnX*pixelsPerH+430,VIEW_WIDTH*.5f,maxX);
        spawnY=MathUtils.clamp(stage.spawnY*pixelsPerH+300,VIEW_HEIGHT*.5f,maxY);
        gemX=MathUtils.clamp(stage.clearGem.x*pixelsPerH+430,VIEW_WIDTH*.5f,maxX);
        gemY=MathUtils.clamp(stage.clearGem.y*pixelsPerH+300,VIEW_HEIGHT*.5f,maxY);
        overviewX=worldWidth*.5f;overviewY=worldHeight*.5f;
        overviewZoom=Math.max(1f,Math.max(worldWidth/VIEW_WIDTH,worldHeight/VIEW_HEIGHT)*1.08f);
        active=true;
    }

    void cancel(){active=false;time=0;overviewZoom=1;}
    boolean active(){return active;}
    float overviewReachedAt(){return ZOOM_OUT_SECONDS+OVERVIEW_HOLD_SECONDS;}
    float gemReachedAt(){return overviewReachedAt()+GEM_APPROACH_SECONDS;}
    float totalDuration(){return gemReachedAt()+GEM_HOLD_SECONDS+RETURN_SECONDS;}

    void advance(float delta,boolean paused) {
        if(active&&!paused) {
            time+=Math.max(0,delta);
            if(time>=totalDuration()) {time=totalDuration();active=false;}
        }
    }

    float x(){return value(spawnX,overviewX,gemX);}
    float y(){return value(spawnY,overviewY,gemY);}
    float zoom(){return value(1,overviewZoom,1);}

    private float value(float spawn,float overview,float gem) {
        float t;
        if(time<=ZOOM_OUT_SECONDS)
            return MathUtils.lerp(spawn,overview,ease(time/ZOOM_OUT_SECONDS));
        if(time<=overviewReachedAt())return overview;
        if(time<=gemReachedAt()) {
            t=(time-overviewReachedAt())/GEM_APPROACH_SECONDS;
            return MathUtils.lerp(overview,gem,ease(t));
        }
        if(time<=gemReachedAt()+GEM_HOLD_SECONDS)return gem;
        t=(time-gemReachedAt()-GEM_HOLD_SECONDS)/RETURN_SECONDS;
        return MathUtils.lerp(gem,spawn,ease(t));
    }

    private float ease(float t){t=MathUtils.clamp(t,0,1);return t*t*(3-2*t);}
}
