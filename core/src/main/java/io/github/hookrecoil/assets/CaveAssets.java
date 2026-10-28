/* 파일: CaveAssets.java
 * 역할: 동굴·광산 챕터 에셋 로더. 책임: 실측 아틀라스 추출·배경 지연 로드·ClampToEdge.
 * 협력: CaveRenderer, MineRenderer, StageSelectView. 수명: GameplayScreen이 해제. 관련 사양: docs/12_CAVE_CHAPTER.md.
 */
package io.github.hookrecoil.assets;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.*;
import java.util.*;
public class CaveAssets implements Disposable {
    private final Map<String,TextureRegion> regions=new HashMap<>();
    private final Map<String,Texture> backgrounds=new HashMap<>();
    private final List<Texture> sheets=new ArrayList<>();
    public CaveAssets(){
        loadAtlas("cave_chapter/atlas.json","cave_chapter/","");
        loadAtlas("mine_chapter/atlas.json","mine_chapter/","mine_");
    }
    private void loadAtlas(String atlasPath,String directory,String prefix) {
        JsonValue root=new JsonReader().parse(Gdx.files.internal(atlasPath));
        for(JsonValue sheet:root.get("sheets")){
            Texture t=new Texture(Gdx.files.internal(directory+sheet.getString("image")));sheets.add(t);
            t.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
            for(JsonValue sprite:sheet.get("sprites")){
                int[] r=sprite.get("rect").asIntArray();regions.put(prefix+sprite.name,new TextureRegion(t,r[0],r[1],r[2],r[3]));
            }
        }
    }
    public TextureRegion region(String name){TextureRegion r=regions.get(name);if(r==null)throw new IllegalArgumentException(name);return r;}
    public Texture background(String path){
        if(path==null)return null;
        String root=path.contains("mine_")?"mine_chapter/":"cave_chapter/";
        Texture t=backgrounds.get(path);if(t==null){t=new Texture(Gdx.files.internal(root+path));
            t.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);t.setWrap(Texture.TextureWrap.ClampToEdge,Texture.TextureWrap.ClampToEdge);backgrounds.put(path,t);}
        return t;
    }
    public void dispose(){for(Texture t:sheets)t.dispose();for(Texture t:backgrounds.values())t.dispose();}
}
