/* 파일: BackgroundMusic.java
 * 역할: 전체 화면 공통 배경음악 재생. 책임: 무한 반복 재생 시작과 음소거 토글, 자원 해제.
 * 협력: HookRecoilGame(수명 소유), Gdx.audio.Music. 관련 사양: 없음(신규 기능).
 */
package io.github.hookrecoil.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

public final class BackgroundMusic {
    private final Music music=Gdx.audio.newMusic(Gdx.files.internal("audio/background_music.ogg"));
    private boolean muted;
    public BackgroundMusic() {music.setLooping(true);music.setVolume(.5f);music.play();}
    public void toggleMute() {
        muted=!muted;
        if(muted) music.pause();else music.play();
    }
    public boolean muted() {return muted;}
    public void dispose() {music.dispose();}
}
