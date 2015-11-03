package ninja.dudley.yamr.ui.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import ninja.dudley.yamr.BuildConfig
import ninja.dudley.yamr.R
import ninja.dudley.yamr.svc.FetchStarter
import java.util.ArrayList
import java.util.HashMap

public class MainActivity : Activity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (BuildConfig.DEBUG)
        {
            // Show version
            val versionView = findViewById(R.id.ver_name) as TextView
            versionView.text = BuildConfig.VERSION_NAME
        }

        val maps = ArrayList<Map<String, *>>()

        // Set up activities
        val browseMap = HashMap<String, Any>()
        browseMap.put(iconKey, R.drawable.ic_explore_black_48dp)
        browseMap.put(nameKey, "Browse by Provider")
        browseMap.put(descriptionKey, "Browse through a provider's series")
        maps.add(browseMap)

        val genreMap = HashMap<String, Any>()
        genreMap.put(iconKey, R.drawable.ic_cake_black_48dp)
        genreMap.put(nameKey, "Browse by Genre")
        genreMap.put(descriptionKey, "Browse through manga by genres")
        maps.add(genreMap)

        val favoritesMap = HashMap<String, Any>()
        favoritesMap.put(iconKey, R.drawable.ic_favorite_black_48dp)
        favoritesMap.put(nameKey, "Favorites")
        favoritesMap.put(descriptionKey, "Check up on your favorites")
        maps.add(favoritesMap)

        val settingsMap = HashMap<String, Any>()
        settingsMap.put(iconKey, R.drawable.ic_settings_black_48dp)
        settingsMap.put(nameKey, "Settings")
        settingsMap.put(descriptionKey, "Twiddle the knobs; Push the buttons")
        maps.add(settingsMap)

        val adapter = SimpleAdapter(this,
                                    maps,
                                    R.layout.activity_item,
                                    arrayOf(iconKey, nameKey, descriptionKey),
                                    intArrayOf(R.id.activity_icon,
                                               R.id.activity_name,
                                               R.id.activity_description))
        val listView = findViewById(R.id.listView) as ListView

        listView.adapter = adapter
        listView.onItemClickListener = object : AdapterView.OnItemClickListener
        {
            override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long)
            {
                // TODO:: Is this the only way?
                val i: Intent
                when (position)
                {
                    0 ->
                    {
                        i = Intent(this@MainActivity, Browse::class.java)
                        i.putExtra(Browse.FlowKey, Browse.FlowType.ProviderDown.name())
                        startActivity(i)
                    }
                    1 ->
                    {
                        i = Intent(this@MainActivity, Browse::class.java)
                        i.putExtra(Browse.FlowKey, Browse.FlowType.Genre.name())
                        startActivity(i)
                    }
                    2 ->
                    {
                        i = Intent(this@MainActivity, Browse::class.java)
                        i.putExtra(Browse.FlowKey, Browse.FlowType.Favorites.name())
                        startActivity(i)
                    }
                    3 ->
                    {
                        i = Intent(this@MainActivity, Settings::class.java)
                        startActivity(i)
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item!!.itemId

        if (id == R.id.action_settings)
        {
            val i = Intent(this, FetchStarter::class.java)
            i.setAction(FetchStarter.StartChecking)
            sendBroadcast(i) // Can't be local, as Android will be creating and managing our BroadcastReceiver
            Log.d("MainActivity", "Starting a fetch new ")
        }

        return super.onOptionsItemSelected(item)
    }

    companion object
    {
        private val iconKey = "iconKey"
        private val nameKey = "nameKey"
        private val descriptionKey = "descriptionKey"
    }
}
