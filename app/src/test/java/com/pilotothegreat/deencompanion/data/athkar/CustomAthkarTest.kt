package com.pilotothegreat.deencompanion.data.athkar

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pilotothegreat.deencompanion.core.athkar.AthkarCategory
import com.pilotothegreat.deencompanion.core.athkar.AthkarIds
import com.pilotothegreat.deencompanion.core.athkar.AthkarItem
import com.pilotothegreat.deencompanion.ui.athkar.AthkarDraft
import com.pilotothegreat.deencompanion.ui.athkar.DraftDhikr
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

/** The reader's own lists: kept, found, changed and let go of like the bundled ones. */
class CustomAthkarTest {

    @get:Rule val folder = TemporaryFolder()

    private val bundled = File("src/main/assets/athkar.json").readText()
    private val today = LocalDate.of(2026, 9, 27)

    private fun store(): DataStore<Preferences> = PreferenceDataStoreFactory.create { File(folder.root, "t.preferences_pb") }

    private fun list(id: String = AthkarIds.newCustom(), title: String = "بعد الجمعة", vararg counts: Int = intArrayOf(100, 7)) =
        AthkarCategory(
            id = id,
            titleEnglish = title,
            titleArabic = title,
            items = counts.mapIndexed { i, count ->
                AthkarItem("item-$i", "أستغفر الله $i", "", "", count, noteEnglish = "note $i", noteArabic = "note $i")
            },
        )

    @Test fun aSavedListIsPartOfTheLibrary() = runTest {
        val repo = AthkarRepository({ bundled }, store())
        val mine = list()
        repo.saveCustom(mine)

        val library = repo.libraryFlow.first()
        assertEquals(listOf(mine), library.custom)
        assertEquals(mine, library.category(mine.id))
        assertTrue("search looks through all, so it finds it too", mine in library.all)
        assertEquals("the bundled lists are all still there", repo.library().all.size + 1, library.all.size)
    }

    @Test fun itComesBackAsItWent() {
        val mine = list()
        assertEquals(listOf(mine), AthkarRepository.decodeCustom(AthkarRepository.encodeCustom(listOf(mine))))
    }

    @Test fun savingAgainReplacesTheListInItsPlace() = runTest {
        val repo = AthkarRepository({ bundled }, store())
        val first = list(title = "one")
        val second = list(title = "two")
        repo.saveCustom(first)
        repo.saveCustom(second)
        val renamed = first.copy(titleEnglish = "renamed", titleArabic = "renamed")
        repo.saveCustom(renamed)
        assertEquals(listOf(renamed, second), repo.custom.first())
    }

    @Test fun deletingAListTakesTodaysCountWithItAndNothingElse() = runTest {
        val repo = AthkarRepository({ bundled }, store())
        val gone = list()
        val kept = list()
        repo.saveCustom(gone)
        repo.saveCustom(kept)
        repo.increment(gone, gone.items[0], today)
        repo.increment(kept, kept.items[0], today)

        repo.deleteCustom(gone.id)

        assertEquals(listOf(kept), repo.custom.first())
        val progress = repo.progress.first()
        assertEquals(0, progress.count(gone.id, "item-0"))
        assertEquals(1, progress.count(kept.id, "item-0"))
    }

    @Test fun unreadableDataIsNoListsAndABadListDoesNotTakeTheOthersWithIt() = runTest {
        assertEquals(emptyList<AthkarCategory>(), AthkarRepository.decodeCustom(null))
        assertEquals(emptyList<AthkarCategory>(), AthkarRepository.decodeCustom("not json"))
        val good = list()
        val mixed = AthkarRepository.encodeCustom(listOf(good)).removeSuffix("]") + """,{"id":"custom-x"},{"id":"morning","title":"t","items":[]}]"""
        assertEquals(listOf(good), AthkarRepository.decodeCustom(mixed))

        val store = store()
        store.edit { it[stringPreferencesKey("athkar_custom")] = "{" }
        assertEquals(emptyList<AthkarCategory>(), AthkarRepository({ bundled }, store).custom.first())
    }

    @Test fun aDraftNeedsANameAndSomethingInIt() {
        val blank = AthkarDraft.blank()
        assertTrue(AthkarIds.isCustom(blank.id))
        assertFalse(blank.canSave)
        assertFalse(blank.copy(title = "t").canSave)
        assertFalse(blank.copy(items = listOf(DraftDhikr(text = "سبحان الله"))).canSave)
        assertTrue(blank.copy(title = "t", items = listOf(DraftDhikr(text = "سبحان الله"))).canSave)
    }

    @Test fun savingADraftDropsEmptyRowsAndKeepsCountsInRange() {
        val draft = AthkarDraft(
            id = AthkarIds.newCustom(),
            title = "  t  ",
            items = listOf(DraftDhikr(id = "a", text = " سبحان الله ", count = 5000), DraftDhikr(id = "b", text = "  ")),
            isNew = true,
        )
        val saved = draft.toCategory()
        assertEquals("t", saved.titleArabic)
        assertEquals(listOf("a"), saved.items.map { it.id })
        assertEquals("سبحان الله", saved.items.single().arabic)
        assertEquals(AthkarRepository.MAX_COUNT, saved.items.single().count)
    }

    @Test fun editingAListKeepsItsIdsSoTodaysCountStaysWithEachDhikr() {
        val mine = list()
        val draft = AthkarDraft.of(mine)
        assertFalse(draft.isNew)
        assertEquals(mine, draft.toCategory())
    }
}
