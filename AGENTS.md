# Инструкция для следующего агента

## Состояние проекта

TechPulse — Android-приложение на Kotlin/Jetpack Compose. Минимальная версия Android — 8.0 (API 26), JDK — 17, Gradle — 8.9. Основная CI-конфигурация находится в `.github/workflows/android-build.yml`.

В текущую ветку уже перенесены ридер статей, перевод на русский, режим обучения, WebView, два виджета рабочего стола и обновление APK через GitHub Releases. Не удаляйте эти возможности при переносе отдельных коммитов из других веток.

## Обязательная проверка перед передачей работы

1. Убедиться, что рабочее дерево чистое: `git status --short`.
2. Проверить формат патча: `git diff --check`.
3. Собрать обе конфигурации с новым номером:
   `./gradlew assembleDebug assembleRelease -PbuildNumber=<N> --stacktrace`.
   Если локально нет JDK/Android SDK, запустить workflow вручную:
   `gh workflow run android-build.yml --ref <ветка>`.
4. Дождаться завершения CI:
   `gh run watch <run-id> --exit-status`.
5. Проверить PR: `gh pr checks <номер-pr>`.
6. Не добавлять в Git каталоги `.gradle/`, `build/`, `app/build/`, APK и временные логи.

## Как выпускается финальная версия

- PR собирает debug и release APK и сохраняет их как Actions artifact.
- GitHub Release намеренно не создаётся из PR или ручного запуска ветки.
- После merge в `main` push-workflow автоматически:
  1. использует `github.run_number` как `versionCode` и `1.0.<N>` как `versionName`;
  2. собирает debug и release APK;
  3. подписывает release APK ключом `release.keystore`;
  4. создаёт тег и GitHub Release `v1.0.<N>` с обоими APK.
- Не создавайте релиз вручную до успешного merge-build в `main`, иначе возможен конфликт тега.

## Автообновление

`AppUpdateManager` проверяет `zigorminsk-debug/NEWS` через GitHub Releases. Интерфейс находится в `ui/update/UpdateHost.kt`. Проверка выполняется не чаще одного раза в 6 часов; в шапке ленты есть ручная проверка. Android всегда требует пользовательского подтверждения установки APK. Все релизы должны оставаться подписанными тем же ключом и иметь возрастающий `versionCode`.

## Перевод

- Переводчик: `data/translate/Translator.kt`.
- Перевод ленты запускается из `FeedViewModel`.
- Перевод и обучение в статье: `ui/reader/ReaderScreen.kt`.
- Не выполняйте сетевые запросы перевода в главном потоке.
- Google Translate endpoint не имеет официальной гарантии стабильности; ошибки должны оставлять пользователю оригинальный текст, а не ломать экран.

## Очистка

Перед коммитом безопасно проверить мусор командами `git clean -nd` и `git clean -ndX`. Удалять разрешается только проверенные генерируемые файлы. Никогда не удаляйте `.git`, `release.keystore`, `keystore.properties`, `gradle/wrapper/gradle-wrapper.jar` и исходники.
