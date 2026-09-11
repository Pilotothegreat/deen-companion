package com.pilotothegreat.deencompanion.data.tasbih

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pilotothegreat.deencompanion.core.tasbih.Dhikr
import com.pilotothegreat.deencompanion.core.tasbih.TasbihEngine
import com.pilotothegreat.deencompanion.core.tasbih.TasbihState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Tasbih counter shared by the home screen and the widget; each tap is one atomic edit. */
class TasbihRepository(private val dataStore: DataStore<Preferences>) {

    val state: Flow<TasbihState> = dataStore.data.map { it.toTasbih() }.distinctUntilChanged()

    suspend fun increment(): TasbihEngine.Step {
        lateinit var step: TasbihEngine.Step
        dataStore.edit {
            step = TasbihEngine.increment(it.toTasbih())
            it.write(step.state)
        }
        return step
    }

    suspend fun setDhikr(dhikr: Dhikr) {
        dataStore.edit {
            it[DHIKR] = dhikr.arabic
            it[COUNT] = 0
        }
    }

    suspend fun setTarget(target: Int) {
        dataStore.edit {
            it[TARGET] = target
            if ((it[COUNT] ?: 0) >= target) it[COUNT] = 0
        }
    }

    suspend fun reset() {
        dataStore.edit { it[COUNT] = 0 }
    }

    /** Puts back a previous state, e.g. to undo a reset. */
    suspend fun restore(state: TasbihState) {
        dataStore.edit { it.write(state) }
    }

    private fun Preferences.toTasbih() = TasbihState(
        count = this[COUNT] ?: 0,
        dhikr = Dhikr.fromStored(this[DHIKR]),
        target = this[TARGET] ?: TasbihEngine.DEFAULT_TARGET,
    )

    private fun MutablePreferences.write(state: TasbihState) {
        this[COUNT] = state.count
        this[DHIKR] = state.dhikr.arabic
        this[TARGET] = state.target
    }

    private companion object {
        val COUNT = intPreferencesKey("tasbih_count")
        val DHIKR = stringPreferencesKey("tasbih_dhikr")
        val TARGET = intPreferencesKey("tasbih_target")
    }
}
