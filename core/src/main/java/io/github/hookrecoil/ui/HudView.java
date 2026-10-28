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
    private float run(SpriteBatch b,String value,float x,float y,float scale,Color color) {
        font.getData().setScale(scale*0.25f);font.setColor(color);
        return font.draw(b,value,x,y).width;
    }
    private void keyLegend(SpriteBatch b,float y,Color cream,Color muted) {
        float x=1650;
        x+=run(b,"M",x,y,2.05f,cream)+5;x+=run(b," 선택    ",x,y,1.8f,muted);
        x+=run(b,"R",x,y,2.05f,cream)+5;x+=run(b," 재시작    ",x,y,1.8f,muted);
        x+=run(b,"ESC",x,y,2.05f,cream)+5;x+=run(b," 정지    ",x,y,1.8f,muted);
        x+=run(b,"F1",x,y,2.05f,cream)+5;run(b," 정보",x,y,1.8f,muted);
    }
    public void wrap(SpriteBatch b,String value,float x,float y,float width,float scale,Color color) {
        font.getData().setScale(scale*.25f);font.setColor(color);font.draw(b,value,x,y,width,com.badlogic.gdx.utils.Align.left,true);
    }
    public void draw(SpriteBatch b,StageRuntime s,boolean paused,boolean debug) {
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
        if(s.noticeTimer>0) {
            box(b,925,1100,710,70,0.04f,0.1f,0.12f,0.93f);
            text(b,"체크포인트 활성화  /  여기서 다시 시작",968,1147,1.8f,gold);
        }
        if(debug) {
            box(b,60,920,730,270,0.02f,0.03f,0.04f,0.88f);
            text(b,String.format("120 Hz | x %.2f  y %.2f\nvx %.2f  vy %.2f\nHook: %s  |  ammo %d\nJump ~3H / Recoil gap 5H",s.player.x,s.player.y,s.player.vx,s.player.vy,s.grapple.attached>=0?"ATTACHED":(s.grapple.flying?"FLYING":"READY"),s.player.ammo),85,1160,1.8f,cream);
        }
        if(paused || s.complete) {
            box(b,0,216,2560,1044,0.015f,0.025f,0.04f,0.8f);
            box(b,650,510,1260,430,0.045f,0.07f,0.085f,1);
            box(b,650,936,1260,4,0.9f,0.65f,0.3f,1);
            text(b,s.complete?s.definition.name+" 완료":"일시정지",748,852,3.6f,cream);
            if(s.complete)text(b,"클리어 보석 획득! 기록이 저장되었습니다.",748,756,2f,muted);
            if(s.complete)box(b,730,600,1100,60,0.09f,0.15f,0.16f,1);
            text(b,s.complete?"ENTER / R / 클릭   재도전":"ESC 계속하기    R 체크포인트 재시작",748,644,2.2f,gold);
            if(s.complete) text(b,"TIME  "+(int)s.elapsed+"s     SHOTS  "+s.shots+"     FALLS  "+s.deaths,748,700,1.8f,muted);
            if(s.complete)text(b,"N 다음 스테이지     M 스테이지 선택",748,568,1.6f,muted);
        }
        if(s.clear.frozen()&&!s.complete&&!paused)text(b,"클리어 보석 획득",1040,1070,2.5f,gold);
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
