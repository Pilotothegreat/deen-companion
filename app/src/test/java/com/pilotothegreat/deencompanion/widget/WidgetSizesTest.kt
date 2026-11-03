package com.pilotothegreat.deencompanion.widget

import androidx.glance.appwidget.SizeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * A widget is only as good as the sizes it fits.
 *
 * Until 2.1 every widget picked the nearest of a few declared sizes and was drawn for that size. A One UI
 * stack hands a widget less height than any of them, so the layout was cropped from the bottom — that is
 * how Isha went missing. Every widget now lays out for the size it is actually given.
 */
class WidgetSizesTest {

    @Test fun everyWidgetLaysOutForTheSizeItIsGiven() {
        WidgetKind.entries.forEach { kind ->
            assertEquals("${kind.name} lays out for its real size", SizeMode.Exact, kind.widget().sizeMode)
        }
    }

    @Test fun everyDeclaredWidgetIsAKindTheConfigurationScreenKnows() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertEquals("every widget has a receiver of its own", WidgetKind.entries.size, WidgetKind.entries.map { it.receiver }.toSet().size)
        WidgetKind.entries.forEach { kind ->
            assertTrue("${kind.receiver.simpleName} is declared in the manifest", ".widget.${kind.receiver.simpleName}\"" in manifest)
            assertEquals(kind, WidgetKind.ofProvider(kind.receiver.name))
        }
    }

    /** The configuration screen used to offer all four options to every widget, and three ignored them all. */
    @Test fun eachWidgetOffersOnlyTheOptionsItUses() {
        val everyone = setOf(WidgetOption.COLOURS, WidgetOption.TRANSPARENCY)
        WidgetKind.entries.forEach { kind -> assertTrue("${kind.name} honours colours and transparency", kind.options.containsAll(everyone)) }
        assertEquals(setOf(WidgetKind.NEXT_PRAYER, WidgetKind.PRAYER_TIMES), WidgetKind.entries.filter { WidgetOption.IQAMA in it.options }.toSet())
        assertEquals(setOf(WidgetKind.ATHKAR_NOW), WidgetKind.entries.filter { WidgetOption.ATHKAR in it.options }.toSet())
    }

    @Test fun everyProviderCanBeResizedBothWaysStretchedForTabletsAndPreviewed() {
        val providers = File("src/main/res/xml").listFiles().orEmpty().filter { "widget" in it.name }
        assertEquals("every widget has a provider to check", WidgetKind.entries.size, providers.size)
        providers.forEach { file ->
            val xml = file.readText()
            assertTrue("${file.name} declares a minimum resize", "minResizeWidth" in xml && "minResizeHeight" in xml)
            assertTrue("${file.name} stretches for tablets", """maxResizeWidth="640dp"""" in xml)
            assertTrue("${file.name} is resizable both ways", """resizeMode="horizontal|vertical"""" in xml)
            assertTrue("${file.name} can be configured", "android:configure" in xml)
            assertTrue("${file.name} has a picture in the widget picker", "android:previewLayout" in xml)
        }
    }

    /**
     * Every widget shrinks to one row and two columns. By Android's 70n - 30 formula a row is 40dp and two
     * columns 110dp; the old 60dp and 180dp minimums held widgets at two rows and three columns however
     * much empty space there was around them.
     */
    @Test fun everyWidgetShrinksToOneRowAndTwoColumns() {
        File("src/main/res/xml").listFiles().orEmpty().filter { "widget" in it.name }.forEach { file ->
            val xml = file.readText()
            val dp = { name: String -> Regex("""android:$name="(\d+)dp"""").find(xml)!!.groupValues[1].toInt() }
            assertTrue("${file.name} shrinks to one row", dp("minResizeHeight") <= 40)
            assertTrue("${file.name} shrinks to two columns", dp("minResizeWidth") <= 110)
        }
    }

    /**
     * What the picker shows and what the launcher places have to be the same widget.
     *
     * A provider that asks for two cells and declares the width of four gets one size from an Android 12
     * launcher and another from anything older, and the widget that arrives is not the one in the picture.
     */
    @Test fun everyProviderAsksForTheSizeItSaysItWants() {
        File("src/main/res/xml").listFiles().orEmpty().filter { "widget" in it.name }.forEach { file ->
            val xml = file.readText()
            val cells = { name: String -> Regex("""android:$name="(\d+)"""").find(xml)!!.groupValues[1].toInt() }
            val dp = { name: String -> Regex("""android:$name="(\d+)dp"""").find(xml)!!.groupValues[1].toInt() }
            assertEquals("${file.name}'s width matches its cells", 70 * cells("targetCellWidth") - 30, dp("minWidth"))
            assertEquals("${file.name}'s height matches its cells", 70 * cells("targetCellHeight") - 30, dp("minHeight"))
        }
    }

    /** The picker's live preview is drawn at [WidgetKind.previewSize], which is the size it will be placed at. */
    @Test fun thePreviewIsDrawnAtTheSizeTheWidgetIsPlacedAt() {
        WidgetKind.entries.forEach { kind ->
            val xml = File("src/main/res/xml/${providerResource(kind)}.xml").readText()
            val dp = { name: String -> Regex("""android:$name="(\d+)dp"""").find(xml)!!.groupValues[1].toInt() }
            assertEquals("${kind.name} is previewed at the width it is placed at", dp("minWidth"), kind.previewSize.width.value.toInt())
            assertEquals("${kind.name} is previewed at the height it is placed at", dp("minHeight"), kind.previewSize.height.value.toInt())
        }
    }

    /** The appwidget-provider the manifest points this widget's receiver at. */
    private fun providerResource(kind: WidgetKind): String {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val receiver = manifest.substringAfter(".widget.${kind.receiver.simpleName}\"")
        return receiver.substringAfter("@xml/").substringBefore("\"")
    }

    @Test fun noPreviewLayoutKeepsItsTextOnlyForTheEditor() {
        // tools:text exists only in Android Studio; on a phone those previews showed empty boxes.
        File("src/main/res/layout").listFiles().orEmpty().filter { "widget" in it.name }.forEach { file ->
            assertTrue("${file.name} has text only the editor sees", "tools:text" !in file.readText())
        }
    }
}
