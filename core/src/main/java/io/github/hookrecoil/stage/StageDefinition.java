/* 파일: StageDefinition.java
 * 역할: 정적 스테이지 배치 데이터. 책임: 발판·천장·고정점·체크포인트·가시·수집품 정의.
 * 협력: StageRuntime, CollisionWorld, CaveRenderer. 장치 상태는 스테이지별 DeviceRuntime이 소유.
 * 관련 사양: docs/10_PROLOGUE.md, docs/12_CAVE_CHAPTER.md.
 */
package io.github.hookrecoil.stage;

public class StageDefinition {
    public String id="prologue", name;
    public float height=12;
    public Hazard[] hazards=new Hazard[0];
    public Gem[] optionalGems=new Gem[0];
    public float width, spawnX, spawnY, exitX;
    /** Feet-height death boundary; lower authored values preserve intentional recovery platforms. */
    public float deathPlaneY=1f;
    public Block[] platforms, ceilings;
    public Point[] anchors, checkpoints;
    public Zone[] zones;
    public MineMover[] mineMovers=new MineMover[0];
    public MineCrusher[] mineCrushers=new MineCrusher[0];
    public MineSaw[] mineSaws=new MineSaw[0];
    public RechargeRing[] rechargeRings=new RechargeRing[0];
    public Gem clearGem;
    public static class Gem { public String id,type; public float x,y,halfWidth,halfHeight; }

    public static class Block {
        public float x, y, width, height, surfaceOffset; public float[] surfaceProfile; public String sprite; public boolean crumble; public transient boolean active=true;
        /** Surface profile samples world X-bin centers; surfaceOffset remains the fallback for older map data. */
        public float surfaceY(){return surfaceY(x+width*.5f);}
        public float surfaceY(float worldX){
            if(surfaceProfile==null||surfaceProfile.length==0)return y+height+surfaceOffset;
            float u=Math.max(0,Math.min(1,(worldX-x)/Math.max(width,1e-6f)));
            float pos=u*surfaceProfile.length-.5f;int lo=(int)Math.floor(pos),hi=lo+1;
            if(lo<0)return y+height+surfaceProfile[0];
            if(hi>=surfaceProfile.length)return y+height+surfaceProfile[surfaceProfile.length-1];
            float f=pos-lo;return y+height+surfaceProfile[lo]*(1-f)+surfaceProfile[hi]*f;
        }
    }
    public static class Hazard { public float x,y,width,height; public String direction="floor"; }
    public static class MineMover {
        public String id,kind; public int platform=-1,anchor=-1;
        public float fromX,fromY,toX,toY,period=4,phase;
        public transient float x,y,vx,vy;
    }
    public static class MineCrusher {
        public String id,direction="down"; public float x,topY,width=1.2f,stroke=2,period=3.6f,phase;
        public transient float extension; public transient String state="open";
    }
    public static class MineSaw {
        public String id; public float fromX,fromY,toX,toY,radius=.55f,period=3,phase;
        public transient float x,y,angle;
    }
    public static class RechargeRing { public String id; public float x,y,halfWidth=.35f,halfHeight=.4f; }
    public static class Point { public float x, y; }
    public static class Zone { public float x; public String title, hint, detail; }

    public void validate() {
        if(clearGem==null || !"clear_gem".equals(clearGem.type) || clearGem.id==null
            || clearGem.halfWidth<=0 || clearGem.halfHeight<=0)
            throw new IllegalArgumentException("Exactly one typed clear gem is required");
        if (platforms == null || ceilings == null || anchors == null || checkpoints == null)
            throw new IllegalArgumentException("Incomplete prologue data");
        for (Point anchor : anchors) {
            boolean supported = false;
            for (Block ceiling : ceilings)
                if (anchor.x >= ceiling.x && anchor.x <= ceiling.x + ceiling.width
                    && anchor.y <= ceiling.y && ceiling.y - anchor.y <= 0.6f) supported = true;
            if (!supported) throw new IllegalArgumentException("Anchor requires an overhead ceiling");
        }
    }
}
