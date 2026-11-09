/* 파일: GameplayScreen.java
 * 역할: 게임 플레이 화면 조립. 책임: ID별 스테이지·모달 입력·120Hz 루프·가로/세로 카메라.
 * 협력: StageRuntime, GameAssets, CaveAssets, CityRenderer, ProgressStore, StageSelectView. 화면 종료 때 자원 해제.
 * 관련 사양: docs/10_PROLOGUE.md, docs/12_CAVE_CHAPTER.md, docs/CITY_CHAPTER_V2.md.
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
    private final FitViewport uiViewport=new FitViewport(2560,1440,ui);
    private final CameraConfig cameraConfig=new Json().fromJson(CameraConfig.class,Gdx.files.internal("config/camera.json"));
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
    private final CityBackgroundSelector cityBackgroundSelector=new CityBackgroundSelector();
    private float cameraY=520;
    private final com.badlogic.gdx.audio.Sound warningSound=Gdx.audio.newSound(Gdx.files.internal("audio/crumble_warning.wav"));
    private final Vector3 mouse=new Vector3();
    private double accumulator;
    private float cameraX;
    private boolean paused,debug;
    private int frames;
    public GameplayScreen() {
        resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
        String base=System.getenv("LOCALAPPDATA");if(base==null)base=System.getProperty("user.home");
        String isolated=System.getProperty("hookrecoil.saveDir");
        boolean preview=System.getProperty("hookrecoil.screenshot")!=null;
        progress=new ProgressStore(isolated!=null?Paths.get(isolated):preview?Paths.get("build/preview-save"):Paths.get(base,"HookRecoil"));
        ProgressStore.Pending recovered=null;String error=null;
        try{recovered=progress.load();}catch(Exception e){error="진행 저장을 읽지 못했습니다. 원본은 보존됩니다.";}
        renderer.cave=caveRenderer;renderer.mine=mineRenderer;renderer.city=cityRenderer;loadStage("prologue",false);
        cameraConfig.validate();
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
        if(cp>=0 && cp<def.checkpoints.length) {stage.checkpoint=cp;stage.respawn();cameraX=stage.player.x*c.pixelsPerH+500*(viewport.getWorldWidth()/2560f);}
        if(preview&&Boolean.getBoolean("hookrecoil.previewGem")) {stage.player.reset(def.clearGem.x-1.2f,2);cameraX=stage.player.x*c.pixelsPerH+430*(viewport.getWorldWidth()/2560f);}
        if(preview&&System.getProperty("hookrecoil.previewCollection")!=null) {
            stage.player.reset(def.clearGem.x,2);stage.player.facingRight=!Boolean.getBoolean("hookrecoil.previewLeft");
            stage.resolveGoal(false);stage.advancePresentation(Double.parseDouble(System.getProperty("hookrecoil.previewCollection")));
            cameraX=stage.player.x*c.pixelsPerH+430*(viewport.getWorldWidth()/2560f);
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
        cityBackgroundSelector.reset();
        stage.noCheckpoint=challenge;stage.completionSink=progress::begin;stage.resultSink=s->progress.finish();
        cameraX=1280;cameraY=cameraConfig.prologueCameraY;camera.zoom=cameraConfig.gameplayZoom;accumulator=0;
        if(System.getProperty("hookrecoil.screenshot")==null) {
            introCamera.begin(stage.definition,config.pixelsPerH,cameraConfig.gameplayZoom,viewport.getWorldWidth(),viewport.getWorldHeight(),
                cameraConfig.verticalFollowOffset,cameraConfig.lowerEdgeAllowance);
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
        if(input.newRunRequested) {if(stage.complete) {if(progress.allCleared(catalog))loadStage("cave_01",false);else stage.restartAll();input.clear();}input.newRunRequested=false;}
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
                float menuX=(Gdx.input.getX()-uiViewport.getScreenX())*2560f/uiViewport.getScreenWidth();
                float menuY=(mouseY-uiViewport.getScreenY())*1440f/uiViewport.getScreenHeight();
                boolean retryClick=frozenCommand.fire&&menuX>=730&&menuX<=1830&&menuY>=535&&menuY<=595;
                if(stage.complete && (frozenCommand.restart||retryClick)) {stage.restartAll();cameraX=viewport.getWorldWidth()*.5f;}
                input.clear();accumulator=0;
                if(System.getProperty("hookrecoil.previewCollection")==null)stage.advancePresentation(delta);
            } else {
            accumulator+=Math.min(delta,0.15f);
            while(accumulator>=1.0/120) {
                InputFrame command=input.consume(ax,ay);
                stage.tick(command,1f/120);if(stage.devices.warning)warningSound.play(.4f);accumulator-=1.0/120;
                if(stage.justReset) {input.clear();cameraX=Math.max(viewport.getWorldWidth()*.5f,stage.player.x*stage.config.pixelsPerH+500*(viewport.getWorldWidth()/2560f));}
                if(stage.justCollected) {input.clear();accumulator=0;break;}
            }
            }
        } else {
            InputFrame commands=input.consume(ax,ay);
            if(commands.restart && !stage.clear.frozen()) {stage.respawn();paused=false;input.clear();}
        }
        float target=stage.player.x*stage.config.pixelsPerH+430*(viewport.getWorldWidth()/2560f);
        if(introFrame) {cameraX=introCamera.x();cameraY=introCamera.y();camera.zoom=introCamera.zoom();}
        else {
            camera.zoom=cameraConfig.gameplayZoom;
            if(!stage.clear.frozen()&&!paused)cameraX=MathUtils.lerp(cameraX,target,1-(float)Math.exp(-3f*delta));
        }
        if(!introFrame&&!stage.clear.frozen()&&!paused) {
            float scale=viewport.getWorldHeight()/1440f;
            float desiredY=stage.player.y*config.pixelsPerH+cameraConfig.verticalFollowOffset*scale;
            float error=desiredY-cameraY,deadZone=cameraConfig.verticalFollowDeadZone*scale;
            if(Math.abs(error)>deadZone) {
                float followTarget=desiredY-Math.signum(error)*deadZone;
                cameraY=MathUtils.lerp(cameraY,followTarget,1-(float)Math.exp(-2.5f*delta));
            }
        }
        float halfCameraWidth=viewport.getWorldWidth()*.5f*camera.zoom,halfCameraHeight=viewport.getWorldHeight()*.5f*camera.zoom;
        float worldWidth=stage.definition.width*config.pixelsPerH;
        float worldHeight=stage.definition.height*config.pixelsPerH;
        float minCameraX=Math.min(halfCameraWidth,worldWidth*.5f),maxCameraX=Math.max(minCameraX,worldWidth-halfCameraWidth);
        float lowerMargin=cameraConfig.lowerEdgeAllowance*(viewport.getWorldHeight()/1440f);
        float minCameraY=Math.min(Math.max(0,halfCameraHeight-lowerMargin),worldHeight*.5f);
        float maxCameraY=Math.max(minCameraY,worldHeight-halfCameraHeight);
        cameraX=MathUtils.clamp(cameraX,minCameraX,maxCameraX);
        cameraY=MathUtils.clamp(cameraY,minCameraY,maxCameraY);
        if("prologue".equals(stage.definition.id)&&!introFrame)cameraY=cameraConfig.prologueCameraY;
        camera.position.set(Math.round(cameraX),Math.round(cameraY),0);camera.update();
        ScreenUtils.clear(0.018f,0.027f,0.04f,1);
        uiViewport.apply();batch.setProjectionMatrix(ui.combined);batch.begin();
        // 도시 배경은 지정 카메라 구역에서만 전환하고 두 장을 짧게 겹쳐 그린다.
        StageCatalog.Entry entry=catalog.find(stage.definition.id);
        float viewWidth=viewport.getWorldWidth(),viewHeight=viewport.getWorldHeight();
        float progressX=MathUtils.clamp((cameraX-viewWidth*.5f)/Math.max(1,stage.definition.width*config.pixelsPerH-viewWidth),0,1);
        float progressY=MathUtils.clamp((cameraY-viewHeight*.5f)/Math.max(1,stage.definition.height*config.pixelsPerH-viewHeight),0,1);
        if(stage.definition.id.startsWith("city_")) {
            cityBackgroundSelector.update(stage.definition,cameraX/config.pixelsPerH,cameraY/config.pixelsPerH,
                delta,!paused);
            float blend=cityBackgroundSelector.blend();
            if(cityBackgroundSelector.previousPath()!=null&&blend<1)
                drawPanorama(caveAssets.background(cityBackgroundSelector.previousPath()),progressX,progressY,.64f,.70f,.76f,1-blend);
            String path=cityBackgroundSelector.activePath();
            Texture bg=caveAssets.background(path==null?(entry==null?null:entry.background):path);
            drawPanorama(bg,progressX,progressY,.64f,.70f,.76f,cityBackgroundSelector.previousPath()==null?1:blend);
        } else {
            Texture bg=entry==null?assets.background:caveAssets.background(entry.background);
            float tintR=stage.definition.id.startsWith("mine_")?.78f:.52f;
            float tintG=stage.definition.id.startsWith("mine_")?.70f:.60f;
            float tintB=stage.definition.id.startsWith("mine_")?.54f:.66f;
            drawPanorama(bg,progressX,progressY,tintR,tintG,tintB,1);
        }
        batch.setColor(Color.WHITE);batch.end();
        batch.setProjectionMatrix(camera.combined);batch.begin();renderer.draw(batch,stage,paused?1:(float)(accumulator*120));batch.end();
        batch.setProjectionMatrix(ui.combined);batch.begin();hud.draw(batch,stage,paused,debug,stage.complete&&progress.allCleared(catalog));
        if(input.insideViewport && !paused && !introFrame && !stage.clear.frozen()) {
            float x=(Gdx.input.getX()-uiViewport.getScreenX())*2560f/uiViewport.getScreenWidth();
            float y=(mouseY-uiViewport.getScreenY())*1440f/uiViewport.getScreenHeight();
            hud.reticle(batch,x,y,stage.grapple.aimTarget(stage.player)>=0);
        }
        batch.end();
        capture();
    }
    private void drawPanorama(Texture texture,float progressX,float progressY,float r,float g,float b,float alpha) {
        if(texture==null)return;
        float viewWidth=viewport.getWorldWidth(),viewHeight=viewport.getWorldHeight();
        float cover=Math.max(viewWidth/texture.getWidth(),viewHeight/texture.getHeight());
        float width=texture.getWidth()*cover,height=texture.getHeight()*cover;
        batch.setColor(r,g,b,alpha);
        batch.draw(texture,-(width-viewWidth)*progressX,-(height-viewHeight)*progressY,width,height);
        batch.setColor(Color.WHITE);
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
    @Override public void resize(int width,int height) {
        // 월드 좌표/화면 픽셀 비율을 1280×720 기준과 같게 유지하면서 창이 커진 만큼 더 넓게 보여준다.
        float logicalHeight=Math.max(1,height)*2f,logicalWidth=logicalHeight*16f/9f;
        viewport.setWorldSize(logicalWidth,logicalHeight);viewport.update(width,height);
        uiViewport.update(width,height);if(menu!=null)menu.resize(width,height);
    }
    @Override public void pause() {paused=true;input.clear();accumulator=0;}
    @Override public void resume() {input.clear();}
    @Override public void dispose() {Gdx.input.setInputProcessor(null);batch.dispose();hud.dispose();assets.dispose();caveRenderer.dispose();caveAssets.dispose();warningSound.dispose();}
}
