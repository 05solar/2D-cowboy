# UI 비트맵 글꼴

`prologue.fnt`와 `prologue.png`는 둥근모꼴+ Fixedsys를 사용해 생성한 글꼴표와 픽셀 글리프다. 원본 폰트의 출처와 사용 조건은 [LICENSE_DungGeunMo.md](LICENSE_DungGeunMo.md)에 기록했다.

`tools/BuildPrologueFont.java`가 포함된 `DungGeunMo.ttf`와 실제 게임 소스·스테이지 문자를 읽어 64px BMFont를 생성한다. 문구가 추가되면 저장소 루트에서 `java tools/BuildPrologueFont.java`를 실행해 갱신한다. Java 21 개발 도구이며 게임 소스의 Java 8 호환 수준과 별개다. 글리프는 안티앨리어싱 없이 그리고 텍스처에 Nearest 필터를 사용해 픽셀 모양을 유지한다.
