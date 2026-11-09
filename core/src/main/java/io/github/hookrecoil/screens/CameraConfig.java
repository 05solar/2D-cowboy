/* 파일: CameraConfig.java
 * 역할: 카메라 표시 설정. 주요 책임: 플레이 줌·수직 추적·맵 하단 여유를 설정 데이터에서 제공한다.
 * 협력 대상: GameplayScreen, StageIntroCamera. 수명: GameplayScreen이 로드해 화면 수명 동안 보유한다.
 * 관련 사양: docs/13_STAGE_INTRO_CAMERA.md, assets/config/camera.json.
 */
package io.github.hookrecoil.screens;

/** JSON으로부터 읽는 화면 카메라 설정. */
public class CameraConfig {
    public float gameplayZoom = 1f;
    public float verticalFollowOffset = 70f;
    public float verticalFollowDeadZone = 150f;
    public float lowerEdgeAllowance = 120f;
    public float prologueCameraY = 520f;

    public void validate() {
        if (!Float.isFinite(gameplayZoom) || gameplayZoom < 1f || gameplayZoom > 2.5f) {
            throw new IllegalArgumentException("gameplayZoom must be between 1.0 and 2.5");
        }
        if (!Float.isFinite(verticalFollowOffset) || verticalFollowOffset < 0 || verticalFollowOffset > 600
            || !Float.isFinite(verticalFollowDeadZone) || verticalFollowDeadZone < 0 || verticalFollowDeadZone > 600
            || !Float.isFinite(lowerEdgeAllowance) || lowerEdgeAllowance < 0 || lowerEdgeAllowance > 400
            || !Float.isFinite(prologueCameraY) || prologueCameraY < 0 || prologueCameraY > 1800) {
            throw new IllegalArgumentException("Invalid vertical camera framing values");
        }
    }
}
