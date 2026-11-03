# 광산 6~10 적용 상태

기준일: 2026-11-03. 광산 6~10은 기존 Java/libGDX 런타임에 연결되어 있고 스테이지 선택에서 테스트할 수 있다. 테스트 가능 상태와 완주·난도 검증 완료는 별개다. 원본 기획은 [게임 명세](specs/HOOK_RECOIL_Game_Spec_v1.md), 에셋·맵 설계는 [광산 에셋 안내](assets/mine_chapter/README.md)와 [스테이지 설계](assets/mine_chapter/STAGES_06_10.md)에 둔다.

## 런타임 연결

- `assets/cave_chapter/stages.json`의 `mine_01`~`mine_05`가 본편 6~10번이며 테스트용으로 `available=true`다. 실제 데이터는 `assets/stages/mine_01.json`~`mine_05.json`이다.
- `CaveAssets`는 명시 atlas rect로 배경·지형·장치를 읽는다. `MineRenderer`는 지형, 이동체, 가시, 압착기, 톱, 재충전 링, 체크포인트를 표시한다. 이동 레일처럼 보이는 반투명 `guide_rail`은 사다리식 이동 기능이 없고 맵 표현에도 필요하지 않아 런타임 atlas와 렌더링에서 제거했다. 원본 시트 PNG는 보존한다.
- `MineDeviceRuntime`은 이동체 시계·속도 전달, 압착기 주기, 위험 스윕, 공중 재충전 1회 사용, 재시작 초기화를 처리한다.
- 지형과 이동체 충돌면은 알파 테두리가 아닌 atlas의 수동 표면 프로파일을 사용한다. 플랫폼·승강기·광차 표면 기준은 [표면 충돌 기록](assets/mine_chapter/SURFACE_COLLISION.md)에 정리했다.
- `tools/BuildMineStages.py`가 광산 JSON 배치를 재생성한다. 코드·데이터를 직접 고칠 때 생성 결과와 원본 생성기를 함께 확인한다.

## 확인된 결과와 미완료 범위

- `:core:verifyMineSurfaces`: 19 assertions 통과.
- `:core:verifyPrologue`: 134 assertions, `:core:verifyCaves`: 최근 기록상 79 assertions 통과.
- `:lwjgl3:verifyMinesNative`: 최근 기록상 19 checks 통과. 카드 시작 가능 여부와 광산 맵·배경 로딩을 확인했다.
- `:core:verifyMines`의 자동 입력 경로는 `mine_01`에서 보석까지 도달하지 못한다. 전체 광산 완주 경로는 검증되지 않았다.
- 각 체크포인트의 정지 출발, 이동체·위험물 최대속도 관통, 재시작 반복, 수동 플레이 및 30/60/144 FPS 조작 일관성은 통과로 표시하지 않는다.

## 다음 확인

1. `mine_01`의 승강기 이후 상승 구간 입력과 맵 배치를 수정해 자동 경로를 보석까지 연결한다.
2. 각 스테이지의 체크포인트에서 정지·2발 상태로 다음 구간을 통과하는 실제 입력 경로를 확인한다.
3. 게임 화면에서 얇은 충돌면, 장치 정렬, 최대속도 충돌, 재시작 초기화와 FPS별 궤도를 확인한다.

실행 명령은 `.\gradlew.bat :lwjgl3:run`, 관련 검증은 `.\gradlew.bat :core:verifyMines`와 `.\gradlew.bat :lwjgl3:verifyMinesNative`다.
