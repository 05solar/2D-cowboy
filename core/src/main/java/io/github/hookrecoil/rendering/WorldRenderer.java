/* 파일: WorldRenderer.java
 * 역할: 플레이 장면 지형·장치·플레이어·보석 표시. 책임: 월드 레이어 순서와 충돌면 정렬.
 * 협력: GameAssets, CaveRenderer, MineRenderer, CityRenderer, StageRuntime. 공유 에셋은 소유하지 않는다.
 * 관련 사양: docs/10_PROLOGUE.md, docs/12_CAVE_CHAPTER.md, docs/assets/city_chapter/STAGES_11_15.md.
 */
package io.github.hookrecoil.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import io.github.hookrecoil.assets.GameAssets;
import io.github.hookrecoil.stage.*;

public class WorldRenderer {
    private final GameAssets a;
    private final PlayerRenderer player;
    private final GemRenderer gems;
    public CaveRenderer cave;
    public MineRenderer mine;
    public CityRenderer city;
    public WorldRenderer(GameAssets a) {this.a=a;player=new PlayerRenderer(a);gems=new GemRenderer(a.gems);}
    public void draw(SpriteBatch batch,StageRuntime s,float alpha) {
        float u=s.config.pixelsPerH;
        if(s.definition.id.startsWith("city_")&&city!=null)city.draw(batch,s);
        else if(s.definition.id.startsWith("mine_")&&mine!=null)mine.draw(batch,s);
        else if(cave!=null&&!"prologue".equals(s.definition.id))cave.draw(batch,s);else {
        for(StageDefinition.Block b:s.definition.platforms) {
            // 상단 타일의 내부 유효 면을 기준으로 충돌선과 맞춘다. 끝 조각은 너비만 잘라서 채운다.
            float left=b.x*u,right=(b.x+b.width)*u,top=(b.y+b.height)*u;
            for(float x=left;x<right;x+=260) {
                String tile=x==left?"top_left":(x+260>=right?"top_right":"top");
                TextureRegion r=a.frame("tiles",tile);
                float w=Math.min(268,right-x);
                batch.draw(r,Math.round(x),Math.round(top-249),w,258);
                for(float y=top-249-250;y>-300;y-=250)
                    batch.draw(a.frame("tiles","fill"),Math.round(x),Math.round(y),w,256);
            }
        }
        for(StageDefinition.Block b:s.definition.ceilings) {
            for(float x=b.x*u;x<(b.x+b.width)*u;x+=300) {
                TextureRegion ceiling=a.frame("terrain","ceiling_00");
                batch.draw(ceiling,Math.round(x),Math.round(b.y*u),304,182);
                batch.draw(a.frame("tiles","fill"),Math.round(x),Math.round(b.y*u+174),304,256);
            }
        }
        for(int i=0;i<s.definition.anchors.length;i++) {
            StageDefinition.Point p=s.definition.anchors[i];
            boolean active=i==s.grapple.attached || i==s.grapple.aimTarget(s.player);
            TextureRegion ring=a.frame("objects",active?"anchor_active":"anchor_inactive");
            batch.draw(ring,Math.round(p.x*u-58),Math.round(p.y*u-63),116,157);
        }
        for(int i=0;i<s.definition.checkpoints.length;i++) {
            StageDefinition.Point p=s.definition.checkpoints[i];
            batch.draw(a.frame("objects",i<=s.checkpoint?"checkpoint_on":"checkpoint_off"),p.x*u-67,p.y*u,135,179);
        }
        }
        for(StageDefinition.Gem g:s.definition.optionalGems)if(!s.optionalCollected.contains(g.id)) {
            gems.optional(batch,s,g.x,g.y);
        }
        if(s.grapple.attached>=0 || s.grapple.flying) {
            float x=s.player.x*u,y=(s.player.y+s.config.height*0.65f)*u;
            float dx=s.grapple.hookX*u-x,dy=s.grapple.hookY*u-y;
            float len=(float)Math.sqrt(dx*dx+dy*dy),angle=MathUtils.atan2(dy,dx)*MathUtils.radiansToDegrees;
            TextureRegion rope=a.frame("objects","rope");
            batch.setColor(0.85f,0.77f,0.55f,1);
            batch.draw(rope,x,y-6,0,6,len,12,1,1,angle);
            batch.setColor(Color.WHITE);
            if(s.grapple.flying) batch.draw(a.frame("objects","hook"),s.grapple.hookX*u-24,s.grapple.hookY*u-15,24,15,70,47,1,1,angle);
        }
        gems.behind(batch,s);
        if(s.clear.frozen())gems.character(batch,s);else player.draw(batch,s,alpha);
        gems.front(batch,s);
    }
}
