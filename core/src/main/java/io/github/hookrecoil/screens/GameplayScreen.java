/* 파일: GameplayScreen.java
 * 역할: 게임 플레이 화면 조립. 책임: ID별 스테이지·모달 입력·120Hz 루프·가로/세로 카메라.
 * 협력: StageRuntime, GameAssets, CaveAssets, CityRenderer, ProgressStore, StageSelectView. 화면 종료 때 자원 해제.
 * 관련 사양: docs/10_PROLOGUE.md, docs/12_CAVE_CHAPTER.md, docs/assets/city_chapter/STAGES_11_15.md.
 */
package io.github.hookrecoil.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.hookrecoil.assets.GameAssets;
import io.github.hookrecoil.input.*;
import io.github.hookrecoil.physics.PhysicsConfig;
import io.github.hookrecoil.rendering.WorldRenderer;
import io.github.hookrecoil.stage.*;
import io.github.hookrecoil.ui.HudView;
import io.github.hookrecoil.save.ProgressStore;
import io.github.hookrecoil.assets.CaveAssets;
import io.github.hookrecoil.rendering.CaveRenderer;
import io.github.hookrecoil.rendering.MineRenderer;
import io.github.hookrecoil.rendering.CityRenderer;
import io.github.hookrecoil.ui.StageSelectView;
import java.nio.file.Paths;

public class GameplayScreen extends ScreenAdapter {
    private final SpriteBatch batch=new SpriteBatch();
    private final GameAssets assets=new GameAssets();
    private final WorldRenderer renderer=new WorldRenderer(assets);
    private final HudView hud=new HudView();
    private final OrthographicCamera camera=new OrthographicCamera();
    private final OrthographicCamera ui=new OrthographicCamera();
    private final FitViewport viewport=new FitViewport(2560,1440,camera);
    private final InputRouter input=new InputRouter();
    private StageRuntime stage;
    private final PhysicsConfig config=new Json().fromJson(PhysicsConfig.class,Gdx.files.internal("config/physics.json"));
    private final StageCatalog catalog=new Json().fromJson(StageCatalog.class,Gdx.files.internal("cave_chapter/stages.json"));
    private final CaveAssets caveAssets=new CaveAssets();
    private final CaveRenderer caveRenderer=new CaveRenderer(caveAssets);
    private final MineRenderer mineRenderer=new MineRenderer(caveAssets);
    private final CityRenderer cityRenderer=new CityRenderer(caveAssets);
    private final ProgressStore progress;
    private StageSelectView menu;
    private final StageIntroCamera introCamera=new StageIntroCamera();
    private float cameraY=800;
    private final com.badlogic.gdx.audio.Sound warningSound=Gdx.audio.newSound(Gdx.files.internal("audio/crumble_warning.wav"));
    private final Vector3 mouse=new Vector3();
    private double accumulator;
    private float cameraX;
    private boolean paused,debug;
    private int frames;
    public GameplayScreen() {
        String base=System.getenv("LOCALAPPDATA");if(base==null)base=System.getProperty("user.home");
        String isolated=System.getProperty("hookrecoil.saveDir");
        boolean preview=System.getProperty("hookrecoil.screenshot")!=null;
        progress=new ProgressStore(isolated!=null?Paths.get(isolated):preview?Paths.get("build/preview-save"):Paths.get(base,"HookRecoil"));
        ProgressStore.Pending recovered=null;String error=null;
        try{recovered=progress.load();}catch(Exception e){error="진행 저장을 읽지 못했습니다. 원본은 보존됩니다.";}
        renderer.cave=caveRenderer;renderer.mine=mineRenderer;renderer.city=cityRenderer;loadStage("prologue",false);
        if(recovered!=null&&("prologue".equals(recovered.stageId)||(catalog.find(recovered.stageId)!=null&&catalog.find(recovered.stageId).available))){
            loadStage(recovered.stageId,recovered.noCheckpoint);stage.player.reset(recovered.x,recovered.y);
            stage.elapsed=recovered.seconds;stage.deaths=recovered.deaths;stage.shots=recovered.shots;
            stage.clear.begin(recovered.x,recovered.y,recovered.facingRight,recovered.seconds);stage.advancePresentation(stage.clear.durationSeconds());
            introCamera.cancel();
        }
        if(preview&&System.getProperty("hookrecoil.previewStage")!=null)loadStage(System.getProperty("hookrecoil.previewStage"),false);
        stage.saveError=error;
        StageDefinition def=stage.definition;PhysicsConfig c=config;
        // 캡처용 시작 지점은 배포 기본 동작과 분리한다.
        int cp=Integer.getInteger("hookrecoil.previewCheckpoint",-1);
        if(cp>=0 && cp<def.checkpoints.length) {stage.checkpoint=cp;stage.respawn();cameraX=stage.player.x*c.pixelsPerH+500;}
        if(preview&&Boolean.getBoolean("hookrecoil.previewGem")) {stage.player.reset(def.clearGem.x-1.2f,2);cameraX=stage.player.x*c.pixelsPerH+430;}
        if(preview&&System.getProperty("hookrecoil.previewCollection")!=null) {
            stage.player.reset(def.clearGem.x,2);stage.player.facingRight=!Boolean.getBoolean("hookrecoil.previewLeft");
            stage.resolveGoal(false);stage.advancePresentation(Double.parseDouble(System.getProperty("hookrecoil.previewCollection")));
            cameraX=stage.player.x*c.pixelsPerH+430;
        }
        Gdx.input.setInputProcessor(input);
        ui.setToOrtho(false,2560,1440);
        if(Boolean.getBoolean("hookrecoil.previewMenu")||(!preview&&progress.cleared("prologue")&&recovered==null))openMenu();
    }
    private void loadStage(String id,boolean challenge){
        StageCatalog.Entry e=catalog.find(id);
        boolean preview=System.getProperty("hookrecoil.screenshot")!=null;
        if(!"prologue".equals(id)&&(e==null||(!e.available&&!preview)))throw new IllegalArgumentException("Unavailable stage: "+id);
        stage=new StageRuntime(new Json().fromJson(StageDefinition.class,Gdx.files.internal("stages/"+id+".json")),config);
        stage.noCheckpoint=challenge;stage.completionSink=progress::begin;stage.resultSink=s->progress.finish();
        cameraX=1280;cameraY=800;accumulator=0;
        if(System.getProperty("hookrecoil.screenshot")==null) {
            float maxX=Math.max(1280,stage.definition.width*config.pixelsPerH-1280);
            float maxY=Math.max(800,stage.definition.height*config.pixelsPerH-720);
            float startX=MathUtils.clamp(stage.player.x*config.pixelsPerH+430,1280,maxX);
            float startY=MathUtils.clamp(stage.player.y*config.pixelsPerH+300,800,maxY);
            float gemX=MathUtils.clamp(stage.definition.clearGem.x*config.pixelsPerH+430,1280,maxX);
            float gemY=MathUtils.clamp(stage.definition.clearGem.y*config.pixelsPerH+300,800,maxY);
            introCamera.begin(startX,startY,gemX,gemY);
        } else introCamera.cancel();
    }
    public void openMenu(){
        if(menu!=null)return;input.clear();accumulator=0;
        menu=new StageSelectView(catalog,progress,caveAssets,hud,new StageSelectView.Actions(){
            public void close(){closeMenu();}
            public void start(String id,boolean challenge){
                if(!"prologue".equals(id)&&(!progress.canStart(catalog.find(id))||(challenge&&!progress.cleared(id))))return;
                closeMenu();loadStage(id,challenge);paused=false;
            }
        });
        if(!"prologue".equals(stage.definition.id))menu.selectedStageId=stage.definition.id;
        Gdx.input.setInputProcessor(menu);
    }
    private void closeMenu(){menu=null;Gdx.input.setInputProcessor(input);input.clear();accumulator=0;}
    @Override public void render(float delta) {
        if(input.menuRequested){input.menuRequested=false;openMenu();}
        if(input.nextRequested){input.nextRequested=false;if(stage.complete){String next=catalog.next(stage.definition.id);if(progress.canStart(catalog.find(next)))loadStage(next,false);else openMenu();}input.clear();}
        if(menu!=null){ScreenUtils.clear(.067f,.098f,.122f,1);menu.draw(batch);capture();return;}
        if(input.pauseRequested) {paused=!paused;input.pauseRequested=false;input.clear();accumulator=0;}
        if(input.debugRequested) {debug=!debug;input.debugRequested=false;}
        if(input.newRunRequested) {if(stage.complete) {stage.restartAll();input.clear();}input.newRunRequested=false;}
        int mouseY=Gdx.graphics.getHeight()-Gdx.input.getY();
        input.insideViewport=Gdx.input.getX()>=viewport.getScreenX() && Gdx.input.getX()<viewport.getScreenX()+viewport.getScreenWidth()
            && mouseY>=viewport.getScreenY() && mouseY<viewport.getScreenY()+viewport.getScreenHeight();
        mouse.set(Gdx.input.getX(),Gdx.input.getY(),0);viewport.unproject(mouse);
        float ax=mouse.x/stage.config.pixelsPerH-stage.player.x,ay=mouse.y/stage.config.pixelsPerH-stage.player.y-stage.config.height*0.65f;
        float len=(float)Math.sqrt(ax*ax+ay*ay);
        if(input.insideViewport && len>0.001f) {ax/=len;ay/=len;} else {ax=stage.player.aimX;ay=stage.player.aimY;}
        boolean introFrame=introCamera.active();
        if(introFrame) {introCamera.advance(delta,paused);input.clear();}
        if(!paused&&!introFrame) {
            boolean wasFrozen=stage.clear.frozen();
            if(wasFrozen) {
                InputFrame frozenCommand=input.consume(ax,ay);
                float menuX=(Gdx.input.getX()-viewport.getScreenX())*2560f/viewport.getScreenWidth();
                float menuY=(mouseY-viewport.getScreenY())*1440f/viewport.getScreenHeight();
                boolean retryClick=frozenCommand.fire&&menuX>=730&&menuX<=1830&&menuY>=600&&menuY<=660;
                if(stage.complete && (frozenCommand.restart||retryClick)) {stage.restartAll();cameraX=1280;}
                input.clear();accumulator=0;
                if(System.getProperty("hookrecoil.previewCollection")==null)stage.advancePresentation(delta);
            } else {
            accumulator+=Math.min(delta,0.15f);
            while(accumulator>=1.0/120) {
                InputFrame command=input.consume(ax,ay);
                stage.tick(command,1f/120);if(stage.devices.warning)warningSound.play(.4f);accumulator-=1.0/120;
                if(stage.justReset) {input.clear();cameraX=Math.max(1280,stage.player.x*stage.config.pixelsPerH+500);}
                if(stage.justCollected) {input.clear();accumulator=0;break;}
            }
            }
        } else {
            InputFrame commands=input.consume(ax,ay);
            if(commands.restart && !stage.clear.frozen()) {stage.respawn();paused=false;input.clear();}
        }
        float target=stage.player.x*stage.config.pixelsPerH+430;
        if(introFrame) {cameraX=introCamera.x();cameraY=introCamera.y();}
        else if(!stage.clear.frozen()&&!paused)cameraX=MathUtils.lerp(cameraX,target,1-(float)Math.exp(-5*delta));
        cameraX=MathUtils.clamp(cameraX,1280,stage.definition.width*stage.config.pixelsPerH-1280);
        if(!introFrame&&!stage.clear.frozen()&&!paused)cameraY=MathUtils.lerp(cameraY,stage.player.y*config.pixelsPerH+300,1-(float)Math.exp(-5*delta));
        cameraY=MathUtils.clamp(cameraY,800,Math.max(800,stage.definition.height*config.pixelsPerH-720));
        if("prologue".equals(stage.definition.id))cameraY=800;
        camera.position.set(Math.round(cameraX),Math.round(cameraY),0);camera.update();
        ScreenUtils.clear(0.018f,0.027f,0.04f,1);
        viewport.apply();batch.setProjectionMatrix(ui.combined);batch.begin();
        // 한 장을 넓게 cover하고 조금만 움직인다. 반복 타일링하지 않는다.
        StageCatalog.Entry entry=catalog.find(stage.definition.id);
        Texture bg=entry==null?assets.background:caveAssets.background(entry.background);
        float cover=Math.max(2560f/bg.getWidth(),1440f/bg.getHeight());
        float bgWidth=bg.getWidth()*cover,bgHeight=bg.getHeight()*cover;
        float progressX=MathUtils.clamp((cameraX-1280)/Math.max(1,stage.definition.width*config.pixelsPerH-2560),0,1);
        float progressY=MathUtils.clamp((cameraY-720)/Math.max(1,stage.definition.height*config.pixelsPerH-1440),0,1);
        if(stage.definition.id.startsWith("mine_"))batch.setColor(.78f,.70f,.54f,1);else if(stage.definition.id.startsWith("city_"))batch.setColor(.64f,.70f,.76f,1);else batch.setColor(.52f,.60f,.66f,1);
        batch.draw(bg,-(bgWidth-2560)*progressX,-(bgHeight-1440)*progressY,bgWidth,bgHeight);
        batch.setColor(Color.WHITE);batch.end();
        batch.setProjectionMatrix(camera.combined);batch.begin();renderer.draw(batch,stage,paused?1:(float)(accumulator*120));batch.end();
        batch.setProjectionMatrix(ui.combined);batch.begin();hud.draw(batch,stage,paused,debug);
        if(input.insideViewport && !paused && !introFrame && !stage.clear.frozen()) {
            float x=(Gdx.input.getX()-viewport.getScreenX())*2560f/viewport.getScreenWidth();
            float y=(mouseY-viewport.getScreenY())*1440f/viewport.getScreenHeight();
            hud.reticle(batch,x,y,stage.grapple.aimTarget(stage.player)>=0);
        }
        batch.end();
        capture();
    }
    private void capture(){
        frames++;
        String screenshot=System.getProperty("hookrecoil.screenshot");
        if(screenshot!=null && frames==30) {
            Pixmap image=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
            PixmapIO.writePNG(Gdx.files.absolute(screenshot),image,6,true);image.dispose();
            Gdx.app.log("SmokeTest","Screenshot captured; closing normally.");Gdx.app.exit();
        }
    }
    @Override public void resize(int width,int height) {viewport.update(width,height);if(menu!=null)menu.resize(width,height);}
    @Override public void pause() {paused=true;input.clear();accumulator=0;}
    @Override public void resume() {input.clear();}
    @Override public void dispose() {Gdx.input.setInputProcessor(null);batch.dispose();hud.dispose();assets.dispose();caveRenderer.dispose();caveAssets.dispose();warningSound.dispose();}
}
