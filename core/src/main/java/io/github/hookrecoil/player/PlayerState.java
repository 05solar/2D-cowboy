/* 파일: PlayerState.java
 * 역할: 플레이어의 물리·애니메이션 상태. 책임: 위치·속도·탄약·착지 수명 관리.
 * 협력: PlayerController, StageRuntime. 수명: 재시작 때 reset. 관련 사양: docs/03_FEATURE_SPEC.md.
 */
package io.github.hookrecoil.player;

public class PlayerState {
    public float x, y, previousX, previousY, vx, vy;
    public float coyote, jumpBuffer, groundedTime, shotCooldown, shotPose, landingPose, age;
    public float aimX = -0.8f, aimY = -0.6f, firedX = -1, firedY;
    public int ammo = 2;
    public boolean grounded, landingCharged, facingRight = true;

    public void reset(float x, float y) {
        this.x = previousX = x; this.y = previousY = y;
        vx = vy = coyote = jumpBuffer = groundedTime = shotCooldown = shotPose = landingPose = age = 0;
        ammo = 2; grounded = true; landingCharged = true;
    }
}
