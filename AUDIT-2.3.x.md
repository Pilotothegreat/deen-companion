# Bilal 2.3.3: post-launch audit

September 2026, on branch `m3-expressive-refresh`.

What was checked:
- Compiler warnings and `lintDebug`.
- The full unit and render suite: 345 tests.
- Every screenshot from the English-light and Arabic-dark walkthroughs.
- A code review of every package. Each reported finding was re-checked against the source before it was listed here.

## Fixed in this branch

| Severity | Where | What went wrong | Fix |
|---|---|---|---|
| Critical | `alarms/PrayerAlarmScheduler.kt` | Any reschedule during a "quiet during prayer" window cancelled the pending restore and never re-armed it. Reschedules come from a tap on "Prayed", an alarm firing, a reboot or the 12-hour worker. The phone stayed in Do Not Disturb until the reader noticed. | The restore is re-armed whenever the window's end is still ahead. When no window covers now (the setting was turned off, the prayer muted, notifications turned off), a silence Bilal started is ended at once. |
| High | `alarms/QuietDuringPrayer.kt` | The restore always set the filter to "all". A reader's own Do Not Disturb (a meeting, sleep) that overlapped the prayer was switched off. | Bilal records that it silenced the phone. It silences only when DND is off, and restores only its own silence, and only if the reader hasn't changed DND since. Covered by `QuietDuringPrayerTest`. |
| High | `data/hadith/HadithRepository.kt` | Cancelling a download and tapping download again quickly: the old job's cleanup removed the new job's entry. That download could then no longer be cancelled, and a further tap started a second, parallel copy. | Each job removes only its own entry: `remove(bookId, job)`. |
| Medium | `data/backup/BackupRepository.kt` | A damaged backup (a malformed bookmark, a bad khatma date) threw halfway through the restore, after the settings had already been replaced. A bad `startedOn` date was stored as-is and then crashed the khatma card every time it was shown. | The whole file is read and validated before anything is written. A damaged file is refused as "unreadable". |
| Visual | Quran, Hadith, reader "jump" tabs | Old underline tab rows. | Connected expressive button group (the existing `ConnectedChoice`), with padding that fits "Bookmarks". |
| Visual | Every segmented list | The rows were drawn in the page colour (`#FFF8F3` on `#FFF8F3`), so the Android 16/17 grouped look never showed. | Rows sit on `surfaceContainer`, via `groupedRowColors()` in `ui/components/Components.kt`. |
| Visual | Settings, About | "Source code" had a bare icon, so its text was indented differently from the other rows. | Badged like the rest. |
| Visual | Arabic Settings, Credits | "The Clear Quran" was split across lines around the Arabic text. | The translation credit gets its own line. |
| Test | `ScreenshotTest` | The shot named Settings actually showed Today: a fixed wait caught the screen transition midway. | Waits for the Settings title. |

## Material 3 Expressive refresh (the Android 17 look)
- Large flexible, collapsing top app bars on Quran, Hadith, Location and Alarm reliability, matching Today, Athkar and Settings.
- Wavy progress ring on the khatma card.
- Expressive selectable menu for the tasbih's dhikr, which marks the current one.
- Moved off every API that material3 `1.5.0-alpha28` deprecated:
  - bottom-sheet state;
  - the `TextFieldState` search field;
  - the V2 window adaptive info;
  - tooltip anchor positioning;
  - swipe-to-dismiss confirmation;
  - the auto-mirrored volume icon;
  - `IntentCompat` for the ringtone picker result.

  The build now has zero compiler warnings.
- Not changed, on purpose: the palette, the prayer shapes, the mushaf page, and every setting. No new modes or options.

## Checked and cleared
- Lint: no errors. The warnings are dependency-version notices and two false positives:
  - `StaticFieldLeak`: the ViewModel holds the Application context.
  - `IconLauncherShape`: with minSdk 26 the adaptive icon is always used.
- The four "missing" Arabic strings are marked `translatable="false"` on purpose (a GitHub URL, "+1", and the language names).
- Also checked and correct:
  - alarm request codes and foreground-service types;
  - exact-alarm fallback;
  - Room migrations 6→9;
  - version comparison in the update checker;
  - HTTP timeouts;
  - Quran reference parsing;
  - compass sensor lifecycle;
  - Navigation3 back stack;
  - the search field's two-way sync.
- The Qibla marker at a ~270° bearing does not cover the west letter. The letters turn with the dial, like a real compass face.

## Open, for you to decide
- **Athkar editor and process death (medium).** An unsaved draft lives only in the ViewModel. If Android kills the app in the background mid-edit, the typing is lost. Fix: save the draft through `SavedStateHandle`.
- **Makkah method in Ramadan.** The +30-minute Isha uses the Umm al-Qura calendar date and ignores the Hijri adjustment setting. That matches how the Umm al-Qura timetable is defined, but it can disagree for one day with the Hijri date the app shows if you have set an adjustment.
- **Reader position (low).** The last page is saved after a 500 ms pause. If the app is killed inside that half-second, the page turn is lost.

## Features that established apps have and Bilal doesn't (not built, by design)
Compared with Quran for Android and the mainstream prayer apps:
- **Tafsir** for an ayah.
- **Word-by-word** meaning.
- **Memorisation aids:** repeat an ayah or range with a count.
- **Wear OS** tile and complication for the next prayer.
- **Hijri calendar screen** listing the occasions Bilal already knows about.

Bilal already has Kahf, suhoor and iftar, the white days, and Zakat al-Fitr, through its occasions.
