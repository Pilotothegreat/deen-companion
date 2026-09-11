# Privacy Policy for Bilal

Last updated: September 11, 2026

Bilal has no accounts, ads, analytics or crash reporting. Everything you set up in the app stays on your device. This page lists exactly what the app stores and every network request it can make.

## What stays on your device

- Your location coordinates and city name, used to calculate prayer times and the Qibla direction.
- Your settings: calculation method, iqama times, notification choices, theme, language and Quran text size.
- Quran bookmarks, your last-read page, hadith favorites and your tasbih count.

This data is stored with Android's DataStore and a local Room database. It is never uploaded, and it is excluded from Android cloud backups.

## Network requests

The app works offline for prayer times, the Quran text, the Qibla compass and tasbih. It only uses the internet for the features below:

| Feature | Service | When | What the service receives |
| --- | --- | --- | --- |
| Internet location fallback | ipapi.co, then freeipapi.com | Only when the device can't provide a location and "Internet location fallback" is on (you can turn it off in Settings) | Your IP address |
| City name | Android's system geocoder | After a location is found | Your coordinates, handled by your device's geocoding provider |
| Quran recitation | everyayah.com | When you play a recitation | Your IP address and the ayah requested |
| Full hadith collections | cdn.jsdelivr.net | Only when you tap Download | Your IP address |
| Update check | api.github.com (sideloaded installs) or Google Play (Play Store installs) | At most once a day, or when you tap the version in Settings | Your IP address |

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
