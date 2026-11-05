# 광산 6~10 적용 상태

기준일: 2026-11-05. 광산 6~10은 기존 Java/libGDX 런타임에 연결되어 있고 스테이지 선택에서 테스트할 수 있다. 테스트 가능 상태와 완주·난도 검증 완료는 별개다. 원본 기획은 [게임 명세](specs/HOOK_RECOIL_Game_Spec_v1.md), 에셋·맵 설계는 [광산 에셋 안내](assets/mine_chapter/README.md)와 [스테이지 설계](assets/mine_chapter/STAGES_06_10.md)에 둔다.

## 런타임 연결

- `assets/cave_chapter/stages.json`의 `mine_01`~`mine_05`가 본편 6~10번이며 테스트용으로 `available=true`다. 실제 데이터는 `assets/stages/mine_01.json`~`mine_05.json`이다.
- `CaveAssets`는 명시 atlas rect로 배경·지형·장치를 읽는다. `MineRenderer`는 지형, 이동체, 가시, 압착기, 톱, 재충전 링, 체크포인트를 표시한다. 이동 레일처럼 보이는 반투명 `guide_rail`은 사다리식 이동 기능이 없고 맵 표현에도 필요하지 않아 런타임 atlas와 렌더링에서 제거했다. 원본 시트 PNG는 보존한다.
- `MineDeviceRuntime`은 이동체 시계·속도 전달, 압착기 주기, 위험 스윕, 공중 재충전 1회 사용, 재시작 초기화를 처리한다.
- 지형과 이동체 충돌면은 알파 테두리가 아닌 atlas의 수동 표면 프로파일을 사용한다. 플랫폼·승강기·광차 표면 기준은 [표면 충돌 기록](assets/mine_chapter/SURFACE_COLLISION.md)에 정리했다.
- `tools/BuildMineStages.py`가 광산 JSON 배치를 재생성한다. 코드·데이터를 직접 고칠 때 생성 결과와 원본 생성기를 함께 확인한다.
- 2026-11-04 맵 실루엣·위험물 수를 6~10 순으로 확장했다: 6은 넓은 지그재그+위험 1개, 7은 광차 S자+3개, 8은 압착기 샤프트+6개, 9는 톱 회랑+8개, 10은 장치 종합 지그재그+10개다. 생성기에서 짧은 내부 발판 폭을 획일적으로 늘리지 않아 세 플랫폼 모양을 섞는다. 세부 구성은 [단계 설계](assets/mine_chapter/STAGES_06_10.md)의 현재 배치 표를 따른다.

## 확인된 결과와 미완료 범위

- 스테이지 10 가장자리 탑승 검사 추가 시 `:core:verifyMineSurfaces` 21 assertions가 통과했다. 이후 맵별 장애물 수·플랫폼 실루엣 검사까지 추가했다.
- 맵 차이·위험 수·실루엣 확인을 추가한 뒤 `:core:verifyMineSurfaces`: 31 assertions 통과. 표면 탑승과 위험물 수·플랫폼 실루엣 구성을 검사한다.
- 2026-11-05 재실행: `:core:verifyPrologue` 134 assertions 통과, `:core:verifyMineSurfaces` 31 assertions 통과, `:lwjgl3:verifyMinesNative` 19 checks 통과.
- `:core:verifyCaves`: 이전 기록상 79 assertions 통과. 이번 문서 갱신에서는 재실행하지 않았다.
- 스테이지 10의 이동 광차 탑승은 플레이어 중심이 발판 밖에 있어도 충돌체 폭이 상판과 겹치면 탑승으로 인식하도록 수정했다. `:core:verifyMineSurfaces`의 21 assertions에서 가장자리 지지와 광차 이동 전달을 확인했다.
- 2026-11-05 `:core:verifyMines` 자동 입력 경로는 `mine_01`의 2단계에서 `(9.041,4.275)`, `grounded=false`로 assertion 실패했다. 드라이버가 갈고리를 사용하지 않아 실제 경로와 구분해야 하며, 전체 광산 수동 완주는 검증되지 않았다.
- 각 체크포인트의 정지 출발, 이동체·위험물 최대속도 관통, 재시작 반복, 수동 플레이 및 30/60/144 FPS 조작 일관성은 통과로 표시하지 않는다.

## 다음 확인

1. `mine_01`의 승강기 이후 상승 구간 입력과 맵 배치를 수정해 자동 경로를 보석까지 연결한다.
2. 각 스테이지의 체크포인트에서 정지·2발 상태로 다음 구간을 통과하는 실제 입력 경로를 확인한다.
3. 게임 화면에서 얇은 충돌면, 장치 정렬, 최대속도 충돌, 재시작 초기화와 FPS별 궤도를 확인한다.

실행 명령은 `.\gradlew.bat :lwjgl3:run`, 관련 검증은 `.\gradlew.bat :core:verifyMines`와 `.\gradlew.bat :lwjgl3:verifyMinesNative`다.

## 2026-11-04 스테이지 6~10 발판·장애물 차별화

`tools/BuildMineStages.py`에서 내부 발판 폭을 일괄 3.2H 이상으로 키우던 보정을 제거했다. 각 맵은 폭에 따라 `platform_long`, `platform_medium`, `platform_small`을 사용한다. 경로도 6은 승강기 도입, 7은 두 광차가 있는 S형, 8은 압착기 사이 좌우 선반, 9는 긴 좌우 횡단 갤러리, 10은 승강기·광차·압착기·톱 조합으로 구분했다. 위험 오브젝트 수(hazards + crushers + saws)는 본편 6~10 순서로 1/3/6/8/10이다.

검증: `:core:verifyMineSurfaces` 31 assertions 통과(장애물 수와 맵별 3종 이상 플랫폼 실루엣 포함), `:lwjgl3:verifyMinesNative` 19 checks 통과(선택·맵·배경 로드). `:core:verifyMines` 자동 입력 경로는 mine_01의 2단계에서 x=9.041, y=4.275, grounded=false로 assertion 실패했다. 드라이버는 갈고리 조작을 재현하지 않으므로 이 실패만으로 실제 플레이 가능성을 판정할 수 없다. 전체 수동 클리어와 난도 상승은 확인되지 않았다.
