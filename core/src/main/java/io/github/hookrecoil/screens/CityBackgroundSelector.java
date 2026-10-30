/*
 * 파일: CityBackgroundSelector.java
 * 역할: 지하도시 V2 카메라 구역에 맞는 가로·세로 원경과 전환 상태를 관리한다.
 * 주요 책임: 우선순위 구역 선택, 히스테리시스, 일시정지 가능한 크로스페이드 시계.
 * 협력 대상: StageDefinition.CityBackgroundRegion, GameplayScreen, CaveAssets.
 * 수명: GameplayScreen이 소유하며 스테이지 교체 시 상태를 초기화한다.
 * 관련 사양: docs/CITY_CHAPTER_V2.md.
 */
package io.github.hookrecoil.screens;

import io.github.hookrecoil.stage.StageDefinition;

/** Selects a non-repeating background panorama from authored world-space camera regions. */
public final class CityBackgroundSelector {
    private static final float HYSTERESIS_H=0.75f;
    private static final float FADE_SECONDS=.35f;
    private String stageId,activePath,previousPath,activeRegionId;
    private float fadeElapsed=FADE_SECONDS;

    public void reset(){stageId=activePath=previousPath=activeRegionId=null;fadeElapsed=FADE_SECONDS;}

    public void update(StageDefinition stage,float xH,float yH,float dt,boolean advance) {
        if(stageId==null||!stage.id.equals(stageId)) {
            reset();stageId=stage.id;
        }
        StageDefinition.CityBackgroundRegion selected=null;
        if(activeRegionId!=null)for(StageDefinition.CityBackgroundRegion region:stage.cityBackgroundRegions)
            if(activeRegionId.equals(region.id)&&contains(region,xH,yH,HYSTERESIS_H)){selected=region;break;}
        if(selected==null)for(StageDefinition.CityBackgroundRegion region:stage.cityBackgroundRegions)
            if(contains(region,xH,yH,0)&&(selected==null||region.priority>selected.priority))selected=region;
        if(selected==null&&stage.cityBackgroundRegions.length>0)selected=stage.cityBackgroundRegions[0];
        if(selected==null)return;
        if(activePath==null) {
            activePath=selected.path;activeRegionId=selected.id;fadeElapsed=FADE_SECONDS;
        } else if(!selected.path.equals(activePath)) {
            previousPath=activePath;activePath=selected.path;activeRegionId=selected.id;fadeElapsed=0;
        } else activeRegionId=selected.id;
        if(advance)fadeElapsed=Math.min(FADE_SECONDS,fadeElapsed+Math.max(0,dt));
        if(fadeElapsed>=FADE_SECONDS)previousPath=null;
    }

    private static boolean contains(StageDefinition.CityBackgroundRegion r,float x,float y,float margin) {
        return x>=r.minX-margin&&x<=r.maxX+margin&&y>=r.minY-margin&&y<=r.maxY+margin;
    }

    public String activePath(){return activePath;}
    public String previousPath(){return previousPath;}
    public float blend(){return FADE_SECONDS<=0?1:Math.max(0,Math.min(1,fadeElapsed/FADE_SECONDS));}
}
