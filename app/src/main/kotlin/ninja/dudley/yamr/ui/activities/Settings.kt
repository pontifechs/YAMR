package ninja.dudley.yamr.ui.fragments

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem

import ninja.dudley.yamr.R

public class Settings : Activity()
{

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val transaction = getFragmentManager().beginTransaction()
        val settingsBox = SettingsFragment();
        transaction.replace(R.id.settingsContainer, settingsBox)
        transaction.commit()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean
    {
        getMenuInflater().inflate(R.menu.menu_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
    {
        val id = item!!.getItemId()

        if (id == R.id.action_settings)
        {
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    // Wow! Much Stupid! So Annoy!
    class SettingsFragment : PreferenceFragment()
    {
        override fun onCreate(savedInstanceState: Bundle?)
        {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)
        }
    }

    companion object
    {
        private val RTL_ENABLED_KEY: String = "rtl_enabled"
        private val PRE_FETCH_BUFFER_KEY: String = "pre_fetch_buffer"

        public fun rtlEnabled(context: Context): Boolean
        {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getBoolean(RTL_ENABLED_KEY, true)
        }

        public fun preFetchEnabled(context: Context): Boolean
        {
            return preFetchSize(context) > 0
        }

        public fun preFetchSize(context: Context): Int
        {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getInt(PRE_FETCH_BUFFER_KEY, -1)
        }
    }
}
