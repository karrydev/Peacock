package com.karrydev.fasttouch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.karrydev.fasttouch.fragment.SettingsFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        
    }
}