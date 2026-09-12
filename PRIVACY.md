# Privacy Policy for Bilal

Last updated: September 11, 2026

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
| Internet location fallback | ipapi.co, then freeipapi.com | Only when the device can't provide a location and you've turned on "Internet location fallback" in Settings (it's off by default) | Your IP address |
| City name | Android's system geocoder | After a location is found. Without it, the name comes from the city list bundled with the app | Your coordinates, handled by your device's geocoding provider |
| Weather | api.open-meteo.com | While the app is open, at most once an hour, when "Weather" is on in Settings (it is on by default) | **Your approximate location**, rounded to two decimal places — about a kilometre — and your IP address. No account, no key, and nothing identifying you |
| Nearby earthquakes | earthquake.usgs.gov | While the app is open, at most once an hour, when "Eclipses and earthquakes" is on | Your IP address. The feed is the same worldwide list for everyone; the filtering by distance happens on your device, so the service is never told where you are |
| Quran recitation | everyayah.com | When you play a recitation | Your IP address and the ayah requested |
| Full hadith collections | cdn.jsdelivr.net | Only when you tap Download | Your IP address |
| Update check | api.github.com (sideloaded installs) or Google Play (Play Store installs) | At most once a day, or when you tap the version in Settings | Your IP address |

Weather is the only feature that sends anything about where you are, and it is the reason this section changed in 1.8.0. The coordinates are blunted to about a kilometre before they leave the device, which is enough to know whether it is raining over you and not enough to place a house. Turning "Weather" off in Settings stops the request entirely, and the rest of the app carries on working.

Eclipses need no request at all: the table is bundled with the app and works offline. Travel is worked out entirely on the device: your home point and your current position never leave it, and the app asks before it treats you as travelling.

No personal information beyond what any web request carries (such as your IP address) is sent with these requests.

## Permissions

| Permission | Why |
| --- | --- |
| Location (approximate and precise) | Calculate prayer times and the Qibla for where you are. Requested only when you choose to use your location, and never used in the background. |
| Notifications | Alert you at adhan and iqama times. |
| Alarms and reminders (`SCHEDULE_EXACT_ALARM`) | Deliver notifications on time. You grant it in system settings; without it, alerts may be a few minutes late. |
| Run at startup (`RECEIVE_BOOT_COMPLETED`) | Reschedule prayer alerts after the device restarts. |
| Internet | The optional features in the table above. |
| Foreground media playback | Keep recitation playing with media controls when the app is in the background. |

## Banking apps

The optional "Support development" sheet can open Omani banking apps (Bank Muscat, bm Wallet, NBO, Bank Dhofar, Sohar International, Oman Arab Bank, Ahli Bank) if they are installed. The app only checks whether they are installed so it can show a button. It never sees any financial information.

## Children

The app collects no personal data from anyone, including children.

## Changes

Changes to this policy are published in the [GitHub repository](https://github.com/Pilotothegreat/deen-companion) with a new date above.

## Contact

Questions or concerns: open an issue at [github.com/Pilotothegreat/deen-companion/issues](https://github.com/Pilotothegreat/deen-companion/issues).
