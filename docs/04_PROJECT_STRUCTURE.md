# 프로젝트 파일 구조

아래는 **정식 게임의 목표 구조**다. 현재 테스트 프롤로그는 이 구조의 기능별 패키지에 구현했다. 실제 구현 범위와 차이는 [프롤로그 문서](10_PROLOGUE.md)를 따른다. 빈 구현 파일을 일괄 생성하지 않고 기능 개발 시 필요한 파일을 만든다. 표의 각 Java 클래스명은 각각 별도 `.java` 파일이다.

## 루트 구성

```text
2D-cowboy/
├── AGENTS.md                    # 개발자·에이전트 공통 필수 규칙
├── README.md                    # 시작 안내와 문서 진입점
├── docs/                        # 프로세스·사양·검증·결정 기록
│   ├── specs/                   # 원본 명세 보관
│   ├── stages/                  # stage_01.md ~ stage_20.md, 제작 시 작성
│   └── reports/                 # 플레이테스트·성능·검증 보고서
├── core/
│   ├── build.gradle
│   └── src/
│       ├── main/java/io/github/hookrecoil/
│       └── test/java/io/github/hookrecoil/
├── lwjgl3/
│   ├── build.gradle
│   ├── src/main/java/io/github/hookrecoil/lwjgl3/
│   └── src/main/resources/      # 실행 아이콘 등 데스크톱 자원
├── assets/                      # 배포에 포함하는 파일만
│   ├── config/                  # physics.json, camera.json, audio.json
│   ├── stages/                  # catalog.json, stage_01.json ~ stage_20.json
│   ├── maps/                    # 테마별 타일 맵, 포맷은 P0/P1 결정
│   ├── textures/                # player, objects, tilesets, backgrounds, effects
│   ├── audio/                   # sfx, music, ambience
│   ├── ui/                      # skin, icons, fonts
│   └── text/                    # 안내·메뉴·스토리 문자열
├── art-source/                  # 편집 원본, 배포 에셋과 분리
├── tools/                       # 데이터·헤더 검증 등 필요 시 추가
└── gradle/, gradlew, gradlew.bat # 기존 빌드 도구 유지
```

저장 데이터는 설치 폴더/`assets` 안에 두지 않는다. `build`, `.gradle`, 개인 IDE 설정은 코드 구조에 포함하지 않는다.

## core 기능별 파일

기준 경로: `core/src/main/java/io/github/hookrecoil/`.

| 패키지 | 파일 | 한 파일의 책임 |
|---|---|---|
| 루트 | `HookRecoilGame` | 서비스 생성·화면 전환·종료. 게임 규칙 구현 금지 |
| app | `GameState`, `GameStateMachine` | 상태 값 / 허용 상태 전환 |
| app | `FixedStepLoop` | 120Hz 누적 갱신·보간 비율 |
| input | `GameAction`, `InputBindings` | 행동 정의 / 키 설정 |
| input | `InputRouter`, `InputFrame` | 이벤트 수집·포커스 처리 / 틱별 입력 데이터 |
| physics | `PhysicsWorld`, `CollisionQuery` | 물리 구현 경계 / 충돌·시야 질의 |
| physics | `PhysicsConfig`, `CollisionLayers` | 초기 수치 로드 / 충돌 분류 |
| player | `PlayerState`, `PlayerController` | 플레이어 상태 / 이동·점프 |
| player | `GroundContactTracker` | 바닥·코요테·유효 착지 판정 |
| grapple | `GrappleSystem`, `GrappleState` | 발사·적중·해제 조정 / 연결 상태 |
| grapple | `RopeConstraint` | 최대 거리·상대 속도 제약 |
| recoil | `RecoilSystem`, `AmmoState` | 발사 간격·반동 / 탄약·재충전 |
| stage | `StageDefinition`, `StageLoader`, `StageValidator` | 데이터 형식 / 파싱 / 참조·범위 검사 |
| stage | `StageRuntime`, `StageCatalog` | 장치 생성·업데이트 조정 / 스테이지 순서 |
| checkpoint | `CheckpointSystem`, `CheckpointSnapshot`, `RespawnService` | 활성화 / 기준 상태 / 원자적 복원 |
| world | `Anchor`, `MovingAnchor`, `MovingPlatform`, `MotionPath` | 고정점 / 이동 고정점 / 탑승면 / 경로 계산 |
| world | `WindZone`, `RechargeRing`, `ExitZone` | 바람 / 1회 충전 / 출구 트리거 |
| hazard | `HazardSystem`, `DeathCause` | 치명 판정 통합 / 사망 원인 |
| hazard | `SpikeHazard`, `CrusherHazard`, `SawHazard`, `ElectricBarrier`, `CrumblingPlatform` | 장치마다 독립된 상태·갱신·복원 |
| collectible | `CollectibleSystem`, `CollectibleState` | 획득·확정·취소 / 임시·영구 상태 |
| run | `RunTracker`, `RunMode`, `RunResult` | 시간·사망·기록 / 모드 / 결과 데이터 |
| progression | `ProgressionService` | 클리어·해금·완주 표식 |
| save | `SaveService`, `SaveData`, `SaveMigrator`, `SaveStorage` | 저장 조정 / 스키마 / 버전 변환 / 파일 I/O 계약 |
| settings | `SettingsService`, `SettingsData` | 설정 적용·저장 요청 / 값 |
| camera | `CameraController`, `AimCoordinateMapper` | 추적·경계 / 화면→월드 변환 |
| screens | `TitleScreen`, `GameplayScreen`, `StageSelectScreen`, `ResultScreen`, `EndingScreen` | 화면별 생성·표시·전환 |
| ui | `HudView`, `PauseMenu`, `SettingsView`, `ReticleView` | 표시·사용자 명령 전달, 물리 상태 직접 수정 금지 |
| rendering | `WorldRenderer`, `PlayerRenderer`, `RopeRenderer`, `EffectRenderer` | 월드 / 플레이어 / 줄 / 효과 그리기 |
| audio | `AudioService`, `AudioEvents` | 음원 재생·음량 / 사운드 이벤트 매핑 |
| assets | `GameAssets` | 공유 리소스 로드·소유·해제 |
| debug | `DebugOverlay`, `InputRecorder` | 충돌·속도·위상 표시 / 입력 재생 자료 |

`lwjgl3/.../lwjgl3/`에는 `Lwjgl3Launcher`, 기존 `StartupHelper`, `DesktopSaveStorage`를 둔다. 플랫폼 파일 경로와 실제 교체 저장 처리는 데스크톱 어댑터가 담당한다. `core`는 `lwjgl3`를 import하지 않는다.

## 의존 방향과 소유권

- 런처 → 게임 조립 → 화면/StageRuntime → 기능 시스템 → 상태·설정·물리 질의 순서로 의존한다.
- UI·렌더·음향은 읽기 상태/이벤트를 사용한다. 물리 결과를 표시 코드에서 변경하지 않는다.
- 이동·갈고리·반동의 최종 속도 반영은 고정 틱 조정자가 순서대로 호출한다. 각 시스템이 따로 플레이어 위치를 갱신하지 않는다.
- `StageRuntime`은 장치를 조정하지만 모든 장치 로직을 담지 않는다. 공통 `Manager` 하나에 게임 전체를 넣지 않는다.
- 스테이지 상태는 맵 교체 시 폐기, `GameAssets`와 저장/설정 서비스는 게임 수명 동안 유지한다.
- 테스트는 대상 패키지를 그대로 따라 `.../grapple/RopeConstraintTest.java`처럼 배치한다. 실험용 맵과 입력 자료는 테스트 전용 자원에 둔다.

## 기존 파일 전환

프롤로그에서 `Main`을 제거하고 `HookRecoilGame` 진입점과 런처 참조를 갱신했다. 플랫폼 런처 패키지와 `application.mainClass`는 기존 경로에 유지했다. 기존 `StartupHelper`의 라이선스·플랫폼 처리는 유지한다. 정식 패키징 identifier 전환은 후속 작업이다.
