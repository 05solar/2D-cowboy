/* 파일: CaveRouteDriver.java
 * 역할: 동굴 실제 입력 경로 재생. 책임: 걷기·점프·반동·링 연결/해제만으로 보석까지 이동.
 * 협력: CaveVerification, StageRuntime, cave_routes.json. 순간이동이나 물리 변경 없음.
 * 관련 사양: docs/12_CAVE_CHAPTER.md.
 */
package io.github.hookrecoil;
import com.badlogic.gdx.utils.JsonValue;
import io.github.hookrecoil.input.InputFrame;
import io.github.hookrecoil.stage.*;
public class CaveRouteDriver {
    private final JsonValue routes;
    public int routeIndex,phase,airTicks,settle;
    public boolean flying;
    public CaveRouteDriver(JsonValue routes){this.routes=routes;}
    public InputFrame next(StageRuntime s){
        InputFrame f=new InputFrame();f.aimX=-.8f;f.aimY=-.6f;
        if(routeIndex>=routes.size){f.move=Math.abs(s.definition.clearGem.x-s.player.x)<.04f?0:s.definition.clearGem.x>s.player.x?1:-1;return f;}
        JsonValue r=routes.get(routeIndex);int dir=r.getInt("direction");float launch=r.getFloat("launch");String kind=r.getString("kind");
        StageDefinition.Block target=s.definition.platforms[r.getInt("target")];
        if(flying&&airTicks>12&&s.player.grounded&&Math.abs(s.player.y-target.y-target.height)<.01f&&s.player.x>target.x-.1f&&s.player.x<target.x+target.width+.1f){
            routeIndex++;flying=false;settle=7;return f;
        }
        if(settle>0){settle--;return f;}
        if(s.player.grounded&&!s.noCheckpoint&&s.checkpoint+1<s.definition.checkpoints.length) {
            StageDefinition.Point cp=s.definition.checkpoints[s.checkpoint+1];boolean samePlatform=false;
            for(StageDefinition.Block b:s.definition.platforms)
                if(b.active&&s.player.x>=b.x&&s.player.x<=b.x+b.width&&cp.x>=b.x&&cp.x<=b.x+b.width
                    &&Math.abs(cp.y-b.y-b.height)<0.001f&&Math.abs(s.player.y-cp.y)<0.001f)samePlatform=true;
            if(samePlatform&&Math.abs(s.player.x-cp.x)>0.45f){f.move=s.player.x<cp.x?1:-1;return f;}
        }
        if(!flying){
            if(Math.abs(s.player.x-launch)>.04f){f.move=s.player.x<launch?1:-1;return f;}
            flying=true;airTicks=0;phase=0;f.jump=true;
        }
        f.move=dir;
        if(kind.equals("hook")){
            int a=r.getInt("anchor");StageDefinition.Point first=s.definition.anchors[a],second=s.definition.anchors[a+1];
            if(phase==0){f.aimX=first.x-s.player.x;f.aimY=first.y-s.player.y-s.config.height*.65f;f.hookHeld=true;f.hookPressed=airTicks==0;
                if(s.grapple.attached==a&&s.player.x>first.x+.7f&&s.player.vy>0){f.hookHeld=false;phase=1;}
            }else if(phase==1){f.aimX=second.x-s.player.x;f.aimY=second.y-s.player.y-s.config.height*.65f;f.hookHeld=f.hookPressed=true;phase=2;
            }else if(phase==2){f.hookHeld=true;float release=second.x+(target.width<4?.6f:1);
                if(s.grapple.attached==a+1&&s.player.x>release&&s.player.vy>0){f.hookHeld=false;phase=3;}
            }
        }else if(kind.equals("up")){f.aimX=0;f.aimY=-1;f.fire=airTicks==12;}
        else if(kind.equals("recoil")||kind.equals("brake")){
            f.aimX=-.8f*dir;f.aimY=-.6f;f.fire=airTicks==12;
            if(kind.equals("brake")&&s.player.x>target.x-.65f&&s.player.vy<0&&phase==0){f.aimX=dir;f.aimY=0;f.fire=true;phase=1;}
        }
        float len=(float)Math.sqrt(f.aimX*f.aimX+f.aimY*f.aimY);if(len>0){f.aimX/=len;f.aimY/=len;}
        airTicks++;return f;
    }
}
