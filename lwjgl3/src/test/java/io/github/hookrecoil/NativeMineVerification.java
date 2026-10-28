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
                capture(id+"-start");
            }
            java.nio.file.Files.write(output.resolve("result.txt"),("PASS native mine load/render: "+checks+" checks.\nAll five mine cards/maps/backgrounds loaded; test-only progress was isolated at "+save+".\n").getBytes(StandardCharsets.UTF_8));
            Gdx.app.exit();
        } catch(Throwable e) {fail(e);}
    }
    @Override public void resize(int w,int h){if(screen!=null)screen.resize(w,h);}
    @Override public void dispose(){if(screen!=null)screen.dispose();if(failure!=null)throw new RuntimeException(failure);}
    public static void main(String[] args){Lwjgl3ApplicationConfiguration c=new Lwjgl3ApplicationConfiguration();c.setTitle("HOOK & RECOIL mine verification");c.setWindowedMode(1280,720);c.setPauseWhenLostFocus(false);c.useVsync(false);c.setForegroundFPS(60);new Lwjgl3Application(new NativeMineVerification(),c);}
}
