/*
 * 파일: CityRenderer.java
 * 역할: 지하도시 V2의 열린 프레임·얇은 보행판·장치를 렌더링한다.
 * 주요 책임: atlas rect 비율을 유지하고 충돌 데이터와 시각 상태를 분리해 표시한다.
 * 협력 대상: CaveAssets, StageRuntime, StageDefinition.
 * 수명: GameplayScreen이 소유하고 CaveAssets의 공유 텍스처는 해제하지 않는다.
 * 관련 사양: docs/CITY_CHAPTER_V2.md, assets/city_chapter/v2_atlas.json.
 */
package io.github.hookrecoil.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.hookrecoil.assets.CaveAssets;
import io.github.hookrecoil.stage.StageDefinition;
import io.github.hookrecoil.stage.StageRuntime;

/** Renders V2 artwork at authored collider surfaces without deriving collision from pixels. */
public final class CityRenderer {
    private final CaveAssets assets;
    private final TextureRegion emitterUpOff,emitterDownOff;
    public CityRenderer(CaveAssets assets){
        this.assets=assets;
        TextureRegion up=assets.region("city_v2_electric_emitter_up"),down=assets.region("city_v2_electric_emitter_down");
        int upCut=Math.round(up.getRegionHeight()*.58f),downHeight=Math.round(down.getRegionHeight()*.43f);
        emitterUpOff=new TextureRegion(up,0,upCut,up.getRegionWidth(),up.getRegionHeight()-upCut);
        emitterDownOff=new TextureRegion(down,0,0,down.getRegionWidth(),downHeight);
    }
    private TextureRegion region(String id){return assets.region("city_v2_"+id);}

    public void draw(SpriteBatch batch,StageRuntime stage) {
        float u=stage.config.pixelsPerH;
        drawRails(batch,stage,u);
        drawPlatforms(batch,stage,u);
        drawStructures(batch,stage,u);
        drawHazards(batch,stage,u);
        drawCrushers(batch,stage,u);
        drawSaws(batch,stage,u);
        drawWindAndBarriers(batch,stage,u);
        drawMovers(batch,stage,u);
        drawAnchors(batch,stage,u);
        drawCheckpoints(batch,stage,u);
        drawRechargeRings(batch,stage,u);
    }

    private void drawPlatforms(SpriteBatch batch,StageRuntime stage,float u) {
        for(int i=0;i<stage.definition.platforms.length;i++) {
            StageDefinition.Block block=stage.definition.platforms[i];
            if(!block.active||isMovingPlatform(stage,block))continue;
            String sprite=block.crumble?breakableFrame(stage,i):(block.sprite==null?"concrete_medium":block.sprite);
            drawAtSurface(batch,region(sprite),block.x,block.y+block.height,block.width,u);
        }
        for(StageDefinition.Block beam:stage.definition.ceilings)
            drawAtSurface(batch,region("long_truss"),beam.x,beam.y+beam.height,beam.width,u);
    }

    private String breakableFrame(StageRuntime stage,int index) {
        String state=stage.devices.frame(index);
        if("warning".equals(state))return "breakable_warning";
        if("collapse".equals(state))return "breakable_collapse";
        if("gone".equals(state))return "breakable_collapse";
        return "breakable_intact";
    }

    private void drawAtSurface(SpriteBatch batch,TextureRegion image,float x,float top,float width,float u) {
        float w=width*u,h=w*image.getRegionHeight()/image.getRegionWidth();
        batch.draw(image,x*u,top*u-h,w,h);
    }

    private void drawStructures(SpriteBatch batch,StageRuntime stage,float u) {
        for(StageDefinition.CityStructure structure:stage.definition.cityStructures) {
            TextureRegion image=region(structure.sprite);
            float scale=Math.min(structure.width*u/image.getRegionWidth(),structure.height*u/image.getRegionHeight());
            float w=image.getRegionWidth()*scale,h=image.getRegionHeight()*scale;
            batch.draw(image,structure.x*u+(structure.width*u-w)*.5f,structure.y*u+(structure.height*u-h)*.5f,w,h);
        }
    }

    private void drawHazards(SpriteBatch batch,StageRuntime stage,float u) {
        for(StageDefinition.Hazard hazard:stage.definition.hazards)
            batch.draw(region("spikes_"+hazard.direction),hazard.x*u,hazard.y*u,hazard.width*u,hazard.height*u);
    }

    private void drawRails(SpriteBatch batch,StageRuntime stage,float u) {
        for(StageDefinition.MineMover mover:stage.definition.mineMovers) {
            if(mover.platform<0)continue;
            if(mover.waypoints!=null&&mover.waypoints.length>1) {
                TextureRegion rail=region("rail_horizontal");batch.setColor(.77f,.82f,.85f,.54f);
                for(int i=0;i<mover.waypoints.length;i++) {
                    float[] a=mover.waypoints[i],b=mover.waypoints[(i+1)%mover.waypoints.length];
                    boolean horizontal=Math.abs(b[0]-a[0])>=Math.abs(b[1]-a[1]);
                    rail=region(horizontal?"rail_horizontal":"rail_vertical");
                    if(horizontal) {
                        float width=Math.abs(b[0]-a[0])*u,height=width*rail.getRegionHeight()/rail.getRegionWidth();
                        batch.draw(rail,Math.min(a[0],b[0])*u,(a[1]+b[1])*.5f*u-height*.5f,width,height);
                    } else {
                        float height=Math.abs(b[1]-a[1])*u,width=height*rail.getRegionWidth()/rail.getRegionHeight();
                        batch.draw(rail,(a[0]+b[0])*.5f*u-width*.5f,Math.min(a[1],b[1])*u,width,height);
                    }
                }
                batch.setColor(Color.WHITE);continue;
            }
            float x0=mover.fromX*u,y0=mover.fromY*u,x1=mover.toX*u,y1=mover.toY*u;
            boolean horizontal=Math.abs(x1-x0)>=Math.abs(y1-y0);
            TextureRegion rail=region(horizontal?"rail_horizontal":"rail_vertical");
            batch.setColor(.77f,.82f,.85f,.54f);
            if(horizontal) {
                float width=Math.abs(x1-x0),height=width*rail.getRegionHeight()/rail.getRegionWidth();
                batch.draw(rail,Math.min(x0,x1),Math.min(y0,y1)-height*.5f,width,height);
            } else {
                float height=Math.abs(y1-y0),width=height*rail.getRegionWidth()/rail.getRegionHeight();
                batch.draw(rail,Math.min(x0,x1)-width*.5f,Math.min(y0,y1),width,height);
            }
            batch.setColor(Color.WHITE);
        }
    }

    private void drawWindAndBarriers(SpriteBatch batch,StageRuntime stage,float u) {
        for(StageDefinition.CityWindZone zone:stage.definition.cityWindZones) {
            TextureRegion fan=region(((int)(stage.elapsed*12)%2)==0?"fan_on_00":"fan_on_01");float size=Math.min(zone.width,zone.height)*.38f*u;
            float x=(zone.x+zone.width*.5f)*u-size*.5f,y=(zone.y+zone.height*.5f)*u-size*.5f;
            batch.setColor(1,1,1,.72f);batch.draw(fan,x,y,size*.5f,size*.5f,size,size,1,1,(stage.elapsed*540f)%360f);
            TextureRegion arrow=region("wind_arrow");float width=zone.width*u,height=Math.min(zone.height*u*.24f,width*arrow.getRegionHeight()/arrow.getRegionWidth());
            batch.setColor(.8f,.88f,.9f,.53f);batch.draw(arrow,zone.x*u,(zone.y+zone.height*.5f)*u-height*.5f,width,height);
            batch.setColor(Color.WHITE);
        }
        for(StageDefinition.CityBarrier barrier:stage.definition.cityBarriers) {
            TextureRegion up=region("electric_emitter_up"),down=region("electric_emitter_down"),arc=region("electric_arc");
            float x=barrier.x*u,y=barrier.y*u,w=barrier.width*u,h=barrier.height*u;
            boolean off="off".equals(barrier.state);
            if(!off) {
                batch.setColor(1,1,1,"warning".equals(barrier.state)?.35f:.76f);
                batch.draw(arc,x-w*.35f,y,w*1.7f,h);
            }
            float ew=Math.max(w*3,.7f*u);
            batch.setColor(Color.WHITE);
            if(off) {
                float upH=ew*emitterUpOff.getRegionHeight()/emitterUpOff.getRegionWidth();
                float downH=ew*emitterDownOff.getRegionHeight()/emitterDownOff.getRegionWidth();
                batch.draw(emitterDownOff,x+w*.5f-ew*.5f,y-downH*.15f,ew,downH);
                batch.draw(emitterUpOff,x+w*.5f-ew*.5f,y+h-upH*.85f,ew,upH);
            } else {
                float eh=ew*up.getRegionHeight()/up.getRegionWidth();
                batch.draw(down,x+w*.5f-ew*.5f,y-eh*.8f,ew,eh);batch.draw(up,x+w*.5f-ew*.5f,y+h-eh*.2f,ew,eh);
            }
            batch.setColor(Color.WHITE);
        }
    }

    private void drawMovers(SpriteBatch batch,StageRuntime stage,float u) {
        for(StageDefinition.MineMover mover:stage.definition.mineMovers) {
            if(mover.platform>=0) {
                StageDefinition.Block platform=stage.definition.platforms[mover.platform];
                String sprite=Math.abs(mover.vx)>=Math.abs(mover.vy)?"moving_horizontal":"moving_vertical";
                drawAtSurface(batch,region(sprite),platform.x,platform.y+platform.height,platform.width,u);
            } else if(mover.anchor>=0) {
                TextureRegion image=region("moving_anchor");float w=.9f*u,h=w*image.getRegionHeight()/image.getRegionWidth();
                batch.draw(image,mover.x*u-w*.5f,mover.y*u-h*.5f,w,h);
            }
        }
    }

    private void drawAnchors(SpriteBatch batch,StageRuntime stage,float u) {
        for(StageDefinition.Point anchor:stage.definition.anchors) {
            TextureRegion image=region("fixed_anchor");float w=.62f*u,h=w*image.getRegionHeight()/image.getRegionWidth();
            batch.draw(image,anchor.x*u-w*.5f,anchor.y*u-h*.5f,w,h);
        }
    }

    private void drawCheckpoints(SpriteBatch batch,StageRuntime stage,float u) {
        for(int i=0;i<stage.definition.checkpoints.length;i++) {
            StageDefinition.Point point=stage.definition.checkpoints[i];
            TextureRegion image=assets.region(i<=stage.checkpoint?"checkpoint_on":"checkpoint_off");
            batch.setColor(1,1,1,stage.noCheckpoint?.28f:1);batch.draw(image,point.x*u-47,point.y*u,94,122);batch.setColor(Color.WHITE);
        }
    }

    private void drawRechargeRings(SpriteBatch batch,StageRuntime stage,float u) {
        for(int i=0;i<stage.definition.rechargeRings.length;i++) {
            StageDefinition.RechargeRing ring=stage.definition.rechargeRings[i];
            TextureRegion image=region(stage.mineDevices.recharged(i)?"recharge_spent":"recharge_active");
            float w=.82f*u,h=w*image.getRegionHeight()/image.getRegionWidth();batch.draw(image,ring.x*u-w*.5f,ring.y*u-h*.5f,w,h);
        }
    }

    private void drawCrushers(SpriteBatch batch,StageRuntime stage,float u) {
        for(StageDefinition.MineCrusher crusher:stage.definition.mineCrushers) {
            boolean upper="down".equals(crusher.direction);String name=upper?(crusher.extension>.05f?"crusher_upper_extended":"crusher_upper_retracted"):
                (crusher.extension>.05f?"crusher_lower_extended":"crusher_lower_retracted");
            TextureRegion image=region(name);float width=Math.max(crusher.width*u,image.getRegionWidth()*.36f),height=width*image.getRegionHeight()/image.getRegionWidth();
            float offset=Math.min(crusher.extension*u,height*.55f);float y=upper?crusher.topY*u-height-offset:crusher.topY*u+offset;
            batch.draw(image,crusher.x*u+(crusher.width*u-width)*.5f,y,width,height);
        }
    }

    private void drawSaws(SpriteBatch batch,StageRuntime stage,float u) {
        for(StageDefinition.MineSaw saw:stage.definition.mineSaws) {
            TextureRegion image=region(((int)(saw.angle/180)%2)==0?"saw_00":"saw_01");float d=saw.radius*2*u;
            batch.draw(image,saw.x*u-d*.5f,saw.y*u-d*.5f,d*.5f,d*.5f,d,d,1,1,saw.angle);
        }
    }

    private boolean isMovingPlatform(StageRuntime stage,StageDefinition.Block block) {
        for(StageDefinition.MineMover mover:stage.definition.mineMovers)
            if(mover.platform>=0&&stage.definition.platforms[mover.platform]==block)return true;
        return false;
    }
}
