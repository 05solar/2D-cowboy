/*
 * 파일: CityRenderer.java
 * 역할: 지하도시 스테이지의 전경 지형과 장치 아트를 렌더링한다.
 * 주요 책임: 고정 발판·가시·이동 발판·링·체크포인트를 구분해 그리고 실제 충돌 데이터만 따른다.
 * 협력 대상: CaveAssets, StageRuntime, StageDefinition.
 * 수명: GameplayScreen이 생성하며 텍스처 소유자인 CaveAssets는 화면 종료 시 해제한다.
 * 관련 사양: docs/assets/city_chapter/STAGES_11_15.md, docs/assets/city_chapter/DEVICES_AND_MOTION.md.
 */
package io.github.hookrecoil.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.hookrecoil.assets.CaveAssets;
import io.github.hookrecoil.stage.StageDefinition;
import io.github.hookrecoil.stage.StageRuntime;

/** Draws the supplied city art without inferring collision from image bounds. */
public final class CityRenderer {
    private final CaveAssets assets;
    public CityRenderer(CaveAssets assets){this.assets=assets;}
    private TextureRegion region(String id){return assets.region("city_"+id);}

    public void draw(SpriteBatch batch,StageRuntime stage) {
        float u=stage.config.pixelsPerH;
        for(StageDefinition.MineMover mover:stage.definition.mineMovers)drawRail(batch,mover,u);
        for(int i=0;i<stage.definition.platforms.length;i++) {
            StageDefinition.Block block=stage.definition.platforms[i];
            if((!block.active&&!block.crumble)||isMovingPlatform(stage,block))continue;
            String sprite=block.crumble?"breakable_"+stage.devices.frame(i):(block.sprite==null?"platform_medium":block.sprite);
            TextureRegion image=region(sprite);float width=block.width*u;
            float height=width*image.getRegionHeight()/image.getRegionWidth();
            batch.draw(image,block.x*u,(block.y+block.height)*u-height,width,height);
        }
        for(StageDefinition.Block ceiling:stage.definition.ceilings) {
            TextureRegion image=region("platform_long");float width=ceiling.width*u;
            float height=width*image.getRegionHeight()/image.getRegionWidth();
            batch.draw(image,ceiling.x*u,(ceiling.y+ceiling.height)*u-height,width,height);
        }
        for(StageDefinition.CityStructure structure:stage.definition.cityStructures) {
            TextureRegion image=region(structure.sprite);
            float fit=Math.min(structure.width*u/image.getRegionWidth(),structure.height*u/image.getRegionHeight());
            float width=image.getRegionWidth()*fit,height=image.getRegionHeight()*fit;
            batch.draw(image,structure.x*u+(structure.width*u-width)*.5f,structure.y*u+(structure.height*u-height)*.5f,width,height);
        }
        for(StageDefinition.Hazard hazard:stage.definition.hazards) {
            TextureRegion image=region("spikes_"+hazard.direction);
            batch.draw(image,hazard.x*u,hazard.y*u,hazard.width*u,hazard.height*u);
        }
        drawWindAndBarriers(batch,stage,u);
        for(StageDefinition.MineMover mover:stage.definition.mineMovers)drawMover(batch,stage,mover,u);
        for(StageDefinition.Point anchor:stage.definition.anchors) {
            TextureRegion image=region("moving_anchor");float w=.9f*u,h=w*image.getRegionHeight()/image.getRegionWidth();
            batch.draw(image,anchor.x*u-w*.5f,anchor.y*u-h*.5f,w,h);
        }
        for(int i=0;i<stage.definition.checkpoints.length;i++) {
            StageDefinition.Point point=stage.definition.checkpoints[i];
            TextureRegion image=assets.region(i<=stage.checkpoint?"checkpoint_on":"checkpoint_off");
            batch.setColor(1,1,1,stage.noCheckpoint?.28f:1);
            batch.draw(image,point.x*u-47,point.y*u,94,122);batch.setColor(Color.WHITE);
        }
        for(int i=0;i<stage.definition.rechargeRings.length;i++) {
            StageDefinition.RechargeRing ring=stage.definition.rechargeRings[i];
            TextureRegion image=region(stage.mineDevices.recharged(i)?"recharge_spent":"recharge_active");
            float w=.82f*u,h=w*image.getRegionHeight()/image.getRegionWidth();
            batch.draw(image,ring.x*u-w*.5f,ring.y*u-h*.5f,w,h);
        }
    }

    private void drawWindAndBarriers(SpriteBatch batch,StageRuntime stage,float u) {
        for(StageDefinition.CityWindZone zone:stage.definition.cityWindZones) {
            TextureRegion arrow=region("wind_arrow");
            float width=zone.width*u,height=Math.min(zone.height*u*.28f,width*arrow.getRegionHeight()/arrow.getRegionWidth());
            batch.setColor(.73f,.82f,.86f,.48f);batch.draw(arrow,zone.x*u,(zone.y+zone.height*.5f)*u-height*.5f,width,height);
            batch.setColor(Color.WHITE);
        }
        for(StageDefinition.CityBarrier barrier:stage.definition.cityBarriers) {
            TextureRegion arc=region("electric_arc"),up=region("electric_emitter_up"),down=region("electric_emitter_down");
            float x=barrier.x*u,y=barrier.y*u,w=barrier.width*u,h=barrier.height*u;
            if(!"off".equals(barrier.state)) {
                batch.setColor(1,1,1,"warning".equals(barrier.state)?.46f:.82f);
                batch.draw(arc,x-w*.25f,y,w*1.5f,h);
            }
            float ew=Math.max(w*2,.7f*u),eh=ew*up.getRegionHeight()/up.getRegionWidth();
            batch.setColor(Color.WHITE);batch.draw(down,x+w*.5f-ew*.5f,y-eh*.85f,ew,eh);
            batch.draw(up,x+w*.5f-ew*.5f,y+h-eh*.15f,ew,eh);
            if("warning".equals(barrier.state)) {
                TextureRegion lamp=region("warning_lamp");float lw=.4f*u,lh=lw*lamp.getRegionHeight()/lamp.getRegionWidth();
                batch.draw(lamp,x+ew*.55f,y+h*.5f-lh*.5f,lw,lh);
            }
            batch.setColor(Color.WHITE);
        }
    }

    private boolean isMovingPlatform(StageRuntime stage,StageDefinition.Block block) {
        for(StageDefinition.MineMover mover:stage.definition.mineMovers)
            if(mover.platform>=0&&stage.definition.platforms[mover.platform]==block)return true;
        return false;
    }

    private void drawRail(SpriteBatch batch,StageDefinition.MineMover mover,float u) {
        float x0=mover.fromX*u,y0=mover.fromY*u,x1=mover.toX*u,y1=mover.toY*u;
        TextureRegion rail=region("guide_rail");batch.setColor(.72f,.78f,.82f,.42f);
        if(Math.abs(x1-x0)>Math.abs(y1-y0)) {
            float width=Math.abs(x1-x0),height=width*rail.getRegionHeight()/rail.getRegionWidth();
            batch.draw(rail,Math.min(x0,x1),Math.min(y0,y1)-height*.5f,width,height);
        } else {
            float height=Math.abs(y1-y0),width=height*rail.getRegionWidth()/rail.getRegionHeight();
            batch.draw(rail,Math.min(x0,x1)-width*.5f,Math.min(y0,y1),width,height);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawMover(SpriteBatch batch,StageRuntime stage,StageDefinition.MineMover mover,float u) {
        if(mover.platform>=0) {
            StageDefinition.Block platform=stage.definition.platforms[mover.platform];
            TextureRegion image=region(Math.abs(mover.toX-mover.fromX)>.01f?"moving_horizontal":"moving_vertical");
            float width=platform.width*u,height=width*image.getRegionHeight()/image.getRegionWidth();
            batch.draw(image,platform.x*u,(platform.y+platform.height)*u-height,width,height);
        } else if(mover.anchor>=0) {
            TextureRegion image=region("moving_anchor");float width=.92f*u,height=width*image.getRegionHeight()/image.getRegionWidth();
            batch.draw(image,mover.x*u-width*.5f,mover.y*u-height*.5f,width,height);
        }
    }
}
