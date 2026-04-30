# Devin Browser

Современный мобильный браузер для Android: WebView + Jetpack Compose, минимум зависимостей, всё нативное.

## Фичи

- **Вкладки** с обычным и инкогнито‑режимом, переключатель‑грид, «Закрыть все»
- **Выбор поисковика** (по умолчанию Google) — Google · DuckDuckGo · Bing · Yandex · Brave Search · Ecosia
- **Адресная строка** с автоопределением URL vs. поискового запроса
- **История** просмотров (Room) с поиском и удалением записей
- **Закладки** (Room) — добавление/удаление одним тапом из тулбара
- **Менеджер паролей** — `EncryptedSharedPreferences` (AES‑256‑GCM, ключ в Android Keystore)
- **Загрузки** через системный `DownloadManager`
- **Тёмная тема** (system / light / dark) + опциональное алгоритмическое затемнение сайтов
- **Поиск на странице** (`findAllAsync` / next / prev)
- **Конфиденциальность** — заголовок `DNT: 1`, блокировка попапов, отключение JS
- **Material You** — динамические цвета на Android 12+
- **Открытие http/https и SEND** из системы — браузер выступает как viewer для URL
- **Edge‑to‑edge** UI, поддержка жестовой навигации

## Стек

- Kotlin 1.9, AGP 8.2, Gradle 8.7, JDK 17
- Jetpack Compose (BOM 2024.02), Material 3
- AndroidX WebKit, Room 2.6, DataStore, Security‑Crypto 1.1
- `minSdk` 24, `targetSdk` 34, `compileSdk` 34

## Сборка

```bash
# debug APK
./gradlew :app:assembleDebug

# release APK (подписан стандартным debug‑ключом, чтобы был установим без перенастройки)
./gradlew :app:assembleRelease
```

APK будут в `app/build/outputs/apk/{debug,release}/`.

## Установка

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

или просто откиньте APK на устройство и разрешите установку из неизвестных источников.
