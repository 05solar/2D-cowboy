/* 파일: RecoilSystem.java
 * 역할: 조준 반대 방향 반동. 책임: 탄약·간격·발사 자세와 실제 충격량 일치.
 * 협력: PlayerState, PhysicsConfig. 관련 사양: docs/03_FEATURE_SPEC.md, docs/10_PROLOGUE.md.
 */
package io.github.hookrecoil.recoil;

import io.github.hookrecoil.player.PlayerState;
import io.github.hookrecoil.physics.PhysicsConfig;

public class RecoilSystem {
    private final PhysicsConfig c;
    public RecoilSystem(PhysicsConfig c) { this.c = c; }
    public boolean fire(PlayerState p) {
        if (p.ammo == 0 || p.shotCooldown > 0) return false;
        float length = (float)Math.sqrt(p.aimX*p.aimX+p.aimY*p.aimY);
        if (length < 0.0001f) return false;
        p.firedX = p.aimX/length; p.firedY = p.aimY/length;
        p.vx -= p.firedX*c.recoil; p.vy -= p.firedY*c.recoil;
        p.ammo--; p.shotCooldown = c.shotInterval; p.shotPose = 0.32f;
        return true;
    }
}
