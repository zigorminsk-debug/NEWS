# TechPulse — IT-новости и ресурсы

[![Android Build](https://github.com/zigorminsk-debug/NEWS/actions/workflows/android-build.yml/badge.svg)](https://github.com/zigorminsk-debug/NEWS/actions/workflows/android-build.yml)

Android-приложение в хай-тек стиле, которое следит за IT-новостями: собирает ленту из
десятка русскоязычных и мировых источников, показывает каждую новость с краткой
аннотацией и позволяет одним касанием перейти к первоисточнику. Отдельный раздел —
каталог полезных сайтов и сервисов для IT-специалистов.

## Возможности

- **Лента новостей** — 10 RSS-источников (Хабр, 3DNews, OpenNET, Hacker News, TechCrunch,
  The Verge, Ars Technica, Wired, MIT Technology Review, DEV.to) объединяются в одну
  ленту, сортируются по свежести и дедуплицируются.
- **Краткие аннотации** — HTML из RSS очищается до читаемого текста; карточка содержит
  источник, время публикации, заголовок, обложку (если есть) и 3 строки аннотации.
- **Переход к источнику** — тап по карточке открывает статью во встроенном браузере
  (Chrome Custom Tabs), фолбэк — системный браузер.
- **Каталог IT-ресурсов** — 60+ проверенных сайтов по категориям: новости и медиа,
  обучение, документация, инструменты, ИИ, сообщества, карьера, практика.
- **Поиск и фильтры** — поиск по заголовкам и аннотациям, фильтр ленты по источникам,
  фильтр каталога по категориям.
- **Избранное** — закладки хранятся локально и переживают перезапуск.
- **Оффлайн-режим** — последняя успешно загруженная лента кэшируется на устройстве.
- **Тёмная неоновая тема** — моноширинные акценты, «терминальная» эстетика.

## Сборка

### Автоматически (GitHub Actions)

Каждый пуш в `main` запускает workflow [`.github/workflows/android-build.yml`](.github/workflows/android-build.yml):

1. Собираются debug- и release-APK.
2. Сборка получает **порядковый номер** = номер запуска workflow (`github.run_number`):
   он становится `versionCode`, попадает в `versionName` (`1.0.<N>`), в имя файла
   (`TechPulse-v1.0.<N>-release.apk`) и отображается в приложении (бейдж `BUILD N`).
3. Release-APK **подписывается постоянным ключом** `release.keystore`, хранящимся в
   репозитории (пароли — в `keystore.properties`).
4. Для `main` создаётся GitHub Release с тегом `v1.0.<N>` и обоими APK.

Готовые сборки: [Releases](https://github.com/zigorminsk-debug/NEWS/releases).
Артефакты каждого прогона — на вкладке Actions.

### Локально (Android Studio)

1. Откройте папку проекта в Android Studio (Hedgehog или новее, JDK 17).
2. Дождитесь синхронизации Gradle.
3. `Run ▶` для запуска на устройстве/эмуляторе (Android 8.0+, API 26).

Через консоль: `./gradlew assembleRelease -PbuildNumber=<номер>` (без параметра
номер сборки будет равен 1).

> **Примечание:** `gradle-wrapper.jar` не хранится в репозитории изначально — он
> автоматически генерируется и коммитится ботом при первом CI-прогоне. Если клонировали
> репозиторий до этого, выполните `gradle wrapper --gradle-version 8.9` один раз или
> просто соберите проект в Android Studio через bundled Gradle.

## Подпись

- Ключ: `release.keystore` (PKCS12, RSA 2048, действителен до 2056 года, alias `techpulse`).
- Пароли: `keystore.properties` в корне репозитория.
- Отпечаток сертификата (SHA-256):
  `7E:B9:47:4C:BE:39:FE:3D:88:B7:E3:2D:DC:09:5D:50:B9:02:31:92:A8:D6:B0:CB:FA:0B:8D:36:96:C6:84:BC`

Все сборки подписываются одним ключом, поэтому APK можно обновлять поверх ранее
установленных без удаления.

### Обновление внутри приложения

При каждом запуске TechPulse проверяет последний GitHub Release. Если его `versionCode`
выше установленного, приложение предлагает обновление, загружает release-APK через
системный Download Manager и открывает стандартный установщик Android. При первой
установке обновления Android попросит разрешить установку приложений из TechPulse;
само подтверждение установки APK остаётся обязательным системным шагом.

Хранение ключа и паролей в репозитории — осознанное
решение для персонального проекта; для публичного production перенесите пароли в
GitHub Secrets (`secrets.STORE_PASSWORD` и т.д.) и удалите `keystore.properties` из истории.

## Структура проекта

```
app/src/main/java/com/techpulse/app/
├── MainActivity.kt            # точка входа, edge-to-edge тёмная тема
├── FeedViewModel.kt           # состояние ленты, поиска, фильтров, закладок
├── data/
│   ├── Models.kt              # FeedItem, NewsSource, ItResource
│   ├── Sources.kt             # RSS-источники и каталог IT-ресурсов
│   ├── RssParser.kt           # парсер RSS 2.0 / Atom (XmlPullParser)
│   ├── HtmlUtils.kt           # очистка HTML → текст аннотации
│   ├── NewsRepository.kt      # параллельная загрузка источников + кэш
│   └── BookmarksStore.kt      # локальное хранилище избранного
└── ui/
    ├── AppRoot.kt             # нижняя навигация (Лента / Ресурсы / Избранное)
    ├── Utils.kt               # Chrome Custom Tabs, TimeAgo, форматирование
    ├── theme/                 # неоновая тёмная тема
    ├── components/            # NewsCard, шапка, поиск, состояния
    └── screens/               # FeedScreen, ResourcesScreen, BookmarksScreen
```

## Как добавить источник новостей

Добавьте элемент `NewsSource(...)` в список `Sources.NEWS`
(`app/src/main/java/com/techpulse/app/data/Sources.kt`) — источник сразу появится
в ленте, в фильтрах и начнёт обновляться вместе с остальными.

## Технологии

Kotlin 2.0 · Jetpack Compose (BOM 2024.09.03, Material 3) · MVVM + StateFlow ·
OkHttp · Coil · Chrome Custom Tabs · Min SDK 26 (Android 8.0) · AGP 8.5.2 / Gradle 8.9
