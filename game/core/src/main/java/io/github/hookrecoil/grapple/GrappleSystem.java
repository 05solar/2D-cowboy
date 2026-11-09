/* 파일: GrappleSystem.java
 * 역할: 천장 링 한정 갈고리. 책임: 발사·시야·짧아지는 줄·접선 가속·해제 관성 보존.
 * 협력: CollisionWorld, PlayerState, StageDefinition. 수명: 복원 때 reset. 관련 사양: docs/10_PROLOGUE.md.
 */
package io.github.hookrecoil.grapple;

import io.github.hookrecoil.physics.*;
import io.github.hookrecoil.player.PlayerState;
import io.github.hookrecoil.stage.StageDefinition;
import io.github.hookrecoil.input.InputFrame;

public class GrappleSystem {
    private final PhysicsConfig c;
    private final StageDefinition stage;
    private final CollisionWorld world;
    public int attached = -1;
    public boolean flying;
    public float hookX, hookY, ropeLength, cooldown;
    private float dx, dy, traveled;
    public GrappleSystem(PhysicsConfig c, StageDefinition stage, CollisionWorld world) {
        this.c=c; this.stage=stage; this.world=world;
    }
    public void reset() { attached=-1; flying=false; cooldown=0; }
    public void update(PlayerState p, InputFrame in, float dt) {
        cooldown=Math.max(0,cooldown-dt);
        if (!in.hookHeld) { attached=-1; flying=false; }
        if (in.hookPressed && in.hookHeld && cooldown <= 0 && attached < 0 && !flying) {
            hookX=p.x; hookY=p.y+c.height*0.65f;
            float len=(float)Math.sqrt(in.aimX*in.aimX+in.aimY*in.aimY);
            if (len>0.001f) { dx=in.aimX/len; dy=in.aimY/len; flying=true; traveled=0; cooldown=c.hookInterval; }
        }
        if (flying) {
            float step=Math.min(c.hookSpeed*dt,c.hookRange-traveled);
            float nx=hookX+dx*step, ny=hookY+dy*step;
            // 적중 반경은 보이는 링 내부이며 천장 데이터와 연결된 링만 존재한다.
            for (int i=0; i<stage.anchors.length; i++) {
                StageDefinition.Point a=stage.anchors[i];
                float t=Math.max(0,Math.min(step,(a.x-hookX)*dx+(a.y-hookY)*dy));
                float ex=hookX+dx*t-a.x, ey=hookY+dy*t-a.y;
                if (ex*ex+ey*ey <= c.hookHitRadius*c.hookHitRadius && !world.blocked(p.x,p.y+c.height*0.65f,a.x,a.y)) {
                    attached=i; flying=false; hookX=a.x; hookY=a.y;
                    ropeLength=distance(p.x,p.y+c.height*0.65f,hookX,hookY); break;
                }
            }
            if (flying) {
                if (world.blocked(hookX,hookY,nx,ny)) flying=false;
                hookX=nx; hookY=ny; traveled+=step;
                if (traveled>=c.hookRange) flying=false;
            }
        }
        if (attached>=0 && world.blocked(p.x,p.y+c.height*0.65f,hookX,hookY)) attached=-1;
        if (attached>=0) {
            // 긴 줄로 구덩이 밑에 처지는 것을 막되, 즉시 위치를 끌어당기지 않는다.
            ropeLength=Math.max(Math.min(ropeLength,c.swingRopeLength),ropeLength-c.ropeRetractSpeed*dt);
            float rx=p.x-hookX,ry=p.y+c.height*0.65f-hookY;
            float radius=distance(0,0,rx,ry);
            if(radius>0.001f && !p.grounded) {
                float tx=-ry/radius,ty=rx/radius;
                float tangentSpeed=p.vx*tx+p.vy*ty;
                float direction=in.move*(tx>=0?1:-1);
                float added=direction*c.swingAcceleration*dt;
                // 일반 공중 이동의 4.5H/s 제한과 별개인 스윙 입력 가속.
                if(direction*tangentSpeed<c.swingSpeed) {
                    if(direction>0) added=Math.min(added,c.swingSpeed-tangentSpeed);
                    if(direction<0) added=Math.max(added,-c.swingSpeed-tangentSpeed);
                    p.vx+=tx*added;p.vy+=ty*added;
                }
            }
        }
    }
    public void constrain(PlayerState p) {
        if (attached<0) return;
        float x=p.x-hookX,y=p.y+c.height*0.65f-hookY, length=distance(0,0,x,y);
        if (length<=ropeLength || length<0.001f) return;
        float nx=x/length,ny=y/length,excess=length-ropeLength;
        world.move(p,-nx*excess,-ny*excess);
        if (distance(p.x,p.y+c.height*0.65f,hookX,hookY)>ropeLength+0.06f) { attached=-1; return; }
        float outward=p.vx*nx+p.vy*ny;
        if (outward>0) { p.vx-=nx*outward; p.vy-=ny*outward; }
    }
    public int aimTarget(PlayerState p) {
        int target=-1; float nearest=c.hookRange;
        for(int i=0;i<stage.anchors.length;i++) {
            StageDefinition.Point a=stage.anchors[i];
            float x=a.x-p.x,y=a.y-p.y-c.height*0.65f;
            float along=x*p.aimX+y*p.aimY;
            float cross=Math.abs(x*p.aimY-y*p.aimX);
            if(along>0 && along<nearest && cross<c.hookHitRadius && !world.blocked(p.x,p.y+c.height*0.65f,a.x,a.y)) {
                target=i;nearest=along;
            }
        }
        return target;
    }
    private float distance(float x,float y,float a,float b) { return (float)Math.sqrt((x-a)*(x-a)+(y-b)*(y-b)); }
}
