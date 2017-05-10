package ninja.dudley.yamr.ui.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import ninja.dudley.yamr.BuildConfig
import ninja.dudley.yamr.R
import ninja.dudley.yamr.svc.FetcherAsync
import ninja.dudley.yamr.svc.FetcherSync
import ninja.dudley.yamr.ui.fragments.SeriesViewer
import java.util.ArrayList
import java.util.HashMap

class MainActivity : Activity()
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
        val continueMap = HashMap<String, Any>()
        continueMap.put(iconKey, R.drawable.ic_label_black_48dp)
        continueMap.put(nameKey, "Continue Reading")
        continueMap.put(descriptionKey, "Pick up right where you left off")
        maps.add(continueMap)

        val favoritesMap = HashMap<String, Any>()
        favoritesMap.put(iconKey, R.drawable.ic_favorite_black_48dp)
        favoritesMap.put(nameKey, "Favorites")
        favoritesMap.put(descriptionKey, "Check up on your favorites")
        maps.add(favoritesMap)

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

        val adapter = SimpleAdapter(this,
                                    maps,
                                    R.layout.activity_item,
                                    arrayOf(iconKey, nameKey, descriptionKey),
                                    intArrayOf(R.id.activity_icon,
                                               R.id.activity_name,
                                               R.id.activity_description))
        val listView = findViewById(R.id.listView) as ListView

        listView.adapter = adapter
        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            // TODO:: Is this the only way?
            val i: Intent
            when (position)
            {
                0 ->
                {
                    val tracked = SeriesViewer.trackedSeries(this@MainActivity)
                    if (tracked == null)
                    {
                        val dialog = AlertDialog.Builder(this@MainActivity)
                                .setTitle("You haven't set a series to track!")
                                .setMessage("Set this up in your favorites menu.")
                                .setNegativeButton("K.", null).create()
                        dialog.show()
                        return@OnItemClickListener
                    }

                    i = Intent(this@MainActivity, Browse::class.java)
                    i.putExtra(Browse.FlowKey, Browse.FlowType.ContinueReading.name)
                    startActivity(i)
                }
                1 ->
                {
                    i = Intent(this@MainActivity, Browse::class.java)
                    i.putExtra(Browse.FlowKey, Browse.FlowType.Favorites.name)
                    startActivity(i)
                }
                2 ->
                {
                    i = Intent(this@MainActivity, Browse::class.java)
                    i.putExtra(Browse.FlowKey, Browse.FlowType.ProviderDown.name)
                    startActivity(i)
                }
                3 ->
                {
                    i = Intent(this@MainActivity, Browse::class.java)
                    i.putExtra(Browse.FlowKey, Browse.FlowType.Genre.name)
                    startActivity(i)
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
            val i = Intent(this, Settings::class.java)
            startActivity(i)
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
