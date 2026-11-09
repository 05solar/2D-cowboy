/* 파일: HudView.java
 * 역할: 플레이 HUD·탄약·시간·진행·사망 대기·일시정지·완주 표시. 책임: 제목/소개는 프롤로그에만 표시한다.
 * 협력: StageRuntime, GameplayScreen. 수명: 픽셀 글꼴·흰 픽셀 자원을 소유/해제.
 * 관련 사양: docs/10_PROLOGUE.md, docs/12_CAVE_CHAPTER.md.
 */
package io.github.hookrecoil.ui;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.Disposable;
import io.github.hookrecoil.stage.*;

public class HudView implements Disposable {
    private final BitmapFont font=new BitmapFont(com.badlogic.gdx.Gdx.files.internal("ui/fonts/prologue.fnt"));
    private final Texture white;
    public HudView() {
        Pixmap pix=new Pixmap(1,1,Pixmap.Format.RGBA8888);pix.setColor(Color.WHITE);pix.fill();white=new Texture(pix);pix.dispose();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
    }
    public void box(SpriteBatch b,float x,float y,float w,float h,float r,float g,float blue,float alpha) {
        b.setColor(r,g,blue,alpha);b.draw(white,x,y,w,h);b.setColor(Color.WHITE);
    }
    public void text(SpriteBatch b,String value,float x,float y,float scale,Color color) {
        font.getData().setScale(scale*0.25f);font.setColor(color);font.draw(b,value,x,y);
    }
    private void centeredText(SpriteBatch b,String value,float centerX,float y,float scale,Color color) {
        font.getData().setScale(scale*0.25f);font.setColor(color);
        GlyphLayout layout=new GlyphLayout(font,value);font.draw(b,value,centerX-layout.width*0.5f,y);
    }
    private float run(SpriteBatch b,String value,float x,float y,float scale,Color color) {
        font.getData().setScale(scale*0.25f);font.setColor(color);
        return font.draw(b,value,x,y).width;
    }
    /** 실제로 그리지 않고 폭만 측정한다. GameplayScreen이 버튼 텍스트의 마우스 클릭 영역을 계산하는 데 쓴다. */
    public float textWidth(String value,float scale) {
        font.getData().setScale(scale*0.25f);
        return new GlyphLayout(font,value).width;
    }
    private void keyLegend(SpriteBatch b,float y,Color cream,Color muted) {
        float x=1650;
        x+=run(b,"M",x,y,2.05f,cream)+5;x+=run(b," 선택    ",x,y,1.8f,muted);
        x+=run(b,"R",x,y,2.05f,cream)+5;x+=run(b," 재시작    ",x,y,1.8f,muted);
        x+=run(b,"ESC",x,y,2.05f,cream)+5;x+=run(b," 정지    ",x,y,1.8f,muted);
        x+=run(b,"F2",x,y,2.05f,cream)+5;run(b," 음악 켜기/끄기",x,y,1.8f,muted);
    }
    public void wrap(SpriteBatch b,String value,float x,float y,float width,float scale,Color color) {
        font.getData().setScale(scale*.25f);font.setColor(color);font.draw(b,value,x,y,width,com.badlogic.gdx.utils.Align.left,true);
    }
    public void draw(SpriteBatch b,StageRuntime s,boolean paused,boolean allClear) {
        Color cream=new Color(0.94f,0.9f,0.78f,1),gold=new Color(0.93f,0.69f,0.34f,1),muted=new Color(0.61f,0.71f,0.76f,1);
        box(b,0,1260,2560,180,0.025f,0.04f,0.055f,0.9f);
        box(b,64,1295,5,89,0.9f,0.64f,0.29f,1);
        text(b,"HOOK & RECOIL",92,1384,3.2f,cream);
        boolean prologue="prologue".equals(s.definition.id);
        if(prologue)text(b,s.definition.name+(s.noCheckpoint?" / 노체크포인트":""),94,1320,1.7f,muted);
        text(b,"RECOIL",1830,1380,1.7f,gold);
        for(int i=0;i<2;i++) {
            boolean loaded=i<s.player.ammo;
            box(b,1830+i*48,1305,30,48,loaded?0.95f:0.18f,loaded?0.7f:0.23f,loaded?0.32f:0.27f,1);
        }
        text(b,String.format("%02d:%02d",(int)s.elapsed/60,(int)s.elapsed%60),2050,1382,2.7f,cream);
        text(b,"FALLS  "+s.deaths+"    CP  "+(s.checkpoint+1)+" / "+s.definition.checkpoints.length,2050,1319,1.5f,muted);
        box(b,0,0,2560,216,0.025f,0.04f,0.055f,0.94f);
        StageDefinition.Zone zone=s.definition.zones[s.zoneIndex()];
        if(prologue){
            text(b,zone.title,64,182,2.1f,gold);
            text(b,zone.hint,64,128,2f,cream);
        }
        text(b,zone.detail,64,77,1.8f,muted);
        keyLegend(b,58,cream,muted);
        if(prologue)for(int i=0;i<s.definition.zones.length;i++) box(b,64+i*2440f/s.definition.zones.length,226,2420f/s.definition.zones.length,5,i<=s.zoneIndex()?0.85f:0.17f,i<=s.zoneIndex()?0.6f:0.24f,0.28f,1);
        if(paused || s.complete) {
            box(b,0,216,2560,1044,0.015f,0.025f,0.04f,0.8f);
            if(s.complete) {
                Color orange=new Color(1f,0.36f,0.06f,1f);
                box(b,560,450,1440,540,0.045f,0.07f,0.085f,1);
                box(b,560,986,1440,4,0.9f,0.65f,0.3f,1);
                centeredText(b,allClear?"ALL CLEAR":"CLEAR",1280,875,4.8f,orange);
                if(allClear)centeredText(b,"전체 15개 스테이지를 모두 클리어했습니다",1280,805,1.7f,cream);
                text(b,"시간  "+(int)s.elapsed+"초     발사  "+s.shots+"     추락  "+s.deaths,748,740,2.5f,muted);
                if(allClear) {
                    text(b,"엔터: 처음부터 다시 시작     M 스테이지 선택",748,535,2.3f,muted);
                } else {
                    text(b,"N 다음 스테이지     M 스테이지 선택",748,535,2.3f,muted);
                }
            } else {
                box(b,650,510,1260,430,0.045f,0.07f,0.085f,1);
                box(b,650,936,1260,4,0.9f,0.65f,0.3f,1);
                text(b,"일시 정지",748,852,3.6f,cream);
                text(b,"ESC 계속하기    R 체크포인트에서 재시작",748,644,2.2f,gold);
            }
        }
        if(s.clear.frozen()&&!s.complete&&!paused)centeredText(b,"CLEAR",1280,790,10f,new Color(1f,0.36f,0.06f,1f));
        if((s.deathTimer>0||s.deathWaiting)&&!paused) {
            box(b,500,535,1560,370,0.025f,0.035f,0.045f,0.95f);
            box(b,500,901,1560,4,0.86f,0.34f,0.28f,1);
            text(b,"YOU DIED",660,790,4.6f,cream);
            text(b,"R 재시작    M 스테이지 선택    ESC 일시정지",660,675,2.7f,muted);
        }
        if(s.saveError!=null)text(b,s.saveError,64,280,1.7f,gold);
    }
    public void reticle(SpriteBatch b,float x,float y,boolean valid) {
        Color c=valid?new Color(0.9f,0.72f,0.35f,1):new Color(0.8f,0.86f,0.88f,0.7f);
        box(b,x-17,y-1,11,2,c.r,c.g,c.b,c.a);box(b,x+6,y-1,11,2,c.r,c.g,c.b,c.a);
        box(b,x-1,y-17,2,11,c.r,c.g,c.b,c.a);box(b,x-1,y+6,2,11,c.r,c.g,c.b,c.a);
        if(valid) box(b,x-3,y-3,6,6,c.r,c.g,c.b,1);
    }
    @Override public void dispose() {font.dispose();white.dispose();}
}
