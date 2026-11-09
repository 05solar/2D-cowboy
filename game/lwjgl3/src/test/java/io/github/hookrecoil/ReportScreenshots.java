/* 파일: ReportScreenshots.java
 * 역할: 중간 프로젝트 보고서에 쓸 실제 게임 화면 4종을 실제 창에서 캡처한다.
 * 주요 책임: 그래플 스윙·챕터별(동굴/광산/지하도시) 장면·전체 클리어 결과·일시정지 화면을
 *          실제 GameplayScreen 렌더 경로로 각각 재현해 PNG로 저장한다. 자동 검증 assertion은
 *          없으며 보고서용 자료 생성 전용이다.
 * 협력 대상: GameplayScreen, StageRuntime. 저장 진행은 격리된 임시 경로만 사용한다.
 * 관련 사양: docs/10_PROLOGUE.md, docs/12_CAVE_CHAPTER.md, docs/MINE_06_10_ROUTE_REWORK_2026-11-09.md,
 *          docs/CITY_11_15_REWORK_2026-11-09.md.
 */
package io.github.hookrecoil;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import io.github.hookrecoil.input.InputFrame;
import io.github.hookrecoil.screens.GameplayScreen;
import io.github.hookrecoil.stage.StageRuntime;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ReportScreenshots extends ApplicationAdapter {
    private GameplayScreen screen;
    private Path output;

    private Object field(String name) throws Exception {
        Field f=GameplayScreen.class.getDeclaredField(name);f.setAccessible(true);return f.get(screen);
    }
    private void setField(String name,Object value) throws Exception {
        Field f=GameplayScreen.class.getDeclaredField(name);f.setAccessible(true);f.set(screen,value);
    }
    private void loadStage(String id) throws Exception {
        Method load=GameplayScreen.class.getDeclaredMethod("loadStage",String.class,boolean.class);
        load.setAccessible(true);load.invoke(screen,id,false);
    }
    private void cancelIntro() throws Exception {
        Object introObj=field("introCamera");
        Method cancel=introObj.getClass().getDeclaredMethod("cancel");cancel.setAccessible(true);cancel.invoke(introObj);
    }
    private void settle(int frames) {for(int i=0;i<frames;i++)screen.render(1f/60);}
    private void capture(String name) {
        Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
        PixmapIO.writePNG(Gdx.files.absolute(output.resolve(name+".png").toString()),p,6,true);p.dispose();
        System.out.println("captured "+name);
    }

    @Override public void create() {
        try {
            output=Paths.get("build/report-screenshots").toAbsolutePath();Files.createDirectories(output);
            Path save=Files.createTempDirectory(output,"isolated-save-");
            System.setProperty("hookrecoil.saveDir",save.toString());
            screen=new GameplayScreen();
            screen.resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());

            // 사진 1: 프롤로그 천장 링 스윙 장면. 실제 PrologueRouteDriver 입력으로 첫 번째 링에
            // 실제로 연결되는 순간까지 정상 재생한 뒤 캡처해, 임의로 끼워 넣은 상태가 아니라
            // 물리적으로 일관된 스윙 자세를 보장한다.
            loadStage("prologue");
            DrivenInputRouter swingInput=new DrivenInputRouter();
            PrologueRouteDriver swingDriver=new PrologueRouteDriver();
            swingInput.setSource(()->{
                try {return swingDriver.next((StageRuntime)field("stage"));}
                catch(Exception e) {throw new RuntimeException(e);}
            });
            setField("input",swingInput);
            Gdx.input.setInputProcessor(swingInput);
            StageRuntime prologue=(StageRuntime)field("stage");
            for(int i=0;i<1200&&prologue.grapple.attached!=0;i++)screen.render(1f/60);
            settle(8);
            capture("photo1_grapple_swing");
            // 이후 캡처는 입력이 전혀 없어야 하므로(제자리에서 가만히 서 있어야 안전) 빈 입력으로 되돌린다.
            swingInput.setSource(InputFrame::new);
            Gdx.input.setInputProcessor(null);

            // 사진 2: 동굴·광산·지하도시 대표 장면. 시작 연출(전체 맵 축소 보기)을 그대로 활용해,
            // 플레이어를 임의로 옮기다 죽는 위험 없이 맵 전체와 장치를 안전하게 보여준다.
            loadStage("cave_04");
            settle(84);
            capture("photo2_cave");

            loadStage("mine_03");
            settle(84);
            capture("photo2_mine");

            loadStage("city_03");
            settle(84);
            capture("photo2_city");

            // 사진 3: 전체 클리어(ALL CLEAR) 결과 화면
            loadStage("mine_05");
            StageRuntime last=(StageRuntime)field("stage");
            last.complete=true;last.elapsed=321f;last.shots=112;last.deaths=0;
            cancelIntro();
            Object progressObj=field("progress");
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
            screen.render(1f/60);
            capture("photo3_all_clear_raw");
            // 같은 프레임에 다른 글자를 먼저 그리면 ALL CLEAR 부제의 한글 음절이 바뀌어 보이는
            // libGDX 렌더링 문제(docs/09_DECISIONS_AND_STATUS.md 2026-11-09)가 있어, 이 한 줄만
            // 별도 배치의 단일 호출로 다시 그려 보고서용 합성 패치로 함께 남긴다.
            com.badlogic.gdx.graphics.g2d.BitmapFont patchFont=new com.badlogic.gdx.graphics.g2d.BitmapFont(Gdx.files.internal("ui/fonts/prologue.fnt"));
            com.badlogic.gdx.graphics.g2d.SpriteBatch patchBatch=new com.badlogic.gdx.graphics.g2d.SpriteBatch();
            com.badlogic.gdx.graphics.OrthographicCamera patchCam=new com.badlogic.gdx.graphics.OrthographicCamera();
            patchCam.setToOrtho(false,2560,1440);
            Gdx.gl.glClearColor(0.045f,0.07f,0.085f,1);Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT);
            patchBatch.setProjectionMatrix(patchCam.combined);patchBatch.begin();
            patchFont.getData().setScale(1.7f*0.25f);
            patchFont.setColor(0.94f,0.9f,0.78f,1);
            com.badlogic.gdx.graphics.g2d.GlyphLayout layout=new com.badlogic.gdx.graphics.g2d.GlyphLayout(
                patchFont,"전체 15개 스테이지를 모두 클리어했습니다");
            patchFont.draw(patchBatch,layout,1280-layout.width*0.5f,805);
            patchBatch.end();
            capture("photo3_all_clear_subtitle_patch");
            patchBatch.dispose();patchFont.dispose();

            // 사진 4: 플레이 도중 ESC로 멈춘 일시정지 화면
            loadStage("cave_02");
            cancelIntro();
            settle(40);
            setField("paused",true);
            settle(3);
            capture("photo4_pause");

            Gdx.app.exit();
        } catch(Throwable t) {t.printStackTrace();Gdx.app.exit();}
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration c=new Lwjgl3ApplicationConfiguration();
        c.setTitle("HOOK & RECOIL - 보고서 스크린샷");
        c.setWindowedMode(1280,720);
        c.setPauseWhenLostFocus(false);
        new Lwjgl3Application(new ReportScreenshots(),c);
    }
}
