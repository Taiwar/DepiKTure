package net.temporus.depikture.fragments
import net.temporus.depikture.utils.getStringAsString
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceFragment
import net.temporus.depikture.R
import net.temporus.depikture.utils.Prefs


class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
        if (activity != null) {
            val prefs = Prefs.with(activity)
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