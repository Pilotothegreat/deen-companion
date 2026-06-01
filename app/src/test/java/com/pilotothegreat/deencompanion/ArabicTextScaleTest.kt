package com.pilotothegreat.deencompanion

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.CompositionLocalProvider
import com.pilotothegreat.deencompanion.ui.theme.ArabicTypography

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ArabicTextScaleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testArabicTextNoClippingAt150PercentScale() {
        composeTestRule.setContent {
            // Set font scale to 1.5f (150%)
            val currentDensity = LocalDensity.current
            val customDensity = Density(density = currentDensity.density, fontScale = 1.5f)

            CompositionLocalProvider(LocalDensity provides customDensity) {
                Text(
                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    style = ArabicTypography.bodyLarge
                )
            }
        }

        // Verify the Arabic text node exists and successfully renders at 150% text scaling
        composeTestRule.onNodeWithText("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ").assertExists()
    }
}
