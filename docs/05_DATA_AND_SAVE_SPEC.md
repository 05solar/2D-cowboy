# 스테이지 데이터와 진행 저장

기준일: 2026-11-09. 이 문서는 현재 JSON 계약과 저장 코드 기준이다. 기획상 목표 필드와 미구현 기능은 실제 데이터와 분리해 쓴다.

## ID와 카탈로그

- 프롤로그 ID는 `prologue`; 본편 카탈로그는 `assets/cave_chapter/stages.json`의 20개 고정 ID다.
- 본편 1~5는 `cave_01`~`cave_05`, 6~10은 `mine_01`~`mine_05`, 11~15는 `city_01`~`city_05`, 16~20은 `surface_01`~`surface_05`다.
- `requires`는 진행 선행 ID, `available`은 현재 빌드가 맵 실행을 지원하는지 나타낸다. `ProgressStore.canStart`는 둘 다 검사한다. 본편 1~15는 사용자가 테스트를 요청해 `available=true`이며 16~20은 false다. 시작 가능 상태는 플레이테스트 통과가 아니다.
- 스테이지 선택의 키보드/마우스 포커스와 선택 스테이지는 UI 상태이며 진행 저장 데이터에 기록하지 않는다.

## 실행 맵 JSON

스테이지 정의는 `StageDefinition`이 로드한다. 실제 필드는 맵별로 다르며, 아래는 공통/주요 타입이다.

| 데이터 | 구현 의미 |
|---|---|
| `id`, `name`, `width`, `height`, `spawnX`, `spawnY`, `deathPlaneY` | 고정 ID·월드 경계·시작·낙사 기준 |
| `platforms`, `ceilings` | 충돌을 가진 정적 지형; 표면 프로파일이 있으면 `surfaceY(x)`가 상판을 결정 |
| `anchors`, `checkpoints`, `zones` | 갈고리 고정점·복원점·HUD/구역 설명 데이터 |
| `clearGem` | 필수 완료 보석의 ID·타입·센서 형상 |
| `optionalGems`, `hazards` | 선택 보석·수동 배치 위험 영역 |
| `mineMovers`, `mineCrushers`, `mineSaws`, `rechargeRings` | 광산 이동체·압착기·톱·공중 재충전 링 |
| `cityWindZones`, `cityBarriers`, `cityStructures`, `cityBackgroundRegions` | 도시 기류·전기 장벽·복합 구조물·배경 구역 |
| `routeDesign` | 설계 목표 모듈 수/경로 길이; `lengthMeasured=false`는 미측정임을 표시 |

맵 배치는 스테이지 JSON과 생성기로 관리한다. 이미지 알파로 충돌을 자동 생성하지 않는다. 배경 및 장식은 충돌이 아니다. `tools/BuildMineStages.py` 실행은 광산 JSON을 재생성하며 `--activate` 옵션은 광산 카탈로그 항목을 실행 가능으로 설정한다.

## 저장 코드

`ProgressStore.Data`는 schemaVersion 2이며 기록을 스테이지 ID로 저장한다. 각 기록에는 완료 여부, 최고 시간, 노체크포인트 시간, 최소 사망 수, 선택 수집품 ID가 들어간다. 진행 파일에는 `progress.json`, 검증된 교체를 위한 `progress.tmp`, 정상 이전 파일의 `progress.backup.json`을 사용한다.

보석 획득 확정은 `Pending`에 스테이지 ID·확정 시간·위치·사망/발사 기록·수집 ID를 먼저 기록한다. 재실행 때 pending이 있으면 단 한 번 완료 처리 후 결과를 복구한다. 저장이 실패하면 원본 진행을 보존하고 로드를 쓰기 불가 상태로 둔다. schema 2보다 높은 미래 버전은 덮어쓰지 않는다. 이전 프롤로그 `completion.json`은 새 진행 파일이 없을 때만 `prologue` 기록으로 가져온다.

저장 디렉터리는 게임 코드가 제공하는 사용자 데이터 경로이며, 이 문서에는 개발자 개인 경로를 넣지 않는다. `CompletionStore`는 이전 단일 프롤로그 형식 호환을 위해 남아 있다.

## 런타임 상태와 저장 분리

플레이 중 속도·줄·장치 타이머·현재 공중 위치·UI 포커스는 일반 진행 저장으로 보존하지 않는다. 클리어 결과는 보석 접촉 시 기록 시간을 확정한다. 보석 획득 연출 시간은 기록에 포함되지 않는다. 체크포인트 재시작은 런타임 상태 복원이며 현재 구현의 일반 저장/이어하기 기능과 다르다.

체크포인트별 모든 장치 위상, 손상 파일 복구 시나리오 전부, 저장 실패 UI, 모든 모드 기록의 완전한 사용자 흐름은 별도 검증 상태를 확인해야 한다. 계획상의 스키마 필드를 구현된 필드로 간주하지 않는다.
