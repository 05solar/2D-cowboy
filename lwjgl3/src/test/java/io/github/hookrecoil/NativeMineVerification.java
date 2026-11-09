/* 파일: NativeMineVerification.java
 * 역할: 광산 6~10의 LWJGL 화면·메뉴·배경·스테이지 로더를 실제 창에서 검증한다.
 * 주요 책임: 격리 저장에 테스트 해금 기록을 만들고 광산 카드와 각 맵을 렌더링한다.
 * 협력: GameplayScreen, StageSelectView, ProgressStore, MineVerification.
 * 수명: 테스트 창과 임시 저장을 소유하며 창 종료 때 게임 화면 자원을 해제한다.
 * 관련 사양: docs/assets/mine_chapter/STAGES_06_10.md, docs/MINE_CHAPTER_STATUS.md.
 */
package io.github.hookrecoil;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Json;
import io.github.hookrecoil.screens.GameplayScreen;
import io.github.hookrecoil.stage.StageRuntime;
import io.github.hookrecoil.stage.StageCatalog;
import io.github.hookrecoil.stage.StageDefinition;
import io.github.hookrecoil.save.ProgressStore;
import io.github.hookrecoil.ui.StageSelectView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public final class NativeMineVerification extends ApplicationAdapter {
    private GameplayScreen screen;
    private Path output,save;
    private int checks;
    private Throwable failure;

    private Object field(String name) throws Exception {
        Field f=GameplayScreen.class.getDeclaredField(name);f.setAccessible(true);return f.get(screen);
    }
    private void check(boolean ok,String message) {
        checks++;if(!ok)throw new AssertionError(message);System.out.println("MINE NATIVE OK: "+message);
    }
    private void capture(String name) {
        Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
        PixmapIO.writePNG(Gdx.files.absolute(output.resolve(name+".png").toString()),p,6,true);p.dispose();
    }
    private void key(int key) {Gdx.input.getInputProcessor().keyDown(key);Gdx.input.getInputProcessor().keyUp(key);}

    @Override public void create() {
        try {
            output=Paths.get("build/native-mine-check").toAbsolutePath();java.nio.file.Files.createDirectories(output);
            save=java.nio.file.Files.createTempDirectory(output,"isolated-save-");System.setProperty("hookrecoil.saveDir",save.toString());
            ProgressStore.Data d=new ProgressStore.Data();
            for(String id:new String[]{"prologue","cave_01","cave_02","cave_03","cave_04","cave_05","mine_01","mine_02","mine_03","mine_04","mine_05"}) {
                ProgressStore.Record r=new ProgressStore.Record();r.completed=true;r.bestSeconds=1;r.minDeaths=0;d.records.put(id,r);
            }
            java.nio.file.Files.write(save.resolve("progress.json"),new Json().toJson(d).getBytes(StandardCharsets.UTF_8));
            screen=new GameplayScreen();screen.resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
            check(((StageSelectView)field("menu"))!=null,"Test progress opens stage selection");
        } catch(Throwable e) {fail(e);}
    }
    private void fail(Throwable e){failure=e;e.printStackTrace();Gdx.app.exit();}

    @Override public void render() {
        if(failure!=null)return;
        try {
            StageSelectView menu=(StageSelectView)field("menu");
            if(menu!=null) {
                menu.focus=0;key(Input.Keys.RIGHT);menu=(StageSelectView)field("menu");
                check(menu.chapter==1&&menu.selected().id.equals("mine_01"),"Mine chapter and first card are selectable");
                check(menu.canStart(),"First implemented mine stage can start");
                for(int i=1;i<=5;i++){menu.selectedStageId=String.format(java.util.Locale.ROOT,"mine_%02d",i);check(menu.canStart(),"Every implemented mine card can start: "+menu.selectedStageId);}
                menu.selectedStageId="mine_01";
                screen.render(1f/60);capture("menu-mine");
                key(Input.Keys.ENTER);key(Input.Keys.ENTER);key(Input.Keys.ENTER);
                check(((StageRuntime)field("stage")).definition.id.equals("mine_01"),"Menu launches mine_01");
            }
            for(int i=1;i<=5;i++) {
                String id=String.format(java.util.Locale.ROOT,"mine_%02d",i);
                if(i>1) {
                    Method load=GameplayScreen.class.getDeclaredMethod("loadStage",String.class,boolean.class);load.setAccessible(true);load.invoke(screen,id,false);
                }
                StageRuntime stage=(StageRuntime)field("stage");check(stage.definition.id.equals(id),id+" map loads");
                Object assets=field("caveAssets");Method background=assets.getClass().getMethod("background",String.class);
                StageCatalog.Entry entry=((StageCatalog)new Json().fromJson(StageCatalog.class,Gdx.files.internal("cave_chapter/stages.json"))).find(id);
                Object texture=background.invoke(assets,entry.background);check(texture!=null,id+" portrait background loads with clamp texture");
                for(int frame=0;frame<3;frame++)screen.render(1f/60);
                if(i==1) {
                    Object intro=field("introCamera");Method cancel=intro.getClass().getDeclaredMethod("cancel");cancel.setAccessible(true);cancel.invoke(intro);
                    StageDefinition.CityBarrier barrier=stage.definition.cityBarriers[0];
                    stage.cityDevices.tickBarriers(barrier.offSeconds+.1f);
                    stage.cityDevices.tickBarriers(barrier.warningSeconds+.1f);
                    check("on".equals(barrier.state)&&barrier.collider.active,"Mine electric barrier becomes visibly and physically active");
                    screen.render(1f/60);capture("mine-barrier-on");
                }
                if(id.equals("mine_04")) {
                    // 보석 위치가 카메라 상한선(스테이지 height 기반 maxCameraY) 아래에 실제로 들어오는지
                    // GameplayScreen과 동일한 공식으로 직접 계산해, 플레이어 낙하/리스폰 흔들림 없이 확인한다.
                    Object cameraConfigObj=field("cameraConfig");
                    float lowerEdgeAllowance=(float)cameraConfigObj.getClass().getField("lowerEdgeAllowance").get(cameraConfigObj);
                    float gameplayZoom=(float)cameraConfigObj.getClass().getField("gameplayZoom").get(cameraConfigObj);
                    Object viewportObj=field("viewport");
                    Method worldHeightMethod=viewportObj.getClass().getMethod("getWorldHeight");
                    float viewportWorldHeight=(float)worldHeightMethod.invoke(viewportObj);
                    float halfCameraHeight=viewportWorldHeight*.5f*gameplayZoom;
                    float worldHeight=stage.definition.height*180f;
                    float lowerMargin=lowerEdgeAllowance*(viewportWorldHeight/1440f);
                    float minCameraY=Math.min(Math.max(0,halfCameraHeight-lowerMargin),worldHeight*.5f);
                    float maxCameraY=Math.max(minCameraY,worldHeight-halfCameraHeight);
                    float gemWorldY=stage.definition.clearGem.y*180f;
                    check(maxCameraY>=gemWorldY,
                        "mine_04 camera ceiling reaches the clear gem instead of clamping below it (maxCameraY="
                        +maxCameraY+" gemY="+gemWorldY+", stage height="+stage.definition.height+")");
                    // 시작 카메라 투어의 "전체 맵 축소 조망" 구간에서 캡처해, 확장된 맵 전체와 보석이
                    // 화면 안에 들어오는 것을 스크린샷으로 남긴다(별도 플레이어 이동 없이 투어만 사용).
                    for(int frame=0;frame<18;frame++)screen.render(1f/60);
                    capture("mine04-gem-camera-tour");
                }
                if(id.equals("mine_03")) {
                    StageDefinition.CityBarrier floor=null;
                    for(StageDefinition.CityBarrier bar:stage.definition.cityBarriers)if(bar.id.endsWith("floor4"))floor=bar;
                    check(floor!=null,"mine_03 has a platform-aligned horizontal barrier");
                    stage.player.reset(floor.x+floor.width*.5f,floor.y+1.6f);
                    stage.cityDevices.tickBarriers(floor.offSeconds+.1f);
                    stage.cityDevices.tickBarriers(floor.warningSeconds+.1f);
                    check("on".equals(floor.state)&&floor.collider.active,"mine_03 horizontal barrier becomes visibly and physically active");
                    for(StageDefinition.CityBarrier bar:stage.definition.cityBarriers)if(bar!=floor){bar.state="off";bar.collider.active=false;}
                    for(int frame=0;frame<30;frame++)screen.render(1f/60);
                    capture("mine03-floor-barrier-on");
                }
                if(id.equals("mine_05")) {
                    for(int frame=0;frame<24;frame++)screen.render(1f/60);
                    capture("mine05-full-map-tour");
                    Object introObj=field("introCamera");Method cancelIntro=introObj.getClass().getDeclaredMethod("cancel");cancelIntro.setAccessible(true);cancelIntro.invoke(introObj);
                    StageDefinition.CityBarrier floor=null;
                    for(StageDefinition.CityBarrier bar:stage.definition.cityBarriers)if(bar.id.endsWith("floor6"))floor=bar;
                    check(floor!=null,"mine_05 has a platform-aligned horizontal barrier");
                    stage.player.reset(floor.x+floor.width*.5f,floor.y+1.6f);
                    stage.cityDevices.tickBarriers(floor.offSeconds+.1f);
                    stage.cityDevices.tickBarriers(floor.warningSeconds+.1f);
                    for(StageDefinition.CityBarrier bar:stage.definition.cityBarriers)if(bar!=floor){bar.state="off";bar.collider.active=false;}
                    for(int frame=0;frame<30;frame++)screen.render(1f/60);
                    capture("mine05-floor-barrier-on");
                    StageDefinition.MineCrusher press=stage.definition.mineCrushers[0];
                    stage.player.reset(press.x-2.5f,press.topY-press.stroke-.5f);
                    for(int frame=0;frame<120;frame++)screen.render(1f/60);
                    capture("mine05-crusher-area");
                    // 세로 전기장벽 1개만 단독 활성화해, 바로 옆 발판에 선 플레이어와 나란히 크기를 비교할 수 있는 기준 샷을 남긴다.
                    StageDefinition.CityBarrier vertical=stage.definition.cityBarriers[0];
                    StageDefinition.Block near=null;
                    for(StageDefinition.Block pl:stage.definition.platforms)
                        if(vertical.x>=pl.x-1f&&vertical.x<=pl.x+pl.width+1f&&Math.abs(pl.y-vertical.y)<3f){near=pl;break;}
                    if(near!=null)stage.player.reset(near.x+near.width*.5f,near.surfaceY(near.x+near.width*.5f));
                    stage.cityDevices.tickBarriers(vertical.offSeconds+.1f);
                    stage.cityDevices.tickBarriers(vertical.warningSeconds+.1f);
                    for(StageDefinition.CityBarrier bar:stage.definition.cityBarriers)if(bar!=vertical){bar.state="off";bar.collider.active=false;}
                    for(int frame=0;frame<30;frame++)screen.render(1f/60);
                    capture("mine05-vertical-barrier-scale");
                }
                capture(id+"-start");
            }
            {
                // 글꼴 재생성 후 결과 패널(CLEAR/ALL CLEAR) 한글 글리프를 실제로 캡처해 확인한다.
                Method load=GameplayScreen.class.getDeclaredMethod("loadStage",String.class,boolean.class);load.setAccessible(true);
                load.invoke(screen,"mine_05",false);
                StageRuntime last=(StageRuntime)field("stage");
                last.complete=true;last.elapsed=27.6f;last.shots=13;last.deaths=0;
                Object introObj=field("introCamera");Method cancelIntro=introObj.getClass().getDeclaredMethod("cancel");cancelIntro.setAccessible(true);cancelIntro.invoke(introObj);
                screen.render(1f/60);capture("result-panel-clear");
                Object progressObj=field("progress");
                Method recordMethod=progressObj.getClass().getMethod("record",String.class);
                java.lang.reflect.Field dataField=progressObj.getClass().getField("data");Object dataObj=dataField.get(progressObj);
                java.lang.reflect.Field recordsField=dataObj.getClass().getField("records");
                java.util.Map<String,Object> records=(java.util.Map<String,Object>)recordsField.get(dataObj);
                Class<?> recordClass=Class.forName("io.github.hookrecoil.save.ProgressStore$Record");
                for(String sid:new String[]{"cave_01","cave_02","cave_03","cave_04","cave_05","mine_01","mine_02","mine_03","mine_04","mine_05","city_01","city_02","city_03","city_04","city_05"}) {
                    Object rec=recordClass.getDeclaredConstructor().newInstance();
                    recordClass.getField("completed").set(rec,true);
                    recordClass.getField("bestSeconds").set(rec,1f);
                    recordClass.getField("minDeaths").set(rec,0);
                    records.put(sid,rec);
                }
                screen.render(1f/60);capture("result-panel-all-clear");
            }
            java.nio.file.Files.write(output.resolve("result.txt"),("PASS native mine load/render: "+checks+" checks.\nAll five mine cards/maps/backgrounds loaded; test-only progress was isolated at "+save+".\n").getBytes(StandardCharsets.UTF_8));
            Gdx.app.exit();
        } catch(Throwable e) {fail(e);}
    }
    @Override public void resize(int w,int h){if(screen!=null)screen.resize(w,h);}
    @Override public void dispose(){if(screen!=null)screen.dispose();if(failure!=null)throw new RuntimeException(failure);}
    public static void main(String[] args){Lwjgl3ApplicationConfiguration c=new Lwjgl3ApplicationConfiguration();c.setTitle("HOOK & RECOIL mine verification");c.setWindowedMode(1280,720);c.setPauseWhenLostFocus(false);c.useVsync(false);c.setForegroundFPS(60);new Lwjgl3Application(new NativeMineVerification(),c);}
}
