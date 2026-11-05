# 작업 인수인계

기준일: 2026-11-05. 새 세션은 [루트 AGENTS.md](../AGENTS.md)와 이 문서를 읽은 뒤 [개발 문서 색인](README.md) 및 해당 챕터 상태를 확인한다. 이 문서는 코드/데이터를 대체하지 않으며 마지막 커밋 이후 작업 내용을 반영한다.

## 프로젝트와 실행

- Java/libGDX 기존 구조: `core` 게임 로직, `lwjgl3` 데스크톱 실행. Gradle wrapper 9.7.1, Java 21 환경, Java 8 source/target 설정.
- 게임: `.\gradlew.bat :lwjgl3:run`; JAR: `.\gradlew.bat :lwjgl3:jar`.
- 본편 ID: `cave_01`~`cave_05`, `mine_01`~`mine_05`, `city_01`~`city_05`, 미래 데이터 `surface_01`~`surface_05`. 프롤로그는 별도 `prologue`.
- 카탈로그 `assets/cave_chapter/stages.json`은 1~15를 테스트 실행 가능, 16~20은 맵이 없어 unavailable로 둔다. 가용성은 클리어 검증이 아니다.

## 코드·데이터 기준 위치

| 영역 | 위치 |
|---|---|
| 게임 진입·화면 | `core/src/main/java/io/github/hookrecoil/HookRecoilGame.java`, `screens/GameplayScreen.java` |
| 물리·조작 | `physics/PhysicsConfig.java`, `physics/CollisionWorld.java`, `player/`, `grapple/`, `recoil/`, `input/` |
| 정적 맵·장치 | `stage/StageDefinition.java`, `stage/StageRuntime.java`, `stage/DeviceRuntime.java`, `MineDeviceRuntime.java`, `CityDeviceRuntime.java` |
| UI·저장·보석 | `ui/`, `save/ProgressStore.java`, `collectible/`, `rendering/GemRenderer.java` |
| 공통 수치 | `assets/config/physics.json` |
| 맵·카탈로그 | `assets/stages/`, `assets/cave_chapter/stages.json` |
| 광산 생성기 | `tools/BuildMineStages.py`; 수동 변경 시 재생성 결과와 동기화 |
| 현재 문서 | `docs/README.md`, `docs/09_DECISIONS_AND_STATUS.md`, 챕터별 상태 문서 |

## 현재 구현 요약

- 보석을 바닥에서 획득하면 시간을 확정하고 캐릭터·물리·장치·입력을 멈춘 뒤 별도 2초 획득 연출을 재생한다. 완료/해금 저장은 한 번 처리한다.
- 새 스테이지 시작은 스폰→전체 맵 축소→보석 확대→스폰 카메라 투어를 한다.
- 사망 화면은 자동 부활하지 않고 사용자의 입력을 기다린다.
- 프롤로그 그림 배율은 0.75, 본편 그림 배율은 0.68이다. 물리 충돌체는 동일하며 `assets/config/physics.json`에서 읽는다.
- 광산 6~10은 생성기를 수정해 내부 발판 폭의 일괄 확대를 제거했다. 테마별 맵 구성은 승강기 도입/S형 광차/압착기 샤프트/톱 갤러리/복합 종합으로 나뉘고 위험 데이터 수는 1/3/6/8/10이다.

## 마지막 작업 트리와 검증

2026-11-05 커밋 준비 작업에서 광산 레이아웃 변경과 기존 미커밋 본편 캐릭터 크기 변경을 함께 반영한다. 문서 설명은 현재 구현에 맞춰 갱신했다.

최근 실제 실행 기록:

- `:core:verifyPrologue` — 134 assertions 통과. 보석 관련 하위 출력 85 assertions.
- `:core:verifyMineSurfaces` — 31 assertions 통과.
- `:lwjgl3:verifyMinesNative` — 19 checks 통과, 다섯 광산 맵/배경 로드 확인.
- `:core:verifyMines` — 실패. 자동 경로가 `mine_01` 2단계에서 위치 `(9.041, 4.275)`, `grounded=false`로 assertion 실패. 이 드라이버는 갈고리를 사용하지 않는다. 수동 완주 여부는 미확인이다.
- 설정·카탈로그·광산 맵/route JSON 파싱과 `git diff --check` 통과.

이번 커밋에는 광산 생성기·맵 및 회귀 검사 변경, 본편 캐릭터 그림 배율 변경, 문서 동기화가 포함된다. 검증 결과는 커밋 전 실행 시점 기준이다.

## 남은 작업·주의점

1. 광산 각 체크포인트에서 실제 갈고리 조작으로 구간을 통과하고 위험물 배치·정상 궤도를 확인한다. `mine_01` 자동 경로 실패를 곧바로 맵 불가능 또는 통과로 판단하지 않는다.
2. 도시 11~15 전체 플레이 경로, 특히 city_05 이동 장치 구간의 사망 원인을 확인한다.
3. 본편 전체 수동 테스트, CP 위상 재시작 반복, 최대속도 관통, 30/60/144FPS 궤도 일관성, 난도 상승 검증은 완료되지 않았다.
4. 본편 캐릭터 그림 크기 변경은 collision size가 아닌 렌더 배율 변경이다. 게임에서 가독성을 직접 확인하고 물리 상수로 보정하지 않는다.
5. 스테이지 선택은 `available=true`이면 테스트 목적으로 시작할 수 있다. 진행 저장의 실제 해금 상태와 혼동하지 않는다.
6. Windows 작업표시줄 재실행 아이콘은 과거 native 설정 오류가 보고됐으며 현재 해결됐다고 단정하지 않는다.

## 재개용 명령

```powershell
.\gradlew.bat :lwjgl3:run
.\gradlew.bat :core:verifyPrologue
.\gradlew.bat :core:verifyCaves
.\gradlew.bat :core:verifyMineSurfaces
.\gradlew.bat :core:verifyMines
.\gradlew.bat :core:verifyCity
.\gradlew.bat :lwjgl3:verifyMinesNative
.\gradlew.bat :lwjgl3:verifyCityNative
```
