# 설정 파일

`physics.json`: 프롤로그 물리 초기값. 거리 H, 시간 초, 속도 H/s. `pixelsPerH`는 월드와 논리 렌더 캔버스의 변환값이다. 플레이어 높이 1H와 무관하게 스프라이트는 모자·머플러를 포함한다.

값 변경 시 `core:verifyPrologue`를 실행하고 구간 도달성을 다시 확인한다.

`physics.json`의 `surfaceStepHeight`는 atlas 기반 광산 플랫폼 표면 프로파일의 작은 높낮이를 걸어서 연결하는 최대 지형 단차(H)다. 큰 발판 간 높이 차나 점프·반동 물리는 바꾸지 않는다.
