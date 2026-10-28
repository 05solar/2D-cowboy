# 광산 챕터 6~10 적용 상태

## 설계 및 리소스

광산 원본 리소스, atlas 추출 좌표와 적용 명세는 `docs/assets/mine_chapter/`에 보존했다. 런타임 리소스는 `assets/mine_chapter/`에 배치했다. 배경은 장식으로만 사용하고 반복하지 않으며, 게임 화면비는 기존 16:9를 유지한다.

맵 데이터는 `tools/BuildMineStages.py`가 `assets/stages/mine_01.json`부터 `mine_05.json`까지 생성한다. 스테이지 ID는 `mine_01`~`mine_05`이며 기존 본편 번호 6~10에 대응한다. 원본 `HOOK_RECOIL_Game_Spec_v1.md`는 수정하지 않았다.

## 구현된 연결

- `StageDefinition`에 광산 이동체, 압착기, 톱, 재충전 링 데이터 타입을 추가했다.
- `MineDeviceRuntime`이 고정 시계 이동, 승강기·광차 속도 전달, 압착기 주기, 스윕 위험 판정, 공중 재충전 1회 사용과 재시작 초기화를 소유한다.
- `MineRenderer`가 광산 atlas 조각, 레일, 이동 장치, 가시와 체크포인트를 렌더링한다.
- `CaveAssets`가 광산 시트와 비반복 배경을 로드한다.
- `MineVerification`과 `MineRouteDriver`가 장치 및 입력 경로 검증을 위해 추가됐다. `:core:verifyMines` Gradle 태스크로 실행한다.
- 광산 플랫폼의 `surfaceProfile`은 atlas에서 지정한 각 모듈의 외곽 표면 픽셀을 월드 좌표로 변환한다. 충돌·승강기 탑승·체크포인트·스폰·클리어 높이가 투명 사각형 경계 대신 프로파일 표면을 참조하고, 작은 높낮이 차이는 걷는 중 연결되도록 설정했다.
- 외곽 표면 픽셀 기준과 충돌 적용은 [`SURFACE_COLLISION.md`](assets/mine_chapter/SURFACE_COLLISION.md)에 기록했다.

## 아직 완료되지 않은 항목

테스트를 위해 구현된 모든 동굴 1~5와 광산 6~10의 카탈로그 `available` 값을 `true`로 열었고, `ProgressStore`가 `available=true`인 스테이지는 저장 진행도와 무관하게 시작할 수 있게 했다. 기존 사용자 저장을 바꾸지 않아도 메뉴에서 전부 선택할 수 있다. 구현되지 않은 지하도시 11~15와 지상 16~20은 `available=false`로 실행이 막힌다. 자동 입력 경로 검증에서 `mine_01` 발판 진행/착지 경로가 목표 보석까지 이어지지 않는 문제가 남아 있어, 잠금 해제와 코스 완주 검증은 별도로 본다.

검증 실행 결과: `:core:compileJava`와 `:core:verifyPrologue`(134 assertions), `:core:verifyCaves`(75 assertions), `:lwjgl3:jar`, `:lwjgl3:verifyMinesNative`(19 checks) 통과. 네이티브 테스트는 별도 임시 저장으로 광산 1~5 카드 모두 시작 가능함을 확인하고 각 맵·배경을 로드해 렌더링했다. `:core:verifyMines`는 광산 1 자동 경로가 목표 보석에 도달하지 못해 실패한다. 실제 코스 완주 검증은 미완료다.


## 다음 작업

1. 승강기 탑승과 출발 위상, 다음 플랫폼 도달 입력을 플레이 가능한 지형으로 조정한다.
2. 6~10 각 체크포인트에서 2발 장전 정지 상태부터 통과하는 입력 경로를 만든다.
3. 고정 120Hz 물리에서 30/60/144 표시 주기 경로 일치, 위험물 연속 충돌, 재시작 초기화와 실제 게임 화면을 확인한다.
4. 광산 카드 5개와 모든 실제 주행 경로가 검증되지 않았으므로, 테스트 해금은 유지하되 정식 완료로 표시하지 않는다. 출시 준비 때 경로가 확인되지 않은 스테이지는 `available=false`로 되돌린다.

게임 실행 명령: `.\gradlew.bat :lwjgl3:run`. 게임 중 `M`으로 스테이지 선택 창을 열면 구현된 스테이지 1~10을 선택할 수 있다. 사용자 진행 저장은 잠금 해제를 위해 수정하지 않는다.

## 플랫폼 외곽 충돌 변경 (2026-10-28)

플랫폼 PNG의 투명 사각형 꼭대기를 충돌면으로 사용하던 처리를 수정했다. terrain atlas에 플랫폼별 16구간 표면 프로파일을 기록하고, 착지와 승강기 탑승·스폰·체크포인트 높이를 실제 그림의 윗면으로 옮겼다. 걸을 때 프로파일이 조금씩 오르내리는 부분은 `surfaceStepHeight` 범위 내에서 연결한다. 상세 기준은 [`SURFACE_COLLISION.md`](assets/mine_chapter/SURFACE_COLLISION.md).

`:core:verifyMineSurfaces` 19 assertions, `:core:verifyPrologue` 134 assertions, `:core:verifyCaves` 76 assertions, `:lwjgl3:verifyMinesNative` 19 checks 통과. 전체 `:core:verifyMines`는 광산 1 자동 경로가 목표 보석에 도달하지 못해 실패하며, 발판 표면 단위 테스트 통과와 구분한다.

## 이동 승강기 상판 보정 (2026-10-28)

초기 수정에서 terrain 플랫폼 표면을 lift/cart 충돌에 그대로 사용해 승강기 윈치/상판 높이와 맞지 않는 문제가 남았다. atlas에 lift deck `surface_profile_px=75`, minecart deck `surface_profile_px=5`를 추가했다. 맵 생성기는 이동체의 `fromY/toY` 기준도 상판 프로파일만큼 보정해 에셋 그림·충돌면·원래 의도한 스테이지 높이를 일치시킨다. native 캡처에서 승강기 상판이 시작 발판 높이로 올라온 것을 확인했다.

최신 검증: `:core:verifyMineSurfaces` 19 assertions, `:core:verifyPrologue` 134 assertions, `:core:verifyCaves` 76 assertions, `:lwjgl3:verifyMinesNative` 19 checks 통과. `:core:verifyMines`는 lift를 통과한 뒤 mine_01 다음 상승 구간에서 실패한다. 이 경로 실패는 상판 정렬 단위 검증과 별개로 남아 있다.
