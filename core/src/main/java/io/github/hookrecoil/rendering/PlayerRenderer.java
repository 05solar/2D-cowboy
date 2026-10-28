/* 파일: PlayerRenderer.java
 * 역할: 발판 접점 기준 캐릭터 애니메이션. 책임: 속도 기반 air, 뒤쪽 발사 반전, 독립 회전 총.
 * 협력: GameAssets, StageRuntime. 관련 사양: docs/10_PROLOGUE.md. 원본 PNG는 변경하지 않는다.
 */
package io.github.hookrecoil.rendering;

import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import io.github.hookrecoil.assets.GameAssets;
import io.github.hookrecoil.player.PlayerState;
import io.github.hookrecoil.stage.StageRuntime;

public class PlayerRenderer {
    private final GameAssets assets;
    public PlayerRenderer(GameAssets assets) {this.assets=assets;}
    public void draw(SpriteBatch batch,StageRuntime stage,float alpha) {
        PlayerState p=stage.player;String group="player_movement",name;
        boolean flip=!p.facingRight;
        if(stage.deathTimer>0) {group="player_actions";name="death_0"+Math.min(3,(int)((0.35f-stage.deathTimer)*12));}
        else if(p.shotPose>0) {
            group="player_actions";name="recoil_0"+Math.min(3,(int)((0.32f-p.shotPose)*12));
            // 원본은 오른쪽 발사. 오른쪽 진행을 위한 뒤쪽 발사는 반드시 좌우반전한다.
            flip=p.firedX<0;
        } else if(stage.grapple.attached>=0) {group="player_actions";name="swing_0"+((int)(p.age*10)%4);}
        else if(stage.grapple.flying) {group="player_actions";name="grapple_0"+((int)(p.age*10)%4);}
        else if(!p.grounded) name=p.vy>2?"air_01":(p.vy<-2?"air_03":"air_02");
        else if(p.landingPose>0) name="land_0"+Math.min(3,(int)((0.24f-p.landingPose)*16));
        else name=(Math.abs(p.vx)>0.1f?"run_0":"idle_0")+((int)(p.age*(Math.abs(p.vx)>0.1f?10:6))%4);
        TextureRegion region=assets.frame(group,name,flip);
        float unit=stage.config.pixelsPerH;
        float x=Math.round(MathUtils.lerp(p.previousX,p.x,alpha)*unit);
        float y=Math.round(MathUtils.lerp(p.previousY,p.y,alpha)*unit);
        float scale=stage.config.playerVisualScale;
        batch.draw(region,Math.round(x-region.getRegionWidth()*scale/2f),y-4*scale,region.getRegionWidth()*scale,region.getRegionHeight()*scale);
        // 시트에 포함된 총 자세는 유지하고, 별도 부품을 손 근처에서 실제 조준 방향으로 회전한다.
        if(stage.deathTimer<=0 && stage.grapple.attached<0) {
            float ax=p.shotPose>0?p.firedX:p.aimX,ay=p.shotPose>0?p.firedY:p.aimY;
            TextureRegion gun=assets.frame("objects","revolver");
            float angle=MathUtils.atan2(ay,ax)*MathUtils.radiansToDegrees;
            float handX=x+(ax<0?-25:25)*scale,handY=y+85*scale;
            batch.setColor(1,1,1,stage.config.gunOpacity);
            batch.draw(gun,handX-20,handY-25,20,25,gun.getRegionWidth(),gun.getRegionHeight(),0.45f*scale,(ax<0?-0.45f:0.45f)*scale,angle);
            batch.setColor(1,1,1,1);
            if(p.shotPose>0.23f) {
                TextureRegion flash=assets.frame("objects","muzzle_flash");
                batch.draw(flash,handX+ax*88*scale-40,handY+ay*88*scale-22,40,22,flash.getRegionWidth(),flash.getRegionHeight(),0.5f*scale,0.5f*scale,angle);
            }
        }
    }
}
