# Privacy Policy: Bilal: Prayer Times & Athkar

**Developer:** Oman Creative Studio
**Last updated:** October 1, 2026

The same policy is published at https://pilotothegreat.github.io/deen-companion/privacy.html

## Who this policy covers

This privacy policy applies to the Android app **Bilal: Prayer Times & Athkar** ("Bilal", "the app"). It is published on Google Play by the developer **Oman Creative Studio** ("we", "us") under the package name `com.pilotothegreat.bilal`. It also applies to the same app distributed from this GitHub repository under the package name `com.pilotothegreat.deencompanion`.

**In short:** Bilal has no accounts, no ads, and no tracking or advertising SDKs. Oman Creative Studio does not receive, collect, sell or share your personal data. Your location, settings and reading history stay on your device. The app contacts a few public services, only for the features listed below.

## 1. Data that stays on your device

The app stores the following only on your phone, in its private storage. None of it is ever sent to us.

- **Your location** (coordinates and city name). It is used to calculate prayer times and the Qibla direction, and to set a home point so the app can tell when you are travelling.
- **Your settings:** calculation method, iqama times, alert choices, theme, language and text size.
- **Your reading and worship history:** Quran bookmarks, your last-read page and khatma plan, hadith favourites, your tasbih count, athkar progress, and athkar lists you write yourself.
- **Automatic backups** of the above, kept in the app's private storage. Backups you export are saved only where you choose.
- **Usage counts**, only if you turn on "Share usage data". It is off unless you choose it.
  - They are daily counts of which app features were opened, such as "app opened" or "page turned".
  - They never include what you read or type, or where you are.
  - They are kept for 90 days and deleted when you turn the setting off.
  - In the versions currently published on Google Play and GitHub, these counts are not sent anywhere; they stay on your device.

You can delete all of this at any time in three ways: Settings → Reset everything, clearing the app's storage, or uninstalling the app.

## 2. Services the app contacts

These requests go directly from your device to the service named. We do not run a server and receive none of this data. As with any internet request, each service can see your device's IP address.

| Feature | Service | What is sent | When |
| --- | --- | --- | --- |
| Weather cards (rain, heat, wind) | Open-Meteo (api.open-meteo.com) | Your location rounded to about 1 km | While the app is open, at most once an hour, after you have set a location; not while battery saver is on |
| Nearby earthquake card | U.S. Geological Survey (earthquake.usgs.gov) | Nothing about you: the app downloads the public worldwide feed and checks the distance on your device | While the app is open, at most once an hour; not while battery saver is on |
| City name for your location | Your device's built-in Android geocoder | Your coordinates, handled by your phone's geocoding provider | When a new location is found |
| Quran recitation | EveryAyah (everyayah.com) | The recitation file requested | When you play or download a recitation |
| Full hadith collections | jsDelivr CDN (cdn.jsdelivr.net) | The collection requested | Only when you tap Download |
| App updates | Google Play (Play In-App Updates) | Handled by Google Play under Google's privacy policy | When the app checks for an update |

Links you tap in the app, such as the source code or this policy, open in your browser.

## 3. Permissions

| Permission | Why |
| --- | --- |
| Location (approximate and precise) | Prayer times and the Qibla for where you are. Used only while the app is open, never in the background, and only after you allow it. You can choose a city instead. |
| Notifications | Adhan, iqama and athkar reminders. |
| Alarms & reminders (exact alarms) | So prayer alerts arrive on time. You grant it in system settings. |
| Do Not Disturb access (optional) | Only if you turn on "Silence during prayer": quiets the phone from the iqama and restores it afterwards. |
| Run at startup | Reschedules prayer alerts after the phone restarts. |
| Foreground service (media playback) | Keeps Quran recitation playing with media controls when you leave the app. |
| Internet | The services listed in section 2. |

## 4. Sharing, selling and advertising

We do not sell, rent or share personal data, and we do not use your data for advertising. The app contains no ads and no analytics, crash-reporting or social-media SDKs.

## 5. Security

Every network request uses HTTPS. Data on the device is kept in the app's private storage, which other apps cannot read.

## 6. Children

Bilal is intended for users aged 13 and over. We do not knowingly collect personal data from anyone, including children.

## 7. Your choices and rights

We hold no data about you, so there is nothing for us to access, correct or delete on our side. You control everything on your device:
- deny the location permission and choose a city instead;
- turn usage counts off;
- delete all app data as described in section 1.

## 8. Changes to this policy

If this policy changes, the new version will be published at the address above with a new "Last updated" date.

## 9. Contact

The developer is **Oman Creative Studio**, publisher of Bilal: Prayer Times & Athkar on Google Play. For questions about this policy or your privacy, open an issue at https://github.com/Pilotothegreat/deen-companion/issues. The app's source code is public at https://github.com/Pilotothegreat/deen-companion.
