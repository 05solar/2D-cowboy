# HOOK & RECOIL 개발 문서

문서 기준일: 2026-11-05. 프로젝트는 기존 Java/libGDX `core`·`lwjgl3` 구조를 사용한다. 프롤로그와 본편 1~15의 실행 데이터가 있으며, 16~20은 카탈로그 메타데이터만 있고 맵이 없다. 테스트용 `available=true`는 클리어 검증을 의미하지 않는다.

## 시작·현재 상태

- 실행: 프로젝트 루트 PowerShell에서 `.\gradlew.bat :lwjgl3:run`
- 새 세션 인수인계: [PROJECT_HANDOFF.md](PROJECT_HANDOFF.md)
- 현재 결정 및 변경 이력: [09_DECISIONS_AND_STATUS.md](09_DECISIONS_AND_STATUS.md)
- 현재 검증 범위: [07_TEST_PLAN.md](07_TEST_PLAN.md)
- 동굴 1~5: [12_CAVE_CHAPTER.md](12_CAVE_CHAPTER.md)
- 광산 6~10: [MINE_CHAPTER_STATUS.md](MINE_CHAPTER_STATUS.md)
- 지하도시 11~15: [CITY_CHAPTER_V2.md](CITY_CHAPTER_V2.md)
- 조작과 시각 표현: [10_PROLOGUE.md](10_PROLOGUE.md)
- 지상 보석 획득·완료 저장: [11_GEM_CLEAR.md](11_GEM_CLEAR.md)
- 시작 카메라: [13_STAGE_INTRO_CAMERA.md](13_STAGE_INTRO_CAMERA.md)

각 결과는 실행 시점에 종속된다. 자동 경로, 네이티브 데이터 로드, 수동 플레이테스트는 서로 다른 검증으로 기록한다.

## 기준 문서

| 문서 | 범위 |
|---|---|
| [원본 게임 명세](specs/HOOK_RECOIL_Game_Spec_v1.md) | 보존된 게임 기획 원문 |
| [개발 프로세스](01_DEVELOPMENT_PROCESS.md) | 단계·완료 조건과 현재 범위 |
| [기술 사양](02_TECHNICAL_SPEC.md) | 실제 엔진·물리·실행 구성 |
| [기능 사양](03_FEATURE_SPEC.md) | 조작·상태·원본과 현재 차이 |
| [파일 구조](04_PROJECT_STRUCTURE.md) | 현재 코드·데이터 책임 |
| [데이터·저장](05_DATA_AND_SAVE_SPEC.md) | 실제 ID·맵·schema 2 저장 |
| [코드 규칙](06_CODING_RULES.md) | 기능 분리·상단 주석·리소스 수명 |
| [검증 방법](07_TEST_PLAN.md) | 명령과 테스트 한계 |
| [콘텐츠·에셋](08_CONTENT_AND_ASSETS.md) | 1~20 리소스 범위 및 연결 상태 |
| [결정·진행 기록](09_DECISIONS_AND_STATUS.md) | 최신 상태 및 변경 이력 |

## 유지 규칙

- 새 작업은 [루트 AGENTS.md](../AGENTS.md)와 관련 최신 상태 문서를 먼저 읽는다.
- 원본 사양과 제공 에셋 라이선스/atlas 자료는 보존한다. 현재 구현과 달라진 부분은 별도 상태 문서에 쓴다.
- 문서의 목표·초기 수치와 실제 검사 결과를 구분한다. 실행하지 않은 검증은 PASS로 쓰지 않는다.
- 공유 문서에 개발자 개인 경로·`local.properties` 내용을 기록하지 않는다.
