/* 파일: MineRouteExposure.java
 * 역할: 경로와 장애물 위험 범위가 겹치는지 누적 검사한다.
 * 책임: 맵 위쪽에 동떨어진 장벽·압착기·톱 검출. 매 경로마다 상태를 새로 생성한다.
 * 협력: MineVerification, StageRuntime. 관련 사양: docs/MINE_CHAPTER_STATUS.md.
 */
package io.github.hookrecoil;
import io.github.hookrecoil.stage.*;
final class MineRouteExposure {
    private final boolean[] gates,presses,saws,spikes;
    MineRouteExposure(StageRuntime s) {
        gates=new boolean[s.definition.cityBarriers.length];presses=new boolean[s.definition.mineCrushers.length];
        saws=new boolean[s.definition.mineSaws.length];spikes=new boolean[s.definition.hazards.length];
    }
    private boolean hit(StageRuntime s,float x,float y,float w,float h,float margin) {
        return s.player.x+s.config.halfWidth+margin>=x&&s.player.x-s.config.halfWidth-margin<=x+w
            &&s.player.y+s.config.height+margin>=y&&s.player.y-margin<=y+h;
    }
    void observe(StageRuntime s) {
        for(int i=0;i<gates.length;i++) {StageDefinition.CityBarrier b=s.definition.cityBarriers[i];gates[i]|=hit(s,b.x,b.y,b.width,b.height,0);}
        for(int i=0;i<presses.length;i++) {StageDefinition.MineCrusher c=s.definition.mineCrushers[i];presses[i]|=hit(s,c.x,c.direction.equals("up")?c.topY:c.topY-c.stroke,c.width,c.stroke,0);}
        for(int i=0;i<saws.length;i++) {
            StageDefinition.MineSaw c=s.definition.mineSaws[i];
            float px=Math.max(Math.min(c.fromX,c.toX),Math.min(Math.max(c.fromX,c.toX),s.player.x));
            float py=Math.max(Math.min(c.fromY,c.toY),Math.min(Math.max(c.fromY,c.toY),s.player.y+s.config.height*.5f));
            float dx=s.player.x-px,dy=s.player.y+s.config.height*.5f-py;
            float r=c.radius+(float)Math.hypot(s.config.halfWidth,s.config.height*.5f);
            saws[i]|=dx*dx+dy*dy<=r*r||hit(s,Math.min(c.fromX,c.toX)-c.radius,c.fromY,
                Math.abs(c.toX-c.fromX)+2*c.radius,4.5f,0);
        }
        for(int i=0;i<spikes.length;i++){StageDefinition.Hazard h=s.definition.hazards[i];spikes[i]|=hit(s,h.x,h.y,h.width,h.height,.7f);}
    }
    void verify(String id) {
        require(id,"barrier",gates);require(id,"crusher",presses);require(id,"saw rail",saws);require(id,"spike proximity",spikes);
        System.out.println(id+" route exposure: "+gates.length+" barriers, "+presses.length+" crushers, "+saws.length+" saw rails, "+spikes.length+" nearby spikes");
    }
    private void require(String id,String kind,boolean[] v){for(int i=0;i<v.length;i++)if(!v[i])throw new AssertionError(id+" off-route "+kind+" "+i);}
}
