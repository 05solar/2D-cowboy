/* 파일: CollisionWorld.java
 * 역할: 정적 사각형 지형 충돌과 시야 검사. 책임: 축별 스윕·바닥 판정·선분 차단.
 * 협력: StageDefinition, PlayerController, GrappleSystem. 관련 사양: docs/10_PROLOGUE.md.
 */
package io.github.hookrecoil.physics;

import io.github.hookrecoil.stage.StageDefinition;
import io.github.hookrecoil.player.PlayerState;
import java.util.ArrayList;
import java.util.List;

public class CollisionWorld {
    private final List<StageDefinition.Block> blocks = new ArrayList<>();
    private final PhysicsConfig c;
    public CollisionWorld(StageDefinition stage, PhysicsConfig config) {
        c = config;
        for (StageDefinition.Block b : stage.platforms) blocks.add(b);
        for (StageDefinition.Block b : stage.ceilings) blocks.add(b);
    }
    public void move(PlayerState p, float dx, float dy) {
        float nextX = p.x + dx;
        for (StageDefinition.Block b : blocks) {
            if(!b.active)continue;
            float top=b.surfaceY(p.x);
            if (p.y + c.height <= b.y + 0.0001f || p.y >= top - 0.0001f
                || (p.grounded && top-p.y<=c.surfaceStepHeight)) continue;
            if (dx > 0 && p.x + c.halfWidth <= b.x + 0.0001f && nextX + c.halfWidth >= b.x) {
                nextX = Math.min(nextX, b.x - c.halfWidth); p.vx = 0;
            }
            if (dx < 0 && p.x - c.halfWidth >= b.x + b.width - 0.0001f && nextX - c.halfWidth <= b.x + b.width) {
                nextX = Math.max(nextX, b.x + b.width + c.halfWidth); p.vx = 0;
            }
        }
        p.x = nextX;
        if(p.grounded&&dy<=0) {
            float stepTop=p.y;
            for(StageDefinition.Block b:blocks)if(b.active&&p.x+c.halfWidth>b.x&&p.x-c.halfWidth<b.x+b.width
                &&p.y+c.height>b.y+0.0001f) {
                float top=b.surfaceY(p.x);
                if(top>p.y&&top-p.y<=c.surfaceStepHeight)stepTop=Math.max(stepTop,top);
            }
            if(stepTop>p.y){p.y=stepTop;p.vy=0;}
        }
        float nextY = p.y + dy;
        p.grounded = false;
        for (StageDefinition.Block b : blocks) {
            if(!b.active)continue;
            if (p.x + c.halfWidth <= b.x || p.x - c.halfWidth >= b.x + b.width) continue;
            float top = b.surfaceY(p.x);
            if (dy <= 0 && p.y >= top - 0.0001f && nextY <= top) {
                nextY = Math.max(nextY, top); p.vy = 0; p.grounded = true;
            }
            if (dy > 0 && p.y + c.height <= b.y + 0.0001f && nextY + c.height >= b.y) {
                nextY = Math.min(nextY, b.y - c.height); p.vy = 0;
            }
        }
        p.y = nextY;
    }
    public boolean blocked(float x1, float y1, float x2, float y2) {
        for (StageDefinition.Block b : blocks) if (b.active && intersects(x1, y1, x2, y2, b)) return true;
        return false;
    }
    public boolean supports(float x,float feetY) {
        for(StageDefinition.Block b:blocks)
            if(b.active && Math.abs(feetY-b.surfaceY(x))<0.0001f && x+c.halfWidth>b.x && x-c.halfWidth<b.x+b.width)return true;
        return false;
    }
    private boolean intersects(float x, float y, float endX, float endY, StageDefinition.Block b) {
        float lo = 0, hi = 1;
        float[] start = {x, y}, delta = {endX-x, endY-y};
        float[] min = {b.x, b.y}, max = {b.x+b.width, b.y+b.height};
        for (int i=0; i<2; i++) {
            if (Math.abs(delta[i]) < 0.00001f) { if (start[i] < min[i] || start[i] > max[i]) return false; }
            else {
                float a = (min[i]-start[i])/delta[i], z = (max[i]-start[i])/delta[i];
                lo = Math.max(lo, Math.min(a,z)); hi = Math.min(hi, Math.max(a,z));
                if (lo > hi) return false;
            }
        }
        return true;
    }
}
