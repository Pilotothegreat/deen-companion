# Bilal · بلال

A private, offline-first Islamic companion for Android, built with Jetpack Compose and Material 3 Expressive. Available in English and Arabic.

## Features

- **Prayer times.** Oman Ministry of Endowments (the default, checked against the official mara.gov.om timetable), Umm al-Qura, Muslim World League, ISNA, Egypt, Karachi, Ja'fari and Tehran methods, with Standard or Hanafi Asr and high-latitude handling.
- **Adhan and iqama notifications.** Per-prayer mute, iqama as an offset or a fixed time, exact alarms when allowed, and automatic rescheduling after reboots, clock changes and time-zone changes.
- **Quran.** The 604-page Madinah mushaf layout in the Uthmanic Hafs script with the Saheeh International translation, diacritic-insensitive search, bookmarks, "continue reading", and ayah-by-ayah recitation (Mishary Alafasy, Al-Husary, Abdul Basit) with a sleep timer.
- **Hadith.** Six major collections with a bundled sample. Full collections download only when you ask. Includes favorites, grades and search.
- **Qibla.** True-north compass with calibration guidance, turn-by-turn directions and the distance to Makkah.
- **Tasbih.** The post-prayer 33/33/34 cycle or 99/100 goals, with haptic feedback.
- **Widgets.** Next prayer with a live countdown, tasbih counter, and a daily verse or hadith.
- **Personalization.** Dynamic color, light/dark/system theme, pure black, and a per-app language with full right-to-left layout.

## Privacy

No accounts, ads, analytics or crash reporting. Location, settings, bookmarks and favorites stay on the device. [PRIVACY.md](PRIVACY.md) lists every network request the app can make.

## Building

Requirements: JDK 17 or newer (21 recommended) and the Android SDK with platform 37.

```bash
./gradlew assembleDebug                 # build a debug APK
./gradlew testDebugUnitTest lintDebug   # unit tests and lint
```

Release builds read signing details from an untracked `keystore.properties` in the project root:

```properties
storeFile=release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

The same values can come from the `RELEASE_KEYSTORE_FILE`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD` environment variables (CI uses repository secrets). Without them, release builds are signed with the debug key.

## Project structure

| Package | Contents |
| --- | --- |
| `core` | Pure Kotlin, unit-tested logic: prayer times, iqama and next-prayer rules, Qibla math, Hijri calendar, Arabic text matching, tasbih |
| `data` | DataStore settings, Room database and migrations, Quran, hadith, location and update repositories |
| `alarms` | Adhan/iqama scheduling, notifications and system-event receivers |
| `playback` | Media3 recitation service and player |
| `widget` | Home-screen widgets |
| `ui` | Compose screens, navigation and the Material 3 Expressive theme |

## Data sources

- Quran text and translation: bundled in `app/src/main/assets/quran.json`.
- Recitation: streamed from [everyayah.com](https://everyayah.com).
- Hadith collections: [fawazahmed0/hadith-api](https://github.com/fawazahmed0/hadith-api) via jsDelivr.
- Fonts: KFGQPC Uthmanic Hafs, Amiri and Google Sans Flex.

## License

GPL-3.0. Bilal (formerly Deen Companion) started from the open-source [Traffic Light](https://github.com/leekleak/traffic-light) app by leekleak; see [ATTRIBUTION.md](ATTRIBUTION.md).
