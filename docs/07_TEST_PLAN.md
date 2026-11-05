# 검증 방법과 현재 결과

기준일: 2026-11-05. 이 문서는 어떤 명령이 무엇을 확인하는지와 한계를 설명한다. 실제 결과는 커밋 시점의 실행 기록 및 챕터 상태 문서에서 확인한다. 검사 이름의 PASS만으로 수동 플레이 가능성이나 출시 준비를 주장하지 않는다.

## 프로젝트 검증 명령

| 명령 | 범위 |
|---|---|
| `:core:verifyPrologue` | 프롤로그 이동·갈고리·반동·보석·복원 관련 상태 검증 |
| `:core:verifyCaves` | 동굴 데이터·진행·충돌 및 자동 경로 검증 |
| `:core:verifyMineSurfaces` | 광산 플랫폼 프로파일, 이동 발판 지지 및 맵 실루엣/위험 수 회귀 |
| `:core:verifyMines` | 광산 장치·맵 및 자동 입력 경로 진단 |
| `:core:verifyCity` | 도시 맵·장치·자동 입력 경로 진단 |
| `:lwjgl3:verifyMinesNative` | 데스크톱 스테이지 선택 및 광산 데이터·배경 로드 |
| `:lwjgl3:verifyCityNative` | 데스크톱 도시 atlas·배경·맵 로드 및 카메라 동작 |

실행 명령 예:

```powershell
.\gradlew.bat :core:verifyPrologue
.\gradlew.bat :core:verifyCaves
.\gradlew.bat :core:verifyMineSurfaces
.\gradlew.bat :core:verifyMines
.\gradlew.bat :core:verifyCity
.\gradlew.bat :lwjgl3:verifyMinesNative
.\gradlew.bat :lwjgl3:verifyCityNative
```

## 알려진 결과와 제한

- 2026-11-05 실행에서 `:core:verifyPrologue`는 134 assertions, `:core:verifyMineSurfaces`는 31 assertions, `:lwjgl3:verifyMinesNative`는 19 checks 통과했다.
- 같은 날 `:core:verifyMines`는 mine_01 자동 경로 2단계에서 `(9.041,4.275)`, `grounded=false`로 실패했다. 경로 드라이버는 갈고리 사용을 재현하지 않으므로 이 검사는 실제 플레이를 대체하지 않는다.
- 문서에 기록된 동굴·도시 검증 숫자는 해당 날짜의 결과다. 현재 코드 변경 후 다시 실행하지 않았다면 최신 통과로 인용하지 않는다.
- 실제 사용자 조작으로 전 스테이지를 클리어하거나 난도 상승을 검증한 결과는 없다.

## 별도 플레이테스트가 필요한 항목

모든 체크포인트에서 정지·2발 출발 통과, 실패 원인 가독성, 장치 위상 초기화 반복, 빠른 이동 중 충돌 관통, 배경/장식 이음새, 30/60/144FPS 입력 일관성, 전체 난도 곡선과 0.7초 이내 재조작을 실제 게임 조작으로 확인한다. 캡처는 시각 스냅샷이며 조작·클리어 검증으로 보고하지 않는다.

## 결과 기록 형식

```text
날짜 / 커밋 / 빌드:
환경(OS, CPU, GPU, RAM, 해상도, 입력 장치):
명령 또는 수동 절차 / 스테이지·체크포인트:
기대 결과 / 실제 결과 / 측정값:
판정(PASS / FAIL / NOT RUN):
로그·증거 / 결함 / 후속 조치:
```
