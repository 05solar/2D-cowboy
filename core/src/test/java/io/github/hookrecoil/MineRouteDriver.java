/* 파일: MineRouteDriver.java
 * 역할: 광산 검증 경로를 실제 입력으로 재생한다.
 * 주요 책임: 발판 착지·이동체 탑승/대기·점프·반동 입력을 물리 틱에 공급한다.
 * 협력 대상: MineVerification, StageRuntime, stages/mine_routes.json.
 * 관련 사양: docs/assets/mine_chapter/STAGES_06_10.md.
 */
package io.github.hookrecoil;

import com.badlogic.gdx.utils.JsonValue;
import io.github.hookrecoil.input.InputFrame;
import io.github.hookrecoil.stage.StageDefinition;
import io.github.hookrecoil.stage.StageRuntime;

public final class MineRouteDriver {
    private final JsonValue route;
    public int stepIndex,airTicks;
    private boolean launched;
    public MineRouteDriver(JsonValue route){this.route=route;}

    public InputFrame next(StageRuntime stage) {
        InputFrame input=new InputFrame();input.aimX=0;input.aimY=-1;
        if(stepIndex>=route.size) {
            input.move=stage.player.x<stage.definition.clearGem.x?1:-1;return input;
        }
        JsonValue step=route.get(stepIndex);int index=step.getInt("platform");
        StageDefinition.Block target=stage.definition.platforms[index];
        float center=target.x+target.width*.5f,top=target.surfaceY(center);
        boolean landed=stage.player.grounded&&Math.abs(stage.player.y-target.surfaceY(stage.player.x))<.04f
            &&stage.player.x+stage.config.halfWidth>target.x&&stage.player.x-stage.config.halfWidth<target.x+target.width;
        if(landed) {
            boolean waitTop=step.has("waitTop")&&top+0.001f<step.getFloat("waitTop");
            boolean waitX=step.has("waitX")&&target.x+0.001f<step.getFloat("waitX");
            if(!waitTop&&!waitX){stepIndex++;launched=false;airTicks=0;return input;}
            input.move=0;return input;
        }
        if(stage.player.grounded&&launched&&airTicks>20) {launched=false;airTicks=0;return input;}
        float currentTop=stage.player.y,rise=top-currentTop;
        if(!stage.player.grounded) {
            if(launched) {
                input.move=Math.abs(center-stage.player.x)<.08f?0:center>stage.player.x?1:-1;
                input.fire=airTicks==10;airTicks++;
            }
            return input;
        }
        if(!launched) {
            boolean jump=step.getBoolean("jump",false)||rise>.15f;
            if(stage.player.grounded)for(StageDefinition.Block source:stage.definition.platforms)
                if(source!=target&&source.active&&Math.abs(stage.player.y-source.surfaceY(stage.player.x))<.04f
                    &&stage.player.x+stage.config.halfWidth>source.x&&stage.player.x-stage.config.halfWidth<source.x+source.width) {
                    float gap=target.x>source.x+source.width?target.x-(source.x+source.width):
                        source.x>target.x+target.width?source.x-(target.x+target.width):0;
                    if(gap>.22f)jump=true;
                    break;
                }
            input.move=Math.abs(center-stage.player.x)<.12f?0:center>stage.player.x?1:-1;
            if(jump){input.jump=true;launched=true;airTicks=0;}
            return input;
        }
        input.move=Math.abs(center-stage.player.x)<.08f?0:center>stage.player.x?1:-1;
        input.fire=airTicks==10&&rise>.85f;
        airTicks++;
        return input;
    }
}
