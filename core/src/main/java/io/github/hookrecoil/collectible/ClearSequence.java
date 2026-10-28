/* 파일: ClearSequence.java
 * 역할: 보석 획득 상태·연출 시계. 책임: 단방향 전환과 프레임 시간 관리.
 * 협력: StageRuntime, GemRenderer. 수명: 새 시도에서 reset. 관련 사양: assets/gem_pickup/APPLY.md.
 */
package io.github.hookrecoil.collectible;

public class ClearSequence {
    public enum State { PLAYING, COLLECTING, CLEARED }
    public State state=State.PLAYING;
    public double time;
    public float x,y,recordTime;
    public boolean facingRight;
    private final int[] durations;
    private final int totalMilliseconds;
    public ClearSequence(int[] durations) {
        if(durations==null||durations.length==0)throw new IllegalArgumentException("Animation needs frames");
        this.durations=durations.clone();int total=0;
        for(int duration:this.durations){if(duration<=0)throw new IllegalArgumentException("Frame time must be positive");total+=duration;}
        totalMilliseconds=total;
    }
    public float durationSeconds(){return totalMilliseconds/1000f;}
    public boolean begin(float x,float y,boolean right,float record) {
        if(state!=State.PLAYING)return false;
        this.x=x;this.y=y;facingRight=right;recordTime=record;time=0;state=State.COLLECTING;return true;
    }
    public void advance(double seconds) {
        if(state!=State.COLLECTING)return;
        time=Math.min(durationSeconds(),time+Math.max(0,seconds));
        if(time>=durationSeconds()-1e-9) {time=durationSeconds();state=State.CLEARED;}
    }
    public int frame() {
        int end=0;
        for(int i=0;i<durations.length;i++) {end+=durations[i];if(time*1000<end-1e-7)return i;}
        return durations.length-1;
    }
    public boolean frozen() {return state!=State.PLAYING;}
    public void reset() {state=State.PLAYING;time=0;}
}
