package ninja.dudley.yamr.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ninja.dudley.yamr.R;
import ninja.dudley.yamr.model.Provider;
import ninja.dudley.yamr.svc.FetcherAsync;

public class MainActivity extends Activity
{
    private static final String iconKey = "iconKey";
    private static final String nameKey = "nameKey";
    private static final String descriptionKey = "descriptionKey";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<Map<String, ?>> maps = new ArrayList<>();

        // Set up activities
        Map<String, Object> browseMap = new HashMap<>();
        browseMap.put(iconKey, R.drawable.ic_explore_black_48dp);
        browseMap.put(nameKey, "Browse Manga");
        browseMap.put(descriptionKey, "Browse through a provider's series");
        maps.add(browseMap);

        Map<String, Object> favoritesMap = new HashMap<>();
        favoritesMap.put(iconKey, R.drawable.ic_favorite_black_48dp);
        favoritesMap.put(nameKey, "Favorites");
        favoritesMap.put(descriptionKey, "Check up on your favorites");
        maps.add(favoritesMap);

        Map<String, Object> settingsMap = new HashMap<>();
        settingsMap.put(iconKey, R.drawable.ic_settings_black_48dp);
        settingsMap.put(nameKey, "Settings");
        settingsMap.put(descriptionKey, "Twiddle the knobs; Push the buttons");
        maps.add(settingsMap);

        SimpleAdapter adapter = new SimpleAdapter(
                this,
                maps,
                R.layout.activity_item,
                new String[]{iconKey, nameKey, descriptionKey},
                new int[]{R.id.activity_icon, R.id.activity_name, R.id.activity_description}
        );
        ListView listView = (ListView) findViewById(R.id.listView);

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                // TODO:: Is this the only way?
                Intent i;
                switch (position)
                {
                    case 0:
                        i = new Intent(MainActivity.this, Reader.class);
                        startActivity(i);
                        break;
                    case 1:
                        i = new Intent(MainActivity.this, Favorites.class);
                        startActivity(i);
                        break;
                    case 2:
                        i = new Intent(MainActivity.this, Settings.class);
                        startActivity(i);
                        break;
                    default:
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings)
        {
            Intent i = new Intent(this, FetcherAsync.class);
            i.setAction(FetcherAsync.FETCH_NEW);
            i.setData(Provider.uri(1));
            startService(i);
            Log.d("FetchNew", "Starting a fetch new ");
        }

        return super.onOptionsItemSelected(item);
    }
}
