/* 파일: GameAssets.java
 * 역할: V3 아틀라스와 보석 에셋 수명 관리. 책임: rect·알파 보존, 반전 복사본 생성.
 * 협력: WorldRenderer, PlayerRenderer. 수명: 화면 종료 시 dispose. 관련 사양: docs/assets/TUTORIAL_V3_README.md.
 */
package io.github.hookrecoil.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.*;

public class GameAssets implements Disposable {
    public final GemAssets gems=new GemAssets();
    private final Array<Texture> textures=new Array<>();
    private final ObjectMap<String,TextureRegion> frames=new ObjectMap<>();
    public final Texture background;
    public GameAssets() {
        JsonValue root=new JsonReader().parse(Gdx.files.internal("tutorial/atlas.json"));
        for(JsonValue atlas=root.get("atlases").child;atlas!=null;atlas=atlas.next) {
            Texture texture=load("tutorial/"+atlas.getString("image"));
            for(JsonValue frame=atlas.get("frames").child;frame!=null;frame=frame.next) {
                int[] r=frame.get("rect").asIntArray();
                if(r[0]+r[2]>texture.getWidth() || r[1]+r[3]>texture.getHeight())
                    throw new IllegalArgumentException("Atlas rect out of bounds: "+frame.name);
                TextureRegion region=new TextureRegion(texture,r[0],r[1],r[2],r[3]);
                frames.put(atlas.name+"/"+frame.name,region);
                TextureRegion flipped=new TextureRegion(region);flipped.flip(true,false);
                frames.put(atlas.name+"/"+frame.name+"/flipped",flipped);
            }
        }
        background=load("tutorial/"+root.get("background").getString("image"));
    }
    private Texture load(String path) {
        Texture t=new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);textures.add(t);return t;
    }
    public TextureRegion frame(String group,String name) { return frames.get(group+"/"+name); }
    public TextureRegion frame(String group,String name,boolean flipped) { return frames.get(group+"/"+name+(flipped?"/flipped":"")); }
    @Override public void dispose() { for(Texture t:textures)t.dispose();gems.dispose(); }
}
