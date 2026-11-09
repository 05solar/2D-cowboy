/* 파일: MineGroundHazards.java
 * 역할: 광산 위험물이 발판 보행면에서 작동하는지 검사한다.
 * 책임: 모든 장벽의 발판 관통, 활성 보행 사망, 비활성 보행 통과 및 톱·압착기의 바닥 도달 확인.
 * 협력: MineVerification, StageRuntime. 각 검사마다 격리된 런타임을 생성한다.
 * 관련 사양: docs/MINE_06_10_ROUTE_REWORK_2026-11-09.md.
 */
package io.github.hookrecoil;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import io.github.hookrecoil.stage.*;
import io.github.hookrecoil.physics.PhysicsConfig;
import io.github.hookrecoil.input.InputFrame;

final class MineGroundHazards {
    private static StageRuntime load(String id,int gate) {
        Json j=new Json();StageDefinition d=j.fromJson(StageDefinition.class,new FileHandle("assets/stages/"+id+".json"));
        d.cityBarriers=new StageDefinition.CityBarrier[]{d.cityBarriers[gate]};
        d.hazards=new StageDefinition.Hazard[0];d.mineSaws=new StageDefinition.MineSaw[0];d.mineCrushers=new StageDefinition.MineCrusher[0];
        return new StageRuntime(d,j.fromJson(PhysicsConfig.class,new FileHandle("assets/config/physics.json")));
    }
    static void verify(StageRuntime authored) {
        String id=authored.definition.id;
        for(int i=0;i<authored.definition.cityBarriers.length;i++) {
            StageDefinition.CityBarrier g=authored.definition.cityBarriers[i];StageDefinition.Block deck=null;
            boolean aligned=g.width>=g.height;
            for(StageDefinition.Block b:authored.definition.platforms) {
                if(aligned) {
                    if(g.x>=b.x&&g.x+g.width<=b.x+b.width&&Math.abs(g.y-b.surfaceY(g.x))<.3f)deck=b;
                } else if(g.x>=b.x&&g.x+g.width<=b.x+b.width&&g.y<b.y&&g.y+g.height>b.surfaceY(g.x)+authored.config.height)deck=b;
            }
            if(deck==null)throw new AssertionError(id+" gate "+i+(aligned?" does not sit flush on a walking platform":" does not penetrate a walking platform"));
            int ticks=aligned?90:24;
            for(boolean on:new boolean[]{false,true}) {
                StageRuntime s=load(id,i);StageDefinition.CityBarrier b=s.definition.cityBarriers[0];
                s.player.reset(b.x-.23f,deck.surfaceY(b.x-.23f));s.player.grounded=true;
                if(on)s.cityDevices.tickBarriers(b.offSeconds+b.warningSeconds+.05f);
                InputFrame input=new InputFrame();input.move=1;
                for(int tick=0;tick<ticks&&s.deaths==0;tick++)s.tick(input,1f/120);
                if(on?s.deaths!=1:s.deaths!=0||s.player.x<=b.x+b.width)
                    throw new AssertionError(id+" ground walking gate "+i+" active="+on+" deaths="+s.deaths+" x="+s.player.x);
            }
        }
        for(StageDefinition.MineCrusher c:authored.definition.mineCrushers) {
            boolean deckHit=false;
            for(StageDefinition.Block b:authored.definition.platforms)
                if(c.x<b.x+b.width&&c.x+c.width>b.x&&c.topY-c.stroke<=b.surfaceY(c.x)&&c.topY>b.surfaceY(c.x))deckHit=true;
            if(!deckHit)throw new AssertionError(id+" crusher cannot reach a platform");
        }
        for(StageDefinition.MineSaw c:authored.definition.mineSaws) {
            boolean deckHit=false;
            for(StageDefinition.Block b:authored.definition.platforms)
                if(c.fromX<=b.x+b.width&&c.toX>=b.x&&Math.abs(c.fromY-b.surfaceY(c.fromX))<authored.config.height)deckHit=true;
            if(!deckHit)throw new AssertionError(id+" saw rail is above walking height");
        }
        System.out.println(id+" grounded hazards PASS: "+authored.definition.cityBarriers.length+" gates kill when on and permit walking when off");
    }
}
