package ninja.dudley.yamr.ui.fragments

import android.app.Activity
import android.os.Bundle
import android.preference.PreferenceFragment
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item!!.getItemId()

        //noinspection SimplifiableIfStatement
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
        public val RTL_ENABLED_KEY: String = "RTL_ENABLED"
    }
}
