package com.pilotothegreat.deencompanion.core.update

object AppVersion {
    /** True when [candidate] (e.g. a release tag "v1.6.1") is newer than [current]. */
    fun isNewer(current: String, candidate: String): Boolean {
        val a = parts(current)
        val b = parts(candidate)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return y > x
        }
        return false
    }

    private fun parts(version: String): List<Int> = version.trim()
        .removePrefix("v").removePrefix("V")
        .substringBefore('-')
        .split('.')
        .map { it.toIntOrNull() ?: 0 }
}
