package com.focusblock.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private data class AppItem(val label: String, val pkg: String)

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private lateinit var status: TextView
    private lateinit var content: FrameLayout
    private lateinit var tabApps: Button
    private lateinit var tabSites: Button

    private val allApps = ArrayList<AppItem>()
    private val blockedApps = HashSet<String>()
    private val sites = ArrayList<String>()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        blockedApps.addAll(Prefs.apps(this))
        sites.addAll(Prefs.sites(this).sorted())
        loadApps()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(8))
        }

        root.addView(TextView(this).apply {
            text = "FocusBlock"
            textSize = 26f
            setTextColor(Color.BLACK)
        })

        status = TextView(this).apply {
            textSize = 14f
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(status)

        root.addView(Button(this).apply {
            text = "Open Accessibility Settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })

        root.addView(Switch(this).apply {
            text = "Blocking enabled"
            isChecked = Prefs.enabled(this@MainActivity)
            setPadding(0, dp(8), 0, dp(8))
            setOnCheckedChangeListener { _, on -> Prefs.setEnabled(this@MainActivity, on) }
        })

        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabApps = Button(this).apply {
            text = "Apps"
            setOnClickListener { showApps() }
        }
        tabSites = Button(this).apply {
            text = "Websites"
            setOnClickListener { showSites() }
        }
        tabs.addView(tabApps, LinearLayout.LayoutParams(0, WRAP, 1f))
        tabs.addView(tabSites, LinearLayout.LayoutParams(0, WRAP, 1f))
        root.addView(tabs)

        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(MATCH, 0, 1f))

        setContentView(root)
        showApps()
    }

    override fun onResume() {
        super.onResume()
        if (isServiceEnabled()) {
            status.text = "✅ Blocking is active"
            status.setTextColor(Color.rgb(0, 120, 0))
        } else {
            status.text = "⚠️ Accessibility permission is OFF. Tap the button below, " +
                "open FocusBlock and turn it on."
            status.setTextColor(Color.rgb(180, 0, 0))
        }
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val cn = ComponentName(this, BlockerService::class.java)
        return enabled.split(':').any {
            it.equals(cn.flattenToString(), true) || it.equals(cn.flattenToShortString(), true)
        }
    }

    private fun loadApps() {
        val pm = packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val found = pm.queryIntentActivities(launcher, 0)
            .map { AppItem(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            // never list ourselves or Settings, so you can't lock yourself out
            .filter { it.pkg != packageName && it.pkg != "com.android.settings" }
            .distinctBy { it.pkg }
            .sortedWith(compareBy({ it.pkg !in blockedApps }, { it.label.lowercase() }))
        allApps.clear()
        allApps.addAll(found)
    }

    private fun showApps() {
        tabApps.alpha = 1f
        tabSites.alpha = 0.5f
        content.removeAllViews()

        val lv = ListView(this)
        lv.adapter = object : BaseAdapter() {
            override fun getCount(): Int = allApps.size
            override fun getItem(position: Int): Any = allApps[position]
            override fun getItemId(position: Int): Long = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val item = allApps[position]
                val cb = (convertView as? CheckBox) ?: CheckBox(this@MainActivity).apply {
                    setPadding(dp(8), dp(14), dp(8), dp(14))
                    textSize = 16f
                }
                cb.setOnCheckedChangeListener(null)
                cb.text = item.label
                cb.isChecked = item.pkg in blockedApps
                cb.setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        blockedApps.add(item.pkg)
                    } else {
                        blockedApps.remove(item.pkg)
                    }
                    Prefs.setApps(this@MainActivity, blockedApps)
                }
                return cb
            }
        }
        content.addView(lv, FrameLayout.LayoutParams(MATCH, MATCH))
    }

    private fun showSites() {
        tabApps.alpha = 0.5f
        tabSites.alpha = 1f
        content.removeAllViews()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, sites)

        val input = EditText(this).apply {
            hint = "example.com"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine()
        }
        val add = Button(this).apply {
            text = "Add"
            setOnClickListener {
                val d = Util.normalizeDomain(input.text.toString())
                if (d.isEmpty() || !d.contains('.')) {
                    Toast.makeText(
                        this@MainActivity, "Enter a domain like youtube.com", Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                if (d !in sites) {
                    sites.add(d)
                    sites.sort()
                    Prefs.setSites(this@MainActivity, sites.toSet())
                    adapter.notifyDataSetChanged()
                }
                input.setText("")
            }
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(input, LinearLayout.LayoutParams(0, WRAP, 1f))
        row.addView(add, LinearLayout.LayoutParams(WRAP, WRAP))

        val list = ListView(this)
        list.adapter = adapter
        list.setOnItemClickListener { _, _, pos, _ ->
            val d = sites[pos]
            AlertDialog.Builder(this)
                .setMessage("Unblock $d?")
                .setPositiveButton("Unblock") { _, _ ->
                    sites.removeAt(pos)
                    Prefs.setSites(this, sites.toSet())
                    adapter.notifyDataSetChanged()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(row)
        col.addView(TextView(this).apply {
            text = "Tap a site below to unblock it. Works in Chrome, Brave, Edge, Firefox, " +
                "Samsung Internet, Opera and DuckDuckGo."
            textSize = 12f
            setPadding(0, dp(4), 0, dp(4))
        })
        col.addView(list, LinearLayout.LayoutParams(MATCH, 0, 1f))
        content.addView(col, FrameLayout.LayoutParams(MATCH, MATCH))
    }
}
