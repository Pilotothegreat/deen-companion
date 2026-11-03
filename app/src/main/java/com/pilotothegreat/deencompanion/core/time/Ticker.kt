package com.pilotothegreat.deencompanion.core.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Clock ticks shared by everything that shows the time. Each flow emits on the boundary rather than
 * on an interval, so a countdown changes the moment the second does.
 */
object Ticker {
    /** Emits at every second boundary. Only for a visible countdown. */
    val seconds: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(1_000 - System.currentTimeMillis() % 1_000)
        }
    }

    /** Emits the epoch minute at every minute boundary; what anything not counting down should use. */
    val minutes: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis() / 60_000)
            delay(60_000 - System.currentTimeMillis() % 60_000)
        }
    }.distinctUntilChanged()

    /** Emits once a day at local midnight, for anything keyed to the date. */
    val days: Flow<Long> = minutes.map { it / (60 * 24) }.distinctUntilChanged()
}
