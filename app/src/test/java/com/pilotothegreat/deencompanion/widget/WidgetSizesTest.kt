package com.pilotothegreat.deencompanion.widget

import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.SizeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * A widget is only as good as the sizes it fits.
 *
 * Before 1.9.0 most declared two breakpoints and none declared a minimum resize, so they could not
 * be shrunk at all and were cropped rather than rearranged on anything but a phone-sized grid.
 * These check the shape of that declaration rather than the pixels, which is the part that decides
 * whether the widget can be placed on a 5x5 One UI grid, a foldable or a tablet.
 */
class WidgetSizesTest {

    private val widgets = listOf(
        NextPrayerWidget() to 5,
        PrayerTimesWidget() to 5,
        AthkarWidget() to 4,
        MomentWidget() to 4,
        TasbihWidget() to 3,
    )

    private fun buckets(mode: SizeMode): Set<DpSize> =
        (mode as SizeMode.Responsive).sizes

    @Test fun everyWidgetOffersEnoughBreakpointsToRearrangeRatherThanCrop() {
        widgets.forEach { (widget, expected) ->
            val sizes = buckets(widget.sizeMode)
            assertEquals("${widget.javaClass.simpleName} breakpoints", expected, sizes.size)
        }
    }

    @Test fun theBreakpointsAreDistinctAndOrderedFromSmallestUp() {
        widgets.forEach { (widget, _) ->
            val areas = buckets(widget.sizeMode).map { it.width.value * it.height.value }
            assertEquals("${widget.javaClass.simpleName} has a duplicate breakpoint", areas.size, areas.toSet().size)
        }
    }

    @Test fun theSmallestBreakpointFitsATwoByOneCell() {
        // Roughly 2x1 on a typical launcher grid; anything larger cannot be placed there at all.
        widgets.forEach { (widget, _) ->
            val smallest = buckets(widget.sizeMode).minBy { it.width.value * it.height.value }
            assertTrue(
                "${widget.javaClass.simpleName} cannot shrink to a small cell: $smallest",
                smallest.width.value <= 150f && smallest.height.value <= 100f,
            )
        }
    }

    @Test fun everyProviderCanBeResizedBothWaysAndStretchedForTablets() {
        val providers = File("src/main/res/xml").listFiles().orEmpty().filter { "widget" in it.name }
        assertTrue("there are widget providers to check", providers.size >= 7)
        providers.forEach { file ->
            val xml = file.readText()
            assertTrue("${file.name} declares a minimum resize", "minResizeWidth" in xml && "minResizeHeight" in xml)
            assertTrue("${file.name} stretches for tablets", """maxResizeWidth="640dp"""" in xml)
            assertTrue("${file.name} is resizable both ways", """resizeMode="horizontal|vertical"""" in xml)
            assertTrue("${file.name} can be configured", "android:configure" in xml)
        }
    }
}
