package com.focusblock.app

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("focusblock", Context.MODE_PRIVATE)

    fun enabled(c: Context): Boolean = sp(c).getBoolean("enabled", true)
    fun setEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("enabled", v).apply()

    fun apps(c: Context): Set<String> = sp(c).getStringSet("apps", emptySet()) ?: emptySet()
    fun setApps(c: Context, s: Set<String>) = sp(c).edit().putStringSet("apps", HashSet(s)).apply()

    fun sites(c: Context): Set<String> = sp(c).getStringSet("sites", emptySet()) ?: emptySet()
    fun setSites(c: Context, s: Set<String>) = sp(c).edit().putStringSet("sites", HashSet(s)).apply()
}

object Util {
    /** "https://www.YouTube.com/watch?v=1" -> "youtube.com" */
    fun normalizeDomain(raw: String): String {
        var s = raw.trim().lowercase()
        s = s.removePrefix("https://").removePrefix("http://")
        s = s.substringBefore('/').substringBefore('?').substringBefore('#').substringBefore(':')
        if (s.startsWith("www.")) s = s.removePrefix("www.")
        return s
    }

    /** true if host is the blocked domain or any subdomain of it */
    fun matches(host: String, blocked: String): Boolean =
        host == blocked || host.endsWith(".$blocked")
}
