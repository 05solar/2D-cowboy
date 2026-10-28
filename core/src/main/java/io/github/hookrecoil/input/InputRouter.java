/* 파일: InputRouter.java
 * 역할: 키·마우스 이벤트를 고정 틱에 전달. 책임: 짧은 클릭 보존·복귀 입력 차단·여백 제외.
 * 협력: GameplayScreen, InputFrame. 수명: 화면이 설치/해제. 관련 사양: docs/03_FEATURE_SPEC.md.
 */
package io.github.hookrecoil.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

public class InputRouter extends InputAdapter {
    private boolean left,right,jump,fire,hookPress,hookHeld,restart,blockedFire,blockedHook,blockedJump;
    private boolean blockedRestart,blockedEnter;
    public boolean pauseRequested,debugRequested,newRunRequested,menuRequested,nextRequested;
    public boolean insideViewport=true;
    @Override public boolean keyDown(int key) {
        if(key==Input.Keys.A || key==Input.Keys.LEFT) left=true;
        if(key==Input.Keys.D || key==Input.Keys.RIGHT) right=true;
        if(key==Input.Keys.SPACE && !blockedJump) {jump=true;blockedJump=true;}
        if(key==Input.Keys.R&&!blockedRestart) {restart=true;blockedRestart=true;}
        if(key==Input.Keys.ESCAPE) pauseRequested=true;
        if(key==Input.Keys.M)menuRequested=true;
        if(key==Input.Keys.N)nextRequested=true;
        if(key==Input.Keys.F1) debugRequested=true;
        if(key==Input.Keys.ENTER&&!blockedEnter) {newRunRequested=true;blockedEnter=true;}
        return true;
    }
    @Override public boolean keyUp(int key) {
        if(key==Input.Keys.A || key==Input.Keys.LEFT) left=false;
        if(key==Input.Keys.D || key==Input.Keys.RIGHT) right=false;
        if(key==Input.Keys.SPACE) blockedJump=false;
        if(key==Input.Keys.R) blockedRestart=false;
        if(key==Input.Keys.ENTER) blockedEnter=false;
        return true;
    }
    @Override public boolean touchDown(int x,int y,int pointer,int button) {
        if(!insideViewport) return false;
        if(button==Input.Buttons.LEFT && !blockedFire) {fire=true;blockedFire=true;}
        if(button==Input.Buttons.RIGHT && !blockedHook) {hookPress=true;hookHeld=true;blockedHook=true;}
        return true;
    }
    @Override public boolean touchUp(int x,int y,int pointer,int button) {
        if(button==Input.Buttons.LEFT) blockedFire=false;
        if(button==Input.Buttons.RIGHT) {blockedHook=false;hookHeld=false;}
        return true;
    }
    public InputFrame consume(float aimX,float aimY) {
        InputFrame in=new InputFrame();
        in.move=(right?1:0)-(left?1:0);in.jump=jump;in.fire=fire;
        in.hookPressed=hookPress;in.hookHeld=hookHeld;in.restart=restart;
        in.aimX=aimX;in.aimY=aimY;jump=fire=hookPress=restart=false;
        return in;
    }
    public void clear() {
        left=right=jump=fire=hookPress=hookHeld=restart=false;
        newRunRequested=false;
        blockedFire=com.badlogic.gdx.Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        blockedHook=com.badlogic.gdx.Gdx.input.isButtonPressed(Input.Buttons.RIGHT);
        blockedJump=com.badlogic.gdx.Gdx.input.isKeyPressed(Input.Keys.SPACE);
        blockedRestart=com.badlogic.gdx.Gdx.input.isKeyPressed(Input.Keys.R);
        blockedEnter=com.badlogic.gdx.Gdx.input.isKeyPressed(Input.Keys.ENTER);
    }
}
