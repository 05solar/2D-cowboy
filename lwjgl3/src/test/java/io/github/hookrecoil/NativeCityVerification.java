/*
 * 파일: NativeCityVerification.java
 * 역할: 지하도시 V2 배경·아틀라스·전경 렌더링을 실제 LWJGL 창에서 스모크 검사한다.
 * 주요 책임: city_01~05의 V2 얇은 아트와 가로·세로 비반복 배경을 로드해 캡처한다.
 * 협력 대상: GameplayScreen, CaveAssets, StageCatalog, StageRuntime.
 * 수명: 임시 창·격리 저장·캡처를 관리하며 종료 시 화면을 해제한다.
 * 관련 사양: docs/CITY_CHAPTER_V2.md. 맵 완주 플레이테스트를 대체하지 않는다.
 */
package io.github.hookrecoil;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import io.github.hookrecoil.assets.CaveAssets;
import io.github.hookrecoil.screens.GameplayScreen;
import io.github.hookrecoil.screens.CameraConfig;
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
        check(assets.region("city_v2_concrete_long")!=null&&assets.region("city_v2_electric_arc")!=null
            &&assets.region("city_v2_l_right")!=null,"Measured V2 thin-platform, open-frame and device rects load");
        Method load=GameplayScreen.class.getDeclaredMethod("loadStage",String.class,boolean.class);load.setAccessible(true);
        StageCatalog catalog=(StageCatalog)field("catalog");
        for(int i=1;i<=5;i++) {
            String id=String.format(java.util.Locale.ROOT,"city_%02d",i);
            if(i>1)load.invoke(screen,id,false);
            StageRuntime stage=(StageRuntime)field("stage");StageCatalog.Entry entry=catalog.find(id);
            check(stage.definition.id.equals(id)&&entry.available&&entry.artAvailable,id+" is available and loads in the native runtime");
            check(assets.background(entry.background)!=null,id+" clamped vertical background loads");
            check(entry.backgroundVariants!=null&&!entry.backgroundVariants.repeat
                &&assets.background(entry.backgroundVariants.horizontal)!=null
                &&assets.background(entry.backgroundVariants.vertical)!=null,id+" horizontal and vertical backgrounds load without repetition");
            check(stage.definition.cityBackgroundRegions.length>0,id+" has camera-region background routing");
            for(int frame=0;frame<2;frame++)screen.render(1f/60);
            capture(id+"-preview");
        }
        System.clearProperty("hookrecoil.screenshot");
        load.invoke(screen,"city_02",false);
        load.invoke(screen,"city_01",false);
        StageRuntime introStage=(StageRuntime)field("stage");
        OrthographicCamera camera=(OrthographicCamera)field("camera");
        float gameplayZoom=((CameraConfig)field("cameraConfig")).gameplayZoom;
        float worldWidth=introStage.definition.width*introStage.config.pixelsPerH;
        float worldHeight=introStage.definition.height*introStage.config.pixelsPerH;
        float halfWidth=1280*gameplayZoom,halfHeight=720*gameplayZoom;
        float minX=Math.min(halfWidth,worldWidth*.5f),maxX=Math.max(minX,worldWidth-halfWidth);
        float minY=Math.min(halfHeight,worldHeight*.5f),maxY=Math.max(minY,worldHeight-halfHeight);
        float spawnCameraX=MathUtils.clamp(introStage.player.x*introStage.config.pixelsPerH+430,minX,maxX);
        float spawnCameraY=MathUtils.clamp(introStage.player.y*introStage.config.pixelsPerH+220,minY,maxY);
        float gemCameraX=MathUtils.clamp(introStage.definition.clearGem.x*introStage.config.pixelsPerH+430,minX,maxX);
        float gemCameraY=MathUtils.clamp(introStage.definition.clearGem.y*introStage.config.pixelsPerH+220,minY,maxY);
        Object intro=field("introCamera");
        Method overviewReached=intro.getClass().getDeclaredMethod("overviewReachedAt");overviewReached.setAccessible(true);
        Method gemReached=intro.getClass().getDeclaredMethod("gemReachedAt");gemReached.setAccessible(true);
        float overviewSeconds=(Float)overviewReached.invoke(intro),gemSeconds=(Float)gemReached.invoke(intro);
        int overviewFrames=(int)Math.ceil(overviewSeconds*60);
        float previousX=camera.position.x,previousY=camera.position.y,maxStep=0,previousZoom=camera.zoom,maxZoomStep=0;
        for(int i=0;i<overviewFrames;i++) {
            screen.render(overviewSeconds/overviewFrames);
            maxStep=Math.max(maxStep,(float)Math.sqrt((camera.position.x-previousX)*(camera.position.x-previousX)
                +(camera.position.y-previousY)*(camera.position.y-previousY)));
            maxZoomStep=Math.max(maxZoomStep,Math.abs(camera.zoom-previousZoom));
            previousX=camera.position.x;previousY=camera.position.y;previousZoom=camera.zoom;
        }
        float requiredZoom=Math.max(worldWidth/2560f,worldHeight/1440f);
        boolean fullMapInFrame=camera.position.x-1280*camera.zoom<=1&&camera.position.x+1280*camera.zoom>=worldWidth-1
            &&camera.position.y-720*camera.zoom<=1&&camera.position.y+720*camera.zoom>=worldHeight-1;
        check(camera.zoom>=requiredZoom&&fullMapInFrame,"Opening camera zooms out to fit the whole stage");
        int gemFrames=(int)Math.ceil((gemSeconds-overviewSeconds)*60);
        for(int i=0;i<gemFrames;i++) {
            screen.render((gemSeconds-overviewSeconds)/gemFrames);
            maxStep=Math.max(maxStep,(float)Math.sqrt((camera.position.x-previousX)*(camera.position.x-previousX)
                +(camera.position.y-previousY)*(camera.position.y-previousY)));
            maxZoomStep=Math.max(maxZoomStep,Math.abs(camera.zoom-previousZoom));
            previousX=camera.position.x;previousY=camera.position.y;previousZoom=camera.zoom;
        }
        check(Math.abs(camera.position.x-Math.round(gemCameraX))<1&&Math.abs(camera.position.y-Math.round(gemCameraY))<1,
            "Opening camera zooms to the clear gem without advancing gameplay");
        check(Math.abs(camera.zoom-gameplayZoom)<.001f,"Opening camera returns to gameplay zoom at the gem");
        check(maxStep<100,"Opening camera zoom and position transitions stay continuous");
        check(maxZoomStep<.12f,"Opening camera zoom transitions stay continuous");
        capture("city-intro-gem");
        screen.render(.45f);screen.render(1.6f);
        check(Math.abs(camera.position.x-Math.round(spawnCameraX))<1&&Math.abs(camera.position.y-Math.round(spawnCameraY))<1,
            "Opening camera returns to the player spawn");
        check(introStage.elapsed==0&&introStage.player.x==introStage.definition.spawnX,
            "Stage time and player remain frozen during the opening camera pass");
        capture("city-intro-return");
        System.out.println("City native preview and intro camera PASS: "+checks+" checks. No full-route playtest performed.");Gdx.app.exit();
    }catch(Throwable e){failure=e;e.printStackTrace();Gdx.app.exit();}}
    @Override public void render(){if(screen!=null)screen.render(1f/60);}
    @Override public void resize(int width,int height){if(screen!=null)screen.resize(width,height);}
    @Override public void dispose(){if(screen!=null)screen.dispose();if(failure!=null)throw new RuntimeException(failure);}
    public static void main(String[] args){Lwjgl3ApplicationConfiguration config=new Lwjgl3ApplicationConfiguration();config.setTitle("City Chapter Verification");config.setWindowedMode(1280,720);config.useVsync(false);config.setForegroundFPS(120);new Lwjgl3Application(new NativeCityVerification(),config);}
}
