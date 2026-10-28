/* 파일: PhysicsConfig.java
 * 역할: H 단위 조작 초기값을 담는다. 책임: JSON 튜닝값 제공.
 * 협력: PlayerController, RecoilSystem, GrappleSystem. 관련 사양: docs/03_FEATURE_SPEC.md.
 */
package io.github.hookrecoil.physics;

public class PhysicsConfig {
    public float playerVisualScale, gunOpacity, swingAcceleration, swingSpeed, ropeRetractSpeed, swingRopeLength;
    public float gravity, jumpSpeed, groundSpeed, airAcceleration, maxSpeed, recoil;
    public float shotInterval, hookRange, hookSpeed, hookInterval, hookHitRadius;
    public float coyoteTime, jumpBuffer, landingTime, halfWidth, height, pixelsPerH;
}
