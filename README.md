# HOOK & RECOIL (2D-CowBoy)

갈고리와 총의 반동을 조합하는 Java/libGDX 2D 등반 게임 프로젝트입니다. 프롤로그와 본편 1~15(동굴·광산·지하도시)의 실행 데이터를 포함하며, 본편 16~20은 아직 제작되지 않았습니다.

**실행:** `gradlew.bat :lwjgl3:run` (프로젝트 루트에서 실행).

**조작:** A/D 이동, Space 점프, 왼쪽 클릭 반동, 오른쪽 유지 천장 갈고리, R 체크포인트, Esc 일시정지.

[개발 문서·현재 상태 색인](docs/README.md)

보석은 바닥에서만 획득하며 8포즈 연출 뒤 결과 화면으로 전환합니다. 자동 경로·데이터 로드 검증은 수동 전체 플레이와 구분합니다. [보석·완료 저장 안내](docs/11_GEM_CLEAR.md)

- [개발 문서 목차](docs/README.md)
- [게임 원본 명세](docs/specs/HOOK_RECOIL_Game_Spec_v1.md)
- [개발 프로세스](docs/01_DEVELOPMENT_PROCESS.md)
- [기능별 파일 구조](docs/04_PROJECT_STRUCTURE.md)
- [파일 상단 주석·코드 작성 규칙](docs/06_CODING_RULES.md)
- [작업 공통 규칙](AGENTS.md)

아래는 기존 템플릿의 Gradle 참고입니다. 실제 게임 상태와 검증 범위는 `docs/README.md`와 `docs/PROJECT_HANDOFF.md`를 기준으로 합니다.

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

This project was generated with a template including simple application launchers and an `ApplicationAdapter` extension that draws libGDX logo.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
