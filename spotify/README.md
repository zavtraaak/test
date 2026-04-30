# NoTube Spotify — apktool-патч официального Spotify APK

Скрипты для сборки модифицированной версии Spotify APK с убранными визуальными
ad-Activity (баннеры, interstitial, CMP-окно), пересобранной через `apktool` и
подписанной кейстором проекта (`notube-release.jks`).

## Что это даёт

- Визуальные ad-Activity (`DisplayAdActivity`, `CMPActivity`, `InAppBrowserActivity`,
  `InAppBrowserLauncherActivity`, `HelpWebViewActivity`) патчатся: их `onCreate`
  заменяется на `super.onCreate(); finish(); return-void` — окно не отображается,
  активити сразу закрывается.
- APK подписан собственным ключом (`notube-release.jks`, alias `notube`,
  пароль `notube123`).

## Что это НЕ даёт

- **Аудио-реклама не блокируется.** Между треками Spotify-сервер отдаёт ad-tracks
  как часть стрима. Чтобы их подавить нужно либо:
  - переопределить ProductState клиента (Spotify ввёл server-side attestation,
    бывшие патчи [DMCA'd сентябрь 2025](https://torrentfreak.com/revanced-complies-with-spotify-takedown-but-explores-options-to-fight-back/)),
  - либо блокировать домены ad-сервиса на уровне DNS/hosts (это уже задача OS,
    не APK).
- **Premium фичи не разблокированы.** Пропуск треков, оффлайн-загрузка,
  HQ-аудио — всё остаётся как на free-аккаунте.
- **Нет гарантий что Spotify не разлогинит.** Spotify умеет проверять подпись
  APK и сверять её с оригинальной (signature attestation). Возможен бесконечный
  цикл "logged out -> can't login". Ставь на одноразовый аккаунт.

## Сборка

### Зависимости

- JDK 17 (`/usr/lib/jvm/java-17-openjdk-amd64`)
- Android SDK build-tools 34+ (для `zipalign` и `apksigner`)
- Python 3.10+
- `apktool.jar` v2.10.0 (скрипт сам скачает)
- Кейстор `notube-release.jks` в корне репо

### Команда

```bash
# 1. Скачиваем оригинальный Spotify v9.1.24.1739 (Aptoide-зеркало, Cloudflare нет)
curl -sLo spotify-orig.apk \
  "https://pool.apk.aptoide.com/sirkesyone-applikes/com-spotify-music-138424205-73673310-5e0f2c75b1dc460c083d4c9cbc1846b0.apk"

# 2. Готовим окружение
export ANDROID_HOME=/path/to/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# 3. Собираем
chmod +x spotify/build.sh
./spotify/build.sh spotify-orig.apk
# → build/spotify-patched.apk
```

## Установка на устройство

1. Перенеси `spotify-patched.apk` на телефон.
2. Удали официальный Spotify, если он установлен (подписи разные, конфликт).
3. Разреши «установка из неизвестных источников» в файловом менеджере.
4. Открой APK → установить.
5. Логинься на одноразовый аккаунт. Если Spotify сразу разлогинит — это
   server-side attestation сработал, патч против неё в этом коммите нет.

## Проверка APK

```bash
$ aapt dump badging build/spotify-patched.apk | head -3
package: name='com.spotify.music' versionCode='138424205' versionName='9.1.24.1739' ...
sdkVersion:'24'
targetSdkVersion:'36'

$ apksigner verify --print-certs build/spotify-patched.apk
Signer #1 certificate DN: CN=NoTube, OU=NoTube, O=NoTube, ...
```

## Файлы

| Файл | Что делает |
|---|---|
| `spotify/patch_smali.py` | Патчит smali ad-Activity (заменяет `onCreate` на пустой `super + finish`) |
| `spotify/build.sh` | Конвейер: apktool d → patch → apktool b → zipalign → apksigner |

## Версия Spotify

`9.1.24.1739` (138424205), март 2026 — Aptoide-зеркало.
Если Spotify обновится и сменит структуру smali, придётся обновить
`TARGET_ACTIVITIES` в `patch_smali.py`.
