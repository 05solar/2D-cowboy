/* 파일: CaveRenderer.java
 * 역할: 동굴 모듈 지형과 장치 표시. 책임: 물리 면 기준 정렬·장식 외피·줄 중복 방지.
 * 협력: CaveAssets, DeviceRuntime. 수명: 흰 픽셀 해제. 관련 사양: docs/12_CAVE_CHAPTER.md.
 */
package io.github.hookrecoil.rendering;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import io.github.hookrecoil.assets.CaveAssets;
import io.github.hookrecoil.stage.*;
public class CaveRenderer implements com.badlogic.gdx.utils.Disposable {
    private final CaveAssets assets;private final Texture white;
    public CaveRenderer(CaveAssets a){assets=a;Pixmap p=new Pixmap(1,1,Pixmap.Format.RGBA8888);p.setColor(Color.WHITE);p.fill();white=new Texture(p);p.dispose();}
    private void line(SpriteBatch b,float x,float y,float w,float h,Color c){b.setColor(c);b.draw(white,x,y,w,h);b.setColor(Color.WHITE);}
    private void top(SpriteBatch b,String name,float x,float y,float width){
        TextureRegion r=assets.region(name);float height=width*r.getRegionHeight()/r.getRegionWidth();
        b.draw(r,Math.round(x),Math.round(y-height),width,height);
    }
    public void draw(SpriteBatch b,StageRuntime s){float u=s.config.pixelsPerH;
        for(int i=0;i<s.definition.platforms.length;i++){
            StageDefinition.Block p=s.definition.platforms[i];float x=p.x*u,y=(p.y+p.height)*u,w=p.width*u;
            if(p.crumble){String state=s.devices.frame(i);if(state.equals("gone"))continue;
                // 파편도 동일 중심과 상단을 유지한다. 폭은 모든 상태에 같은 배율을 적용한다.
                TextureRegion r=assets.region("breakable_"+state);float scale=w/318f;
                b.draw(r,x+(w-r.getRegionWidth()*scale)/2,y-r.getRegionHeight()*scale,r.getRegionWidth()*scale,r.getRegionHeight()*scale);
                if(state.equals("warning")){line(b,x+w*.33f,y-23,w*.34f,7,new Color(.85f,.43f,.25f,1));}
            }else{
                line(b,x,p.y*u,w,p.height*u,new Color(.16f,.21f,.24f,1));
                int pieces=Math.max(1,(int)Math.ceil(p.width/3.5f));float piece=w/pieces;
                for(int j=0;j<pieces;j++)top(b,"platform_long",x+j*piece,y,piece);
            }
            if(p.active)line(b,x,y-3,w,3,new Color(.58f,.70f,.71f,1));
        }
        for(StageDefinition.Block p:s.definition.ceilings){
            int pieces=Math.max(1,(int)Math.ceil(p.width/3));float piece=p.width*u/pieces;
            TextureRegion r=assets.region("ceiling");float h=piece*r.getRegionHeight()/r.getRegionWidth();
            for(int j=0;j<pieces;j++)b.draw(r,p.x*u+j*piece,p.y*u,piece,h);
            line(b,p.x*u,p.y*u,p.width*u,3,new Color(.42f,.54f,.57f,1));
        }
        for(int i=0;i<s.definition.anchors.length;i++){
            StageDefinition.Point p=s.definition.anchors[i];TextureRegion r=assets.region("anchor_idle");
            if(i==s.grapple.attached||i==s.grapple.aimTarget(s.player))b.setColor(.5f,1,.85f,1);
            b.draw(r,p.x*u-43,p.y*u-30,86,102);b.setColor(Color.WHITE);
        }
        for(int i=0;i<s.definition.checkpoints.length;i++){
            StageDefinition.Point p=s.definition.checkpoints[i];TextureRegion r=assets.region(i<=s.checkpoint?"checkpoint_on":"checkpoint_off");
            b.setColor(1,1,1,s.noCheckpoint?.25f:1);b.draw(r,p.x*u-47,p.y*u,94,122);b.setColor(Color.WHITE);
        }
        for(StageDefinition.Hazard h:s.definition.hazards){
            TextureRegion r=assets.region("spikes_"+h.direction);
            // 위험 사각형은 날 끝 안쪽의 작은 영역, 그림은 별도 외피다.
            float w=h.width*u+36,hh=h.height*u+60;
            b.draw(r,h.x*u-18,h.y*u-30,w,hh);
        }
    }
    public void dispose(){white.dispose();}
}
