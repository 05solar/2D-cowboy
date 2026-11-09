/* 파일: HookRecoilGame.java
 * 역할: 게임 진입점. 책임: 프롤로그 화면 생성·종료 수명 연결, 배경음악 재생·음소거 입력 중계.
 * 협력: GameplayScreen, BackgroundMusic, 데스크톱 런처. 관련 사양: docs/10_PROLOGUE.md.
 */
package io.github.hookrecoil;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import io.github.hookrecoil.audio.BackgroundMusic;
import io.github.hookrecoil.screens.GameplayScreen;

public class HookRecoilGame extends Game {
    private BackgroundMusic music;
    @Override public void create() {music=new BackgroundMusic();setScreen(new GameplayScreen());}
    @Override public void render() {
        if(Gdx.input.isKeyJustPressed(Input.Keys.F2)) music.toggleMute();
        super.render();
    }
    @Override public void dispose() {if(screen!=null)screen.dispose();music.dispose();}
}
