# Custom Workspace Rules

- **Use Installed Skills**: A library of community skills has been registered at `C:/Users/king/.agents/skills` via `.agents/skills.json`. Always check this library or your available skills list first when tasked with code refactoring, database queries, security testing, cloud setup, or other specialized tasks, and load the relevant `SKILL.md` file using the `view_file` tool to follow its best practices.
- **No Perplexity for Coding or Planning**: Do NOT use Perplexity tools for coding, planning, reasoning, or pre-code workflows. All coding and planning tasks should be performed directly without invoking Perplexity.

# Memoirs & Custom Guidelines (Persisted Notes)

## Build & Release Protocol
1. **Gradle Build APK**: Run `./gradlew assembleDebug` to compile and package the latest version of the app.
2. **GitHub Push**: Commit and push changes to GitHub after completing major iterations (using standard Git commands: `git add`, `git commit`, `git push`).
3. **M3 Design Kits**: Align with Material 3 Expressive guidelines, using the custom shape-morphing wrapper (`MorphPolygonShape`) and centralized shapes/motion tokens.
4. **Direct Execution**: Perform all architectural design, planning, and code changes directly without Perplexity MCP calls.
5. **Command & Rule Logging**: Log all critical executed commands, instructions, and newly discovered project rules/protocols directly in `AGENTS.md` (or relevant workspace memoirs/logs) to maintain complete project history in the workstation.
6. **GitHub Context**: Proactively pull context, check repository status/history (`git log`, `git status`, etc.), and remain fully aware of the repository's state to guide decisions.

## Executed Iteration Logs
- **Comprehensive App Audit & Upgrade (2026-08-27)**:
  - **M3 Expressive Compliance**: Enforced `@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)` across all 14 Kotlin composable files (`QuranReader.kt`, `Settings.kt`, `MainActivity.kt`, `App.kt`). Replaced legacy button usages with `FilledTonalButton` and `ButtonGroup`.
  - **Accessibility (A11y & RTL)**: Added `isRtlLanguage(lang)` supporting `ar`, `fa`, `ur`, `ckb`, `he`, `ps`. Replaced all raw English strings in `contentDescription` with localized string resources in English and Arabic. Added semantic TalkBack descriptions to all +/- stepper buttons in Settings.
  - **Security & Socket Hardening**: Implemented `finally { disconnect() }` socket lifecycle guards in `LocationHelper.kt`, `HadithRepository.kt`, and `OverviewVM.kt`. Replaced hardcoded Windows User-Agents with app identifier `DeenCompanion/1.5.42 (Android)`. Hardened `TasbihWidgetProvider` with `FLAG_IMMUTABLE`.
  - **Logging Modernization**: Replaced unhandled `e.printStackTrace()` with structured `Timber` logging across all background receivers and ViewModels.
- **Dual-Distribution Update Engine & Launch Readiness v1.5.43 (2026-08-27)**:
  - **Dual Update Subsystem**: Implemented installer detection (`isPlayStoreInstalled`) in `Util.kt`, `OverviewVM.kt`, and `SettingsVM.kt`. Supports Google Play In-App Updates and market link for Play Store installs, with automatic fallback to GitHub APK releases for sideloads.
  - **Quran & Audio Hardening**: Standardized `QuranPlaybackService` User-Agent to `DeenCompanion/1.5.43 (Android; Media3)`.
  - **App Store Optimization & Release Notes**: Authored `STORE_LISTING_METADATA.md` with complete ASO descriptions, keywords, feature breakdown, and bilingual release notes. Bumped `versionCode = 193`, `versionName = "1.5.43"`.
- **Comprehensive Production Code Audit & Zero-Debt Hardening (2026-08-27)**:
  - **Zero Unhandled Stack Traces**: Eliminated all remaining `e.printStackTrace()` calls across widgets (`PrayerWidgetProvider`, `InspirationWidgetProvider`), receivers (`IqamaAlarmReceiver`), managers (`IqamaAlarmManager`), helpers (`HadithHelper`), and composables (`Qibla.kt`), converting 100% of catch blocks to structured `Timber` logging.
  - **Arabic & RTL Typography Generalization**: Updated `Theme.kt` font selection to evaluate `isRtlLanguage(appLang)`, automatically routing Arabic, Persian, Urdu, Kurdish, and Pashto through `ArabicTypography` and Google Sans Arabic font glyphs.
  - **Quran Reader Complete Interaction Flow**: Added `onLongPress` support for `FatihaBismillah` block in `QuranReader.kt` with localized clipboard toast notifications.
  - **Audio Reciter Reselection Guard**: Bounded `currentAyah` index to `maxOf(1, ...)` in `QuranPlaybackManager.setReciter`.
  - **User-Agent Sync**: Aligned all network components (`LocationHelper.kt`, `HadithRepository.kt`) to `DeenCompanion/1.5.43 (Android)`.

