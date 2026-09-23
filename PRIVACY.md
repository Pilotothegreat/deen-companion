# Privacy Policy for Bilal

Last updated: September 19, 2026

Bilal has no accounts, ads or crash reporting, and no analytics unless you turn them on. Everything you set up in the app stays on your device. This page lists exactly what the app stores and every network request it can make.

## What stays on your device

- Your location coordinates and city name, or the city you picked, used to calculate prayer times and the Qibla direction.
- Your settings: calculation method, time adjustments, iqama times, notification and reminder choices, theme, language and text size.
- Quran bookmarks, your last-read page, hadith favorites, your tasbih count, and today's athkar progress and streak.

This data is stored with Android's DataStore and a local Room database. It is never uploaded, and it is excluded from Android cloud backups.

## Counting what is used

Settings has a switch called **Count what I use**. It is off in a new install, and while it is off the app records nothing at all.

Turned on, the app keeps a counter per day for each of a fixed list of things — the screens you open, a recitation played, a search made, an athkar session finished, a widget configured. It is a list of names and numbers, and that is the whole of it:

- **No text.** Not what you search for, not an ayah you read, bookmarked or shared, not a note.
- **No location**, not even a country beyond the language your phone is set to.
- **No identifier.** No account, no device id, no advertising id, nothing random kept to recognise this phone again. Two reports from one phone cannot be told apart from two reports from two phones.
- **No times.** A counter says "seven today", never when.

The app asks once, on first run, alongside its permissions, and the answer starts as no. **Settings → General → Share usage data** turns it off again at any time, and turning it off deletes what was counted. Counters older than 90 days are deleted anyway.

Whether the totals are sent anywhere at all depends on the build. The open-source build is compiled with no collector address, so nothing is uploaded however the switch is set. A build that was given one sends the day's totals, described above, once a day over Wi-Fi and never on a low battery.

## Network requests

The app works offline for prayer times, city search, the Quran text, athkar, the Qibla compass, tasbih and the widgets. It only uses the internet for the features below:

| Feature | Service | When | What the service receives |
| --- | --- | --- | --- |
| City name | Android's system geocoder | After a location is found. Without it, the name comes from the city list bundled with the app | Your coordinates, handled by your device's geocoding provider |
| Weather | api.open-meteo.com | While the app is open, at most once an hour, and never while battery saver is on | **Your approximate location**, rounded to two decimal places — about a kilometre — and your IP address. No account, no key, and nothing identifying you |
| Nearby earthquakes | earthquake.usgs.gov | While the app is open, at most once an hour, and never while battery saver is on | Your IP address. The feed is the same worldwide list for everyone; the filtering by distance happens on your device, so the service is never told where you are |
| Quran recitation | everyayah.com | When you play a recitation | Your IP address and the ayah requested |
| Full hadith collections | cdn.jsdelivr.net | Only when you tap Download | Your IP address |
| Update check | api.github.com (sideloaded installs) or Google Play (Play Store installs) | Up to four times a day while the app is open, once a day in the background, or when you tap the version in Settings | Your IP address |
| Usage totals | Only a build configured with a collector, and only with **Share usage data** turned on | Once a day, on Wi-Fi | The counters and build described above, and your IP address |

Weather is the only feature that sends anything about where you are. The coordinates are blunted to about a kilometre before they leave the device, which is enough to know whether it is raining over you and not enough to place a house. Under battery saver the request is not made at all, and the rest of the app carries on without it.

**1.9.0 removed the internet location fallback.** It was the one feature that sent your IP address to a third party — ipapi.co and freeipapi.com — for an approximate city that the device's own location and the offline city list bundled with the app already provide. It is gone, along with those two hosts, rather than hidden behind a switch that nobody reads.

**1.9.0 also added a weekly automatic backup.** It is written to the app's own private files directory, keeps the last three copies, and never leaves the device. Nothing about it is uploaded anywhere; it exists so a reinstall does not lose a khatma.

Eclipses need no request at all: the table is bundled with the app and works offline. Travel is worked out entirely on the device: your home point and your current position never leave it, and the app asks before it treats you as travelling.

No personal information beyond what any web request carries (such as your IP address) is sent with these requests.

## Permissions

| Permission | Why |
| --- | --- |
| Location (approximate and precise) | Calculate prayer times and the Qibla for where you are. Requested only when you choose to use your location, and never used in the background. |
| Notifications | Alert you at adhan and iqama times. |
| Alarms and reminders (`SCHEDULE_EXACT_ALARM`) | Deliver notifications on time. You grant it in system settings; without it, alerts may be a few minutes late. |
| Run at startup (`RECEIVE_BOOT_COMPLETED`) | Reschedule prayer alerts after the device restarts. |
| Internet | The features in the table above. |
| Install packages (`REQUEST_INSTALL_PACKAGES`) | Only in the version downloaded from GitHub, so it can install its own update after checking it against the published checksum. The Google Play version does not have this permission at all. |
| Do Not Disturb access | Only if you turn on "silence during prayer". The app asks at that moment and never otherwise. |
| Foreground media playback | Keep recitation playing with media controls when the app is in the background. |

## Banking apps

The optional "Support development" sheet can open Omani banking apps (Bank Muscat, bm Wallet, NBO, Bank Dhofar, Sohar International, Oman Arab Bank, Ahli Bank) if they are installed. The app only checks whether they are installed so it can show a button. It never sees any financial information.

## Children

The app collects no personal data from anyone, including children. The usage counters described above contain nothing personal and are off unless turned on.

## Changes

Changes to this policy are published in the [GitHub repository](https://github.com/Pilotothegreat/deen-companion) with a new date above.

## Contact

Questions or concerns: open an issue at [github.com/Pilotothegreat/deen-companion/issues](https://github.com/Pilotothegreat/deen-companion/issues).
