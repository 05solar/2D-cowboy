/* 파일: HookRecoilGame.java
 * 역할: 게임 진입점. 책임: 프롤로그 화면 생성·종료 수명 연결.
 * 협력: GameplayScreen, 데스크톱 런처. 관련 사양: docs/10_PROLOGUE.md.
 */
package io.github.hookrecoil;

import com.badlogic.gdx.Game;
import io.github.hookrecoil.screens.GameplayScreen;

public class HookRecoilGame extends Game {
    @Override public void create() {setScreen(new GameplayScreen());}
    @Override public void dispose() {if(screen!=null)screen.dispose();}
}
