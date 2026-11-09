# 프로젝트 파일 구조와 현재 책임

기준일: 2026-11-09. 이 문서는 실제 저장소의 주요 파일을 안내한다. 각 소스 파일의 한국어 상단 주석과 코드를 세부 책임의 최종 기준으로 삼는다.

## 루트

```text
AGENTS.md                 작업 규칙
core/                     게임 규칙·화면·검증 코드
lwjgl3/                   데스크톱 런처·아이콘·네이티브 검증
assets/config/            공통 물리 설정
assets/stages/            프롤로그 및 cave/mine/city 실제 맵 데이터
assets/cave_chapter/      고정 ID 기반 20개 본편 카탈로그와 cave art
assets/mine_chapter/      광산 아틀라스·설계 자료
assets/city_chapter/      도시 V1/V2 atlas 및 배경 리소스
assets/gem_pickup/        보석 획득 애니메이션 원본·추출 자료
tools/                    맵/폰트 자산 생성 스크립트
docs/                     현재 사양·상태·인수인계·검증 기록
```

## 런타임 구조

| 위치 | 주요 책임 |
|---|---|
| `core/.../HookRecoilGame.java` | libGDX 초기 조립, 화면 소유·종료 |
| `core/.../input/` | 이벤트 수집 및 틱 단위 입력 프레임 |
| `core/.../player/`, `recoil/`, `grapple/` | 플레이어 상태·이동·총 반동·갈고리 줄 |
| `core/.../physics/` | 중앙 물리 설정과 충돌/표면 질의 |
| `core/.../stage/` | 카탈로그·정적 맵 모델·공통/광산/도시 장치 런타임 |
| `core/.../collectible/` | 보석 감지와 정지 획득 시퀀스 |
| `core/.../save/` | ID 진행 기록, pending 완료 journal, 원자 저장 |
| `core/.../rendering/` | 플레이어·챕터 지형·장치·보석 렌더러 |
| `core/.../screens/` | 실제 게임 화면과 시작 카메라 투어·도시 배경 선택 |
| `core/.../ui/` | HUD·스테이지 선택·사망/결과 패널 |
| `core/.../assets/` | 공유 텍스처·사운드 리소스 로딩 및 해제 |
| `core/src/test/java/io/github/hookrecoil/` | `verify*` Gradle 작업에서 실행하는 검증 프로그램 |
| `lwjgl3/.../Lwjgl3Launcher.java` | 데스크톱 창 실행 설정 |
| `assets/config/physics.json`, `camera.json` | 월드/조작 수치와 플레이 카메라 배율 |
| `tools/BuildMineStages.py` | 광산 6~10 맵 JSON 생성의 원본 |
| `tools/BuildCityStages.py`, `BuildCaveMaps.py` | 챕터 맵 데이터 생성 보조 도구 |

`...`는 `core/src/main/java/io/github/hookrecoil/`의 실제 경로 축약이다. 데스크톱 패키지는 기존 템플릿 이름 `io.github.some_example_name.lwjgl3`를 유지한다.

## 스테이지 데이터

- `assets/stages/prologue.json`은 본편 번호에서 분리된 `prologue`다.
- `assets/cave_chapter/stages.json`은 고정 ID `cave_01`~`surface_05`를 관리한다. cave/mine/city 1~15는 `available=true`, 데이터 없는 surface 16~20은 `false`다. `available`은 완주 검증 표시가 아니다.
- `assets/stages/cave_01.json`~`cave_05.json`, `mine_01.json`~`mine_05.json`, `city_01.json`~`city_05.json`은 실행 맵이다. `mine_routes.json`은 자동 경로 진단 데이터다.
- 도시 V2의 가로·세로 배경 변형과 카메라 영역은 실제 맵/카탈로그 정의에서 읽는다. 배경 그림은 충돌 지형이 아니다.
- `GameplayScreen`의 월드와 UI는 별도 viewport를 쓴다. 기본 데스크톱 창 1600×900에서 월드 픽셀 배율을 유지하며 화면 크기에 따라 보여주는 영역을 조정한다.
- 광산 생성기를 실행하면 다섯 광산 JSON과 카탈로그 항목을 다시 쓴다. 생성 데이터를 편집하기 전에 생성 원본을 수정한다.

## 직접 작성 파일 규칙

Java·Python·스크립트의 상단 주석에는 파일명, 역할, 책임, 협력 대상, 관련 사양과 필요한 경우 수명 책임을 적는다. 자동 생성 파일, 에셋 원본, 라이선스 헤더는 별도 규칙 없이 기계적으로 바꾸지 않는다. 세부 기준은 [코드 작성 규칙](06_CODING_RULES.md)을 따른다.
