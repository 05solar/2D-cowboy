# 콘텐츠·에셋 현황

기준일: 2026-11-05. 이 문서는 현재 카탈로그·맵·에셋 연결 상태를 요약한다. 개별 설계 문서의 경로·난도 수치는 실제 플레이 검증 전까지 목표값이다.

## 본편 목록과 가용성

프롤로그는 별도 ID다. 본편 고정 ID는 `assets/cave_chapter/stages.json`에서 관리한다. 본편 1~15는 테스트용으로 사용할 수 있고, 16~20은 실행 맵이 없어 `available=false`다. `available`은 구현 및 실행 데이터 가용성이지 진행 해금이나 완주 검증 표식이 아니다.

| 범위 | 실제 데이터·에셋 | 상태 |
|---|---|---|
| 프롤로그 | `assets/stages/prologue.json`, `assets/tutorial_v3/`, `assets/gem_pickup/` | 플레이 가능. [프롤로그](10_PROLOGUE.md), [보석 클리어](11_GEM_CLEAR.md) |
| 동굴 1~5 | `assets/stages/cave_01..05.json`, cave chapter atlas/backgrounds | 런타임 통합. [동굴 상태](12_CAVE_CHAPTER.md) |
| 광산 6~10 | `assets/stages/mine_01..05.json`, `assets/mine_chapter/` | 런타임 통합, 생성기 사용. 자동 경로·수동 검증 제한은 [광산 상태](MINE_CHAPTER_STATUS.md) |
| 지하도시 11~15 | `assets/stages/city_01..05.json`, city V2 atlas와 가로/세로 배경 | 런타임 통합, 전체 실제 경로 미검증. [도시 V2](CITY_CHAPTER_V2.md) |
| 지상 16~20 | 카탈로그 항목만 있음 | 맵·아트가 없어 시작 불가 |

광산 설계 생성기는 `tools/BuildMineStages.py`, 동굴·도시 생성기는 `tools/BuildCaveMaps.py` 및 `tools/BuildCityStages.py`다. 스크립트가 관리하는 JSON을 직접 수정할 때 생성 결과가 다시 덮어쓰는지 확인한다.

## 실제 적용 정책

- 시트 이미지는 atlas에 기록된 실측 rect로 추출한다. 배경·장식 PNG 알파로 콜라이더를 자동 생성하지 않는다.
- 배경은 반복하지 않는 원경이다. 상호작용 지형은 스테이지 데이터에 따로 둔다.
- 현재 목표 보석은 스테이지별 `clear_gem`이며, 선택 수집품은 별도 ID로 관리한다. 획득은 지상 접촉이어야 한다.
- 지하도시 V2의 플랫폼 경로 길이 100/125/150/180/210H 및 module/action 수치는 설계 메타데이터다. `routeLengthMeasured=false`이며 실측·난도 검증이 아니다.
- 광산 6~10의 최근 생성 데이터는 위험 오브젝트 수가 1/3/6/8/10으로 증가하며 세 종류 플랫폼 실루엣을 사용한다. 수량만으로 가독성이나 난도가 검증되진 않는다.

## 원본·계획 자료

`docs/specs/HOOK_RECOIL_Game_Spec_v1.md`와 사용자 제공 에셋의 README/APPLY/atlas 문서는 원본 참고 자료이므로 임의로 현재 상태에 맞춰 고쳐 쓰지 않는다. 적용 결과와 차이는 상태 문서 및 [결정·진행 기록](09_DECISIONS_AND_STATUS.md)에 기록한다. 계획 단계 스테이지 이름·장치 조합이 데이터와 다르면 실제 JSON과 챕터 상태가 우선이다.
