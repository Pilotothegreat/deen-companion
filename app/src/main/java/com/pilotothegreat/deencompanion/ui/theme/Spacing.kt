package com.pilotothegreat.deencompanion.ui.theme

import androidx.compose.ui.unit.dp

/**
 * The app's spacing scale, in one place.
 *
 * Every gap and inset in the app was already on a 4/8/12/16/24 rhythm — 16.dp appeared seventy-two
 * times, 8.dp forty-three — so nothing here changes what anything looks like. What it changes is
 * that the rhythm is now written down: a screen that wants a different gap has to pick one of these
 * or say why, and a future decision to loosen the whole layout is one file rather than two hundred
 * literals.
 */
object Spacing {
    /** Between a glyph and the word beside it. */
    val hair = 4.dp

    /** Between related items in a row or a column. */
    val small = 8.dp

    /** Inside a chip, a list row, a compact card. */
    val medium = 12.dp

    /** The screen's own margin, and the gap between cards. */
    val large = 16.dp

    /** Inside a card that carries a heading and a body. */
    val xlarge = 20.dp

    /** Around a sheet's content, and above a section that starts a new thought. */
    val xxlarge = 24.dp
}
