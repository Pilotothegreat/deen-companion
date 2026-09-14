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
        assertEquals("seven widgets, seven receivers", 7, WidgetKind.entries.map { it.receiver }.toSet().size)
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
        assertEquals("there are seven widget providers to check", 7, providers.size)
        providers.forEach { file ->
            val xml = file.readText()
            assertTrue("${file.name} declares a minimum resize", "minResizeWidth" in xml && "minResizeHeight" in xml)
            assertTrue("${file.name} stretches for tablets", """maxResizeWidth="640dp"""" in xml)
            assertTrue("${file.name} is resizable both ways", """resizeMode="horizontal|vertical"""" in xml)
            assertTrue("${file.name} can be configured", "android:configure" in xml)
            assertTrue("${file.name} has a picture in the widget picker", "android:previewLayout" in xml)
        }
    }

    @Test fun noPreviewLayoutKeepsItsTextOnlyForTheEditor() {
        // tools:text exists only in Android Studio; on a phone those previews showed empty boxes.
        File("src/main/res/layout").listFiles().orEmpty().filter { "widget" in it.name }.forEach { file ->
            assertTrue("${file.name} has text only the editor sees", "tools:text" !in file.readText())
        }
    }
}
