# 지하도시 챕터 11~15 적용 상태

원본 자산과 설계는 [적용 가이드](assets/city_chapter/README.md), [단계 설계](assets/city_chapter/STAGES_11_15.md), [장치/이동 규칙](assets/city_chapter/DEVICES_AND_MOTION.md)를 따른다. 아트 원본은 `assets/city_chapter/`에 보존하고, 스테이지 초기 배치는 `tools/BuildCityStages.py`로 `assets/stages/city_01.json`~`city_05.json`에 생성한다.

## 연결된 항목

- 실측 `atlas.json` rect로 지하도시 블록·기구·위험 시트를 로드한다. 배경은 비반복 세로 원경으로 ClampToEdge를 사용하며 플레이 화면은 기존 16:9 카메라를 유지한다.
- 지하도시 전용 렌더러가 발판·천장·가시·복합 구조 블록·이동체·기류·장벽·링·체크포인트를 그린다. 구조 그림만으로 충돌을 만들지 않고 `blocks.json`의 정규화된 collider union을 사용한다.
- `CityDeviceRuntime`은 고정 120Hz 이동·위상 시계, waypoint smoothstep 경로, 탑승 이동량과 이탈 속도, 고정 기류, 전기 장벽 off/warning/on 및 리셋을 처리한다.
- city 11~15 ID·이름·선행 관계·배경·목표·난도 메타데이터가 본편 카탈로그에 병합됐다. `artAvailable=true`, `available=false`로 아트와 플레이 검증 상태를 분리했다.
- 사망 0.35초 포즈 애니메이션이 끝난 뒤 자동 부활하지 않고 YOU DIED 패널과 마지막 사망 포즈에서 입력을 기다린다. R로 재시작하며 패널·제목·조작 글자 크기도 키웠다.

## 검증 기록

- `:core:compileJava`, `:lwjgl3:compileJava`: 통과.
- `:core:verifyCity`: 34 assertions 통과. 5개 스테이지 데이터·고정 ID·체크포인트 수·천장 링·최종 발판 지원·복합 L 콜라이더·4점 이동 경로·기류 세기·전기 장벽 상태/차단/접촉·120Hz 경로의 30/60/144 표시률 일관성을 검사했다.
- `:core:verifyCaves`: 79 assertions 통과. 죽음 화면 자동 부활 없음, R 재시작, 동굴 기존 경로/FPS 일관성을 확인했다.
- `:core:verifyPrologue`: 134 assertions 통과.
- `:core:verifyMines`: 실패. 기존 미해결 `mine_01` 자동 경로가 2단계에서 목표에 닿지 못한다. 지하도시 작업으로 해결되지 않았다.

## 아직 하지 않은 검증 및 가용성

도시 11~15는 생성된 프로토타입 배치이며 각 체크포인트에서 실제 통과 궤도·탄약 분배·최대속도 충돌·줄 차단·장치 끼임·보석 완주를 조작으로 검증하지 않았다. 따라서 다섯 카드는 `available=false`로 유지한다. `:core:verifyCity`는 엔진 렌더나 전체 맵 클리어를 대신하지 않는다. 네이티브 창에서 아틀라스/배경/카드 상태를 확인한 후, 체크포인트별 플레이 경로를 만들고 성공·실패 궤도를 확인해야 각 스테이지를 사용 가능으로 바꿀 수 있다.
