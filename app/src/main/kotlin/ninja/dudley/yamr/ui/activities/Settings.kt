package ninja.dudley.yamr.ui.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.preference.CheckBoxPreference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem

import ninja.dudley.yamr.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Settings : Activity()
{

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val transaction = fragmentManager.beginTransaction()
        val settingsBox = SettingsFragment();
        transaction.replace(R.id.settingsContainer, settingsBox)
        transaction.commit()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean
    {
        menuInflater.inflate(R.menu.menu_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
    {
        return super.onOptionsItemSelected(item)
    }

    // Wow! Much Stupid! So Annoy!
    class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener
    {
        override fun onResume()
        {
            super.onResume()
            preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause()
        {
            super.onPause()
            preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?)
        {
            val pref = findPreference("use_external_storage") as CheckBoxPreference
            if (pref.isChecked)
            {
                createNoMedia()
            }
        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?)
        {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            createNoMedia()
        }

        private fun createNoMedia()
        {
            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            {
                AlertDialog.Builder(activity)
                        .setTitle("External Storage Privileges")
                        .setMessage("YAMR needs External Storage privileges to save to External Storage.")
                        .setNegativeButton("Just Kidding.", {dialogInterface, which ->
                            val pref = findPreference("use_external_storage") as CheckBoxPreference
                            pref.isChecked = false
                        })
                        .setPositiveButton("Fair enough.", {dialogInterface, which ->
                            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), 42);
                        })
                        .create().show()
            }
            else
            {
                val root = Environment.getExternalStorageDirectory();
                try
                {
                    val noMedia = File(root.absolutePath + "/YAMR/.nomedia");
                    noMedia.parentFile.mkdirs();
                    noMedia.createNewFile();
                    val nomediaStream = FileOutputStream(noMedia);
                    val body = "Sigh.... I really don't like these sorts of magic files. The more work I do in android, the less I like it.";
                    nomediaStream.write(body.toByteArray());
                    nomediaStream.close();
                    Log.d("Settings", "Creating .nomedia")
                }
                catch (e: IOException)
                {
                    e.printStackTrace();
                    throw RuntimeException(e);
                }
            }
        }

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
        private val USE_EXTERNAL_STORAGE_KEY: String = "use_external_storage"
        private val BATOTO_USERNAME: String = "batoto_username"
        private val BATOTO_PASSWORD: String = "batoto_password"

        fun rtlEnabled(context: Context): Boolean
        {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getBoolean(RTL_ENABLED_KEY, false)
        }

        fun preFetchEnabled(context: Context): Boolean
        {
            return preFetchSize(context) > 0
        }

        fun preFetchSize(context: Context): Int
        {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getInt(PRE_FETCH_BUFFER_KEY, -1)
        }

        fun useExternalStorage(context: Context): Boolean
        {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getBoolean(USE_EXTERNAL_STORAGE_KEY, false)
        }

        fun batotoUsername(context: Context): String?
        {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getString(BATOTO_USERNAME, null)
        }

        fun batotoPassword(context: Context): String?
        {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getString(BATOTO_PASSWORD, null)
        }
    }
}
