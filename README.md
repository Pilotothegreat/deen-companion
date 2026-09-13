# Bilal · بلال

A private, offline-first Islamic companion for Android, built with Jetpack Compose and Material 3 Expressive. Available in English and Arabic.

## Features

- **Prayer times.** Calculated with the [Adhan](https://github.com/batoulapps/adhan-kotlin) library. Fourteen methods, including Oman's Ministry of Endowments (checked against the official mara.gov.om timetable), Umm al-Qura, Dubai, Kuwait, Qatar, Singapore, Turkey, Moonsighting Committee, Ja'fari and Tehran. By default the method follows your country. Also offers Standard or Hanafi Asr, per-prayer fine-tuning, a choice of high-latitude rule, and the middle and last third of the night.
- **Location.** GPS or network location with travel detection, or an offline search of about 11,600 cities in English and Arabic. A chosen city keeps its own time zone.
- **One notification per prayer.** The adhan plays as alarm audio through a short foreground service, so it is heard through Do Not Disturb and is never truncated the way a channel sound can be. That one notification then becomes a live countdown and a filling bar to the iqama, and announces the iqama in place — Android draws the countdown itself, so it ticks with no process running. A quieter reminder before the adhan, "Prayed" and snooze actions, per-prayer mute, iqama as an offset or a fixed time, silence during prayer that covers the khutbah on Friday, exact alarms when allowed, and automatic rescheduling after reboots, clock changes and time-zone changes.
- **Make alerts arrive.** A screen that shows every reason an alert might not: notification permission, exact alarms, battery optimisation, a blocked channel, and the per-manufacturer battery managers on Xiaomi, Huawei, Oppo, vivo and Samsung, each with the exact path through that phone's own settings.
- **Reacting to the day.** Jumu'ah and Surah al-Kahf from Thursday sunset, Ramadan's last ten and odd nights, suhoor and iftar, Zakat al-Fitr, the two Eids and the takbir, the ten of Dhul-Hijjah and Arafah, Tasu'a and Ashura, the white days, Monday and Thursday, the six of Shawwal and the new moon — with the Islamic day turning over at Maghrib. Contested dates are deliberately left out, and every card that depends on the moon says so.
- **Reacting to the world.** The dua for rain, thunder, wind and extreme temperatures while they are actually happening (Open-Meteo, on by default, location rounded to about a kilometre, and skipped entirely under battery saver). Travel is suspected from distance and then asked about, never assumed. Eclipses come from a bundled NASA table that works offline, with real local visibility for lunar ones. Nearby earthquakes show a dua and never a notification. And "times of calamity" is a mode you switch on with an expiry, in place of a news feed this app deliberately does not have.
- **Khatma.** A reading plan with a length you choose, following the pages you actually reach, quiet on the days you are ahead.
- **Athkar.** Morning and evening, after prayer, sleep and waking, plus 31 everyday situations. Each dhikr has its count, virtue and source, with a big tap counter, a daily streak, optional morning and evening reminders, and a time-aware suggestion on Today.
- **Quran.** The 604-page Madinah mushaf in the King Fahd Complex's own Uthmanic Hafs text and font, shipped verbatim and fully vowelled, with The Clear Quran translation by Talal Itani. **Set like the print**: every page breaks its fifteen lines where the King Fahd Complex edition breaks them, each line stretched to both margins with kashida (ـ) as the printed page is, and the page fits the screen. Tap an ayah and the recitation bar comes up on it; long-press it for its translation, bookmark, copy, share and repeat. Swipe to turn the page. Page, juz, hizb, rub' al-hizb, manzil and ruku' from Tanzil's data, a jump sheet for any surah, juz or page, diacritic-insensitive search, bookmarks, and ayah-by-ayah recitation (Mishary Alafasy, Al-Husary, Abdul Basit) that caches to disk and plays offline, with repeat, speed and a sleep timer. The screen stays awake while you read, the page follows the recitation for as long as you stay with it, and after Isha it comes down a shade until Fajr.
- **Hadith.** Six major collections with a bundled sample. Full collections download only when you ask. Includes favorites, grades and search.
- **Qibla.** True-north compass with calibration guidance, turn-by-turn directions and the distance to Makkah.
- **Tasbih.** The post-prayer 33/33/34 cycle or 99/100 goals, with haptic feedback.
- **Accessibility.** Simple mode in one switch, text scale on top of the system's, contrast, reduced motion honoured everywhere and updated live, larger touch targets, and spoken Qibla guidance.
- **Backup.** A copy of your settings, bookmarks and khatma written quietly once a week to the app's own storage, with the last three kept, offered first when you restore. Export to a file you keep as well. Nothing is uploaded anywhere; cloud backup stays off.
- **Daily reading.** A verse of the day that opens in the mushaf, and a daily hadith or prophetic dua.
- **Widgets.** Seven of them, each with four or five sizes that say more as you make them bigger — a 2x1 countdown through to the full day with iqama times. They resize on Samsung's grid, take the launcher's own corner radius, and carry a shape that changes with the prayer and a bar that fills as it approaches.
- **Personalization.** Dynamic color, light/dark/system theme, pure black, a floating navigation bar on phones, and a per-app language with full right-to-left layout.

- **Settings you can read, and that work.** Six groups and about thirty rows, each describing what a feature does for you rather than which service its data comes from. The adhan sound is picked in the app, haptics and reduced motion are honoured everywhere, and every icon says its own name on a long press. Provenance lives in [ATTRIBUTION.md](ATTRIBUTION.md) and [PRIVACY.md](PRIVACY.md), where it belongs.

## Privacy

No accounts, ads, analytics or crash reporting. Location, settings, bookmarks and favorites stay on the device. [PRIVACY.md](PRIVACY.md) lists every network request the app can make.

## Building

Requirements: JDK 17 or newer (21 recommended) and the Android SDK with platform 37.

```bash
./gradlew assembleDebug                 # build a debug APK
./gradlew testDebugUnitTest lintDebug   # unit tests and lint
./gradlew assembleRelease               # the release APK published on GitHub
./gradlew bundlePlay                    # the Google Play App Bundle (no donation sheet)
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
| `core` | Pure Kotlin, unit-tested logic: prayer times, iqama and next-prayer rules, athkar progress and streaks, Qibla math, Hijri calendar, Arabic text matching, tasbih |
| `data` | DataStore settings, Room database and migrations, Quran, hadith, athkar, city search, location and update repositories |
| `alarms` | Adhan/iqama scheduling, notifications and system-event receivers |
| `playback` | Media3 recitation service and player |
| `widget` | Home-screen widgets built with Jetpack Glance |
| `ui` | Compose screens, navigation and the Material 3 Expressive theme |

## Data sources

- Quran text and translation: bundled in `app/src/main/assets/quran-ar.json` and `quran-tr-clearquran.json`, rebuilt from source by `scripts/quran/build_quran.py` and checksummed by `QuranTextIntegrityTest`.
- Recitation: streamed from [everyayah.com](https://everyayah.com).
- Hadith collections: [fawazahmed0/hadith-api](https://github.com/fawazahmed0/hadith-api) via jsDelivr.
- Athkar: [Morning & Evening Adhkar DB](https://github.com/Seen-Arabic/Morning-And-Evening-Adhkar-DB) and Hisn al-Muslim via [hisnmuslim.com](https://hisnmuslim.com), bundled in `app/src/main/assets/athkar.json` (rebuilt with `scripts/athkar/build_athkar.py`).
- Cities: [GeoNames](https://www.geonames.org), bundled in `app/src/main/assets/cities.json` (rebuilt with `scripts/cities/build_cities.py`).
- Fonts: KFGQPC Uthmanic Hafs, Amiri and Google Sans Flex.
- Icon: the Kufic بلال mark, with its sources in `branding/`.

## License

GPL-3.0. Bilal (formerly Deen Companion) started from the open-source [Traffic Light](https://github.com/leekleak/traffic-light) app by leekleak; see [ATTRIBUTION.md](ATTRIBUTION.md).
