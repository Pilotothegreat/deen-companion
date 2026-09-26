package com.pilotothegreat.deencompanion.core.device

/**
 * Why an alarm did not arrive.
 *
 * Every one of these is a real thing a phone does to a working app, and none of them produces an
 * error the app can see. They are listed as checks so the reader is shown the one that is actually
 * wrong, rather than a page of advice that mostly does not apply to them.
 */
enum class ReliabilityCheck {
    /** Android 13+ will not let a notification through without it. */
    NOTIFICATION_PERMISSION,

    /** Without it an alarm can drift by many minutes, which for a prayer time is a wrong time. */
    EXACT_ALARMS,

    /** Doze and app standby can hold an alarm until the phone is next unlocked. */
    BATTERY_OPTIMISATION,

    /** The reader turned the channel off in system settings; the app cannot turn it back on. */
    CHANNEL_BLOCKED,

    /**
     * Xiaomi, Huawei, Oppo, Vivo, Samsung and others kill background apps by their own rules, which
     * no permission covers and no API reports. All the app can do is say where the switch lives.
     */
    MANUFACTURER_RESTRICTION,
}

/** The phone makers whose own battery managers stop alarms, and where their setting lives. */
enum class OemGuidance(val manufacturers: Set<String>) {
    XIAOMI(setOf("xiaomi", "redmi", "poco")),
    HUAWEI(setOf("huawei", "honor")),
    OPPO(setOf("oppo", "realme")),
    VIVO(setOf("vivo", "iqoo")),
    SAMSUNG(setOf("samsung")),
    OTHER(emptySet());

    companion object {
        fun forManufacturer(manufacturer: String): OemGuidance {
            val name = manufacturer.lowercase()
            return entries.firstOrNull { guidance -> guidance.manufacturers.any { it in name } } ?: OTHER
        }

        /** True where the manufacturer is known to need a step beyond Android's own settings. */
        fun needsExtraStep(manufacturer: String): Boolean = forManufacturer(manufacturer) != OTHER
    }
}
