package com.focusblock.app

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView

class BlockerService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var overlay: View? = null
    private var lastBlock = 0L

    // browser package -> possible view ids of its address bar
    private val urlBars: Map<String, List<String>> = mapOf(
        "com.android.chrome" to listOf("com.android.chrome:id/url_bar"),
        "com.chrome.beta" to listOf("com.chrome.beta:id/url_bar"),
        "com.brave.browser" to listOf("com.brave.browser:id/url_bar"),
        "com.microsoft.emmx" to listOf("com.microsoft.emmx:id/url_bar"),
        "com.opera.browser" to listOf("com.opera.browser:id/url_field"),
        "com.sec.android.app.sbrowser" to listOf(
            "com.sec.android.app.sbrowser:id/location_bar_edit_text"
        ),
        "org.mozilla.firefox" to listOf(
            "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
            "org.mozilla.firefox:id/url_bar_title"
        ),
        "com.duckduckgo.mobile.android" to listOf(
            "com.duckduckgo.mobile.android:id/omnibarTextInput"
        )
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !Prefs.enabled(this)) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return

        // 1) blocked app
        if (pkg in Prefs.apps(this)) {
            block("This app is blocked")
            return
        }

        // 2) blocked website in a known browser
        val ids = urlBars[pkg] ?: return
        val sites = Prefs.sites(this)
        if (sites.isEmpty()) return
        val root = rootInActiveWindow ?: return

        var url: String? = null
        for (id in ids) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) {
                url = nodes[0].text?.toString()
                break
            }
        }
        if (url.isNullOrBlank()) return

        val host = Util.normalizeDomain(url)
        if (host.isEmpty() || host.contains(' ')) return // a search query, not a URL
        if (sites.any { Util.matches(host, it) }) {
            block("This website is blocked")
        }
    }

    private fun block(message: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastBlock < 1500) return
        lastBlock = now
        performGlobalAction(GLOBAL_ACTION_HOME)
        showOverlay(message)
    }

    private fun showOverlay(message: String) {
        handler.post {
            removeOverlay()
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            val tv = TextView(this).apply {
                text = "🚫  $message\n\nStay focused."
                setTextColor(Color.WHITE)
                textSize = 22f
                gravity = Gravity.CENTER
                setBackgroundColor(0xEE111827.toInt())
                setPadding(48, 48, 48, 48)
            }
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            try {
                wm.addView(tv, lp)
                overlay = tv
                handler.postDelayed({ removeOverlay() }, 2500)
            } catch (_: Exception) {
            }
        }
    }

    private fun removeOverlay() {
        val v = overlay ?: return
        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(v)
        } catch (_: Exception) {
        }
        overlay = null
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }
}
