/* 파일: StageSelectView.java
 * 역할: 네이티브 스테이지 선택 모달. 책임: 데이터 목록·포커스·스크롤·입력 소비·반응형 배치.
 * 협력: StageCatalog, ProgressStore, CaveAssets, HudView. 수명: 화면이 설치/해제, 진행은 소유하지 않음.
 * 관련 사양: docs/assets/cave_chapter/STAGE_SELECT.md, docs/12_CAVE_CHAPTER.md.
 */
package io.github.hookrecoil.ui;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.*;
import io.github.hookrecoil.assets.CaveAssets;
import io.github.hookrecoil.stage.StageCatalog;
import io.github.hookrecoil.save.ProgressStore;
import java.util.*;
public class StageSelectView extends InputAdapter {
    public interface Actions {void start(String id,boolean noCheckpoint);void close();}
    private final StageCatalog catalog;private final ProgressStore progress;private final CaveAssets assets;private final HudView hud;private final Actions actions;
    public String selectedStageId="cave_01";
    public int chapter,focus=1,scroll;
    public boolean noCheckpoint;
    private final OrthographicCamera camera=new OrthographicCamera();
    private final ArrayList<StageCatalog.Entry> rows=new ArrayList<>();
    private final Rectangle list=new Rectangle(),detail=new Rectangle(),start=new Rectangle(),back=new Rectangle(),challenge=new Rectangle(),prologue=new Rectangle();
    private final ArrayList<Rectangle> cards=new ArrayList<>();
    private final Color text=Color.valueOf("e0e4df"),muted=Color.valueOf("a4afb1"),accent=Color.valueOf("76aaa8");
    private float w,h,scale=1,margin=64,cardHeight=76,gap=8;private int capacity;
    public StageSelectView(StageCatalog c,ProgressStore p,CaveAssets a,HudView v,Actions actions){
        catalog=c;progress=p;assets=a;hud=v;this.actions=actions;
        JsonValue layout=new JsonReader().parse(Gdx.files.internal("cave_chapter/ui_layout.json"));
        cardHeight=layout.getFloat("cardHeight",76);gap=layout.getFloat("cardGap",8);
        refresh();resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
    }
    private void refresh(){rows.clear();for(StageCatalog.Entry e:catalog.stages)if(e.chapter.equals(catalog.chapters[chapter].id))rows.add(e);
        if(!rows.isEmpty()&&!rows.contains(catalog.find(selectedStageId)))selectedStageId=rows.get(0).id;
        scroll=0;noCheckpoint=false;
    }
    public void resize(int width,int height){scale=Math.max(1,Math.min(width/1280f,height/720f));w=width/scale;h=height/scale;
        margin=w>=1000?64:20;camera.setToOrtho(false,w,h);float areaTop=h-168;
        if(w>=1000){list.set(margin,128,(w-2*margin)*.382f,Math.max(120,areaTop-128));detail.set(list.x+list.width+24,128,w-margin-list.x-list.width-24, list.height);}
        else {float available=Math.max(200,h-236);float listH=Math.max(72,available*.42f);cardHeight=60;
            list.set(margin,h-156-listH,w-2*margin,listH);detail.set(margin,80,w-2*margin,Math.max(90,list.y-92));}
        start.set(w-margin-220,24,220,48);back.set(margin,24,120,48);prologue.set(margin+136,24,150,48);
        capacity=Math.max(1,(int)(list.height/(cardHeight+gap)));ensureVisible();
    }
    public StageCatalog.Entry selected(){return catalog.find(selectedStageId);}
    public boolean canStart(){return progress.canStart(selected())&&(!noCheckpoint||progress.cleared(selectedStageId));}
    private void choose(int index){if(rows.isEmpty())return;selectedStageId=rows.get(Math.max(0,Math.min(rows.size()-1,index))).id;noCheckpoint=false;ensureVisible();}
    private void ensureVisible(){int i=rows.indexOf(selected());if(i<scroll)scroll=i;if(i>=scroll+capacity)scroll=i-capacity+1;scroll=Math.max(0,scroll);}
    private void panel(SpriteBatch b,Rectangle r,boolean selected,boolean focused){
        hud.box(b,r.x,r.y,r.width,r.height,.098f,.137f,.169f,1);
        Color c=selected?accent:Color.valueOf("42515b");float t=focused?3:1;
        hud.box(b,r.x,r.y,r.width,t,c.r,c.g,c.b,1);hud.box(b,r.x,r.y+r.height-t,r.width,t,c.r,c.g,c.b,1);
        hud.box(b,r.x,r.y,t,r.height,c.r,c.g,c.b,1);hud.box(b,r.x+r.width-t,r.y,t,r.height,c.r,c.g,c.b,1);
        if(focused)hud.text(b,">",r.x+6,r.y+r.height-8,1,text);
    }
    private String time(float seconds){return seconds<0?"—":String.format(java.util.Locale.ROOT,"%.2f초",seconds);}
    public void draw(SpriteBatch b){
        Gdx.gl.glViewport(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());b.setProjectionMatrix(camera.combined);b.begin();
        hud.box(b,0,0,w,h,.067f,.098f,.122f,1);hud.text(b,"스테이지 선택",margin,h-40,1.8f,text);
        hud.text(b,"ESC 닫기",w-margin-94,h-46,1,muted);
        float tabW=(w-2*margin)/catalog.chapters.length;
        for(int i=0;i<catalog.chapters.length;i++){Rectangle r=new Rectangle(margin+i*tabW,h-144,tabW-8,40);panel(b,r,i==chapter,focus==0&&i==chapter);hud.text(b,catalog.chapters[i].name,r.x+32,r.y+28,1,i==chapter?text:muted);}
        cards.clear();
        for(int n=0;n<capacity&&scroll+n<rows.size();n++){
            StageCatalog.Entry e=rows.get(scroll+n);Rectangle r=new Rectangle(list.x,list.y+list.height-(n+1)*(cardHeight+gap)+gap,list.width,cardHeight);cards.add(r);
            boolean sel=e.id.equals(selectedStageId);panel(b,r,sel,sel&&focus==1);
            hud.wrap(b,String.format("%02d  %s",e.number,e.name),r.x+32,r.y+r.height-16,r.width-92,1,progress.unlocked(e)?text:muted);
            String state=progress.cleared(e.id)?"클리어":!progress.unlocked(e)?"잠금":e.available?"시작 가능":"준비 중";
            ProgressStore.Record record=progress.record(e.id);
            hud.text(b,state+"  /  "+time(record==null?-1:record.bestSeconds),r.x+32,r.y+28,.9f,muted);
            if(progress.cleared(e.id)||!progress.unlocked(e))b.draw(assets.region(progress.cleared(e.id)?"check":"lock"),r.x+r.width-32,r.y+r.height/2-10,20,20);
        }
        panel(b,detail,false,focus==2);StageCatalog.Entry e=selected();
        if(e!=null){
            boolean compact=w<1000;float dx=detail.x+28,top=detail.y+detail.height-28;
            hud.wrap(b,e.name,dx,top,detail.width-56,1.35f,text);
            float body=top-40;
            if(!compact&&e.available){Texture image=assets.background(e.background);float bw=detail.width-56,bh=Math.min(130,detail.height*.3f);
                // 썸네일은 비율 보존 contain, 게임 카메라와 독립이다.
                float k=Math.min(bw/image.getWidth(),bh/image.getHeight());b.setColor(.8f,.85f,.87f,1);b.draw(image,dx+(bw-image.getWidth()*k)/2,body-bh,image.getWidth()*k,image.getHeight()*k);b.setColor(Color.WHITE);body-=bh+16;
            }
            ProgressStore.Record r=progress.record(e.id);
            String records="최고 "+time(r==null?-1:r.bestSeconds)+"  /  최소 사망 "+(r==null||r.minDeaths<0?"—":r.minDeaths)+"  /  수집 "+(r==null?0:r.collectibles.length);
            if(h>=520){hud.wrap(b,records,dx,body,detail.width-56,.95f,muted);body-=27;}
            else body-=compact?12:16;
            String need=!e.available?"아직 구현되지 않은 스테이지입니다.":!progress.unlocked(e)?"해금 조건: "+("prologue".equals(e.requires)?"프롤로그":catalog.find(e.requires).name)+" 완료":"";
            if(!need.isEmpty())hud.wrap(b,need,dx,body,detail.width-56,.95f,muted);
            if(h<520)challenge.set(margin+294,24,72,48);
            else {
                challenge.set(dx,detail.y+8,detail.width-56,28);
                if(detail.height>150)hud.text(b,(noCheckpoint?"[x]":"[ ]")+" 노체크포인트  "+(progress.cleared(e.id)?time(r.noCheckpointSeconds):"첫 클리어 후 활성화"),dx,detail.y+32,.95f,progress.cleared(e.id)?text:muted);
            }
        }
        panel(b,start,canStart(),focus==3);hud.text(b,canStart()?"시작  ENTER":"시작 불가",start.x+34,start.y+32,1.15f,canStart()?text:muted);
        panel(b,back,false,focus==4);hud.text(b,"뒤로",back.x+35,back.y+32,1.15f,text);
        panel(b,prologue,false,focus==5);hud.text(b,"프롤로그",prologue.x+32,prologue.y+32,1.05f,text);
        if(h<520){panel(b,challenge,noCheckpoint,focus==2);hud.text(b,noCheckpoint?"노CP":"도전",challenge.x+8,challenge.y+32,.92f,progress.cleared(selectedStageId)?text:muted);}
        b.end();
    }
    private void launch(){if(canStart())actions.start(selectedStageId,noCheckpoint);}
    @Override public boolean keyDown(int key){
        if(key==Input.Keys.ESCAPE){actions.close();return true;}
        if(key==Input.Keys.TAB){focus=(focus+1)%6;return true;}
        if(key==Input.Keys.LEFT||key==Input.Keys.RIGHT){if(focus==0){chapter=(chapter+(key==Input.Keys.RIGHT?1:catalog.chapters.length-1))%catalog.chapters.length;refresh();}else focus=0;return true;}
        if(key==Input.Keys.DOWN||key==Input.Keys.S||key==Input.Keys.UP||key==Input.Keys.W){focus=1;choose(rows.indexOf(selected())+((key==Input.Keys.DOWN||key==Input.Keys.S)?1:-1));return true;}
        if(key==Input.Keys.SPACE&&focus==2){if(progress.cleared(selectedStageId))noCheckpoint=!noCheckpoint;return true;}
        if(key==Input.Keys.ENTER){if(focus==1)focus=3;else if(focus==3)launch();else if(focus==4)actions.close();else if(focus==5)actions.start("prologue",false);else if(focus==0)focus=1;else if(focus==2&&progress.cleared(selectedStageId))noCheckpoint=!noCheckpoint;}
        return true;
    }
    @Override public boolean touchDown(int x,int y,int pointer,int button){if(button!=Input.Buttons.LEFT)return true;float xx=x/scale,yy=h-y/scale;
        if(yy>h-80&&xx>w-margin-110){actions.close();return true;}
        if(yy>=h-144&&yy<=h-104){int index=(int)((xx-margin)/((w-2*margin)/catalog.chapters.length));if(xx>=margin&&index>=0&&index<catalog.chapters.length){chapter=index;focus=0;refresh();}return true;}
        for(int i=0;i<cards.size();i++)if(cards.get(i).contains(xx,yy)){focus=1;choose(scroll+i);return true;}
        if(challenge.contains(xx,yy)){focus=2;if(progress.cleared(selectedStageId))noCheckpoint=!noCheckpoint;}
        else if(start.contains(xx,yy)){focus=3;launch();}else if(back.contains(xx,yy))actions.close();else if(prologue.contains(xx,yy))actions.start("prologue",false);else if(detail.contains(xx,yy))focus=2;
        return true;
    }
    @Override public boolean scrolled(float amountX,float amountY){scroll=Math.max(0,Math.min(Math.max(0,rows.size()-capacity),scroll+(int)Math.signum(amountY)));return true;}
    @Override public boolean touchUp(int x,int y,int pointer,int button){return true;}
    @Override public boolean keyUp(int key){return true;}
    @Override public boolean keyTyped(char c){return true;}
}
