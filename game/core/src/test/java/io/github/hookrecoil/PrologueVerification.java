/* 파일: PrologueVerification.java
 * 역할: 프롤로그 실제 시스템 회귀 검증. 책임: 반동 도달성·천장 제약·자원·복원·FPS 일관성 검증.
 * 협력: StageRuntime와 실제 JSON 데이터. 관련 사양: docs/07_TEST_PLAN.md, docs/10_PROLOGUE.md.
 */
package io.github.hookrecoil;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import io.github.hookrecoil.physics.PhysicsConfig;
import io.github.hookrecoil.stage.*;
import io.github.hookrecoil.input.InputFrame;

public class PrologueVerification {
    private static int assertions;
    private static StageRuntime game() {
        Json j=new Json();
        return new StageRuntime(j.fromJson(StageDefinition.class,new FileHandle("assets/stages/prologue.json")),
            j.fromJson(PhysicsConfig.class,new FileHandle("assets/config/physics.json")));
    }
    private static void check(boolean value,String message) {
        assertions++;if(!value)throw new AssertionError(message);
    }
    private static float jump(boolean recoil) {
        StageRuntime s=game();s.player.reset(15.5f,2);
        float furthest=15.5f;
        for(int i=0;i<220;i++) {
            InputFrame f=new InputFrame();f.move=1;f.jump=i==0;f.fire=recoil&&i==12;f.aimX=-0.8f;f.aimY=-0.6f;
            s.tick(f,1f/120);furthest=Math.max(furthest,s.player.x);
            if(i>12&&s.player.grounded) {check(recoil,"Basic jump unexpectedly crosses 5H gap");return s.player.x;}
            if(s.player.y<2 && !recoil) break;
            if(s.deathTimer>0)break;
        }
        check(!recoil,"Recoil failed to reach the far platform");return furthest;
    }
    private static void route(boolean useRecoil, float releaseX) {
        StageRuntime s=game();s.player.reset(26,2);
        int phase=0,attachedTicks=0;
        boolean gotFirst=false,gotSecond=false;
        for(int i=0;i<1800;i++) {
            InputFrame f=new InputFrame();f.move=1;f.jump=i==0;
            if(phase==0) {
                f.aimX=29-s.player.x;f.aimY=6.6f-s.player.y-s.config.height*0.65f;
                f.hookHeld=true;f.hookPressed=i==0;
                if(s.grapple.attached==0) {gotFirst=true;attachedTicks++;}
                if(gotFirst && s.player.x>29.7f && s.player.vy>0) {
                    phase=1;f.hookHeld=false;f.aimX=-0.4f;f.aimY=-0.916515f;f.fire=useRecoil;
                }
            } else if(phase==1) {
                phase=2;
                f.aimX=33.5f-s.player.x;f.aimY=6.6f-s.player.y-s.config.height*0.65f;f.hookHeld=true;f.hookPressed=true;
            } else if(phase==2) {
                f.hookHeld=true;
                if(s.grapple.attached==1) gotSecond=true;
                if(gotSecond && s.player.x>releaseX && s.player.vy>0) {phase=3;f.hookHeld=false;}
            } else if(phase==3) {
                f.aimX=-0.4f;f.aimY=-0.916515f;f.fire=useRecoil;phase=4;
            } else if(phase==4 && s.player.shotCooldown<=0) {
                f.aimX=-0.8f;f.aimY=-0.6f;f.fire=useRecoil;phase=5;
            }
            float len=(float)Math.sqrt(f.aimX*f.aimX+f.aimY*f.aimY);if(len>0){f.aimX/=len;f.aimY/=len;}
            s.tick(f,1f/120);
            if(useRecoil&&s.clear.frozen()) {check(gotFirst&&gotSecond,"Boosted route reaches gem");return;}
            if(s.player.grounded&&s.player.x>=36) {
                System.out.printf("Ceiling route landed x=%.3f; recoil=%s releaseX=%.2f%n",s.player.x,useRecoil,releaseX);
                check(gotFirst&&gotSecond,"Both ceiling rings must be exercised");
                if(!useRecoil) {check(s.shots==0,"Swing-only route must not fire");check(s.player.x<=41,"Swing should land on the opposite platform");}
                return;
            }
            if(s.deathTimer>0)break;
        }
        throw new AssertionError("Ceiling route failed phase="+phase+" x="+s.player.x+" y="+s.player.y+" attachedTicks="+attachedTicks);
    }
    public static void main(String[] args) {
        float basic=jump(false),boosted=jump(true);
        check(boosted>basic,"Recoil must travel farther");
        System.out.printf("Basic jump fails gap: max x=%.3f. One-shot recoil lands x=%.3f (far edge 21H).%n",basic,boosted);
        StageRuntime s=game();
        for(int i=0;i<120;i++) {
            InputFrame f=new InputFrame();f.hookHeld=true;f.hookPressed=i==0;f.aimX=0;f.aimY=1;s.tick(f,1f/120);
        }
        check(s.grapple.attached<0,"Open sky cannot accept hook");
        s=game();s.player.reset(25,5.5f);
        float hookY=s.player.y+s.config.height*.65f,dx=29-s.player.x,dy=6.6f-hookY;
        float distance=(float)Math.sqrt(dx*dx+dy*dy),angle=(float)Math.asin(.26f/distance);
        s.player.aimX=dx/distance*(float)Math.cos(angle)-dy/distance*(float)Math.sin(angle);
        s.player.aimY=dx/distance*(float)Math.sin(angle)+dy/distance*(float)Math.cos(angle);
        check(s.grapple.aimTarget(s.player)==0,"Expanded hook reticle radius accepts near-ring aim");
        for(int i=0;i<18&&s.grapple.attached<0;i++){
            InputFrame f=new InputFrame();f.hookHeld=true;f.hookPressed=i==0;f.aimX=s.player.aimX;f.aimY=s.player.aimY;s.grapple.update(s.player,f,1f/120);
        }
        check(s.grapple.attached==0,"Expanded hook hit radius connects near-ring shot");
        s=game();s.player.reset(25,5.5f);hookY=s.player.y+s.config.height*.65f;dx=29-s.player.x;dy=6.6f-hookY;
        distance=(float)Math.sqrt(dx*dx+dy*dy);angle=(float)Math.asin(.34f/distance);
        s.player.aimX=dx/distance*(float)Math.cos(angle)-dy/distance*(float)Math.sin(angle);
        s.player.aimY=dx/distance*(float)Math.sin(angle)+dy/distance*(float)Math.cos(angle);
        check(s.grapple.aimTarget(s.player)<0,"Aim outside expanded hook radius stays invalid");
        s.player.reset(23,2);s.checkpoint=1;
        for(int i=0;i<100;i++) {s.player.ammo=0;s.player.vx=15;s.respawn();check(s.player.x==23&&s.player.vx==0&&s.player.ammo==2&&s.grapple.attached<0,"Reset consistency");}
        s=game();s.player.reset(4,2);
        for(int i=0;i<60;i++) {InputFrame f=new InputFrame();f.fire=i==0||i==30;f.aimX=-1;f.aimY=0;s.tick(f,1f/120);}
        check(s.player.ammo==0,"Standing does not refill every tick");
        // 실제 짧은 구멍과 마지막 출구까지 같은 컨트롤러로 검증한다.
        for(int segment=0;segment<2;segment++) {
            s=game();s.player.reset(segment==0?7.4f:40,2);
            boolean landed=false;
            for(int i=0;i<240;i++) {
                InputFrame f=new InputFrame();f.move=1;f.jump=i==0;f.fire=segment==1&&i==12;f.aimX=-0.8f;f.aimY=-0.6f;
                s.tick(f,1f/120);
                if(i>2&&(s.player.grounded||s.clear.frozen())) {landed=true;break;}
                if(s.deathTimer>0)break;
            }
            check(landed,"Short/final gap reachability");
            if(segment==1) {
                for(int i=0;i<120&&!s.clear.frozen();i++) {InputFrame f=new InputFrame();f.move=s.player.x>s.definition.clearGem.x?-1:1;s.tick(f,1f/120);}
                check(s.clear.frozen(),"Final platform reaches clear gem");s.advancePresentation(s.clear.durationSeconds());check(s.complete,"Gem sequence completes");
            }
        }
        s=game();s.player.reset(5,5);s.player.vy=-16;
        s.collision.move(s.player,0,-10);
        check(s.player.y==2&&s.player.grounded,"Swept floor cannot tunnel");
        s.player.reset(19,0.5f);s.collision.move(s.player,8,0);
        check(Math.abs(s.player.x-(21-s.config.halfWidth))<0.0001f,"Swept side wall cannot tunnel");
        check(s.collision.blocked(25,7.5f,38,7.5f),"Ceiling blocks projectiles");
        s.player.reset(29,3);s.player.vx=3;s.player.vy=2;s.grapple.attached=0;
        s.grapple.update(s.player,new InputFrame(),1f/120);
        check(s.grapple.attached<0&&s.player.vx==3&&s.player.vy==2,"Release preserves velocity");
        s=game();s.player.reset(5,2);
        for(int i=0;i<10;i++) {InputFrame f=new InputFrame();f.fire=true;f.aimX=-1;f.aimY=0;s.tick(f,1f/120);}
        check(s.player.ammo==1,"Cooldown prevents repeated immediate shots");
        s=game();s.definition.anchors[0].x=2;
        boolean rejected=false;try{s.definition.validate();}catch(IllegalArgumentException e){rejected=true;}
        check(rejected,"Unsupported anchor rejected by map validation");
        float[] reference=null;
        for(int fps:new int[]{30,60,144}) {
            s=game();int tick=0;double accumulator=0;
            for(int frame=0;frame<fps*2;frame++) {
                accumulator+=1.0/fps;
                while(accumulator+1e-10>=1.0/120) {
                    InputFrame f=new InputFrame();f.move=1;f.jump=tick==30;f.fire=tick==45;f.aimX=-0.8f;f.aimY=-0.6f;
                    s.tick(f,1f/120);tick++;accumulator-=1.0/120;
                }
            }
            check(tick==240,"Fixed tick count");
            if(reference==null)reference=new float[]{s.player.x,s.player.y};
            check(Math.abs(s.player.x-reference[0])<0.0001f&&Math.abs(s.player.y-reference[1])<0.0001f,"FPS invariant trajectory");
        }
        route(true,34.5f);
        route(false,34.1f);
        route(false,34.5f);
        route(false,34.9f);
        GemClearVerification.run();
        System.out.println("PASS: "+assertions+" assertions.");
    }
}
