# 기술 사양과 현재 구현

기준일: 2026-11-05. 이 문서는 저장소의 현재 구현과 아직 검증되지 않은 목표를 구분한다. 게임 규칙의 원본은 [HOOK_RECOIL 원본 명세](specs/HOOK_RECOIL_Game_Spec_v1.md)이며 수정하지 않는다.

## 실행 기반

| 항목 | 현재 구현 | 근거 |
|---|---|---|
| 언어·모듈 | Java, Gradle `core` + `lwjgl3` | `settings.gradle`, 모듈별 `build.gradle` |
| 게임 프레임워크 | libGDX 1.14.2, LWJGL3 3.4.3 | `gradle.properties` |
| 실행 환경 | Gradle wrapper 9.7.1, Java 21에서 사용 | `gradlew.bat`, 실행 검증 기록 |
| 소스 호환 | Java 8 source/target 설정 | Gradle 설정 |
| 게임 시작 클래스 | `io.github.hookrecoil.HookRecoilGame` | `core/src/main/java/io/github/hookrecoil/HookRecoilGame.java` |
| 데스크톱 진입점 | `io.github.some_example_name.lwjgl3.Lwjgl3Launcher` | `lwjgl3/build.gradle` |
| 물리 | 자체 고정 스텝, 플레이어 충돌·줄·장치 코드. Box2D 미사용 | `StageRuntime`, `CollisionWorld`, `GrappleSystem` |
| 좌표 | 월드 단위 H, 기본 에셋 기준 `pixelsPerH=180` | `assets/config/physics.json` |
| 표시 | 플레이 시야 16:9, 픽셀 아트 Nearest 렌더링 | 화면·에셋 로더 |

게임은 기존 Java/libGDX 구조를 유지한다. 기준 PC의 성능 수치, 배포용 JRE 포함 패키지와 Windows 외 플랫폼은 검증된 사양으로 선언하지 않는다.

## 물리 설정

공통 초기값은 `assets/config/physics.json`에서 읽는다. 현재 값은 중력 24H/s², 점프 8H/s, 지상 속도 4.5H/s, 공중 가속 5H/s², 속도 상한 16H/s, 반동 5.5H/s, 탄창 2발, 발사 간격 0.22초, 갈고리 사거리 7H, 발사 속도 40H/s, 적중 반경 0.28H, 발사 간격 0.15초다. 코요테 시간 0.08초, 점프 버퍼 0.10초, 착지 유효 시간 0.05초다.

물리 루프는 120Hz 고정 스텝이다. 프레임 표시와 판정을 분리한다. 플레이어 충돌체는 폭 0.345H, 높이 0.75H다. 프롤로그 캐릭터 그림 배율 0.75, 본편 그림 배율 0.68이다. 이 변경은 시각 크기만 바꾸고 충돌체·물리 크기는 유지한다.

일부 프롤로그 이동 피드백 값은 설정에 별도로 있다: 스윙 가속 20H/s², 스윙 입력 속도 12H/s, 줄 당김 2.4H/s, 최종 줄 길이 3.7H, 별도 총 알파 0.45. 이 값은 프롤로그의 누적 조정 결과이며 모든 맵의 수동 밸런스 검증을 뜻하지 않는다.

## 런타임 갱신 책임

- `InputRouter`는 키보드·마우스 이벤트를 `InputFrame`으로 전달한다.
- `StageRuntime`은 스테이지 시간, 플레이어, 장치, 충돌, 사망·클리어 전이 순서를 소유한다.
- `CollisionWorld`는 지형·플랫폼 충돌과 스윕을 처리한다. 광산 플랫폼은 수동 `surfaceProfile`을 사용하며 이미지 알파에서 자동 충돌을 만들지 않는다.
- `GrappleSystem`, `RecoilSystem`, `PlayerController`는 각 조작 영역을 나눠 담당한다.
- `DeviceRuntime`, `MineDeviceRuntime`, `CityDeviceRuntime`은 해당 장치 상태·시간·재시작을 관리한다.
- `GameplayScreen`은 로딩·입력 차단·카메라·렌더 연결을 맡는다. `StageIntroCamera`는 스폰→전체 맵→보석→스폰 투어를 별도로 관리한다.
- `ClearSequence`는 바닥에서 확인된 보석 획득 위치에 캐릭터를 고정하고 별도 연출 시계를 진행한다. 연출 시계는 일시정지/앱 비활성에서 멈춘다.

위 목록은 파일 책임 요약이다. 실제 메서드 계약과 초기화 경로는 해당 클래스를 기준으로 한다.

## 화면·자원·저장

자원은 `GameAssets`, `CaveAssets`, `GemAssets`가 로드하고 소유자가 해제한다. 스테이지 데이터는 `StageCatalog` 및 `StageDefinition`이 읽는다. 진행 저장은 ID 기반 `ProgressStore` schema 2이며 획득 중단 복구를 위한 pending journal을 가진다. 프롤로그의 과거 `CompletionStore` 기록은 새 진행 파일이 없을 때 `prologue` 완료로 이관한다. 저장 형식 세부사항은 [데이터·저장 상태](05_DATA_AND_SAVE_SPEC.md)를 참조한다.

기본 창 실행은 `.\gradlew.bat :lwjgl3:run`, JAR 생성은 `.\gradlew.bat :lwjgl3:jar`다. 실행 아이콘·작업표시줄 재실행 아이콘 상태는 [작업 인수인계](PROJECT_HANDOFF.md)의 미해결 항목을 본다.

## 자동 검증 명령

```powershell
.\gradlew.bat :core:verifyPrologue
.\gradlew.bat :core:verifyCaves
.\gradlew.bat :core:verifyMineSurfaces
.\gradlew.bat :core:verifyMines
.\gradlew.bat :core:verifyCity
.\gradlew.bat :lwjgl3:verifyMinesNative
.\gradlew.bat :lwjgl3:verifyCityNative
```

검증 결과는 시점별 기록이다. 기준일 현재 광산 표면 검증 31 assertions와 광산 네이티브 19 checks가 통과했다. `:core:verifyMines`는 `mine_01` 자동 경로가 2단계에서 grounded=false로 실패한다. 드라이버가 갈고리 조작을 재현하지 않으므로 실패 원인은 자동 경로의 한계일 수 있으며, 이는 수동 통과를 입증하지도 반증하지도 않는다. 챕터별 최신 결과는 [진행 기록](09_DECISIONS_AND_STATUS.md)을 본다.

## 아직 완료로 볼 수 없는 항목

성능 목표 1080p/60FPS, 30/60/144FPS 전체 맵 궤도 일관성, 모든 체크포인트에서 정지·2발 출발 통과, 100회 재시작, 챕터별 난도 상승 및 전체 수동 플레이는 완전히 검증되지 않았다. 최소·권장 PC 사양과 배포 패키지도 미정이다. 계획 문서의 수치나 성공 목표를 측정 결과로 인용하지 않는다.
