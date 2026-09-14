package com.pilotothegreat.deencompanion.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.text.DailyVerse
import com.pilotothegreat.deencompanion.core.text.Inspirations
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.isArabic
import com.pilotothegreat.deencompanion.ui.navigation.DeepLinks
import java.time.LocalDate

/** A daily text: Arabic first, the translation below it when there's room, and where it comes from. */
internal data class QuoteState(
    val dynamic: Boolean,
    val label: String,
    val arabic: String,
    val translation: String?,
    val source: String,
    val open: Intent,
)

/** Today's ayah; tapping it opens the mushaf at the ayah. */
class VerseWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(WidgetKind.VERSE.previewSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent(loadVerse(context, configOf(context, id)))

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent(loadVerse(context, WidgetConfig()))
}

internal suspend fun loadVerse(context: Context, config: WidgetConfig): @Composable () -> Unit {
    val settings = WidgetDeps.settings.current()
    val res = AppLanguage.localizedContext(context, settings.appLanguage)
    val locale = AppLanguage.locale(settings.appLanguage)
    val ref = DailyVerse.forDate(LocalDate.now(settings.zone))
    val quran = WidgetDeps.quran.quran()
    val verse = requireNotNull(quran.verse(ref.surah, ref.ayah)) { "Daily verse $ref is missing" }
    // The same "Al-Baqarah 2:152" reference as the app, from the widget's localized resources.
    val surah = quran.surah(ref.surah)
    val surahName = if (locale.isArabic) res.getString(R.string.surah_title, surah.nameArabic) else surah.nameEnglish
    val state = QuoteState(
        dynamic = settings.dynamicColor,
        label = res.getString(R.string.verse_of_the_day),
        arabic = verse.text,
        translation = verse.standaloneTranslation.takeUnless { locale.isArabic },
        source = "$surahName ${Formatters.number(surah.number, locale)}:${Formatters.number(ref.ayah, locale)}",
        open = DeepLinks.reader(context, quran.pageOf(ref.surah, ref.ayah), ref.surah, ref.ayah),
    )
    return { BilalWidgetTheme(config.dynamicColor ?: state.dynamic) { QuoteContent(state, config) } }
}

/** Today's hadith or dua. */
class InspirationWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(WidgetKind.INSPIRATION.previewSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent(loadInspiration(context, configOf(context, id)))

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent(loadInspiration(context, WidgetConfig()))
}

internal suspend fun loadInspiration(context: Context, config: WidgetConfig): @Composable () -> Unit {
    val settings = WidgetDeps.settings.current()
    val res = AppLanguage.localizedContext(context, settings.appLanguage)
    val locale = AppLanguage.locale(settings.appLanguage)
    val inspiration = Inspirations.forDate(LocalDate.now(settings.zone))
    val state = QuoteState(
        dynamic = settings.dynamicColor,
        label = res.getString(R.string.daily_inspiration),
        arabic = inspiration.arabic,
        translation = inspiration.english.takeUnless { locale.isArabic },
        source = inspiration.source(locale),
        open = WidgetUpdater.openApp(context),
    )
    return { BilalWidgetTheme(config.dynamicColor ?: state.dynamic) { QuoteContent(state, config) } }
}

@Composable
internal fun QuoteContent(state: QuoteState, config: WidgetConfig = WidgetConfig()) {
    val colors = GlanceTheme.colors
    val budget = rememberBudget()
    val size = LocalSize.current
    val largest = if (size.height >= 170.dp) 19f else 17f
    val fontScale = LocalContext.current.resources.configuration.fontScale
    val width = (size.width - WidgetPadding * 2).value
    // Letters and spaces; the harakat above and below take no width of their own.
    val letters = state.arabic.count { it.isLetter() || it.isWhitespace() }
    // Whether the Arabic fits whole in [room] at [sp]: an estimate from its letters, since a widget cannot measure text.
    val holds = { sp: Float, room: Dp, maxLines: Int ->
        (room / budget.line(sp)).toInt().coerceAtMost(maxLines) * width / (ARABIC_EM * sp * fontScale) >= letters
    }
    val sizes = (largest.toInt() downTo SMALLEST_ARABIC_SP).map { it.toFloat() }
    // The text is the widget, and a verse cut off mid-word is the worst thing it can show. Its source goes
    // beneath unless leaving it out is what lets the text be read whole; the label only if the text keeps its size.
    val showSource = sizes.any { holds(it, budget.left - budget.line(SOURCE_SP), 6) } || sizes.none { holds(it, budget.left, 6) }
    if (showSource) budget.spend(budget.line(SOURCE_SP))
    val labelRoom = budget.line(LABEL_SP) + GAP
    val best = sizes.firstOrNull { holds(it, budget.left, 6) }
    val showLabel = (best == null || holds(best, budget.left - labelRoom, 6)) && budget.take(labelRoom)
    val withTranslation = state.translation != null &&
        budget.left >= budget.line(largest) * 2 + budget.line(TRANSLATION_SP) * 2 + GAP
    val arabicRoom = if (withTranslation) budget.left - budget.line(TRANSLATION_SP) * 2 - GAP else budget.left
    val maxArabicLines = if (withTranslation) 4 else 6
    // The largest size that holds it whole; a text too long for any keeps the usual size and ends in an ellipsis.
    val arabicSp = sizes.firstOrNull { holds(it, arabicRoom, maxArabicLines) } ?: largest
    val arabicLines = (arabicRoom / budget.line(arabicSp)).toInt().coerceIn(if (withTranslation) 2 else 1, maxArabicLines)
    budget.spend(budget.line(arabicSp) * arabicLines)
    val translationLines = if (withTranslation) ((budget.left - GAP) / budget.line(TRANSLATION_SP)).toInt().coerceIn(1, 4) else 0
    WidgetSurface(colors.widgetBackground, GlanceModifier.clickable(actionStartActivity(state.open)), transparency = config.transparency) {
        Column(GlanceModifier.fillMaxSize()) {
            if (showLabel) {
                Text(state.label, style = textStyle(colors.primary, LABEL_SP.sp, FontWeight.Medium), maxLines = 1)
                Spacer(GlanceModifier.height(GAP))
            }
            // Arabic always reads from the right, whatever the launcher's direction.
            Text(
                state.arabic,
                GlanceModifier.fillMaxWidth(),
                textStyle(colors.onSurface, arabicSp.sp, align = TextAlign.Right),
                maxLines = arabicLines,
            )
            if (translationLines > 0 && state.translation != null) {
                Spacer(GlanceModifier.height(GAP))
                Text(state.translation, GlanceModifier.fillMaxWidth(), textStyle(colors.onSurfaceVariant, TRANSLATION_SP.sp), maxLines = translationLines)
            }
            if (showSource) {
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    state.source,
                    GlanceModifier.fillMaxWidth(),
                    textStyle(colors.onSurfaceVariant, SOURCE_SP.sp, align = TextAlign.End),
                    maxLines = 1,
                )
            }
        }
    }
}

private const val LABEL_SP = 12f
private const val TRANSLATION_SP = 13f
private const val SOURCE_SP = 11f

/** The Arabic's average width per letter or space, in ems; generous, so a text estimated to fit does. */
private const val ARABIC_EM = 0.5f

/** The smallest the Arabic is set to be read whole; below it the harakat crowd the letters. */
private const val SMALLEST_ARABIC_SP = 12
private val GAP = 6.dp
