/* 파일: StageCatalog.java
 * 역할: 본편 카탈로그. 책임: 고정 ID·챕터·가용성 조회, 프롤로그 번호 분리.
 * 협력: ProgressStore, StageSelectView. 관련 사양: docs/12_CAVE_CHAPTER.md.
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
        public int number,checkpoints;
        public boolean available,backgroundRepeat,artAvailable;
    }
    public Entry find(String id) {for(Entry e:stages)if(e.id.equals(id))return e;return null;}
    public String next(String id) {for(Entry e:stages)if(id.equals(e.requires))return e.id;return null;}
}
