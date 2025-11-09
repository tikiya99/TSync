package com.example.lumanotifier

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class AppSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_selection)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val listView = findViewById<ListView>(R.id.appListView)
        val saveButton = findViewById<Button>(R.id.saveButton)

        toolbar.setNavigationOnClickListener { finish() }

        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                // Show all launchable apps plus known messaging/navigation apps
                pm.getLaunchIntentForPackage(app.packageName) != null ||
                app.packageName.contains("maps", ignoreCase = true) ||
                app.packageName.contains("messenger", ignoreCase = true) ||
                app.packageName.contains("whatsapp", ignoreCase = true) ||
                app.packageName.contains("telegram", ignoreCase = true)
            }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }

        val appNames = apps.map { pm.getApplicationLabel(it).toString() }
        val packageNames = apps.map { it.packageName }

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val saved = prefs.getStringSet("allowed_apps", emptySet()) ?: emptySet()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, appNames)
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        listView.adapter = adapter

        // Restore previous selections
        for (i in apps.indices) {
            if (saved.contains(packageNames[i])) {
                listView.setItemChecked(i, true)
            }
        }

        saveButton.setOnClickListener {
            val selected = mutableSetOf<String>()
            for (i in 0 until listView.count) {
                if (listView.isItemChecked(i)) {
                    selected.add(packageNames[i])
                }
            }

            prefs.edit().putStringSet("allowed_apps", selected).apply()
            Toast.makeText(this, "Saved ${selected.size} apps", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
