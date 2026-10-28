/*
 * 파일: NativeCityVerification.java
 * 역할: 지하도시 배경·아틀라스·전경 렌더링을 실제 LWJGL 창에서 스모크 검사한다.
 * 주요 책임: 테스트 모드로 city_01~05를 로드하고 배경/시트/잠금 메타데이터를 확인해 캡처한다.
 * 협력 대상: GameplayScreen, CaveAssets, StageCatalog, StageRuntime.
 * 수명: 임시 창·격리 저장·캡처를 관리하며 종료 시 화면을 해제한다.
 * 관련 사양: docs/CITY_CHAPTER_STATUS.md. 맵 완주 플레이테스트를 대체하지 않는다.
 */
package io.github.hookrecoil;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import io.github.hookrecoil.assets.CaveAssets;
import io.github.hookrecoil.screens.GameplayScreen;
import io.github.hookrecoil.stage.StageCatalog;
import io.github.hookrecoil.stage.StageRuntime;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.*;

public final class NativeCityVerification extends ApplicationAdapter {
    private GameplayScreen screen;private Path output,save;private int checks;private Throwable failure;
    private void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);System.out.println("CITY NATIVE OK: "+message);}
    private Object field(String name)throws Exception{Field f=GameplayScreen.class.getDeclaredField(name);f.setAccessible(true);return f.get(screen);}
    private void capture(String name){Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());PixmapIO.writePNG(Gdx.files.absolute(output.resolve(name+".png").toString()),p,6,true);p.dispose();}
    @Override public void create(){try{
        output=Paths.get("build/native-city-check").toAbsolutePath();java.nio.file.Files.createDirectories(output);save=java.nio.file.Files.createTempDirectory(output,"isolated-save-");
        System.setProperty("hookrecoil.saveDir",save.toString());System.setProperty("hookrecoil.previewStage","city_01");
        System.setProperty("hookrecoil.screenshot",output.resolve("auto.png").toString());
        screen=new GameplayScreen();screen.resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
        CaveAssets assets=(CaveAssets)field("caveAssets");
        check(assets.region("city_platform_long")!=null&&assets.region("city_electric_arc")!=null,"Measured city atlas rects load");
        Method load=GameplayScreen.class.getDeclaredMethod("loadStage",String.class,boolean.class);load.setAccessible(true);
        StageCatalog catalog=(StageCatalog)field("catalog");
        for(int i=1;i<=5;i++) {
            String id=String.format(java.util.Locale.ROOT,"city_%02d",i);
            if(i>1)load.invoke(screen,id,false);
            StageRuntime stage=(StageRuntime)field("stage");StageCatalog.Entry entry=catalog.find(id);
            check(stage.definition.id.equals(id)&&!entry.available&&entry.artAvailable,id+" loads only through native preview and remains locked");
            check(assets.background(entry.background)!=null,id+" clamped vertical background loads");
            for(int frame=0;frame<2;frame++)screen.render(1f/60);
            capture(id+"-preview");
        }
        System.out.println("City native preview PASS: "+checks+" checks. No full-route playtest performed.");Gdx.app.exit();
    }catch(Throwable e){failure=e;e.printStackTrace();Gdx.app.exit();}}
    @Override public void render(){if(screen!=null)screen.render(1f/60);}
    @Override public void resize(int width,int height){if(screen!=null)screen.resize(width,height);}
    @Override public void dispose(){if(screen!=null)screen.dispose();if(failure!=null)throw new RuntimeException(failure);}
    public static void main(String[] args){Lwjgl3ApplicationConfiguration config=new Lwjgl3ApplicationConfiguration();config.setTitle("City Chapter Verification");config.setWindowedMode(1280,720);config.useVsync(false);config.setForegroundFPS(120);new Lwjgl3Application(new NativeCityVerification(),config);}
}
