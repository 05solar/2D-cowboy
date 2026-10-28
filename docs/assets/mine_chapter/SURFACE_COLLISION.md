# 광산 플랫폼 외곽 표면 충돌

## 기준

플랫폼 충돌은 atlas 사각형의 위쪽 경계가 아니라 PNG에 그려진 발판의 바깥 금속 표면을 따른다. `assets/mine_chapter/atlas.json`의 `surface_profile_px`는 `platform_long`, `platform_medium`, `platform_small` 각각에 대해 가로 16구간의 표면 높이를 atlas rect 내부 픽셀로 지정한다. 이는 그림 크기로 자동 충돌을 만드는 방식이 아니라 에셋별로 기록한 표면 데이터다.

## 적용

`tools/BuildMineStages.py`는 픽셀 프로파일을 플랫폼의 실제 월드 폭에 맞춰 `surfaceProfile` 월드 오프셋으로 변환한다. `StageDefinition.Block.surfaceY(x)`가 구간 사이를 보간한다. 충돌 착지, 수평 이동 중 작은 표면 단차, 승강기 탑승, 체크포인트, 스폰과 클리어 보석 지점이 이 함수를 사용한다. 렌더된 그림은 변형하지 않는다.

`assets/config/physics.json`의 `surfaceStepHeight`는 표면 프로파일의 작은 오르막을 걸어서 연결하는 최대 단차다. 발판 사이 큰 높이 차를 오르는 능력이나 점프·반동 수치는 바꾸지 않는다.

## 검증

- `:core:verifyMineSurfaces`: atlas 여백은 착지면에서 제외되고, 좌우 프로파일 차이와 실제 표면 보행이 유지되는지 확인한다.
- `:lwjgl3:verifyMinesNative`: 광산 발판 그림과 플레이어가 실행 렌더에서 맞물리는지 화면으로 확인한다.
- `:core:verifyMines`: 전체 광산 입력 경로까지 검사한다. 현재 이 테스트는 mine_01 자동 경로가 보석까지 닿지 않아 실패하며, 표면 충돌 단위 테스트 결과와 구분해서 본다.
