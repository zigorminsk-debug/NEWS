#!/bin/sh
#
# Минимальный Gradle wrapper launcher.
# Полная каноничная версия скрипта будет сгенерирована автоматически
# задачей `gradle wrapper` при первом CI-прогоне (см. .github/workflows).
#

APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$CLASSPATH" ]; then
    echo "Ошибка: не найден gradle/wrapper/gradle-wrapper.jar." >&2
    echo "Запустите 'gradle wrapper' или откройте проект после первого CI-прогона." >&2
    exit 1
fi

exec java $JAVA_OPTS $GRADLE_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
