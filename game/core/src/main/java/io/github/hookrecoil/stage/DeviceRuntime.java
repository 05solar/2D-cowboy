/* 파일: DeviceRuntime.java
 * 역할: 동굴 장치 시뮬레이션. 책임: 착지 기점 붕괴·경고 이벤트·가시 스윕.
 * 협력: StageRuntime, CollisionWorld, CaveRenderer. 수명: 재시작/부활 때 초기화.
 * 관련 사양: docs/12_CAVE_CHAPTER.md.
 */
package io.github.hookrecoil.stage;
import io.github.hookrecoil.collectible.GemSensor;
import io.github.hookrecoil.player.PlayerState;
import io.github.hookrecoil.physics.PhysicsConfig;
import java.util.Arrays;
public class DeviceRuntime {
    public final float[] clocks;
    public boolean warning;
    private final StageDefinition def;
    public DeviceRuntime(StageDefinition d){def=d;clocks=new float[d.platforms.length];reset();}
    public void reset(){Arrays.fill(clocks,-1);for(StageDefinition.Block b:def.platforms)b.active=true;warning=false;}
    public void tick(float dt){warning=false;for(int i=0;i<clocks.length;i++)if(clocks[i]>=0){
        float old=clocks[i];clocks[i]+=dt;
        if(old<0.45f&&clocks[i]>=0.45f)warning=true;
        if(clocks[i]>=1)def.platforms[i].active=false;
    }}
    public void landed(PlayerState p,PhysicsConfig c){if(!p.grounded)return;
        for(int i=0;i<clocks.length;i++){StageDefinition.Block b=def.platforms[i];
            if(b.crumble&&b.active&&clocks[i]<0&&Math.abs(p.y-b.surfaceY(p.x))<0.001f
                &&p.x+c.halfWidth>b.x&&p.x-c.halfWidth<b.x+b.width)clocks[i]=0;
        }
    }
    public String frame(int i){float t=clocks[i];return t<0.45f?"intact":t<1?"warning":t<1.24f?"collapse":"gone";}
    public boolean lethal(PlayerState p,PhysicsConfig c){
        for(StageDefinition.Hazard h:def.hazards){
            // 명시된 위험 영역만 사용하며 축별 이동을 연속 검사한다.
            if(GemSensor.contact(p.previousX,p.previousY,p.x,p.previousY,c.halfWidth,c.height,
                h.x+h.width/2,h.y+h.height/2,h.width/2,h.height/2)>=0
                ||GemSensor.contact(p.x,p.previousY,p.x,p.y,c.halfWidth,c.height,
                h.x+h.width/2,h.y+h.height/2,h.width/2,h.height/2)>=0)return true;
        }return false;
    }
}
