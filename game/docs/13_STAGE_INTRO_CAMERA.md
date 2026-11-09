# 스테이지 진입 카메라와 창 크기

기준일: 2026-11-09. 화면 동작은 `GameplayScreen`, 투어 단계·시간은 `StageIntroCamera`, 배율 설정은 `assets/config/camera.json`이 담당한다.

## 진입 투어

스테이지가 새로 로드되면 카메라는 다음 순서로 움직인다.

1. 주인공 스폰 위치에서 시작한다.
2. 카메라를 축소하고 맵 중앙으로 이동해 전체 스테이지를 보여준다.
3. 클리어 보석 위치를 향해 이동하면서 일반 플레이 배율로 확대한다.
4. 보석을 잠시 보여준 뒤 스폰 위치로 돌아와 조작을 넘긴다.

위치와 줌은 완만하게 보간하며 전체 투어 중 플레이어·장치 물리, 위험 시뮬레이션, 스테이지 시계는 진행하지 않는다. 입력은 소비한다. 일시정지/앱 포커스 동작은 화면 수명주기에서 처리한다. 스크린샷 미리보기는 투어를 생략한다.

## 기본 창과 월드 시야

- 기본 데스크톱 창 크기는 1600×900이다.
- 게임 화면비는 16:9로 유지한다. 월드 viewport는 창 높이의 2배를 논리 높이로, 이에 맞춘 16:9 폭으로 리사이즈한다.
- 창이 커지면 월드에 표시되는 영역이 넓어지지만 월드 단위당 화면 픽셀 비율과 `gameplayZoom=1.2`는 유지한다. 게임 스프라이트가 창 크기 때문에 커지지 않는다.
- 수직 카메라 중심은 `verticalFollowOffset=70`, `lowerEdgeAllowance=120` 논리 픽셀 기준으로 낮게 잡아 플레이어 발판 아래를 더 보여준다. 프롤로그의 고정 카메라 높이는 `prologueCameraY=520`이다. 스폰·보석 투어도 같은 수직 기준을 쓴다.
- 시작 투어의 전체 맵 축소 이동은 1.4초에서 0.85초로 단축했다. 전체 맵 홀드 시간은 유지한다.
- UI는 2560×1440의 별도 논리 viewport다. 조준 입력은 게임 viewport, UI 클릭은 UI 좌표계로 변환한다.
- 창 비율이 16:9와 다르면 viewport가 레터박스를 둘 수 있다. 가용 화면보다 큰 기본 창의 최소 해상도 동작은 별도 확인 대상이다.

## 검증 상태

2026-11-09 `:lwjgl3:verifyCityNative` 29 checks에서 오프닝 카메라의 전체 맵 포함·보석 확대·스폰 복귀·위치/줌 연속성·시뮬레이션 정지·빠른 맵 공개를 확인했다. 검증 런처의 창은 1280×720이며, 기본 1600×900의 UI/조준 정렬은 별도 확인 대상이다. 1600×900 cave_01 미리보기 캡처에서도 하단 framing을 확인했다.

1600×900 기본 창에서 cave_01 미리보기 캡처를 확인해 플레이어의 시작 발판이 하단에서 잘리지 않는 것을 확인했다. 이 확인은 단일 프레임 화면 검사다.

확인 명령:

```powershell
.\gradlew.bat :lwjgl3:verifyCityNative
```

이 검증은 카메라·렌더 상태를 확인하며 전체 맵을 실제로 플레이해 통과하는지 확인하지 않는다.

## 2026-11-09 overview framing correction

The level overview now fits against the unobscured play area (logical view height minus the top and bottom HUD bands), adds a small framing margin, and centers the level slightly into the unobscured region. Native verification asserts that both vertical level bounds remain visible outside the HUD, the camera still approaches the gem and returns to spawn, and transitions remain continuous.
