# 광산 6~10 적용 상태

**2026-11-09 장벽 주기·수평 장벽·회귀 수정:** 전기장벽 off/on 시간을 스테이지 진행에 맞춰 조정했다(6:8.0/1.2, 7:6.6/1.4, 8:5.6/1.6, 10:4.0/2.0; 9는 톱 타이밍 충돌로 7.4/1.2 원복, 아래 한계 참조). mine_03에 발판과 일직선인 수평 장벽(`mine_03.barrier.floor4`, x=22.6~24.2,y=4.42)을 추가해 수직 장벽만 있던 6~9 구간에 수평 배치를 1곳 더했다(기존에는 mine_05에만 1개, offSeconds=40으로 사실상 비활성 상태였음 → 5.0/1.0으로 실제 작동하게 수정). mine_03의 `press.6` 크러셔를 x=27.7→27.3으로 옮겨 플랫폼6 점프 이탈 지점과 거의 겹치던 기존 설계 결함(원래부터 CP 재출발 진단에서만 간헐적으로 드러나던 문제)을 본 경로에서도 고쳤다. `:core:verifyMines` 274 assertions PASS, `:lwjgl3:compileJava` PASS로 확인했다.

**한계:** 클리어 시간이 6→10 순서로 14.125/11.133/17.383/12.567/27.608s로, 7번(mine_02)과 9번(mine_04)이 단조 증가하지 않는다. 9번은 장벽 off 시간을 줄이면 기존에 타이밍으로만 우연히 안전했던 톱(`mine_04.saw.*`) 접근 구간이 즉시 사망으로 바뀌어(원복으로 확인됨) 원래 값으로 되돌렸다. 근본 원인은 `MineRouteDriver`가 장벽·크러셔는 범용 안전 대기 로직이 있지만 톱에는 없어, 앞 구간의 대기 시간 변화가 뒤 구간의 톱 위상에 그대로 전파되는 구조적 취약점이다. 다음 작업자는 (1) `MineRouteDriver`에 톱 전용 안전 대기 로직을 추가하거나 (2) 클리어 시간을 장벽 대기가 아닌 경로 길이/발판 배치로 늘리는 방식으로 9번을 다시 조정해야 한다.

**최신 수정:** [실제 경로 위 장애물과 발판 재배치](MINE_06_10_ROUTE_REWORK_2026-11-09.md). 상부에 고립된 장치 배치를 폐기했다. 모든 장벽·압착기·톱의 위험 범위가 자동 클리어 경로와 겹치는지 검사하며 9개 CP 재출발도 확인한다. 아래 과거 기록보다 링크 문서를 우선한다.

기준일: 2026-11-09. 광산 6~10은 기존 Java/libGDX 런타임에 연결되어 있고 스테이지 선택에서 테스트할 수 있다. 테스트 가능 상태와 완주·난도 검증 완료는 별개다. 아래의 수치화된 광산 검사 기록은 실행 당시 날짜의 스냅샷이며 2026-11-09에 재실행한 결과가 아니다. 현재 전체 상태는 [결정·현재 상태](09_DECISIONS_AND_STATUS.md), 작업 재개는 [인수인계](PROJECT_HANDOFF.md)를 우선한다. 원본 기획은 [게임 명세](specs/HOOK_RECOIL_Game_Spec_v1.md), 에셋·맵 설계는 [광산 에셋 안내](assets/mine_chapter/README.md)와 [스테이지 설계](assets/mine_chapter/STAGES_06_10.md)에 둔다.

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

1. `mine_01` 자동 검증 드라이버가 실제 갈고리 조작을 재현할 수 있는지 검토한다. 드라이버를 보완하기 전 자동 경로 실패만으로 맵 배치를 바꾸지 않는다.
2. 각 스테이지의 체크포인트에서 정지·2발 상태로 다음 구간을 통과하는 실제 입력 경로를 확인한다.
3. 게임 화면에서 얇은 충돌면, 장치 정렬, 최대속도 충돌, 재시작 초기화와 FPS별 궤도를 확인한다.

실행 명령은 `.\gradlew.bat :lwjgl3:run`, 관련 검증은 `.\gradlew.bat :core:verifyMines`와 `.\gradlew.bat :lwjgl3:verifyMinesNative`다.

## 2026-11-04 스테이지 6~10 발판·장애물 차별화

`tools/BuildMineStages.py`에서 내부 발판 폭을 일괄 3.2H 이상으로 키우던 보정을 제거했다. 각 맵은 폭에 따라 `platform_long`, `platform_medium`, `platform_small`을 사용한다. 경로도 6은 승강기 도입, 7은 두 광차가 있는 S형, 8은 압착기 사이 좌우 선반, 9는 긴 좌우 횡단 갤러리, 10은 승강기·광차·압착기·톱 조합으로 구분했다. 위험 오브젝트 수(hazards + crushers + saws)는 본편 6~10 순서로 1/3/6/8/10이다.

검증: `:core:verifyMineSurfaces` 31 assertions 통과(장애물 수와 맵별 3종 이상 플랫폼 실루엣 포함), `:lwjgl3:verifyMinesNative` 19 checks 통과(선택·맵·배경 로드). `:core:verifyMines` 자동 입력 경로는 mine_01의 2단계에서 x=9.041, y=4.275, grounded=false로 assertion 실패했다. 드라이버는 갈고리 조작을 재현하지 않으므로 이 실패만으로 실제 플레이 가능성을 판정할 수 없다. 전체 수동 클리어와 난도 상승은 확인되지 않았다.
# 2026-11-09 스테이지 6~10 경로 검증 갱신

`tools/BuildMineStages.py`가 생성하는 광산 플랫폼은 위쪽 진입만 받는 one-way 착지면으로 설정했다. 이전 full-solid 겹침 구조는 위로 오르는 플레이어가 상부 발판 밑면에 막힐 수 있었다. 6~10의 발판 위치·간격과 광산 경로 입력 시퀀스를 조정해 수직 상승, 좌우 전환, 반동/착지를 실제 `StageRuntime`에서 검증했다. 광차·승강기 접촉은 발판 표면과 플레이어 몸체 폭으로 계산하고, 탑승 이동량 및 이탈 속도는 각각 한 번만 전달한다.

`.\gradlew.bat :core:verifyMines` 결과: PASS, 71 assertions. mine_01~mine_05 모두 clear_gem에 도달하고 체크포인트 조건을 만족했다. 각 경로는 30/60/144FPS 표시 주기에서 같은 고정 120Hz 물리 경로/기록 시간을 냈다. 6번 승강기 승차·이탈과 7번 광차 가장자리 탑승 검사를 포함한다. 이는 자동 입력 경로 검증이며 난도 상승이나 사람의 수동 플레이테스트를 증명하지 않는다.

맵 재생기에서 확인한 실제 실패 원인은 7번 같은 높이 발판 사이 점프 불가능한 간격, 8~10번 연속 상승에서 반동 탄약을 고려하지 않은 수직 간격, 승강기 경로 위 위험물 배치였다. 이후 보장된 착지면·명확한 연결로 고쳤다. `assets/stages/mine_routes.json`은 검증 재생 시나리오이고 실제 클리어 궤도를 보장하는 레벨 데이터 자체는 아니다.

## Electric barrier update (2026-11-09)

Mine stages 6-10 now contain 2/3/4/5/6 electric barriers in progression order. Barrier phases are shared with the existing city-barrier runtime: off, warning, active. A fresh checkpoint restart resets phase and lethality. `:core:verifyMines` passed 80 assertions; scripted routes cleared every mine stage at 30/60/144 FPS with zero deaths. These are automated runtime-input checks, not human playtests.

## 2026-11-09 electrical timing and native rendering

Mine stages 6-10 keep their authored 2/3/4/5/6 barrier counts. The barrier timing is now 8.5 seconds off, 0.4 seconds warning, and 1.2 seconds active. The first off interval remains long enough for the checked opening route, then each gate visibly warns and activates. Core tests verify active collision and swept lethal contact; the native LWJGL test captures an active gate in `build/native-mine-check/mine-barrier-on.png`.


## Mine stage map update
For the current gem endpoint, route, hazard-count, and automated completion checks, see [Mine stages 6-10 route rework](MINE_06_10_ROUTE_REWORK_2026-11-09.md). This supersedes the earlier 8.5-second barrier off-time entry; current data uses 7.8/0.4/1.2 seconds.
