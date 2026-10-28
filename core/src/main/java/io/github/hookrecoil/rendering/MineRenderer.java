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
import com.badlogic.gdx.math.MathUtils;
import io.github.hookrecoil.assets.CaveAssets;
import io.github.hookrecoil.stage.StageDefinition;
import io.github.hookrecoil.stage.StageRuntime;

public final class MineRenderer {
    private final CaveAssets assets;
    public MineRenderer(CaveAssets assets){this.assets=assets;}
    private TextureRegion region(String name){return assets.region("mine_"+name);}

    public void draw(SpriteBatch batch,StageRuntime stage) {
        float u=stage.config.pixelsPerH;
        drawRails(batch,stage,u);
        drawTerrain(batch,stage,u);
        drawSpikes(batch,stage,u);
        drawCrushers(batch,stage,u);
        drawSaws(batch,stage,u);
        drawMovers(batch,stage,u);
        drawRechargeRings(batch,stage,u);
        drawCheckpoints(batch,stage,u);
    }

    private void drawRails(SpriteBatch b,StageRuntime s,float u) {
        for(StageDefinition.MineMover m:s.definition.mineMovers) {
            float x0=Math.min(m.fromX,m.toX)*u,y0=Math.min(m.fromY,m.toY)*u;
            float x1=Math.max(m.fromX,m.toX)*u,y1=Math.max(m.fromY,m.toY)*u;
            if(Math.abs(m.fromX-m.toX)>.01f) {
                TextureRegion rail=region("guide_rail");float h=(x1-x0)*rail.getRegionHeight()/rail.getRegionWidth();
                b.setColor(.78f,.69f,.52f,.55f);b.draw(rail,x0,(m.fromY+m.toY)*u*.5f-h*.5f,x1-x0,h);b.setColor(Color.WHITE);
            } else if(Math.abs(m.fromY-m.toY)>.01f) {
                TextureRegion rail=region("guide_rail");float h=(y1-y0),w=h*rail.getRegionWidth()/rail.getRegionHeight();
                b.setColor(.78f,.69f,.52f,.55f);b.draw(rail,(m.fromX*u)-w*.5f,y0,w,h);b.setColor(Color.WHITE);
            }
        }
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
            // 각 모듈은 맵에서 지정한 개별 조각으로 사용한다. 충돌면은 강철 상단이다.
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
            float slide=Math.min(c.extension*u,Math.max(0,h*.45f));
            b.draw(r,c.x*u+(c.width*u-w)*.5f,upper?top-h-slide:top+slide,w,h);
            TextureRegion lamp=assets.region("mine_warning_lamp");float lw=.42f*u,lh=lw*lamp.getRegionHeight()/lamp.getRegionWidth();
            b.setColor(1,1,1,"warning".equals(c.state)?1:.38f);
            b.draw(lamp,(c.x+c.width+.1f)*u,top-lh*.5f,lw,lh);b.setColor(Color.WHITE);
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
