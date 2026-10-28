/* 파일: InputFrame.java
 * 역할: 한 고정 틱의 입력. 책임: 버튼 엣지와 조준 방향 전달.
 * 협력: InputRouter, StageRuntime. 관련 사양: docs/03_FEATURE_SPEC.md.
 */
package io.github.hookrecoil.input;

public class InputFrame {
    public float move, aimX = -0.8f, aimY = -0.6f;
    public boolean jump, fire, hookPressed, hookHeld, restart;
}
