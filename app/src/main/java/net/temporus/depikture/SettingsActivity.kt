package net.temporus.depikture

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceFragment
import android.util.Log
import net.temporus.depikture.utils.Prefs
import net.temporus.depikture.utils.getStringAsString

class SettingsActivity : Activity(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        val RESULT_CODE_SETTINGS_NOT_CHANGED = 100
        val RESULT_CODE_SETTINGS_CHANGED = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        fragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        setResult(RESULT_CODE_SETTINGS_NOT_CHANGED)
    }

    override fun onResume() {
        super.onResume()
        Prefs.with(this).registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        Prefs.with(this).unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        setResult(RESULT_CODE_SETTINGS_CHANGED)
    }

}

class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
        if (activity != null) {
            val prefs = Prefs.with(activity)
            val q = prefs.all
            updateServerUrlSummary(prefs)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        updateServerUrlSummary(Prefs.with(context))
    }

    override fun onResume() {
        super.onResume()
        Prefs.with(activity).registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        Prefs.with(activity).unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        when (key) {
            getString(R.string.pref_server_url) -> updateServerUrlSummary(prefs)
        }
    }

    private fun updateServerUrlSummary(prefs: SharedPreferences) {
        findPreference(getString(R.string.pref_server_url))?.let { pref ->
            val serverUrl = prefs.getStringAsString(activity, R.string.pref_server_url,
                    R.string.pref_server_url_default)
            pref.summary = serverUrl
        }
    }

}
