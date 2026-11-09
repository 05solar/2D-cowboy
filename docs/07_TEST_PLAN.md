# 검증 방법과 실행 결과

기준일: 2026-11-09. 아래 검사는 범위가 서로 다르다. 자동 route, 정적 데이터 회귀, 네이티브 렌더·화면 검증, 사람의 조작 플레이를 같은 의미로 취급하지 않는다. 실제 결과는 실행된 날짜와 현재 코드 변경 여부를 함께 기록한다.

## Gradle 검증 작업

프로젝트 루트에서 실행한다.

```powershell
.\gradlew.bat :lwjgl3:compileJava
.\gradlew.bat :core:verifyPrologue
.\gradlew.bat :core:verifyCaves
.\gradlew.bat :core:verifyMineSurfaces
.\gradlew.bat :core:verifyMines
.\gradlew.bat :core:verifyCity
.\gradlew.bat :lwjgl3:verifyMinesNative
.\gradlew.bat :lwjgl3:verifyCityNative
.\gradlew.bat :lwjgl3:verifyCavesNative
```

작업 이름은 Gradle에 등록된 검증 프로그램을 뜻한다. 최신 실행 결과가 없는 명령은 다시 실행하기 전까지 미검증이다.

| 작업 | 확인 범위 | 한계 |
|---|---|---|
| `:core:verifyPrologue` | 프롤로그 물리·링·반동·보석·저장 회귀 | 사람의 난도 평가 아님 |
| `:core:verifyCaves` | cave 데이터·진행·충돌·30/60/144 FPS 자동 경로 | 사람 입력 완주 아님 |
| `:core:verifyMineSurfaces` | surface profile, 이동 발판 지지, 실루엣/위험 수 회귀 | 광산 전체 클리어 아님 |
| `:core:verifyMines` | 광산 데이터·장치·자동 route | 현재 driver는 갈고리 입력을 재현하지 않아 경로 실패 이력이 있음 |
| `:core:verifyCity` | 도시 데이터·장치·자동 route | 자동 driver 결과는 수동 플레이와 다름 |
| `:lwjgl3:verify*Mines/City/Caves*Native` | 실제 데스크톱 창에서 UI·리소스·화면 동작 일부 | 스크린샷이나 렌더 확인만으로 경로를 통과했다고 할 수 없음 |

## 최근 실행 결과

2026-11-09 게임 코드 작업 중 확인:

- `:core:verifyCaves`: PASS, 79 assertions. cave_01~05 재현 경로·관련 회귀 포함.
- `:lwjgl3:compileJava`: PASS.
- `:lwjgl3:verifyCityNative`: PASS, 28 checks. 도시 atlas/배경/스테이지 로드와 오프닝 카메라 전환 확인. 해당 검증 창은 1280×720이므로 기본 1600×900의 화면 크기 검증은 포함하지 않음.
- `git diff --check`: PASS, 줄바꿈 변환 경고 외 공백 오류 없음.

이전 기록에서 광산 표면 31 assertions 및 광산 native 19 checks 통과가 남아 있다. `:core:verifyMines`는 mine_01 자동 입력이 2단계에서 grounded가 되지 않아 실패했다. 해당 driver가 갈고리를 사용하지 않으므로 이를 실제 플레이의 성공/실패로 단정하지 않는다. cave native 검사는 사망 패널 대기 assertion 실패 이력이 있으며 재실행 전까지 통과로 표기하지 않는다. 챕터별 과거 결과는 해당 상태 문서의 날짜를 본다.

## 실제 수동 플레이 체크리스트

- 각 체크포인트에서 정지·2발 상태로 다음 구간을 시작하고 통과 여부 확인
- 가시/낙사/보석 동시 접촉 우선순위, 최대 속도 관통, 얇은 지형·이동 발판 접촉 확인
- 갈고리 시야 차단, 발사체/줄과 장벽 충돌, 이동체 탑승·이탈 속도 중복, 기류 경계 확인
- 붕괴/압착/톱/전기 장치의 예고·위상·재시작 복원 확인
- 기본 1600×900, 작은 창, 고해상도에서 게임 픽셀 배율·시야·UI·마우스 조준 확인
- 실패 원인을 화면에서 읽을 수 있는지, 사용자 입력 뒤 빠르게 재시작되는지 확인
- 챕터 간 난도 상승과 새 장치 학습 부담을 참가자 관찰로 기록

테스트 입력 경로는 동일한 고정 루프의 결정론을 확인할 수 있지만 사람의 반응·경로 발견·난도는 평가하지 않는다. 캡처는 화면 상태 증거이지 플레이테스트가 아니다.

## 결과 기록 양식

```text
날짜 / 커밋 또는 작업 트리 상태:
환경(OS, CPU, GPU, RAM, 해상도, 입력 장치):
명령 또는 수동 절차 / 스테이지·체크포인트:
기대 결과 / 실제 결과 / 측정값:
판정(PASS / FAIL / NOT RUN):
로그·캡처 / 결함 / 후속 조치:
```
