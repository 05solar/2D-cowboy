# HOOK & RECOIL 개발 문서

문서 기준일: 2026-11-09. 프로젝트는 기존 Java/libGDX `core`·`lwjgl3` 구조를 사용한다. 프롤로그와 본편 1~15에 실행 맵이 있고 16~20은 카탈로그 항목만 있다. `available=true`는 테스트 실행 허용이며, 맵의 클리어 가능성이나 난도 검증을 뜻하지 않는다.

## 먼저 읽기

새 세션은 [루트 작업 규칙](../AGENTS.md)과 [작업 인수인계](PROJECT_HANDOFF.md)를 읽는다. 다음으로 이 색인, [결정·현재 상태](09_DECISIONS_AND_STATUS.md), 그리고 해당 챕터 상태 문서를 확인한다. 문서는 코드와 실제 스테이지 JSON을 설명하며 그 대신하지 않는다.

## 실행과 검증

- 실행: 프로젝트 루트 PowerShell에서 `.\gradlew.bat :lwjgl3:run`.
- JAR 빌드: `.\gradlew.bat :lwjgl3:jar`.
- 기본 창: 1600×900. 월드 오브젝트의 화면상 배율은 유지하며 창 크기에 맞춰 더 넓은 월드 영역을 보여준다.
- 검증 명령·최근 실행 결과: [07_TEST_PLAN.md](07_TEST_PLAN.md), 요약은 [09_DECISIONS_AND_STATUS.md](09_DECISIONS_AND_STATUS.md).

## 현재 상태 문서

| 문서 | 책임 |
|---|---|
| [01_DEVELOPMENT_PROCESS.md](01_DEVELOPMENT_PROCESS.md) | 작업 순서와 완료 기준 |
| [02_TECHNICAL_SPEC.md](02_TECHNICAL_SPEC.md) | 엔진·물리·카메라·저장 기술 구성 |
| [03_FEATURE_SPEC.md](03_FEATURE_SPEC.md) | 현재 조작과 원본 기획 차이 |
| [04_PROJECT_STRUCTURE.md](04_PROJECT_STRUCTURE.md) | 코드·도구·데이터의 실제 위치 |
| [05_DATA_AND_SAVE_SPEC.md](05_DATA_AND_SAVE_SPEC.md) | 카탈로그·맵 JSON·진행 저장 |
| [06_CODING_RULES.md](06_CODING_RULES.md) | 파일 분리·주석·리소스 수명 규칙 |
| [07_TEST_PLAN.md](07_TEST_PLAN.md) | 검증 명령·기록·미실행 범위 |
| [08_CONTENT_AND_ASSETS.md](08_CONTENT_AND_ASSETS.md) | 스테이지·에셋 연결 상태 |
| [09_DECISIONS_AND_STATUS.md](09_DECISIONS_AND_STATUS.md) | 현재 기준 결정·최근 변경·검증 요약 |
| [10_PROLOGUE.md](10_PROLOGUE.md) | 프롤로그 조작·맵·실행 |
| [11_GEM_CLEAR.md](11_GEM_CLEAR.md) | 보석 완료·획득 연출·저장 |
| [12_CAVE_CHAPTER.md](12_CAVE_CHAPTER.md) | 동굴 1~5, 선택 창, 동굴 렌더링 |
| [13_STAGE_INTRO_CAMERA.md](13_STAGE_INTRO_CAMERA.md) | 시작 투어와 동적 월드 뷰포트 |
| [MINE_CHAPTER_STATUS.md](MINE_CHAPTER_STATUS.md) | 광산 6~10 적용·검증 상태 |
| [CITY_CHAPTER_V2.md](CITY_CHAPTER_V2.md) | 지하도시 V2 적용·검증 상태 |

에셋 폴더의 설계 프롬프트·라이선스·atlas 안내는 제작 당시 참고 자료다. 현재 코드의 상태는 위 상태 문서가 우선한다. [원본 게임 명세](specs/HOOK_RECOIL_Game_Spec_v1.md)는 기획 원문으로 보존한다.

## 문서 유지 원칙

- 현재 구현, 계획값, 자동 검사, 네이티브 렌더 검사, 사람의 수동 플레이 결과를 구분한다.
- 실행하지 않은 검증은 통과로 기록하지 않는다. 실패한 자동 경로는 실제 플레이 결과로 해석하지 않는다.
- 구조·데이터 계약이 바뀌면 관련 문서와 [결정·현재 상태](09_DECISIONS_AND_STATUS.md)를 함께 갱신한다.
- 사용자 제공 사양·라이선스·atlas 원문과 개인 환경 파일은 공유용 문서에 복제하거나 임의 수정하지 않는다.
