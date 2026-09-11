# Bilal: Play Store listing

Everything Google Play asks for, in one place. The listing text itself lives in `fastlane/metadata/android/` (`en-US` and `ar`), so it can be pasted into the Play Console or uploaded with `fastlane supply`.

## App details

| Field | Value |
| --- | --- |
| Package | `com.pilotothegreat.deencompanion` |
| Version | 1.7.0 (`versionCode` 195) |
| Category | Books & Reference |
| Tags | Prayer times, Quran, Islam |
| Price | Free, no ads, no in-app purchases |
| Privacy policy | https://pilotothegreat.github.io/deen-companion/privacy.html |
| Website | https://github.com/Pilotothegreat/deen-companion |

## Listing text

| Field | Limit | English | Arabic |
| --- | --- | --- | --- |
| App name | 30 | Bilal: Prayer Times & Athkar | بلال: مواقيت الصلاة والأذكار |
| Short description | 80 | `en-US/short_description.txt` | `ar/short_description.txt` |
| Full description | 4000 | `en-US/full_description.txt` | `ar/full_description.txt` |
| Release notes | 500 | `en-US/changelogs/195.txt` | `ar/changelogs/195.txt` |

## Graphics

| Asset | Spec | File |
| --- | --- | --- |
| App icon | 512 x 512 PNG | `fastlane/metadata/android/en-US/images/icon.png` |
| Feature graphic | 1024 x 500 PNG | `…/en-US/images/featureGraphic.png`, `…/ar/images/featureGraphic.png` |
| Phone screenshots | 1080 x 1920 PNG, 9:16 | `…/en-US/images/phoneScreenshots/`, `…/ar/images/phoneScreenshots/` |

Sources are in `branding/` (the icon and mark) and `branding/store/` (the feature graphics). The raw screenshots come from `StoreScreenshotTest`, and `scripts/store/frame_screenshots.py` frames them with captions.

## Play Console answers

**App access:** everything works without an account or login.

**Ads:** the app contains no ads.

**Content rating (IARC questionnaire):** reference or educational app with no violence, sexual content, profanity, drugs, gambling or user-generated content. Expected rating: Everyone / PEGI 3.

**Target audience:** 13 and over. Choosing a younger age group enrols the app in the Families programme, which has extra requirements.

**Data safety:**
- *Does your app collect or share any of the required user data types?* **No.**
  - Location, settings, bookmarks and athkar progress are stored and processed only on the device. Nothing is sent to the developer.
  - The optional internet location fallback is off by default. When the user turns it on, ipapi.co or freeipapi.com see the device's IP address, the same as any web request, and send back an approximate city.
- *Is data encrypted in transit?* Yes, every request uses HTTPS.
- *Can users request deletion?* There's no account and nothing held off the device; uninstalling removes everything.

**Permissions that need a declaration:**
- *Foreground service (media playback):* keeps Quran recitation playing with media controls while the app is in the background. Play asks for a short video of starting a recitation and leaving the app.
- *Exact alarms (`SCHEDULE_EXACT_ALARM`):* adhan and iqama notifications must arrive at the prayer time. The user grants it in system settings, and the app still works without it.
- *Location:* used only in the foreground to calculate prayer times and the Qibla. It's never used in the background.

## App signing

Upload `bilal-1.7.0-play.aab`, built with `./gradlew bundlePlay`. It's the release build without the donation sheet, signed with the key in `~/Documents/bilal-signing/`, which becomes the Play **upload key**.

When Play App Signing asks which key to use, choose to **use the same key** by uploading it with Google's PEPK tool. Then the Play build and the APKs on GitHub have the same signature, and people can move between them without reinstalling. If Google generates its own app-signing key instead, the Play and GitHub builds can't update each other.

## Before submitting

- **Support development sheet:** the Play build (`bundlePlay`) leaves it out, along with its banking-app queries, because Google Play's payments policy generally requires Play Billing for payments to the developer. The GitHub APK keeps it.
- **First upload:** Google only accepts a new app's first bundle through the Play Console. Upload it by hand to **Internal testing**; later releases can be uploaded automatically.
- **Contact email:** the Play Console requires a public support email for the listing.
