# Deen Companion

Deen Companion is a modern, comprehensive, and privacy-first Islamic companion application designed using Material Design 3 and Jetpack Compose. 

It provides daily inspiration, offline Quranic reading, Hadith references, prayer times, Iqama offsets, a Qibla compass, and a Tasbih counter in a beautifully crafted user interface.

## Features

- **Daily Inspiration**: Curated bilingual Quranic verses and Hadiths that automatically adapt to your chosen application language.
- **Offline Quran Reader**:
  - Combined authentic Arabic Hafs text with Saheeh International English translation.
  - Custom traditional *Scheherazade New* font packaging for high legibility.
  - Right-to-Left (RTL) layout support.
  - Interactive font size settings with live preview.
  - Sajdah at-Tilawah (prostration) verse badges.
  - Quick scroll navigation directly to verse search results.
- **Hadith Library**:
  - Selected authentic Hadiths categorized by compilers (Bukhari, Muslim, etc.).
  - Dynamic grade badge classifications (Sahih, Hasan, Da'if, Mawdu') mapped to themed status colors.
  - Expandable card interactions presenting previews first to avoid walls of text.
  - Favorite bookmarks saved locally.
- **Prayer Times & Iqama**:
  - High-precision calculation methods (MWL, Umm al-Qura, Karachi, Jafari, etc.).
  - Offline fallback geolocation (via IP) if GPS is unavailable.
  - Customizable Iqama reminder notification offset alarms.
- **Qibla Direction**: Shows the angle direction directly towards Makkah.
- **Tasbih Counter**: Tap-to-count Dhikr companion with haptic feedback.
- **Bilingual Interface**: Full, dynamic English and Arabic toggling.

## Build Setup

To build and compile the application locally:

1. Clone the repository:
   ```bash
   git clone https://github.com/Pilotothegreat/deen-companion.git
   ```
2. Open in Android Studio or build via command line using Gradle:
   ```bash
   ./gradlew assembleDebug
   ```

## Privacy & Local Integrations

Deen Companion is designed to be fully privacy-respecting and run completely offline:
- **Zero Tracking**: We do not collect or upload telemetry, analytics, or personal profiles.
- **Microphone**: Used exclusively for local offline searches (voice transcription) in the Assistant tab.
- **Omani Banking Shortcuts**: The app manifest declares `<queries>` blocks for Omani banking applications. This enables Omani users who wish to support development to quickly deep-link directly to their local mobile banking apps to complete bank transfers securely. Deen Companion never reads or handles any financial credentials or transactions.

## Attribution & Credits

Deen Companion is an open-source project based on the UI/UX structures and preferences engine of the privacy-centric network usage tracker [Traffic Light](https://github.com/leekleak/traffic-light) by [leekleak](https://github.com/leekleak). For licensing and credits details, please refer to [ATTRIBUTION.md](ATTRIBUTION.md).
