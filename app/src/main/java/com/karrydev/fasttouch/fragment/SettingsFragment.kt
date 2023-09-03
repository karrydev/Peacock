package com.karrydev.fasttouch.fragment

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.karrydev.fasttouch.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}