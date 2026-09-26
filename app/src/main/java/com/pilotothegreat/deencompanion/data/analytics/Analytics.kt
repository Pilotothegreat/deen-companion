package com.pilotothegreat.deencompanion.data.analytics

import com.pilotothegreat.deencompanion.core.analytics.UsageEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * The one line a screen writes to say something happened.
 *
 * A global rather than a constructor argument threaded through twenty classes, for the same reason
 * `WidgetDeps` is one: a counter is not a dependency of anything, and a screen that has to be given
 * an analytics object in order to be built is a screen that cannot be rendered in a test without one.
 * Every failure is swallowed — nothing the app does should break because a tally could not be kept.
 */
object Analytics : KoinComponent {
    fun record(event: UsageEvent) {
        runCatching { get<AnalyticsRepository>().record(event) }
    }
}
