/* 파일: GemAssets.java
 * 역할: 보석 묶음 로드. 책임: 실측 rect·피벗·소켓·시간, 독립 효과의 필터 보존.
 * 협력: GemRenderer, GameAssets. 수명: GameAssets가 해제. 관련 사양: assets/gem_pickup/APPLY.md.
 */
package io.github.hookrecoil.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.*;

public class GemAssets implements Disposable {
    public static class Pose {
        public TextureRegion normal,flipped;
        public int[] pivot,socket;
        public int duration;
    }
    public static class Effect {public Texture texture;public int[] visible;}
    public final Pose[] poses=new Pose[8];
    public final ObjectMap<String,Effect> effects=new ObjectMap<>();
    private final Array<Texture> owned=new Array<>();
    public GemAssets() {
        JsonValue images=new JsonReader().parse(Gdx.files.internal("gem_pickup/atlas.json")).get("images");
        for(JsonValue entry=images.child;entry!=null;entry=entry.next) {
            Texture texture=new Texture(Gdx.files.internal("gem_pickup/"+entry.getString("file")),false);owned.add(texture);
            Texture.TextureFilter filter="linear".equals(entry.getString("recommended_filter"))?Texture.TextureFilter.Linear:Texture.TextureFilter.Nearest;
            texture.setFilter(filter,filter);
            if(!entry.name.equals("player_collect")) {
                Effect e=new Effect();e.texture=texture;e.visible=entry.get("visible_bbox_alpha_gt_32").asIntArray();effects.put(entry.name,e);
            } else {
                int i=0,total=0;
                for(JsonValue frame=entry.get("frames").child;frame!=null;frame=frame.next) {
                    int[] r=frame.get("rect").asIntArray();
                    if(i>=8||r[0]<0||r[1]<0||r[0]+r[2]>texture.getWidth()||r[1]+r[3]>texture.getHeight())throw new IllegalArgumentException("Invalid collection atlas");
                    Pose p=new Pose();p.normal=new TextureRegion(texture,r[0],r[1],r[2],r[3]);
                    p.flipped=new TextureRegion(p.normal);p.flipped.flip(true,false);
                    p.pivot=frame.get("pivot_px").asIntArray();
                    p.socket=frame.get("gem_socket_px").isNull()?null:frame.get("gem_socket_px").asIntArray();
                    p.duration=frame.getInt("duration_ms");total+=p.duration;poses[i++]=p;
                }
                if(i!=8||total!=1000)throw new IllegalArgumentException("Expected eight poses / one second");
            }
        }
    }
    @Override public void dispose(){for(Texture t:owned)t.dispose();}
}
