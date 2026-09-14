# Privacy Policy for Bilal

Last updated: September 12, 2026

Bilal has no accounts, ads, analytics or crash reporting. Everything you set up in the app stays on your device. This page lists exactly what the app stores and every network request it can make.

## What stays on your device

- Your location coordinates and city name, or the city you picked, used to calculate prayer times and the Qibla direction.
- Your settings: calculation method, time adjustments, iqama times, notification and reminder choices, theme, language and text sizes.
- Quran bookmarks, your last-read page, hadith favorites, your tasbih count, and today's athkar progress and streak.

This data is stored with Android's DataStore and a local Room database. It is never uploaded, and it is excluded from Android cloud backups.

## Network requests

The app works offline for prayer times, city search, the Quran text, athkar, the Qibla compass, tasbih and the widgets. It only uses the internet for the features below:

| Feature | Service | When | What the service receives |
| --- | --- | --- | --- |
| City name | Android's system geocoder | After a location is found. Without it, the name comes from the city list bundled with the app | Your coordinates, handled by your device's geocoding provider |
| Weather | api.open-meteo.com | While the app is open, at most once an hour, and never while battery saver is on | **Your approximate location**, rounded to two decimal places — about a kilometre — and your IP address. No account, no key, and nothing identifying you |
| Nearby earthquakes | earthquake.usgs.gov | While the app is open, at most once an hour, and never while battery saver is on | Your IP address. The feed is the same worldwide list for everyone; the filtering by distance happens on your device, so the service is never told where you are |
| Quran recitation | everyayah.com | When you play a recitation | Your IP address and the ayah requested |
| Full hadith collections | cdn.jsdelivr.net | Only when you tap Download | Your IP address |
| Update check | api.github.com (sideloaded installs) or Google Play (Play Store installs) | At most once a day, or when you tap the version in Settings | Your IP address |

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

The app collects no personal data from anyone, including children.

## Changes

Changes to this policy are published in the [GitHub repository](https://github.com/Pilotothegreat/deen-companion) with a new date above.

## Contact

Questions or concerns: open an issue at [github.com/Pilotothegreat/deen-companion/issues](https://github.com/Pilotothegreat/deen-companion/issues).
