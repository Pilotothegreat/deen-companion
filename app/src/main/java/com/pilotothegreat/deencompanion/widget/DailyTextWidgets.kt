package com.pilotothegreat.deencompanion.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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

internal val QUOTE_SHORT = DpSize(180.dp, 100.dp)
internal val QUOTE_TALL = DpSize(200.dp, 170.dp)

/** Today's ayah; tapping it opens the mushaf at the ayah. */
class VerseWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(QUOTE_SHORT, QUOTE_TALL))
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(QUOTE_TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) = show(context)

    override suspend fun providePreview(context: Context, widgetCategory: Int) = show(context)

    private suspend fun show(context: Context): Nothing {
        val state = load(context)
        provideContent { BilalWidgetTheme(state.dynamic) { QuoteContent(state) } }
    }

    private suspend fun load(context: Context): QuoteState {
        val settings = WidgetDeps.settings.current()
        val res = AppLanguage.localizedContext(context, settings.appLanguage)
        val locale = AppLanguage.locale(settings.appLanguage)
        val ref = DailyVerse.forDate(LocalDate.now(settings.zone))
        val quran = WidgetDeps.quran.quran()
        val verse = requireNotNull(quran.verse(ref.surah, ref.ayah)) { "Daily verse $ref is missing" }
        // The same "Al-Baqarah 2:152" reference as the Today card, from the widget's localized resources.
        val surah = quran.surah(ref.surah)
        val surahName = if (locale.isArabic) res.getString(R.string.surah_title, surah.nameArabic) else surah.nameEnglish
        return QuoteState(
            dynamic = settings.dynamicColor,
            label = res.getString(R.string.verse_of_the_day),
            arabic = verse.text,
            translation = verse.standaloneTranslation.takeUnless { locale.isArabic },
            source = "$surahName ${Formatters.number(surah.number, locale)}:${Formatters.number(ref.ayah, locale)}",
            open = DeepLinks.reader(context, quran.pageOf(ref.surah, ref.ayah), ref.surah, ref.ayah),
        )
    }
}

/** Today's hadith or dua, the same one as on the Today screen. */
class InspirationWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(QUOTE_SHORT, QUOTE_TALL))
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(QUOTE_TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) = show(context)

    override suspend fun providePreview(context: Context, widgetCategory: Int) = show(context)

    private suspend fun show(context: Context): Nothing {
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
        provideContent { BilalWidgetTheme(state.dynamic) { QuoteContent(state) } }
    }
}

@Composable
internal fun QuoteContent(state: QuoteState) {
    val colors = GlanceTheme.colors
    val tall = LocalSize.current.height >= QUOTE_TALL.height
    WidgetSurface(colors.widgetBackground, GlanceModifier.clickable(actionStartActivity(state.open))) {
        Column(GlanceModifier.fillMaxSize()) {
            Text(state.label, style = textStyle(colors.primary, 12.sp, FontWeight.Medium), maxLines = 1)
            Spacer(GlanceModifier.height(6.dp))
            // Arabic always reads from the right, whatever the launcher's direction.
            Text(
                state.arabic,
                GlanceModifier.fillMaxWidth(),
                textStyle(colors.onSurface, if (tall) 19.sp else 17.sp, align = TextAlign.Right),
                maxLines = if (tall) 4 else 2,
            )
            if (tall && state.translation != null) {
                Spacer(GlanceModifier.height(6.dp))
                Text(state.translation, GlanceModifier.fillMaxWidth(), textStyle(colors.onSurfaceVariant, 13.sp), maxLines = 3)
            }
            Spacer(GlanceModifier.defaultWeight())
            Text(
                state.source,
                GlanceModifier.fillMaxWidth(),
                textStyle(colors.onSurfaceVariant, 11.sp, align = TextAlign.End),
                maxLines = 1,
            )
        }
    }
}
