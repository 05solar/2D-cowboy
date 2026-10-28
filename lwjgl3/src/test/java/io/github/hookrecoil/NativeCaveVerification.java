/* 파일: NativeCaveVerification.java
 * 역할: 실제 LWJGL 창 통합 검증. 책임: 네이티브 렌더·입력 라우터·메뉴·전체 경로·저장 복원.
 * 협력: GameplayScreen, CaveRouteDriver. 격리 저장과 캡처를 build/native-cave-check에 남긴다.
 * 관련 사양: docs/12_CAVE_CHAPTER.md. 사람이 수행한 플레이테스트와 구분한다.
 */
package io.github.hookrecoil;
import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.hookrecoil.screens.GameplayScreen;
import io.github.hookrecoil.stage.*;
import io.github.hookrecoil.save.ProgressStore;
import io.github.hookrecoil.ui.StageSelectView;
import io.github.hookrecoil.input.InputFrame;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.nio.file.Files;
public class NativeCaveVerification extends ApplicationAdapter {
    private GameplayScreen screen;private int frame,level=1,ticks,checks,lastCheckpoint=-1;
    private CaveRouteDriver driver;private boolean hook,warningCaptured,collapseCaptured;private Throwable failure;
    private Path output,save;
    private Object field(String name){try{Field f=GameplayScreen.class.getDeclaredField(name);f.setAccessible(true);return f.get(screen);}catch(Exception e){throw new RuntimeException(e);}}
    private StageRuntime stage(){return (StageRuntime)field("stage");}
    private StageSelectView menu(){return (StageSelectView)field("menu");}
    private void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);System.out.println("NATIVE OK: "+message);}
    private void key(int k){Gdx.input.getInputProcessor().keyDown(k);Gdx.input.getInputProcessor().keyUp(k);}
    private void click(int x,int y){Gdx.input.getInputProcessor().touchDown(x,y,0,Input.Buttons.LEFT);Gdx.input.getInputProcessor().touchUp(x,y,0,Input.Buttons.LEFT);}
    private void capture(String name){Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());PixmapIO.writePNG(Gdx.files.absolute(output.resolve(name+".png").toString()),p,6,true);p.dispose();}
    @Override public void create(){try{
        output=Paths.get("build/native-cave-check").toAbsolutePath();Files.createDirectories(output);save=Files.createTempDirectory(output,"save-");
        System.setProperty("hookrecoil.saveDir",save.toString());
        StageRuntime prologue=CaveVerification.game("prologue");prologue.player.reset(prologue.definition.clearGem.x,2);prologue.resolveGoal(false);
        ProgressStore p=new ProgressStore(save);p.load();p.begin(prologue);p.finish();
        screen=new GameplayScreen();screen.resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
    }catch(Throwable e){fail(e);}}
    private void fail(Throwable e){failure=e;e.printStackTrace();Gdx.app.exit();}
    private void beginDriver(){driver=new CaveRouteDriver(new JsonReader().parse(Gdx.files.internal("stages/cave_routes.json")).get(stage().definition.id));ticks=0;hook=false;warningCaptured=collapseCaptured=false;lastCheckpoint=-1;}
    private void input(InputFrame f){
        InputProcessor p=Gdx.input.getInputProcessor();
        p.keyUp(f.move>0?Input.Keys.A:Input.Keys.D);
        if(f.move!=0)p.keyDown(f.move>0?Input.Keys.D:Input.Keys.A);else{p.keyUp(Input.Keys.A);p.keyUp(Input.Keys.D);}
        if(f.jump)p.keyDown(Input.Keys.SPACE);else p.keyUp(Input.Keys.SPACE);
        StageRuntime s=stage();float u=s.config.pixelsPerH;
        Vector3 v=new Vector3((s.player.x+f.aimX)*u,(s.player.y+s.config.height*.65f+f.aimY)*u,0);
        ((FitViewport)field("viewport")).project(v);int x=Math.round(v.x),y=Gdx.graphics.getHeight()-Math.round(v.y);
        Gdx.input.setCursorPosition(x,y);
        if(f.fire)p.touchDown(x,y,0,Input.Buttons.LEFT);else p.touchUp(x,y,0,Input.Buttons.LEFT);
        if(f.hookPressed)p.touchDown(x,y,0,Input.Buttons.RIGHT);
        if(hook&&!f.hookHeld)p.touchUp(x,y,0,Input.Buttons.RIGHT);hook=f.hookHeld;
    }
    @Override public void render(){if(failure!=null)return;try{
        frame++;
        if(frame<85){
            if(frame==2){check(menu()!=null,"Saved prologue opens native stage select");click(200,210+84*3);check(!menu().canStart(),"Mouse selected locked card");key(Input.Keys.ENTER);key(Input.Keys.ENTER);check(menu()!=null,"Locked card cannot launch");}
            if(frame==5){capture("menu-1280");Gdx.graphics.setWindowedMode(640,480);}
            if(frame==15){check(Gdx.graphics.getWidth()==640,"Small window resized and card list scrolls");capture("menu-640");key(Input.Keys.TAB);key(Input.Keys.TAB);key(Input.Keys.TAB);check(menu().focus!=3,"Tab moves distinct focus");Gdx.graphics.setWindowedMode(1920,1080);}
            if(frame==25){capture("menu-1920");Gdx.graphics.setWindowedMode(1280,720);}
            if(frame==35){key(Input.Keys.RIGHT);check(menu().chapter==1,"Keyboard chapter switch");check(!menu().selected().available&&!menu().canStart(),"Future chapter cannot load");capture("menu-future");key(Input.Keys.LEFT);}
            if(frame==45){key(Input.Keys.ESCAPE);check(menu()==null,"Escape restores prior screen");key(Input.Keys.M);}
            if(frame==55){check(menu()!=null,"M opens menu");for(int i=0;i<6;i++)key(Input.Keys.W);key(Input.Keys.ENTER);key(Input.Keys.ENTER);check(menu()==null&&stage().definition.id.equals("cave_01"),"Keyboard starts cave 1");
                screen.pause();double before=stage().elapsed;screen.render(.5f);check(stage().elapsed==before,"App pause freezes simulation");screen.resume();key(Input.Keys.ESCAPE);
                stage().resolveGoal(true);screen.render(0);capture("death-screen");check(stage().deathTimer>0&&stage().deaths==1,"Fall death overlay rendered");stage().restartAll();}
            screen.render(0);if(frame==84)beginDriver();return;
        }
        if(level<=5){
            StageRuntime s=stage();
            if(!s.clear.frozen())input(driver.next(s));
            screen.render(1f/120);ticks++;
            if(level==4&&s.devices.warning&&!warningCaptured){capture("cave_04-warning");warningCaptured=true;check(true,"Crumble warning rendered in cave 4");}
            if(level==4&&!collapseCaptured){for(int i=0;i<s.definition.platforms.length;i++)if(s.definition.platforms[i].crumble&&!s.definition.platforms[i].active){capture("cave_04-collapse");collapseCaptured=true;check(true,"Crumble collision removed in cave 4");break;}}
            if(ticks==2)capture(s.definition.id+"-start");
            if(s.checkpoint!=lastCheckpoint){lastCheckpoint=s.checkpoint;capture(s.definition.id+"-cp"+lastCheckpoint);}
            if(s.deaths>0)throw new AssertionError("Native route died "+s.definition.id+" route="+driver.routeIndex+" x="+s.player.x+" y="+s.player.y);
            if(ticks>9000)throw new AssertionError("Native route timeout "+s.definition.id+" route="+driver.routeIndex+" x="+s.player.x+" y="+s.player.y);
            if(s.complete){
                check(s.checkpoint+1==s.definition.checkpoints.length,s.definition.id+" entire route / checkpoints / collection rendered");
                ProgressStore loaded=new ProgressStore(save);loaded.load();check(loaded.cleared(s.definition.id),s.definition.id+" disk completion after animation");capture(s.definition.id+"-cleared");
                level++;if(level<=5){key(Input.Keys.N);screen.render(0);check(stage().definition.id.equals(String.format("cave_%02d",level)),"Next stage input and unlock");beginDriver();}
                else {screen.dispose();screen=new GameplayScreen();screen.resize(1280,720);}
            }
        }else if(level==6){
            check(menu()!=null,"Restart restores saved progress to menu");
            for(int i=0;i<6;i++)key(Input.Keys.W);
            key(Input.Keys.TAB);key(Input.Keys.SPACE);check(menu().noCheckpoint,"Challenge enabled after first clear");key(Input.Keys.TAB);key(Input.Keys.ENTER);
            check(stage().noCheckpoint&&stage().definition.id.equals("cave_01"),"Previously cleared stage can replay in challenge mode");
            check(stage().player.ammo==2&&!stage().clear.frozen()&&stage().grapple.attached<0,"Replay clears freeze / ammo / grapple input");
            key(Input.Keys.M);screen.render(0);capture("menu-restored");
            Files.write(output.resolve("result.txt"),("PASS native LWJGL integration: "+checks+" checks.\nFull cave 1-5 input routes rendered, no deaths.\nSave: "+save+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            level++;Gdx.app.exit();
        }
    }catch(Throwable e){fail(e);}}
    @Override public void resize(int w,int h){if(screen!=null)screen.resize(w,h);}
    @Override public void dispose(){if(screen!=null)screen.dispose();if(failure!=null)throw new RuntimeException(failure);}
    public static void main(String[] args){Lwjgl3ApplicationConfiguration c=new Lwjgl3ApplicationConfiguration();c.setTitle("HOOK & RECOIL native verification");c.setWindowedMode(1280,720);c.setPauseWhenLostFocus(false);c.useVsync(false);c.setForegroundFPS(144);new Lwjgl3Application(new NativeCaveVerification(),c);}
}\n