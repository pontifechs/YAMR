package ninja.dudley.yamr.ui.activities

import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceFragment

import ninja.dudley.yamr.R
import ninja.dudley.yamr.ui.fragments.ProviderViewer

public class Settings : PreferenceActivity()
{

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val transaction = getFragmentManager().beginTransaction()
        val settings = SettingsFragment()
        transaction.replace(R.id.settingsContainer, settings)
        transaction.commit()
    }

    public class SettingsFragment : PreferenceFragment()
    {
        override fun onCreate(savedInstanceState: Bundle?)
        {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)
        }
    }


    companion object
    {
        public val RTL_ENABLED_KEY: String = "rtl_enabled"
    }
}
