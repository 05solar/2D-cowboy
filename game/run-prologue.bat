@echo off
rem 파일: run-prologue.bat
rem 역할: 프로젝트 Gradle wrapper로 프롤로그를 실행한다.
rem 책임: 작업 폴더 설정과 실행 오류 표시. 협력: lwjgl3:run.
rem 관련 사양: docs/10_PROLOGUE.md.
pushd "%~dp0"
call gradlew.bat lwjgl3:run
if errorlevel 1 pause
popd
