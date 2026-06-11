# Privacy Policy for Deen Companion

Last Updated: June 12, 2026

At Deen Companion, we prioritize your privacy above all else. This application is designed to function entirely offline, keeping your personal data safe, secure, and under your control.

## 1. Information We Process and How We Use It

### Location Data (GPS & Network coordinates)
* **Purpose**: Used solely to calculate high-precision offline prayer times and to determine Qibla direction relative to your location.
* **Storage**: Your coordinates are processed on-device and are never transmitted to our servers or third parties.
* **IP-based Geolocation Backup**: If GPS signals are unavailable, the application can request geolocation details from privacy-friendly, standard HTTP/HTTPS geolocation providers (such as `ipapi.co` and `ip-api.com`). These requests only process your IP address to return latitude and longitude coordinates. This data is handled in memory, is not saved, and is processed locally.

### Voice and Microphone Input (`RECORD_AUDIO`)
* **Purpose**: Used to record short voice queries for search inputs in the offline Assistant/Lookup screens.
* **Storage**: Voice data is transcribed locally on your device. Audio files or recordings are never stored, saved, or uploaded to any external servers.

### Settings and Personal Preferences
* **Purpose**: Reminders, settings (dhikr targets, font configurations, calculations preference, and city overrides) are kept to customize your experience.
* **Storage**: Stored locally on your device using Android Jetpack DataStore and Room Database.

## 2. Third-Party Integrations & App Queries

### Local Omani Banking Apps
To allow users in Oman to support developer operations locally, the settings and donation panels provide deep links to launch local banking applications (such as Bank Muscat, bm Wallet, NBO, Bank Dhofar, etc.) on the device.
* **Data Privacy**: Deen Companion queries if these banking applications are installed to display the shortcut buttons. We do not access, collect, or store any financial details, account credentials, card information, or transaction records. All interactions are handled directly by your installed bank's official security systems.

## 3. Third-Party Analytics and Advertising
Deen Companion is an open-source, non-commercial app.
* We do not include any tracking software, analytic trackers (e.g., Firebase Analytics), advertising SDKs (e.g., Google AdMob), or marketing frameworks.
* There are no background analytic processes sending your usage patterns to external entities.

## 4. Updates to This Policy
Since the application operates offline, we do not notify users of privacy updates dynamically. We recommend reviewing the latest policy updates directly in our GitHub repository: [github.com/Pilotothegreat/deen-companion](https://github.com/Pilotothegreat/deen-companion).

## 5. Contact & Support
If you have any questions or feedback, please open an issue in our official repository on GitHub.
