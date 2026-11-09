/* 파일: FullPlaythroughDemo.java
 * 역할: 프롤로그부터 city_05까지 실시간 속도로 자동 플레이해 실제 창에서 보여준다.
 * 주요 책임: 실제 GameplayScreen을 그대로 구동하되 DrivenInputRouter로 키보드·마우스 대신
 *          스테이지별 경로 드라이버의 입력을 공급하고, 클리어 시 실제 "다음 스테이지" 진행
 *          경로(catalog.next)를 그대로 재사용해 자동으로 다음 스테이지를 연다.
 * 협력 대상: GameplayScreen, DrivenInputRouter, PrologueRouteDriver, CaveRouteDriver,
 *          MineRouteDriver, CityRouteDriver, StageCatalog.
 * 관련 사양: docs/10_PROLOGUE.md, docs/12_CAVE_CHAPTER.md, docs/MINE_06_10_ROUTE_REWORK_2026-11-09.md,
 *          docs/CITY_11_15_REWORK_2026-11-09.md.
 */
package io.github.hookrecoil;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.github.hookrecoil.input.InputFrame;
import io.github.hookrecoil.screens.GameplayScreen;
import io.github.hookrecoil.stage.StageRuntime;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class FullPlaythroughDemo extends ApplicationAdapter {
    private GameplayScreen screen;
    private DrivenInputRouter drivenInput;
    private JsonValue caveRoutes,mineRoutes;
    private Object currentDriver;
    private String currentStageId="";

    private Object field(String name) throws Exception {
        Field f=GameplayScreen.class.getDeclaredField(name);f.setAccessible(true);return f.get(screen);
    }
    private void setField(String name,Object value) throws Exception {
        Field f=GameplayScreen.class.getDeclaredField(name);f.setAccessible(true);f.set(screen,value);
    }

    @Override public void create() {
        try {
            caveRoutes=new JsonReader().parse(new FileHandle("assets/stages/cave_routes.json"));
            mineRoutes=new JsonReader().parse(new FileHandle("assets/stages/mine_routes.json"));
            screen=new GameplayScreen();
            screen.resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
            drivenInput=new DrivenInputRouter();
            drivenInput.setSource(this::currentFrame);
            setField("input",drivenInput);
            Gdx.input.setInputProcessor(drivenInput);
            adoptStage("prologue");
        } catch(Exception e) {throw new RuntimeException(e);}
    }

    private InputFrame currentFrame() {
        StageRuntime stage=null;
        try {
            stage=(StageRuntime)field("stage");
            if(currentDriver instanceof PrologueRouteDriver)return ((PrologueRouteDriver)currentDriver).next(stage);
            if(currentDriver instanceof CaveRouteDriver)return ((CaveRouteDriver)currentDriver).next(stage);
            if(currentDriver instanceof MineRouteDriver)return ((MineRouteDriver)currentDriver).next(stage);
            if(currentDriver instanceof CityRouteDriver)return ((CityRouteDriver)currentDriver).next(stage);
            return new InputFrame();
        } catch(Exception e) {
            System.out.println("DIAG currentStageId="+currentStageId+" driver="+currentDriver
                +" stage.definition.id="+(stage==null?"?":stage.definition.id)
                +" platforms="+(stage==null?"?":stage.definition.platforms.length));
            throw new RuntimeException(e);
        }
    }

    private void adoptStage(String id) {
        currentStageId=id;
        if(id.equals("prologue"))currentDriver=new PrologueRouteDriver();
        else if(id.startsWith("cave_"))currentDriver=new CaveRouteDriver(caveRoutes.get(id));
        else if(id.startsWith("mine_"))currentDriver=new MineRouteDriver(mineRoutes.get(id));
        else currentDriver=new CityRouteDriver();
        try {
            StageRuntime s=(StageRuntime)field("stage");
            System.out.println("== "+id+" 시작 == platforms="+s.definition.platforms.length+" definitionId="+s.definition.id);
        } catch(Exception e) {System.out.println("== "+id+" 시작 == (diagnostic failed: "+e+")");}
    }

    @Override public void render() {
        try {
            float delta=Gdx.graphics.getDeltaTime();
            StageRuntime before=(StageRuntime)field("stage");
            Object catalog=field("catalog");
            if(before.complete) {
                String next=(String)catalog.getClass().getMethod("next",String.class).invoke(catalog,before.definition.id);
                if(next!=null)drivenInput.nextRequested=true;
            }
            screen.render(delta);
            StageRuntime after=(StageRuntime)field("stage");
            if(!after.definition.id.equals(currentStageId))adoptStage(after.definition.id);
        } catch(Exception e) {throw new RuntimeException(e);}
    }

    @Override public void resize(int width,int height) {if(screen!=null)screen.resize(width,height);}
    @Override public void dispose() {if(screen!=null)screen.dispose();}

    public static void main(String[] args) throws Exception {
        // 실제 사용자 진행 저장(%LOCALAPPDATA%\HookRecoil)을 건드리지 않도록 격리된 임시 저장 경로를 쓴다.
        java.nio.file.Path output=java.nio.file.Paths.get("build/full-playthrough-demo").toAbsolutePath();
        java.nio.file.Files.createDirectories(output);
        java.nio.file.Path save=java.nio.file.Files.createTempDirectory(output,"isolated-save-");
        System.setProperty("hookrecoil.saveDir",save.toString());
        Lwjgl3ApplicationConfiguration c=new Lwjgl3ApplicationConfiguration();
        c.setTitle("HOOK & RECOIL - 자동 전체 클리어");
        c.setWindowedMode(1600,900);
        c.setPauseWhenLostFocus(false);
        c.useVsync(true);
        c.setForegroundFPS(60);
        new Lwjgl3Application(new FullPlaythroughDemo(),c);
    }
}
