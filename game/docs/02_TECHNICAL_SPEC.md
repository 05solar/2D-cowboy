# 기술 사양과 현재 구현

기준일: 2026-11-09. 원본 게임 규칙은 [HOOK_RECOIL_Game_Spec_v1.md](specs/HOOK_RECOIL_Game_Spec_v1.md)에 보존한다. 아래는 현재 코드/설정 설명이며 목표·미검증 사양은 별도로 표시한다.

## 실행 기반

| 항목 | 현재 값 |
|---|---|
| 언어·모듈 | Java, Gradle `core` + `lwjgl3` |
| 프레임워크 | libGDX 1.14.2, LWJGL3 3.4.3 |
| 개발 실행 환경 | Gradle wrapper, Java 21; Java 소스 호환 설정은 8 |
| 게임 조립 | `io.github.hookrecoil.HookRecoilGame` |
| 데스크톱 진입점 | `io.github.some_example_name.lwjgl3.Lwjgl3Launcher` |
| 물리 | 자체 구현, 고정 120Hz. Box2D는 사용하지 않음 |
| 월드 단위 | H, 설정 기본 `pixelsPerH=180` |
| 기본 데스크톱 창 | 1600×900 |
| 플레이 화면 | 16:9. 창 크기에 따라 월드 viewport를 늘려 더 넓은 영역을 표시 |

최소/권장 하드웨어, 배포용 JRE, Windows 이외 플랫폼 지원, 실제 1080p 성능은 확정되지 않았다.

## 물리·조작 설정

`assets/config/physics.json`이 공통 설정의 기준이다. 현재 기본 항목은 중력 24H/s², 점프 8H/s, 지상 속도 4.5H/s, 공중 가속 5H/s², 전체 속도 상한 16H/s, 반동 5.5H/s, 탄창 2발, 반동 간격 0.22초, 갈고리 사거리 7H·발사 40H/s·적중 반경 0.28H·발사 간격 0.15초다. 코요테 시간 0.08초, 점프 입력 버퍼 0.10초, 유효 착지 판정 0.05초다.

플레이어 충돌체는 0.345H×0.75H다. 시각 배율은 프롤로그 0.75, 본편 0.68이며 물리 크기와 별개다. 필수 클리어 보석은 `clearGemVisualScale=2.0`이다. 프롤로그에는 스윙 가속·줄 수축 등의 추가 튜닝값이 설정 파일에 있으며 프롤로그 전용 동작이다.

충돌은 지형에 대해 축별 스윕, 이동체·위험물에는 해당 런타임의 연속 검사/접촉 규칙을 사용한다. 투명 알파를 충돌로 간주하지 않는다. 광산 표면은 맵의 수동 surface profile을 따른다. 충돌이 모든 고속/복합 상황에서 검증되었다고 가정하지 않는다.

## 화면·카메라·입력 좌표

- `GameplayScreen`은 게임 렌더와 UI에 서로 다른 viewport를 쓴다. 월드 viewport는 창 높이×2를 논리 높이로, 16:9 논리 폭으로 갱신한다. 큰 창에서도 월드/화면 픽셀 배율을 유지한다.
- UI는 2560×1440 논리 좌표를 별도로 유지한다. 마우스 조준은 월드 viewport로 역변환하며 메뉴 좌표는 UI viewport로 변환한다.
- `assets/config/camera.json`의 `gameplayZoom=1.2`는 창 확대와 독립적인 게임 카메라 배율이다. `verticalFollowOffset=70`, `lowerEdgeAllowance=120`, `prologueCameraY=520`은 하단 발판이 보이도록 화면을 내리는 기준값이다. 카메라 시작 투어는 스폰→전체 맵→보석→스폰 순이며 전체 맵 공개까지 0.85초, 연출 중 스테이지 시뮬레이션은 멈춘다.
- 배경은 장식 레이어로 반복하지 않는다. 도시 V2는 구역 선택과 크로스페이드가 있다.

## 상태 소유·저장

| 책임 | 주요 구현 |
|---|---|
| 이벤트 수집·틱 입력 | `InputRouter`, `InputFrame` |
| 물리·플레이어 상태·사망/클리어 우선순위 | `StageRuntime`, `PlayerController`, `CollisionWorld` |
| 갈고리·반동 | `GrappleSystem`, `RecoilSystem` |
| 공통/챕터 장치 | `DeviceRuntime`, `MineDeviceRuntime`, `CityDeviceRuntime` |
| 리소스 로드·표시 | `GameAssets`, `CaveAssets`, `GemAssets`, `rendering/` |
| 완료 연출·저장 | `ClearSequence`, `ProgressStore` |
| 화면 조립·카메라 | `GameplayScreen`, `StageIntroCamera`, `CameraConfig` |

진행은 고정 stage ID의 ProgressStore schema 2에 저장한다. 보석 완료 journal 복구와 과거 프롤로그 단일 완료 파일 가져오기가 있다. 임의 위치에서 이어하기와 체크포인트 시점의 영구 저장은 현재 플레이 중 체크포인트 복귀와 구분한다. 상세는 [05_DATA_AND_SAVE_SPEC.md](05_DATA_AND_SAVE_SPEC.md).

## 확인 명령

```powershell
.\gradlew.bat :lwjgl3:compileJava
.\gradlew.bat :core:verifyPrologue
.\gradlew.bat :core:verifyCaves
.\gradlew.bat :core:verifyMineSurfaces
.\gradlew.bat :core:verifyMines
.\gradlew.bat :core:verifyCity
.\gradlew.bat :lwjgl3:verifyMinesNative
.\gradlew.bat :lwjgl3:verifyCityNative
```

위 목록은 실행 가능한 작업이지 모두 현재 통과했다는 선언이 아니다. 최근 실행 결과와 자동 검사 한계는 [07_TEST_PLAN.md](07_TEST_PLAN.md)와 [09_DECISIONS_AND_STATUS.md](09_DECISIONS_AND_STATUS.md)에 날짜와 함께 기록한다.

## 미확정·미완료

모든 맵의 사람 조작 완주, 난도 곡선, 모든 체크포인트 정지 출발, 고속 충돌·장치 상호작용 전수, 전체 FPS 비교, 반복 재시작, 출시 성능/하드웨어와 배포 패키지는 완료 사실로 기록하지 않는다. 설계 수치는 실행 측정과 분리한다.
