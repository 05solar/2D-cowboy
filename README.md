# HOOK & RECOIL

갈고리와 총의 반동으로 이동하는 Java/libGDX 2D 액션 게임 프로토타입입니다. 기존 `core`·`lwjgl3` 구조를 유지합니다.

## 실행

프로젝트 루트 PowerShell에서:

```powershell
.\gradlew.bat :lwjgl3:run
```

기본 데스크톱 창은 1600×900입니다. 게임 시야는 16:9를 유지하고 창이 커질수록 더 넓은 월드가 보입니다. JAR 빌드는 `.gradlew.bat :lwjgl3:jar`입니다.

## 기본 조작

| 입력 | 동작 |
|---|---|
| A/D 또는 좌우 방향키 | 이동 |
| Space | 점프 |
| 마우스 | 조준 |
| 왼쪽 클릭 | 반동 사격 |
| 오른쪽 버튼 유지/해제 | 갈고리 연결/해제 |
| R | 체크포인트에서 재시작 |
| M | 스테이지 선택 |
| Esc | 일시정지 |

## 현재 범위

프롤로그와 본편 동굴 1~5, 광산 6~10, 지하도시 11~15의 실행 데이터가 있습니다. 본편 16~20은 카탈로그 이름만 있고 실행 맵은 없습니다. 보석은 바닥에서 획득하며, 결과 제목은 `CLEAR`입니다. 사망 화면은 사용자가 재시작할 때까지 유지됩니다. 자동 검증과 수동 전체 플레이는 구분합니다.

## 문서

- [개발 문서 색인](docs/README.md)
- [현재 상태·결정](docs/09_DECISIONS_AND_STATUS.md)
- [새 세션 인수인계](docs/PROJECT_HANDOFF.md)
- [작업 규칙](AGENTS.md)
- [원본 게임 기획](docs/specs/HOOK_RECOIL_Game_Spec_v1.md)
