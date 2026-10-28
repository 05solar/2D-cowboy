/* 파일: GemSensor.java
 * 역할: 정상 신체 충돌체의 연속 보석 접촉 판정. 책임: 최초 접촉 비율 계산.
 * 협력: StageRuntime. 관련 사양: assets/gem_pickup/APPLY.md. 시각 효과는 판정에서 제외한다.
 */
package io.github.hookrecoil.collectible;

public final class GemSensor {
    private GemSensor() {}
    public static float contact(float x,float y,float endX,float endY,float halfWidth,float height,
                                float gemX,float gemY,float gemHalfWidth,float gemHalfHeight) {
        float lo=0,hi=1;
        float[] start={x,y},delta={endX-x,endY-y};
        float[] min={gemX-gemHalfWidth-halfWidth,gemY-gemHalfHeight-height};
        float[] max={gemX+gemHalfWidth+halfWidth,gemY+gemHalfHeight};
        for(int i=0;i<2;i++) {
            if(Math.abs(delta[i])<1e-8f) {if(start[i]<min[i]||start[i]>max[i])return -1;}
            else {
                float a=(min[i]-start[i])/delta[i],b=(max[i]-start[i])/delta[i];
                lo=Math.max(lo,Math.min(a,b));hi=Math.min(hi,Math.max(a,b));if(lo>hi)return -1;
            }
        }
        return lo;
    }
}
