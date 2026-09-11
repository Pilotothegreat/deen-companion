package com.pilotothegreat.deencompanion.ui.navigation

import android.content.Context
import android.content.Intent
import androidx.navigation3.runtime.NavKey
import com.pilotothegreat.deencompanion.MainActivity

/** Intents that open a screen from a notification or widget, and parsing them back into nav keys. */
object DeepLinks {
    private const val EXTRA_DESTINATION = "com.pilotothegreat.deencompanion.destination"
    private const val ATHKAR = "athkar/"
    private const val READER = "reader/"

    fun athkar(context: Context, categoryId: String): Intent = open(context, ATHKAR + categoryId)

    fun reader(context: Context, page: Int, surah: Int = 0, ayah: Int = 0): Intent =
        open(context, "$READER$page/$surah/$ayah")

    fun parse(intent: Intent?): NavKey? {
        val value = intent?.getStringExtra(EXTRA_DESTINATION) ?: return null
        return when {
            value.startsWith(ATHKAR) -> AthkarSessionKey(value.removePrefix(ATHKAR))
            value.startsWith(READER) -> value.removePrefix(READER).split('/').mapNotNull(String::toIntOrNull)
                .takeIf { it.size == 3 && it[0] in 1..604 }
                ?.let { (page, surah, ayah) -> ReaderKey(page, surah, ayah) }
            else -> null
        }
    }

    /** Each destination gets its own action so PendingIntents for different screens stay distinct. */
    private fun open(context: Context, destination: String): Intent =
        Intent(context, MainActivity::class.java)
            .setAction("$EXTRA_DESTINATION.$destination")
            .putExtra(EXTRA_DESTINATION, destination)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
}
