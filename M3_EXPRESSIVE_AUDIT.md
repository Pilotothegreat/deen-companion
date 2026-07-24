# Material 3 Expressive Audit Checklist (`M3_EXPRESSIVE_AUDIT.md`)

> **Project:** Deen Companion (Android / Jetpack Compose)  
> **Target Package:** `app/src/main/java/com/pilotothegreat/deencompanion`  
> **Audit Date:** July 24, 2026  
> **Specification Reference:** [M3 Expressive Redesign Skill](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/.agents/skills/m3_expressive_redesign/SKILL.md)

---

## 🎯 Executive Summary

This audit evaluates all **14 Composable Kotlin files** across `app/src/main/java/...` against the **Material 3 Expressive Design System** guidelines. 

### Key Audit Findings
1. **Missing `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`**:
   - **4 Files** lack explicit file/function-level `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` annotations: `QuranReader.kt`, `Settings.kt`, `MainActivity.kt`, and `App.kt`.
2. **Standard Non-Expressive Components**:
   - **Standard `Button` / `IconButton` / `TextButton`**: Found in **9 files** (e.g., `Overview.kt` with 11 button usages, `Settings.kt` with 10, `NavigationManager.kt`, `QuranReader.kt`).
   - **Standard `Card`**: Found in **6 files** (e.g., `Overview.kt` with 10 card instances, `Qibla.kt` with 6, `QuranReader.kt`, `Settings.kt`, `Hadith.kt`, `Quran.kt`).
   - **Standard Progress Indicators**: `CircularProgressIndicator` in `Qibla.kt:L376` (needs swap to `LoadingIndicator` / `ContainedLoadingIndicator`).
3. **Hardcoded `RoundedCornerShape`**:
   - Found in **8 files** (e.g., `Overview.kt`, `Qibla.kt`, `QuranReader.kt`, `Settings.kt`, `QuranAudioPlayer.kt`, `NavigationManager.kt`, `Theme.kt`, `Util.kt`).
   - Static radii (`4.dp`, `12.dp`, `16.dp`, `24.dp`, `32.dp`) should be replaced by M3 shape tokens or shape morphing (`MorphPolygonShape` / `androidx.graphics.shapes`).

---

## 📋 Mandatory M3 Expressive Replacement Mapping

| Non-Expressive / Legacy Element | Mandatory M3 Expressive Replacement | Expressive Rule / Reference |
| :--- | :--- | :--- |
| Standard `Button` / `Row` of Buttons | `ButtonGroup` or `SplitButton` with `ButtonGroupDefaults` | [SKILL.md:L211](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/.agents/skills/m3_expressive_redesign/SKILL.md#L211) |
| Standard `Card` / `OutlinedCard` | Expressive Container Shapes (`ExpressiveCardShape`, `MorphPolygonShape`) | [SKILL.md:L212](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/.agents/skills/m3_expressive_redesign/SKILL.md#L212) |
| Standard `BottomAppBar` / `NavigationBar` | `FloatingToolbar` or `DockedToolbar` | [SKILL.md:L212](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/.agents/skills/m3_expressive_redesign/SKILL.md#L212) |
| `CircularProgressIndicator` / `LinearProgressIndicator` | `LoadingIndicator` or `ContainedLoadingIndicator` | [SKILL.md:L213](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/.agents/skills/m3_expressive_redesign/SKILL.md#L213) |
| Standard `FloatingActionButton` | Expressive FAB / FAB Menu variants | [SKILL.md:L214](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/.agents/skills/m3_expressive_redesign/SKILL.md#L214) |
| Static `RoundedCornerShape(dp)` | Dynamic M3 Shape Tokens or `androidx.graphics.shapes` Morphing | [SKILL.md:L215](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/.agents/skills/m3_expressive_redesign/SKILL.md#L215) |

---

## 🔍 Detailed Composable File Audit Checklist

---

### 1. [`QuranReader.kt`](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/quran/QuranReader.kt)
- [ ] **OptIn Annotation**: ❌ **MISSING** `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`
- [ ] **Standard `Button` Elements**:
  - [ ] [Line 868](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/quran/QuranReader.kt#L868): Standard `TextButton` for toggling translation.
  - [ ] [Line 996](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/quran/QuranReader.kt#L996): Standard `IconButton` for top bar back navigation.
  - [ ] [Line 1029](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/quran/QuranReader.kt#L1029): Standard `IconButton` for sleep menu.
- [ ] **Standard `Card` Elements**:
  - [ ] [Line 813](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/quran/QuranReader.kt#L813): Standard `Card` used for verse container.
- [ ] **Hardcoded Shapes**:
  - [ ] [Line 914](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/quran/QuranReader.kt#L914): Hardcoded `RoundedCornerShape(4.dp)` background.
  - [ ] [Line 915](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/quran/QuranReader.kt#L915): Hardcoded `RoundedCornerShape(4.dp)` border.
- 💡 **Action Items**: Add `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`, replace standard `Card` with Expressive container surfaces, and swap `IconButton`s for Expressive button styles.

---

### 2. [`Settings.kt`](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/settings/Settings.kt)
- [ ] **OptIn Annotation**: ❌ **MISSING** `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`
- [ ] **Standard `Button` Elements**:
  - [ ] [Line 243](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/settings/Settings.kt#L243): Standard `Button` for location updates.
  - [ ] [Line 789](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/settings/Settings.kt#L789): Standard `Button` for resetting settings.
  - [ ] [Line 800](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/settings/Settings.kt#L800): Standard `Button` action.
  - [ ] [Line 827](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/settings/Settings.kt#L827): Standard `Button` action.
  - [ ] [Lines 865, 878, 913, 925, 958, 970](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/settings/Settings.kt#L865-L970): Multiple standard `IconButton`s for settings controls.
- [ ] **Standard `Card` Elements**:
  - [ ] [Line 121](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/settings/Settings.kt#L121): Standard `Card` for settings section.
  - [ ] [Line 661](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/settings/Settings.kt#L661): Standard `Card` for info dialog/section.
- [ ] **Hardcoded Shapes**:
  - [ ] [Line 262](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/settings/Settings.kt#L262): Hardcoded `RoundedCornerShape(16.dp)`.
- 💡 **Action Items**: Add `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`, convert settings button groups into `ButtonGroup` or `SplitButton`, and replace static cards with expressive shape containers.

---

### 3. [`MainActivity.kt`](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/MainActivity.kt)
- [ ] **OptIn Annotation**: ❌ **MISSING** `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`
- [ ] **Composable Entry Point**: Contains `@Composable` root `setContent` block.
- 💡 **Action Items**: Add `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` to the root activity entry point and ensure `MotionScheme.expressive()` is passed to the main theme wrapper.

---

### 4. [`App.kt`](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/app/App.kt)
- [ ] **OptIn Annotation**: ❌ **MISSING** `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`
- [ ] **Root App Layout**: Contains `@Composable fun App()` wrapper for navigation.
- 💡 **Action Items**: Annotate with `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` for top-level navigation and scaffold consistency.

---

### 5. [`Overview.kt`](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/overview/Overview.kt)
- [x] **OptIn Annotation**: ✅ Present (`@OptIn(ExperimentalMaterial3ExpressiveApi::class)`)
- [ ] **Standard `Button` Elements**:
  - [ ] [Line 528](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/overview/Overview.kt#L528): Standard `Button` for update prompt.
  - [ ] [Line 548](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/overview/Overview.kt#L548): Standard `TextButton` for dismiss action.
  - [ ] [Line 615](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/overview/Overview.kt#L615): Standard `IconButton`.
  - [ ] [Line 702](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/overview/Overview.kt#L702): Standard `IconButton`.
  - [ ] [Line 818](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/overview/Overview.kt#L818): Standard `IconButton`.
  - [ ] [Lines 931, 974, 1034](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/overview/Overview.kt#L931): Standard `Button` usages in prayer/inspiration dialogs.
  - [ ] [Lines 990, 1048](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/overview/Overview.kt#L990): Standard `TextButton` usages.
  - [ ] [Line 1988](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/overview/Overview.kt#L1988): Standard `FilledTonalIconButton` in Tasbih card.
- [ ] **Standard `Card` Elements**:
  - [ ] [Lines 578, 662, 1277, 1624, 1869](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/overview/Overview.kt#L578): 5 Direct standard `Card` calls.
  - [ ] [Lines 1330, 1337, 1408, 1812](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/overview/Overview.kt#L1330): `LiveQiblaCompassCard` and `TasbihDialCard` wrapping standard `Card` surfaces.
- [ ] **Hardcoded Shapes**:
  - [ ] [Line 950](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/overview/Overview.kt#L950): `RoundedCornerShape(16.dp)`.
  - [ ] [Line 1884](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/overview/Overview.kt#L1884): `.clip(RoundedCornerShape(16.dp))`.
  - [ ] [Line 2050](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/overview/Overview.kt#L2050): `RoundedCornerShape(12.dp)`.
- 💡 **Action Items**: Group action buttons into `ButtonGroup` or `SplitButton`, update `TasbihDialCard` to use shape morphing (`MorphPolygonShape`), and replace hardcoded corner shapes with M3 shape tokens.

---

### 6. [`Qibla.kt`](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/qibla/Qibla.kt)
- [x] **OptIn Annotation**: ✅ Present (`@OptIn(ExperimentalMaterial3ExpressiveApi::class)`)
- [ ] **Standard `Button` Elements**:
  - [ ] [Line 288](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/qibla/Qibla.kt#L288): Standard `IconButton` for back button.
  - [ ] [Line 349](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/qibla/Qibla.kt#L349): Standard `IconButton` for calibration.
- [ ] **Standard `Card` Elements**:
  - [ ] [Lines 313, 398, 433, 473, 767](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/qibla/Qibla.kt#L313): 5 Standard `Card` components for Qibla compass & stats headers.
- [ ] **Standard Progress Indicator**:
  - [ ] [Line 376](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/qibla/Qibla.kt#L376): Standard `CircularProgressIndicator` during location loading.
- [ ] **Hardcoded Shapes**:
  - [ ] [Line 739](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/qibla/Qibla.kt#L739): `RoundedCornerShape(16.dp)`.
  - [ ] [Line 770](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/qibla/Qibla.kt#L770): `RoundedCornerShape(24.dp)`.
- 💡 **Action Items**: Swap `CircularProgressIndicator` to `LoadingIndicator` / `ContainedLoadingIndicator`, and replace static cards with expressive morphing shapes.

---

### 7. [`NavigationManager.kt`](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/navigation/NavigationManager.kt)
- [x] **OptIn Annotation**: ✅ Present (`@OptIn(ExperimentalMaterial3ExpressiveApi::class)`)
- [ ] **Standard `Button` / Navigation**:
  - [ ] [Line 200](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/navigation/NavigationManager.kt#L200): Standard `Button` inside `NavigationButton`.
- [ ] **Legacy Navigation Container**:
  - [ ] Navigation bar uses a custom pill layout instead of Expressive `FloatingToolbar` / `DockedToolbar`.
- [ ] **Hardcoded Shapes**:
  - [ ] [Line 181](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/navigation/NavigationManager.kt#L181): `val buttonShape = remember(cornerSize) { RoundedCornerShape(cornerSize) }`.
- 💡 **Action Items**: Upgrade the navigation bar to `FloatingToolbar` or `DockedToolbar` with morphing selection indicators.

---

### 8. [`Hadith.kt`](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/hadith/Hadith.kt)
- [x] **OptIn Annotation**: ✅ Present (`@OptIn(ExperimentalMaterial3ExpressiveApi::class)`)
- [ ] **Standard `Button` Elements**:
  - [ ] [Line 90](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/hadith/Hadith.kt#L90): Standard `IconButton`.
- [ ] **Standard `Card` Elements**:
  - [ ] [Line 388](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/hadith/Hadith.kt#L388): Standard `Card` for Hadith book collection item.
  - [ ] [Line 503](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/hadith/Hadith.kt#L503): Standard `Card` for Hadith detail view.
- 💡 **Action Items**: Replace `Card` with `ExpressiveCardShape` / tonal container surfaces.

---

### 9. [`Quran.kt`](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/quran/Quran.kt)
- [x] **OptIn Annotation**: ✅ Present (`@OptIn(ExperimentalMaterial3ExpressiveApi::class)`)
- [ ] **Standard `Card` Elements**:
  - [ ] [Line 197](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/quran/Quran.kt#L197): Standard `Card` for Surah list items.
- 💡 **Action Items**: Upgrade Surah list item card to use expressive container styling and press physics.

---

### 10. [`QuranAudioPlayer.kt`](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/quran/QuranAudioPlayer.kt)
- [x] **OptIn Annotation**: ✅ Present (`@OptIn(ExperimentalMaterial3ExpressiveApi::class)`)
- [ ] **Standard `Button` Elements**:
  - [ ] [Line 119](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/quran/QuranAudioPlayer.kt#L119): Standard `FilledIconButton` for play/pause.
  - [ ] [Line 216](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/quran/QuranAudioPlayer.kt#L216): Standard `IconButton`.
- [ ] **Hardcoded Shapes**:
  - [ ] [Line 197](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/quran/QuranAudioPlayer.kt#L197): `RoundedCornerShape(12.dp)`.
- 💡 **Action Items**: Replace play/pause button with shape-morphing `MorphPolygonShape` container (e.g. circle to cookie shape morphing on play/pause).

---

### 11. [`SettingsWidgets.kt`](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/settings/SettingsWidgets.kt)
- [x] **OptIn Annotation**: ✅ Present (`@OptIn(ExperimentalMaterial3ExpressiveApi::class)`)
- [ ] **Standard `Button` Elements**:
  - [ ] [Line 488](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/settings/SettingsWidgets.kt#L488): Standard `FilledIconButton`.
  - [ ] [Line 514](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/settings/SettingsWidgets.kt#L514): Standard `FilledIconButton`.
- [ ] **Standard Card Wrappers**: `PermissionCard` wraps non-expressive container.
- 💡 **Action Items**: Convert permission action buttons to Expressive action buttons.

---

### 12. [`Theme.kt`](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/theme/Theme.kt)
- [x] **OptIn Annotation**: ✅ Present (`@OptIn(ExperimentalMaterial3ExpressiveApi::class)`)
- [ ] **Hardcoded `RoundedCornerShape` Definitions**:
  - [ ] [Lines 150-154](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/theme/Theme.kt#L150-L154): Static `RoundedCornerShape(4.dp)`, `8.dp`, `16.dp`, `24.dp`, `32.dp`.
  - [ ] [Lines 160-171](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/theme/Theme.kt#L160-L171): Hardcoded `ExtraSmall`, `Small`, `Medium`, `Large`, `ExtraLarge`, `CardHero`, `Container`.
  - [ ] [Lines 185-186](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/theme/Theme.kt#L185-L186): `ExpressiveCardShape = RoundedCornerShape(26.dp)`, `ExpressiveContainerShape = RoundedCornerShape(24.dp)`.
- 💡 **Action Items**: Integrate M3 Expressive dynamic shape tokens and centralize polygon morphing definitions.

---

### 13. [`Util.kt`](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/util/Util.kt)
- [x] **OptIn Annotation**: ✅ Present (`@OptIn(ExperimentalMaterial3ExpressiveApi::class)`)
- [ ] **Standard `Button` Elements**:
  - [ ] [Line 141](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/util/Util.kt#L141): Standard `IconButton`.
  - [ ] [Line 205](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/util/Util.kt#L205): Standard `IconButton`.
- [ ] **Hardcoded Shapes**:
  - [ ] [Lines 344, 348](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/util/Util.kt#L344): `inline val shelfShape: RoundedCornerShape`.
- 💡 **Action Items**: Replace static `MiniCard` and `shelfShape` with expressive container tokens.

---

### 14. [`HadithBookReader.kt`](file:///c:/Users/king/.gemini/antigravity-ide/scratch/deen-companion/app/src/main/java/com/pilotothegreat/deencompanion/ui/hadith/HadithBookReader.kt)
- [x] **OptIn Annotation**: ✅ Present (`@OptIn(ExperimentalMaterial3ExpressiveApi::class)`)
- [ ] **Expressive Audit**: Uses raw structural containers instead of expressive surface hierarchy.
- 💡 **Action Items**: Upgrade content surface wrappers to tonal container levels (`surfaceContainerLow`, `surfaceContainerHigh`).

---

## 🛠️ Summary Checklist for Expressive Migration

- [ ] **Phase 1: Opt-In Annotations**
  - Add `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` to `QuranReader.kt`, `Settings.kt`, `MainActivity.kt`, and `App.kt`.
- [ ] **Phase 2: Component Migration**
  - Replace all standard `Button` / `TextButton` instances with `ButtonGroup` or `SplitButton`.
  - Replace standard `Card` calls with Expressive Container shapes.
  - Swap `CircularProgressIndicator` in `Qibla.kt` with `LoadingIndicator` or `ContainedLoadingIndicator`.
  - Refactor Navigation Bar in `NavigationManager.kt` to `FloatingToolbar` or `DockedToolbar`.
- [ ] **Phase 3: Shape & Motion Modernization**
  - Replace static `RoundedCornerShape(dp)` instances with dynamic M3 shape tokens or `MorphPolygonShape`.
  - Ensure all state transitions (press, select, expand) trigger spring-based expressive motion physics.

---
