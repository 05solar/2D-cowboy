# 작업 인수인계

기준일: 2026-11-03. 새 세션이나 새 에이전트는 작업을 시작하기 전에 이 문서와 [루트 AGENTS.md](../AGENTS.md)를 읽는다. 현재 구현 범위는 아래와 같으며, 체크리스트의 완료는 실제 코드와 검증 기록으로만 판단한다. 문서의 초기 기획·에셋 프롬프트는 최신 런타임 상태가 아닐 수 있다.

## 프로젝트 기준

- 게임은 Java/libGDX의 기존 `core`와 `lwjgl3` 모듈을 사용한다. 새 엔진이나 웹 런타임으로 대체하지 않는다.
- 루트 Gradle wrapper는 9.7.1, 개발 환경은 Java 21이다. `lwjgl3/build.gradle`의 소스·바이트코드 호환 설정은 Java 8이다.
- 게임 규칙의 원본은 [HOOK_RECOIL_Game_Spec_v1.md](specs/HOOK_RECOIL_Game_Spec_v1.md)이며 원본을 덮어쓰지 않는다.
- 문서 탐색은 [docs/README.md](README.md), 설계 변경·날짜별 기록은 [09_DECISIONS_AND_STATUS.md](09_DECISIONS_AND_STATUS.md)를 사용한다.
- 본편 고정 ID는 `cave_01`~`surface_05`이고 프롤로그는 별도 `prologue`다. 현재 테스트 카탈로그는 본편 1~15를 `available=true`, 맵 데이터가 없는 16~20을 `available=false`로 둔다. 시작 가능은 완주 검증을 뜻하지 않는다.

## 현재 구현 위치

| 기능 | 코드·데이터 위치 | 상태 문서 |
|---|---|---|
| 공통 게임, 고정 120Hz 물리, 스테이지 런타임 | `core/src/main/java/io/github/hookrecoil/` | [기능 사양](03_FEATURE_SPEC.md) |
| 프롤로그 맵과 물리 설정 | `assets/stages/prologue.json`, `assets/config/physics.json` | [프롤로그](10_PROLOGUE.md) |
| ID 카탈로그, 1~15 맵 | `assets/cave_chapter/stages.json`, `assets/stages/` | [동굴](12_CAVE_CHAPTER.md), [광산](MINE_CHAPTER_STATUS.md), [도시 V2](CITY_CHAPTER_V2.md) |
| 동굴·광산 공통 리소스 로더 | `core/.../assets/CaveAssets.java` | 위 챕터 상태 문서 |
| 광산 이동체·충돌·위험 | `core/.../stage/MineDeviceRuntime.java`, `core/.../rendering/MineRenderer.java`, `assets/stages/mine_*.json` | [광산](MINE_CHAPTER_STATUS.md) |
| 지하도시 V2 이동체·배경 전환·장치 | `core/.../stage/CityDeviceRuntime.java`, `core/.../rendering/CityRenderer.java`, `assets/stages/city_*.json` | [도시 V2](CITY_CHAPTER_V2.md) |
| 보석 획득·결과 저장 | `core/.../collectible/`, `core/.../save/`, `assets/gem_pickup/` | [보석 클리어](11_GEM_CLEAR.md) |
| 스테이지 시작 카메라 | `core/.../screens/StageIntroCamera.java`, `GameplayScreen.java` | [시작 카메라](13_STAGE_INTRO_CAMERA.md) |
| 스테이지 선택·HUD·사망 대기 화면 | `core/.../ui/` | [동굴 통합](12_CAVE_CHAPTER.md), [진행 기록](09_DECISIONS_AND_STATUS.md) |
| LWJGL 창·Windows 아이콘 식별 | `lwjgl3/src/main/java/io/github/some_example_name/lwjgl3/` | 아래 미해결 항목 |
| 원본 사용자 아이콘 이미지 | `lwjgl3/icons/logo-source.png`; Windows ICO `lwjgl3/icons/logo.ico`; 창 크기별 PNG `lwjgl3/src/main/resources/hook-recoil-window-icon-{16,32,64,128}.png` | 작업표시줄 표시 문제는 미해결 |

소스 위치 표의 `...`는 `core/src/main/java/io/github/hookrecoil/`를 뜻한다. 모든 직접 작성 Java 파일은 파일명·역할·책임·협력 대상·사양 링크가 있는 한국어 상단 주석을 갖춰야 한다. 생성 스크립트와 맵 생성기는 수동 변경보다 생성 원본을 먼저 확인한다.

## 완료된 주요 작업

- 프롤로그, 동굴 1~5, 광산 6~10, 지하도시 11~15의 아트와 맵 데이터를 기존 런타임에 연결했다. 도시 런타임은 V2 얇은 발판·열린 프레임과 가로/세로 배경 변형을 사용한다.
- 보석 접촉은 바닥 지지 상태에서만 확정한다. `COLLECTING` 중 게임 입력·중력·장치 물리를 정지하고 연출 시계만 진행한다. 기록 시간은 접촉 시점에 확정한다.
- 새 스테이지를 열면 스폰에서 전체 맵을 축소 화면으로 보여주고, 보석 위치로 이동해 확대한 뒤 스폰으로 복귀하는 카메라 투어를 재생한다.
- 사망 화면은 자동 부활하지 않고 입력을 기다린다. R 재시작, M 스테이지 선택, ESC 일시정지를 제공한다.
- 2026-11-03 광산의 미사용 반투명 `guide_rail`을 런타임 atlas 정의와 `MineRenderer`에서 제거했다. 원본 시트 PNG는 보존한다.
- 2026-11-03 스테이지 10의 이동 광차 탑승 판정을 플레이어 중심점이 아닌 플레이어 충돌체 폭과 플랫폼의 겹침으로 맞췄다. 기존 충돌 코드가 가장자리 겹침을 착지로 허용하는데 이동체 탑승 검사만 이를 거부하던 불일치를 고쳤다.

## 최근 검증과 한계

2026-11-03 스테이지 10 수정 후 수행:

- `:core:verifyMineSurfaces`: 21 assertions 통과. 승강기·광차 표면 및 스테이지 10 광차 가장자리에서의 지지·이동량 전달 포함.
- `:lwjgl3:verifyMinesNative`: 19 checks 통과. 테스트 저장으로 광산 카드 선택, 광산 1~5 맵·배경 로드 확인. 전체 플레이 경로 검증은 아님.
- `git diff --check`, 문서 상대 링크 검사, 광산 atlas 및 스테이지 JSON 파싱 통과.

기존 최신 챕터 기록에 남은 미완료 항목:

- `:core:verifyMines` 전체 자동 경로는 `mine_01`에서 보석에 도달하지 못한다. 스테이지 10 광차 탑승 수정은 국소 회귀 테스트만 통과했으며 mine_05 전체 완주는 확인하지 않았다.
- 도시 자동 입력 경로는 `city_01`~`city_04`에서 완료, `city_05`는 이동 장치 구간에서 사망한다. 11~15 전체 수동 플레이·체크포인트 경로·난도 상승은 미검증이다.
- 광산·도시 실제 플레이 중 최대 속도 관통, 장치 위상과 재시작 반복, 30/60/144 FPS 입력 일관성은 전부 확인된 것으로 간주하지 않는다. 각 세부 결과는 [광산 상태](MINE_CHAPTER_STATUS.md)와 [도시 V2 상태](CITY_CHAPTER_V2.md)를 확인한다.
- Windows 창 아이콘 PNG와 배포 ICO는 설정되어 있다. `WindowsTaskbarIdentity`가 프로세스 AppUserModelID를 설정하지만 `SHGetPropertyStoreForWindow`의 작업표시줄 재실행 아이콘 설정은 과거 실행에서 `HRESULT 0x80070578`로 실패했다. 작업표시줄 아이콘 문제는 해결·시각 확인되지 않았다.
- 클리어 결과 글자 크기는 커졌지만 1280×720 및 작은 창에서 잘림·겹침을 최신 실행으로 재확인하지 않았다.

## 다음 작업 순서

1. 게임을 실행해 mine_05의 실제 이동 광차에 착지하고, 가운데와 양쪽 가장자리에서 이동·정지·점프·재시작을 직접 확인한다. 실패가 남으면 표면 프로파일, 이동체 업데이트 시점과 화면상의 데크를 함께 캡처해 비교한다.
2. `mine_01` 자동 경로가 끊기는 승강기 이후 구간을 조사한다. `MineRouteDriver`의 경로 진단은 자동 조작 한계가 있으므로 클리어 불가 원인과 스크립트 누락을 구분한다.
3. `city_05` 이동 장치 사망 원인을 조사하고, 전체 11~15 수동 경로·체크포인트·배경 이음새 검증은 자동 스크린샷과 구분해 기록한다.
4. Windows 작업표시줄 아이콘은 현재 코드가 실패를 로그로 남기므로 새 접근을 검증하기 전 해결로 표시하지 않는다. `lwjgl3:run`의 Java 창과 배포 실행 파일을 분리해 원인을 좁힌다.
5. 관련 코드·데이터·검증을 바꾸면 이 문서, 챕터 상태 문서, `09_DECISIONS_AND_STATUS.md`를 함께 갱신한다.

## 재개용 명령

프로젝트 루트 PowerShell에서:

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

`:core:verifyMines`는 위에 적은 `mine_01` 경로 실패를 재현하는 진단용으로도 사용한다. 명령이 실패하면 과거 통과 기록을 근거로 현재 통과라고 보고하지 않는다.
