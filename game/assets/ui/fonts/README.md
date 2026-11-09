# UI 비트맵 글꼴

`prologue.fnt`와 `prologue.png`는 둥근모꼴+ Fixedsys를 사용해 생성한 글꼴표와 픽셀 글리프다. 원본 폰트의 출처와 사용 조건은 [LICENSE_DungGeunMo.md](LICENSE_DungGeunMo.md)에 기록했다.

`tools/BuildPrologueFont.py`가 포함된 `DungGeunMo.ttf`와 실제 게임 소스·스테이지 문자를 읽어 64px BMFont를 생성한다. 문구가 추가되면 저장소 루트에서 `py -3 tools/BuildPrologueFont.py`를 실행해 갱신한다(Pillow 필요). 글리프는 안티앨리어싱 없이 그리고 텍스처에 Nearest 필터를 사용해 픽셀 모양을 유지한다.

이전 `tools/BuildPrologueFont.java`(AWT `Font`/`GlyphVector`)는 DungGeunMo.ttf 자체는 정상임에도 Java2D의 한글 레이아웃 경로가 전/체/모/정 등 일부 음절을 진/제/보/성 등 다른 음절의 글리프로 바꿔 그리는 버그가 있어 제거했다. fontTools로 cmap을 확인해 두 음절이 서로 다른 glyph name으로 정확히 분리되어 있음을 확인했고, Pillow(FreeType) 렌더링은 같은 문자들을 전부 올바르게 그려 AWT 레이아웃 엔진 쪽 버그로 특정했다.
