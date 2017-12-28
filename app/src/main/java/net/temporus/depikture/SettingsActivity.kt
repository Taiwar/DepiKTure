package net.temporus.depikture

import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import net.temporus.depikture.fragments.SettingsFragment
import net.temporus.depikture.utils.Prefs

class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

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


