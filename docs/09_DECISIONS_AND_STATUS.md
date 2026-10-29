# 결정·진행 기록

기준일: 2026-10-26. 테스트 프롤로그 구현·검증 단계다. 아래 초기 계획은 정식 게임 작업을 위한 이력으로 유지한다.

2026-10-28 진행 갱신: 프롤로그 이후 `cave_01`~`cave_05`를 기존 Java/libGDX에 통합하고 ID 기반 선택/저장·장치 런타임·반응형 선택 창을 구현했다. 실제 LWJGL 창 자동 입력 경로로 전 맵 클리어와 UI/복원을 확인했다. 상세 구현·검증/수동 플레이 한계는 [동굴 챕터](12_CAVE_CHAPTER.md)를 참조한다.

## 프롤로그 구현 결정

- 2026-10-27 후속 수정: 공중 보석 접촉은 무시하고, 접지 및 실제 바닥 지원을 확인한 뒤 획득한다. 착지 틱의 공중 위치 되감기와 공중 접촉 예약을 하지 않는다. 이전 공중 고정 정책을 대체한다.

- 2026-10-27: 제공된 gem_pickup_set_v1 적용. 출구 대신 clear_gem 신체 스윕 접촉, 최초 접촉 위치/시간 고정, 획득 확정 원자 저장, 재실행 결과 복귀 정책을 채택했다. 최초 연출 시간은 1초였다.

- 2026-10-28: 보석 획득 8포즈의 런타임 프레임 시간을 2배로 늘려 전체 2초로 재생한다. 스테이지 선택 상세에서 각 스테이지 목표 설명과 공통 보석 안내 문구를 제거하고 이름·기록·잠금 해금 정보만 표시한다. [상세](11_GEM_CLEAR.md).

- 2026-10-28: UI 글꼴을 둥근모꼴+ Fixedsys 기반 픽셀 글꼴로 변경하고 프로젝트에 원본 TTF·라이선스 안내와 재생성 도구를 포함했다. 글리프 PNG는 Nearest 필터로 렌더링한다.

- 2026-10-28: 게임 HUD의 스테이지명·소개·구역 진행 표시는 프롤로그 전용으로 제한한다. 본편은 조작 키 안내와 시간·탄약·사망·체크포인트 정보를 유지한다.

- 추가 플레이 피드백: 총 불투명도 45%, 캐릭터·충돌체 75%, 스윙 접선 가속과 점진적 줄 당김 적용. 원본 줄 길이 고정 규칙의 프롤로그 예외다. 총 없이 두 링을 거쳐 반대편에 착지하는 경로를 해제 시점 3가지로 검증했다.

- 사용자 제공 V3 에셋을 원본 보존 복사하고 명시 atlas rect로 로드했다.
- 사용자 요청을 반영해 천장 부착 링만 연결하며, 천장 없는 긴 구멍은 반동으로 넘는다.
- 뒤쪽 발사 때 원본 recoil 시트를 좌우반전한다. 조준 총 부품은 별도 회전한다.
- P0 실행 환경은 Java 21 / Gradle 9.7.1에서 확인했다. 게임 소스는 계획된 새 패키지로 분리하되 런처 패키지는 기존 경로에 유지했다.
- 프롤로그는 정적 사각형 스윕·줄 제약을 사용한다. 실제 구현·예외·검증은 [프롤로그 문서](10_PROLOGUE.md)에 기록한다.
- 영구 저장·정식 20개 맵·후순위 도전 모드를 완료한 것으로 간주하지 않는다.

## 채택한 계획

| ID | 결정 | 이유·영향 |
|---|---|---|
| D01 | 기존 Java/libGDX 구조 활용 | 이미 core/lwjgl3 템플릿이 있으므로 이를 구현 기반으로 사용 |
| D02 | 원본 보존, 후속 사양 분리 | 원본 초기값과 구현 설계·검증 결과를 구분 |
| D03 | 목표 패키지 `io.github.hookrecoil` | 예제 패키지 제거 계획. 소스·Gradle 참조 전환은 P0에서 수행 |
| D04 | 기능별 패키지·클래스, 파일 상단 설명 필수 | 파일 역할과 변경 범위를 명확히 유지 |
| D05 | 물리 먼저, 대표 맵 다음, 20개 확장 마지막 | 물리 변경으로 인한 전체 맵 재작업 방지 |
| D06 | Windows x64 우선 검증 | 현재 개발 환경 기준 설계안. 다른 OS의 출시 지원은 미확정 |

## 결정이 필요한 항목

| 항목 | 현재 상태 | 결정 시점·증거 |
|---|---|---|
| 실행 JDK·Gradle 호환 | wrapper 9.7.1, daemon Java 21 선언 확인. 실행 미검증 | P0 실행/빌드 로그 |
| 물리 백엔드 | 미정, 별도 물리 의존성 없음 | P1 줄·CCD·이동체 실험 |
| 지상 가감속·충돌체·접촉 기준 | 미정 | P1 조작·충돌 검증 |
| 타일 편집 포맷·도구 | 미정 | P0/P1 데이터 왕복·오브젝트 ID 검증 |
| 뷰포트 H 크기·카메라 수치 | 미정 | P2 시야·조준 검증 |
| 화풍·해상도·애니메이션 | 미정 | P2 가독성 샘플 검토 |
| 기준 PC·최소/권장 사양 | 미정 | P2 기준 환경 기록, P6 성능 측정 |
| 배포 JRE·패키징·배포처 | 미정 | P6 새 환경 실행·저장 확인 |
| 목표 시간 표식 | 미정 | 일반 20개 플레이테스트 후 |
| 후속 설계안 세부 규칙 | R 사망 수 미가산, 바닥 재충전 이벤트, 동일 틱 CP 우선순위 등 | 해당 기능 실험 후 확정·필요 시 수정 |

## 진행 상태

- [x] 원본을 Downloads에서 `docs/specs/HOOK_RECOIL_Game_Spec_v1.md`로 이동
- [x] 이동 전후 SHA-256 동일 확인: `AB91AEAE7377185FFE462387B96C0B8163AE0D120A9B46E770DB0667F02994E1`
- [x] 기존 프로젝트 구조·선언 버전 확인
- [x] 프로세스·기술·기능·구조·데이터·작성 규칙·검증·콘텐츠 문서 작성
- [x] 루트 AGENTS.md에 기능별 분리·상단 주석 규칙 명시
- [x] P0 실행·빌드 확인, 게임 소스 새 패키지 전환(플랫폼 런처 패키지는 유지)
- [x] 테스트 프롤로그 이동·반동·천장 갈고리·정적 충돌 회귀 검증
- [ ] P1 전체 조작·물리 실험(이동체·정식 맵·광범위 수동 테스트 포함)
- [ ] P2 대표 맵 검증 이후 P3~P7 진행

- [x] 2026-10-28 cave_01~05 맵 구성 및 실제 입력 경로 30/60/144 FPS 회귀
- [x] ID 기반 진행 저장·선택 창, 체크포인트·가시·붕괴 장치, 동굴 배경 연결
- [x] 실제 LWJGL 창에서 입력·리사이즈·잠금·해금·저장 복원 검증
- [ ] 수동 조작 플레이테스트 후 붕괴 경고 시간·지형 접합·가시 체감 난이도 조정

## 이후 변경 기록 형식

`날짜 / 결정 ID / 변경 내용 / 원본 관련 절 / 이유 / 영향받는 파일·맵·저장 버전 / 검증 결과 / 남은 항목`을 남긴다. 미정 항목을 확정하면 이 표와 해당 사양을 함께 수정한다.


## 2026-10-28 낙하 사망 경계와 글리프 겹침 수정

폰트 아틀라스 셀 폭이 한글 글리프보다 좁아 HUD와 메뉴 글자가 깨져 보이던 문제를 수정했다. 폰트 생성 시 실제 글리프 픽셀 폭을 검사한다. 낭떠러지 사망은 화면 아래까지 지연되지 않도록 스테이지 데이터의 deathPlaneY 기준으로 처리한다. 동굴 1은 낮은 안전 복구 발판을 유지하고, 동굴 4의 붕괴 발판 아래 받침은 제거해 추락 사망하도록 했다. 사망 안내는 YOU DIED와 단축키만 표시한다.

검증 완료: java tools/BuildPrologueFont.java가 455개 글리프를 생성했고 셀 경계 검사를 통과했다. :core:verifyCaves는 72 assertions, :core:verifyPrologue는 131 assertions 통과. 동굴 1~5의 고정 입력 경로는 30/60/144 FPS에서 전부 사망 없이 완료했고, 추락 후 0.7초 이내 복귀 및 동굴 1·4 안전 발판 착지를 검사했다. :lwjgl3:verifyCavesNative는 31 checks 통과. 생성된 build/native-cave-check/menu-1280.png와 cave_01-start.png에서 한글 HUD·메뉴 가독성을 확인했다. Java 8 source/target obsolete 경고만 출력됐다.


## 2026-10-28 HUD·스테이지 선택·보석 표현
하단 조작 안내를 확대하고 키 이름을 설명 글자보다 크게 렌더했다. 스테이지 선택 카드·상세 패널 내부 여백을 늘렸다. clear_gem과 선택 보석을 키우고, 기존 개별 aura PNG로 저강도 청록 광원을 맥동시킨다. 충돌 판정과 보석 좌표는 유지한다.

검증: :lwjgl3:verifyCavesNative 31 checks 통과, :lwjgl3:jar 빌드 성공. 1280×720·640×480 스테이지 선택 화면과 HUD를 실제 실행 캡처로 확인했다. 동굴 1~5 전체 입력 경로도 네이티브 실행에서 통과했다.


## 2026-10-28 붕괴 발판 추락과 사망 문구
동굴 4 붕괴 발판 아래의 받침 지형을 제거하고 낙하 경계를 y=1로 맞췄다. 따라서 발판이 붕괴한 뒤 바닥에 닿기 전에 사망하고 체크포인트로 복귀한다. 사망 패널은 YOU DIED와 R/M/ESC 단축키만 표시한다. 이전 일시정지 안내 문구는 제거했다.
검증: :core:verifyCaves 73 assertions, :core:verifyPrologue 131 assertions, :lwjgl3:verifyCavesNative 32 checks 통과. cave_04 낙하 사망·부활 및 네이티브 사망 화면 캡처를 확인했고 JAR 빌드도 성공했다.


## 2026-10-28 갈고리 연결 허용 반경
갈고리 조준 표식과 발사체 적중 판정에 공유하는 hookHitRadius를 중앙 물리 설정에 추가하고 0.28H로 조정했다. 사거리·속도·줄 물리는 그대로다. 테스트 경로는 착지 지점에서 체크포인트를 밟도록 보완했다.
검증: 근거리 빗맞춤 연결 및 허용 범위 밖 조준 거부를 확인했다. :core:verifyPrologue 134 assertions, :core:verifyCaves 73 assertions, :lwjgl3:verifyCavesNative 32 checks 통과. 동굴 1~5 경로와 JAR 빌드도 통과했다.

## 2026-10-28 광산 챕터 6~10 통합 진행

광산 배경·장치 PNG, 명시 atlas와 설계 문서를 `assets/mine_chapter/`, `docs/assets/mine_chapter/`에 보존했다. `StageDefinition`, `MineDeviceRuntime`, `MineRenderer`, 에셋 로더와 맵 생성기를 추가해 정적 데이터와 렌더·물리 장치를 연결했다. 광산 맵 JSON은 `tools/BuildMineStages.py`에서 생성한다. 원본 20개 스테이지 카탈로그는 ID 병합으로 보존한다.

테스트 접근을 위해 구현된 cave_01~05와 mine_01~05를 모두 `available=true`로 열고, `ProgressStore`에서 사용 가능한 스테이지는 저장 진행도와 관계없이 시작하도록 변경했다. `:core:verifyPrologue` 134 assertions와 `:core:verifyCaves` 76 assertions 통과. `:lwjgl3:verifyMinesNative`는 격리 저장으로 전체 카드 해금, 5개 맵·배경 로드와 렌더를 통과했다. `:core:verifyMines`는 장치 단위 검사 후 mine_01 입력 경로가 목표 보석에 도달하지 못해 실패한다. 30/60/144 FPS 완료 경로와 체크포인트별 실주행은 미검증이다. 자세한 내용과 실행법은 `docs/MINE_CHAPTER_STATUS.md`에 기록한다.

## 2026-10-28 광산 플랫폼 외곽 표면 충돌

광산 terrain atlas의 세 플랫폼 모듈에 16구간 `surface_profile_px`를 명시했다. 스테이지 생성기가 픽셀 프로파일을 월드 폭으로 환산하고, `CollisionWorld`, 승강기 탑승, 체크포인트, 스폰 및 clear_gem 배치가 `Block.surfaceY(x)`를 공유한다. 투명 atlas 테두리는 충돌 표면에서 제외하고, 설정된 작은 단차 이내는 걷는 중 연결한다.

검증: `:core:verifyMineSurfaces` 19 assertions 통과, `:core:verifyPrologue` 134 assertions, `:core:verifyCaves` 76 assertions 통과, `:lwjgl3:verifyMinesNative` 19 checks 통과. `:core:verifyMines` 전체 경로는 mine_01 자동 경로가 보석에 닿지 않아 실패하므로 완주 검증은 미완료다. 세부 기준은 `docs/assets/mine_chapter/SURFACE_COLLISION.md`.

## 2026-10-28 지하도시 초기 통합과 사망 대기 화면

지하도시 11~15 원본 PNG·atlas·장치/이동/레벨 설계를 `assets/city_chapter/`, `docs/assets/city_chapter/`에 보존했다. 실측 atlas 로더·16:9 카메라의 clamp 배경·전용 렌더러·복합 collider union·고정 기류·전기 장벽·waypoint 이동 런타임을 연결했다. `tools/BuildCityStages.py`는 단계별 정적 초기 프로토타입 데이터를 생성한다. artAvailable은 true, available은 false로 유지해 미검증 맵이 저장 해금과 별개로 실행되지 않게 했다. 도시 적용 상태는 [CITY_CHAPTER_STATUS.md](CITY_CHAPTER_STATUS.md).

사망 애니메이션 0.35초 뒤에는 시뮬레이션과 시계를 멈추고 마지막 포즈 및 YOU DIED 패널에서 대기한다. R은 체크포인트 재시작, M은 스테이지 선택, ESC는 일시정지로 유지한다. 패널과 제목·키 안내 글자를 확대했다.

검증: `:core:verifyCity` 34 assertions, `:core:verifyCaves` 79 assertions, `:core:verifyPrologue` 134 assertions, `:lwjgl3:verifyCityNative` 11 checks, `:lwjgl3:verifyCavesNative` 33 checks 통과. 네이티브 캡처 `build/native-city-check/city_01-preview.png`~`city_05-preview.png`, `build/native-cave-check/death-waiting.png`를 확인했다. `:core:verifyMines`의 기존 mine_01 입력 경로 실패는 남아 있다. 지하도시 전체 체크포인트 경로·클리어 궤도 및 난도 검증은 아직 안 했다.


## 지하도시 11~15 진행 상태 (2026-10-29)

도시 11~15를 테스트 선택 가능하도록 카탈로그에 등록했다. 11·12는 자동 입력 경로가 clear gem 접촉까지 도달했으며, 13은 장벽 구간에서 실패하고 14·15는 자동 주행 완주를 확인하지 못했다. 네이티브 로딩·렌더 검증은 별도 수행한다. 상세 항목과 미검증 범위는 [CITY_CHAPTER_11_15_IMPLEMENTATION.md](CITY_CHAPTER_11_15_IMPLEMENTATION.md)를 참조한다. 자동 주행 미완료 스테이지는 수동 통과 검증 완료로 간주하지 않는다.


## 2026-10-29 스테이지 시작 카메라 연출

일반 스테이지 로드 시 스폰 위치에서 clear gem을 보여준 뒤 플레이어 위치로 돌아오는 카메라 투어를 적용했다. 1.8초 이동, 0.4초 보석 위치 유지, 1.5초 복귀다. 연출 중 물리·장치·타이머·조작 입력을 정지하고, 일시정지 시 연출 시계를 멈춘다. 저장 복원과 스크린샷 미리보기는 연출을 생략한다. 구현·검증: [13_STAGE_INTRO_CAMERA.md](13_STAGE_INTRO_CAMERA.md).


## 2026-10-29 도시 발판 수직 정렬 수정

도시 11~15의 발판 중심 X를 단계마다 이동시켜 같은 수직선에 반복 배치되지 않도록 했다. 도시15의 stairs_right는 플레이어 사다리 기능이 없고 충돌만 제공하므로 제거했다. 장벽·기류·충전 링 위치는 새 경로에 맞게 조정했다. 자동 주행 보조는 갈고리를 조작하지 않으므로 새 동선의 통과 증명으로 쓰지 않는다. 상세: [도시 챕터 구현 기록](CITY_CHAPTER_11_15_IMPLEMENTATION.md).


## 2026-10-29 미사용 도시 에셋 정리

도시 아틀라스에서 미사용 stairs_right/left, pillar, 팬 애니메이션 프레임, endpoint_stop, route_arrow와 이들 충돌 정의를 제거했다. 가시·붕괴 발판 등 장애물 에셋은 보존했다. 현재 도시 아틀라스 28개 스프라이트가 남아 있다. :core:verifyCity 39 assertion과 :lwjgl3:verifyCityNative 14 checks 통과.
