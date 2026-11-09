/* 파일: FullRouteDryRun.java
 * 역할: 프롤로그부터 city_05까지 전체 경로를 빠른 배속으로 미리 검증한다.
 * 주요 책임: FullPlaythroughDemo에서 쓸 각 스테이지 드라이버가 사망 없이 클리어 보석에 도달하는지
 *          실시간 재생 전에 먼저 확인해, 느린 실시간 데모를 띄우기 전 문제를 빨리 잡는다.
 * 협력 대상: PrologueRouteDriver, CaveRouteDriver, MineRouteDriver, CityRouteDriver, StageRuntime.
 * 관련 사양: docs/10_PROLOGUE.md, docs/12_CAVE_CHAPTER.md, docs/MINE_06_10_ROUTE_REWORK_2026-11-09.md, docs/CITY_11_15_REWORK_2026-11-09.md.
 */
package io.github.hookrecoil;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.github.hookrecoil.input.InputFrame;
import io.github.hookrecoil.physics.PhysicsConfig;
import io.github.hookrecoil.stage.StageDefinition;
import io.github.hookrecoil.stage.StageRuntime;

public final class FullRouteDryRun {
    private static StageRuntime game(String id) {
        Json j=new Json();
        return new StageRuntime(j.fromJson(StageDefinition.class,new FileHandle("assets/stages/"+id+".json")),
            j.fromJson(PhysicsConfig.class,new FileHandle("assets/config/physics.json")));
    }
    public static void main(String[] args) {
        JsonValue caveRoutes=new JsonReader().parse(new FileHandle("assets/stages/cave_routes.json"));
        JsonValue mineRoutes=new JsonReader().parse(new FileHandle("assets/stages/mine_routes.json"));
        String[] ids=new String[]{"prologue","cave_01","cave_02","cave_03","cave_04","cave_05",
            "mine_01","mine_02","mine_03","mine_04","mine_05","city_01","city_02","city_03","city_04","city_05"};
        for(String id:ids) {
            StageRuntime s=game(id);
            Object driver;
            if(id.equals("prologue"))driver=new PrologueRouteDriver();
            else if(id.startsWith("cave_"))driver=new CaveRouteDriver(caveRoutes.get(id));
            else if(id.startsWith("mine_"))driver=new MineRouteDriver(mineRoutes.get(id));
            else driver=new CityRouteDriver();
            int ticks=0;
            while(ticks<24000&&!s.clear.frozen()) {
                InputFrame frame=
                    driver instanceof PrologueRouteDriver?((PrologueRouteDriver)driver).next(s):
                    driver instanceof CaveRouteDriver?((CaveRouteDriver)driver).next(s):
                    driver instanceof MineRouteDriver?((MineRouteDriver)driver).next(s):
                    ((CityRouteDriver)driver).next(s);
                s.tick(frame,1f/120);ticks++;
                if(s.deaths>0) {
                    System.out.println("FAIL "+id+" died at tick "+ticks+" x="+s.player.x+" y="+s.player.y);
                    return;
                }
            }
            if(!s.clear.frozen()) {
                System.out.println("FAIL "+id+" timeout at tick "+ticks+" x="+s.player.x+" y="+s.player.y);
                return;
            }
            System.out.printf("PASS %s cleared in %.3fs, %d ticks, %d shots%n",id,s.clear.recordTime,ticks,s.shots);
        }
        System.out.println("ALL STAGES PASS");
    }
}
