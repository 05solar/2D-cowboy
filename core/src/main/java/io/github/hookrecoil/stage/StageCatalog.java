/* 파일: StageCatalog.java
 * 역할: 20개 본편 카탈로그. 책임: 고정 ID·챕터·가용성·단계별 설계 메타데이터 조회, 프롤로그 번호 분리.
 * 협력: ProgressStore, StageSelectView. 수명: GameplayScreen이 JSON으로 로드하고 화면 수명 동안 유지한다.
 * 관련 사양: docs/12_CAVE_CHAPTER.md, docs/assets/city_chapter/STAGES_11_15.md.
 */
package io.github.hookrecoil.stage;
public class StageCatalog {
    public int schemaVersion;
    public boolean prologueSeparate;
    public Chapter[] chapters;
    public Entry[] stages;
    public static class Chapter { public String id,name; }
    public static class Entry {
        public String id,chapter,name,requires,objective,orientation,background,goal,designDocument;
        public String[] routePattern;
        public String[] motionPresets;
        public int number,checkpoints,difficultyRank;
        public int[] targetChainActions;
        public float narrowLandingWidthH;
        public boolean available,backgroundRepeat,artAvailable;
    }
    public Entry find(String id) {for(Entry e:stages)if(e.id.equals(id))return e;return null;}
    public String next(String id) {for(Entry e:stages)if(id.equals(e.requires))return e.id;return null;}
}
