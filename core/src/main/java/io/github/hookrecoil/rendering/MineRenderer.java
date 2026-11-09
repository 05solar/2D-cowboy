/* 파일: MineRenderer.java
 * 역할: 광산 스테이지의 실측 에셋과 움직이는 장치를 그린다.
 * 주요 책임: 배경 분리 지형, 이동체, 이동 고정점, 가시·압착기·톱·재충전 링 표시.
 * 협력 대상: CaveAssets, MineDeviceRuntime, StageDefinition.
 * 수명: 텍스처는 CaveAssets가 소유하며 이 렌더러는 해제하지 않는다.
 * 관련 사양: docs/assets/mine_chapter/STAGES_06_10.md.
 */
package io.github.hookrecoil.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.hookrecoil.assets.CaveAssets;
import io.github.hookrecoil.stage.StageDefinition;
import io.github.hookrecoil.stage.StageRuntime;

public final class MineRenderer {
    private final CaveAssets assets;
    private final TextureRegion emitterUpOff,emitterDownOff;
    public MineRenderer(CaveAssets assets){
        this.assets=assets;
        TextureRegion up=assets.region("city_v2_electric_emitter_up"),down=assets.region("city_v2_electric_emitter_down");
        int upCut=Math.round(up.getRegionHeight()*.58f),downHeight=Math.round(down.getRegionHeight()*.43f);
        emitterUpOff=new TextureRegion(up,0,upCut,up.getRegionWidth(),up.getRegionHeight()-upCut);
        emitterDownOff=new TextureRegion(down,0,0,down.getRegionWidth(),downHeight);
    }
    private TextureRegion region(String name){return assets.region("mine_"+name);}

    public void draw(SpriteBatch batch,StageRuntime stage) {
        float u=stage.config.pixelsPerH;
        drawTerrain(batch,stage,u);
        drawSpikes(batch,stage,u);
        drawElectricBarriers(batch,stage,u);
        drawCrushers(batch,stage,u);
        drawSaws(batch,stage,u);
        drawMovers(batch,stage,u);
        drawRechargeRings(batch,stage,u);
        drawCheckpoints(batch,stage,u);
    }

    private void drawTerrain(SpriteBatch b,StageRuntime s,float u) {
        for(StageDefinition.Block p:s.definition.platforms) {
            if(!p.active)continue;
            boolean moving=false;
            for(StageDefinition.MineMover m:s.definition.mineMovers)if(m.platform>=0&&s.definition.platforms[m.platform]==p){moving=true;break;}
            if(moving)continue;
            String sprite=p.sprite==null?"platform_medium":p.sprite;
            TextureRegion r=region(sprite);float width=p.width*u;
            float height=width*r.getRegionHeight()/r.getRegionWidth();
            // Atlas surfaceProfile matches collision to the visible metal edge; transparent rect margins stay non-colliding.
            b.draw(r,p.x*u,(p.y+p.height)*u-height,width,height);
        }
        for(StageDefinition.Block c:s.definition.ceilings) {
            TextureRegion r=region("overhead_beam");float width=c.width*u,height=width*r.getRegionHeight()/r.getRegionWidth();
            b.draw(r,c.x*u,c.y*u,width,height);
        }
    }

    private void drawSpikes(SpriteBatch b,StageRuntime s,float u) {
        for(StageDefinition.Hazard h:s.definition.hazards) {
            float x=h.x*u,y=h.y*u,w=h.width*u,height=h.height*u;
            TextureRegion r=region("spikes_"+h.direction);
            float rw=Math.max(w,r.getRegionWidth()*.42f),rh=Math.max(height,r.getRegionHeight()*.42f);
            b.draw(r,x-(rw-w)*.5f,y-(rh-height)*.5f,rw,rh);
        }
    }

    private void drawMovers(SpriteBatch b,StageRuntime s,float u) {
        for(StageDefinition.MineMover m:s.definition.mineMovers) {
            if(m.platform>=0) {
                StageDefinition.Block p=s.definition.platforms[m.platform];
                TextureRegion r=region("lift".equals(m.kind)?"lift":"minecart");
                float w=p.width*u,h=w*r.getRegionHeight()/r.getRegionWidth();
                b.draw(r,p.x*u,(p.y+p.height)*u-h,w,h);
            } else if(m.anchor>=0) {
                TextureRegion r=region("moving_anchor");float w=1.12f*u,h=1.28f*u;
                b.draw(r,m.x*u-w*.5f,m.y*u-h*.58f,w,h);
            }
        }
    }

    private void drawCrushers(SpriteBatch b,StageRuntime s,float u) {
        for(StageDefinition.MineCrusher c:s.definition.mineCrushers) {
            boolean upper="down".equals(c.direction);
            String name=upper?(c.extension>.08f?"crusher_upper_extended":"crusher_upper_retracted"):
                (c.extension>.08f?"crusher_lower_extended":"crusher_lower_retracted");
            TextureRegion r=region(name);float w=Math.max(c.width*u,r.getRegionWidth()*.45f),h=w*r.getRegionHeight()/r.getRegionWidth();
            float top=c.topY*u;
            float slide=c.extension*u;
            // 실제 압착 길이를 표시해 바닥에 닿는 판정이 공중의 그림 아래로 숨지 않게 한다.
            if(slide>0) {
                b.setColor(.6f,.65f,.7f,1);
                b.draw(region("solid_fill"),c.x*u,upper?top-slide:top,c.width*u,slide);
                b.setColor(Color.WHITE);
            }
            b.draw(r,c.x*u+(c.width*u-w)*.5f,upper?top-slide:top+slide-h,w,h);
        }
    }

    private void drawElectricBarriers(SpriteBatch b,StageRuntime s,float u) {
        for(StageDefinition.CityBarrier barrier:s.definition.cityBarriers) {
            TextureRegion up=assets.region("city_v2_electric_emitter_up");
            TextureRegion down=assets.region("city_v2_electric_emitter_down");
            float x=barrier.x*u,y=barrier.y*u,w=barrier.width*u,h=barrier.height*u;
            boolean off="off".equals(barrier.state);
            if(!off) {
                TextureRegion arc=assets.region("city_v2_electric_arc");
                b.setColor(1,1,1,"warning".equals(barrier.state)?.35f:.76f);
                b.draw(arc,x-w*.35f,y,w*1.7f,h);
            }
            float ew=Math.max(w*3,.7f*u);b.setColor(Color.WHITE);
            if(off) {
                float uh=ew*emitterUpOff.getRegionHeight()/emitterUpOff.getRegionWidth();
                float dh=ew*emitterDownOff.getRegionHeight()/emitterDownOff.getRegionWidth();
                b.draw(emitterDownOff,x+w*.5f-ew*.5f,y-dh*.15f,ew,dh);
                b.draw(emitterUpOff,x+w*.5f-ew*.5f,y+h-uh*.85f,ew,uh);
            } else {
                float eh=ew*up.getRegionHeight()/up.getRegionWidth();
                b.draw(down,x+w*.5f-ew*.5f,y-eh*.8f,ew,eh);
                b.draw(up,x+w*.5f-ew*.5f,y+h-eh*.2f,ew,eh);
            }
            b.setColor(Color.WHITE);
        }
    }

    private void drawSaws(SpriteBatch b,StageRuntime s,float u) {
        for(StageDefinition.MineSaw saw:s.definition.mineSaws) {
            TextureRegion r=region(((int)(saw.angle/180)%2)==0?"saw_00":"saw_01");
            float d=saw.radius*2.5f*u;
            b.draw(r,saw.x*u-d*.5f,saw.y*u-d*.5f,d*.5f,d*.5f,d,d,1,1,saw.angle);
        }
    }

    private void drawRechargeRings(SpriteBatch b,StageRuntime s,float u) {
        for(int i=0;i<s.definition.rechargeRings.length;i++) {
            StageDefinition.RechargeRing ring=s.definition.rechargeRings[i];
            TextureRegion r=region("recharge_ring");float w=.92f*u,h=w*r.getRegionHeight()/r.getRegionWidth();
            b.setColor(1,1,1,s.mineDevices.recharged(i)?.36f:1);
            b.draw(r,ring.x*u-w*.5f,ring.y*u-h*.5f,w,h);b.setColor(Color.WHITE);
        }
    }

    private void drawCheckpoints(SpriteBatch b,StageRuntime s,float u) {
        for(int i=0;i<s.definition.checkpoints.length;i++) {
            StageDefinition.Point p=s.definition.checkpoints[i];
            b.setColor(1,1,1,s.noCheckpoint?.25f:1);
            b.draw(assets.region(i<=s.checkpoint?"checkpoint_on":"checkpoint_off"),p.x*u-47,p.y*u,94,122);
            b.setColor(Color.WHITE);
        }
    }
}
