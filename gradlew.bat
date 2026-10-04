@echo off
rem Минимальный Gradle wrapper launcher.
rem Полная каноничная версия будет сгенерирована задачей `gradle wrapper`
rem при первом CI-прогоне (см. .github/workflows).

set DIRNAME=%~dp0
set CLASSPATH=%DIRNAME%gradle\wrapper\gradle-wrapper.jar

if not exist "%CLASSPATH%" (
    echo Ошибка: не найден gradle\wrapper\gradle-wrapper.jar.
    echo Запустите 'gradle wrapper' или откройте проект после первого CI-прогона.
    exit /b 1
)

java %JAVA_OPTS% %GRADLE_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
