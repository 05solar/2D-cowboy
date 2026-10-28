/* 파일: PlayerController.java
 * 역할: 고정 틱 이동·점프·착지. 책임: 공중 가속, 코요테, 입력 버퍼, 착지 충전 이벤트.
 * 협력: PhysicsConfig, CollisionWorld, InputFrame. 관련 사양: docs/03_FEATURE_SPEC.md.
 */
package io.github.hookrecoil.player;

import io.github.hookrecoil.physics.*;
import io.github.hookrecoil.input.InputFrame;

public class PlayerController {
    private final PhysicsConfig c;
    public PlayerController(PhysicsConfig c) { this.c = c; }
    public void prepare(PlayerState p, InputFrame in, float dt) {
        p.previousX = p.x; p.previousY = p.y; p.age += dt;
        p.shotCooldown = Math.max(0, p.shotCooldown-dt);
        p.shotPose = Math.max(0, p.shotPose-dt); p.landingPose = Math.max(0, p.landingPose-dt);
        p.coyote = p.grounded ? c.coyoteTime : Math.max(0, p.coyote-dt);
        p.jumpBuffer = in.jump ? c.jumpBuffer : Math.max(0, p.jumpBuffer-dt);
        if (p.grounded) p.vx = in.move*c.groundSpeed;
        else if (Math.abs(p.vx) < c.groundSpeed || in.move*p.vx < 0) {
            float next = p.vx + in.move*c.airAcceleration*dt;
            if (Math.abs(p.vx) <= c.groundSpeed) next = Math.max(-c.groundSpeed, Math.min(c.groundSpeed, next));
            p.vx = next;
        }
        if (in.move != 0) p.facingRight = in.move > 0;
        if (p.jumpBuffer > 0 && p.coyote > 0) {
            p.vy = c.jumpSpeed; p.grounded = false; p.coyote = p.jumpBuffer = 0;
        }
        p.aimX = in.aimX; p.aimY = in.aimY;
    }
    public void integrate(PlayerState p, CollisionWorld world, float dt) {
        boolean wasGrounded = p.grounded;
        p.vy -= c.gravity*dt;
        float speed = (float)Math.sqrt(p.vx*p.vx+p.vy*p.vy);
        if (speed > c.maxSpeed) { p.vx *= c.maxSpeed/speed; p.vy *= c.maxSpeed/speed; }
        world.move(p, p.vx*dt, p.vy*dt);
        if (p.grounded) {
            if (!wasGrounded) p.landingPose = 0.24f;
            p.groundedTime += dt;
            if (!p.landingCharged && p.groundedTime >= c.landingTime) { p.ammo = 2; p.landingCharged = true; }
        } else { p.groundedTime = 0; p.landingCharged = false; }
    }
}
