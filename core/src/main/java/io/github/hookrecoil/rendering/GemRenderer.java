/* 파일: GemRenderer.java
 * 역할: 획득 8포즈·소켓 보석·독립 광원 렌더. 책임: 발 고정·반전 좌표·계층 순서.
 * 협력: GemAssets, StageRuntime. 관련 사양: assets/gem_pickup/APPLY.md. 원본 이미지 수정 없음.
 */
package io.github.hookrecoil.rendering;

import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import io.github.hookrecoil.assets.GemAssets;
import io.github.hookrecoil.stage.StageRuntime;

public class GemRenderer {
    private final GemAssets assets;
    public GemRenderer(GemAssets a){assets=a;}
    private float scale(StageRuntime s){return s.config.playerVisualScale*0.84f;}
    private float gemHeight(StageRuntime s){return s.config.height*s.config.pixelsPerH*0.52f;}
    private float glow(StageRuntime s){
        double time=s.clear.frozen()?s.clear.time:s.elapsed;
        return 0.20f+0.05f*(0.5f+0.5f*(float)Math.sin(time*Math.PI*2/1.25));
    }
    private float[] gemPosition(StageRuntime s) {
        float u=s.config.pixelsPerH,wx=s.definition.clearGem.x*u;
        float wy=s.definition.clearGem.y*u+(float)Math.sin(s.elapsed*Math.PI*2/1.6)*s.config.height*u*0.02f;
        double t=s.clear.time;
        float total=s.clear.durationSeconds();
        if(!s.clear.frozen()||t<total*0.1)return new float[]{wx,wy};
        GemAssets.Pose p=assets.poses[s.clear.frame()];
        if(p.socket==null)return new float[]{wx,wy};
        float sign=s.clear.facingRight?1:-1;
        float x=s.clear.x*u+sign*(p.socket[0]-p.pivot[0])*scale(s);
        float y=s.clear.y*u+(p.pivot[1]-p.socket[1])*scale(s)+gemHeight(s)*0.45f;
        float progress=MathUtils.clamp((float)(t-total*0.1)/(total*0.2f),0,1);
        return new float[]{MathUtils.lerp(wx,x,progress),MathUtils.lerp(wy,y,progress)};
    }
    private float fade(StageRuntime s){float total=s.clear.durationSeconds();return s.clear.frozen()?MathUtils.clamp((total*.84f-(float)s.clear.time)/(total*.12f),0,1):1;}
    public void behind(SpriteBatch batch,StageRuntime s) {
        float[] p=gemPosition(s);
        effect(batch,"aura",p[0],p[1],gemHeight(s)*3.1f,(s.clear.frozen()?0.38f:glow(s))*fade(s),.7f,.9f,1);
    }
    public void character(SpriteBatch batch,StageRuntime s) {
        GemAssets.Pose p=assets.poses[s.clear.frame()];
        TextureRegion r=s.clear.facingRight?p.normal:p.flipped;
        float pivotX=s.clear.facingRight?p.pivot[0]:r.getRegionWidth()-p.pivot[0];
        float k=scale(s),u=s.config.pixelsPerH;
        batch.draw(r,Math.round(s.clear.x*u-pivotX*k),Math.round(s.clear.y*u-(r.getRegionHeight()-p.pivot[1])*k),r.getRegionWidth()*k,r.getRegionHeight()*k);
    }
    public void front(SpriteBatch batch,StageRuntime s) {
        float[] p=gemPosition(s);float height=gemHeight(s);
        effect(batch,"gem",p[0],p[1],height,fade(s));
        if(s.clear.frozen()) {
            float total=s.clear.durationSeconds(),t=(float)s.clear.time-total*.54f;
            if(t>=0&&t<total*.24f)effect(batch,"pulse_ring",p[0],p[1],height*3*MathUtils.lerp(0.65f,1.8f,t/(total*.24f)),0.4f*(1-t/(total*.24f)));
            if(t>=0&&t<total*.14f)effect(batch,"sparkle",p[0],p[1],height*2.2f,0.4f*(1-t/(total*.14f)));
        }
    }
    public void optional(SpriteBatch batch,StageRuntime s,float x,float y) {
        float px=x*s.config.pixelsPerH,py=y*s.config.pixelsPerH,height=gemHeight(s)*.9f;
        effect(batch,"aura",px,py,height*2.5f,glow(s),.65f,.9f,1);
        effect(batch,"gem",px,py,height,.92f,.52f,.78f,1);
    }
    private void effect(SpriteBatch batch,String name,float x,float y,float visibleHeight,float alpha) {
        effect(batch,name,x,y,visibleHeight,alpha,1,1,1);
    }
    private void effect(SpriteBatch batch,String name,float x,float y,float visibleHeight,float alpha,float red,float green,float blue) {
        if(alpha<=0)return;
        GemAssets.Effect e=assets.effects.get(name);
        float k=visibleHeight/e.visible[3];
        float centerX=e.visible[0]+e.visible[2]/2f,centerY=e.texture.getHeight()-e.visible[1]-e.visible[3]/2f;
        batch.setColor(red,green,blue,alpha);
        // 전체 PNG를 그리되 배율과 중심 계산에만 실측 빛 경계를 사용한다.
        batch.draw(e.texture,Math.round(x-centerX*k),Math.round(y-centerY*k),e.texture.getWidth()*k,e.texture.getHeight()*k);
        batch.setColor(1,1,1,1);
    }
}
