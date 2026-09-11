# Contributing to Bilal

Thank you for helping improve Bilal. Bug reports, translations and pull requests are all welcome.

## Before you start

- For anything larger than a small fix, open an issue first so we can agree on the approach.
- Religious content (prayer calculations, Quran text, hadith grading, translations) must come from a cited, reliable source. Mention the source in your pull request.

## Development

1. Install JDK 17+ and the Android SDK (platform 37).
2. Build and test:
   ```bash
   ./gradlew assembleDebug testDebugUnitTest lintDebug
   ```
3. Keep logic in `core/` free of Android dependencies and cover it with unit tests.
4. UI uses Jetpack Compose with Material 3 Expressive components. Take colors, typography and shapes from `MaterialTheme`, not hardcoded values.
5. Every user-visible string belongs in `res/values/strings.xml` with an Arabic translation in `res/values-ar/strings.xml`.

## Pull requests

- Keep each pull request focused on one change and describe what it does and how you tested it.
- Make sure CI (lint, unit tests and both builds) passes.

## License

Bilal is licensed under the GNU General Public License v3.0. By contributing, you agree that your contribution is licensed under the same terms.
