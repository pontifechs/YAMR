package ninja.dudley.yamr;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import ninja.dudley.yamr.db.DBHelper;
import ninja.dudley.yamr.fetch.Fetcher;
import ninja.dudley.yamr.fetch.impl.MangaPandaFetcher;
import ninja.dudley.yamr.model.Provider;
import ninja.dudley.yamr.model.Series;

public class SeriesViewer extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>
{
    private SimpleCursorAdapter adapter;

    private BroadcastReceiver fetchStatusReceiver;
    private BroadcastReceiver fetchCompleteReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_series_viewer);

        fetchStatusReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                float percent = intent.getFloatExtra(Fetcher.FETCH_PROVIDER_STATUS, 0.0f);
                int grey = (int)(percent * 255);
                getListView().setBackgroundColor(Color.argb(255, grey, grey, grey));
            }
        };

        fetchCompleteReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                getLoaderManager().restartLoader(0, null, SeriesViewer.this);
                adapter.notifyDataSetChanged();
            }
        };

        Intent i = new Intent(this, MangaPandaFetcher.class);
        i.setAction(Fetcher.FETCH_PROVIDER);
        i.setData(Provider.uri(1));   // Hard-code to the first (mangapanda) for now.
        startService(i);

        adapter = new SimpleCursorAdapter(
                this,
                R.layout.series_item,
                null,
                new String[]{DBHelper.SeriesEntry.COLUMN_NAME},
                new int[]{R.id.series_name},
                0
        );
        getListView().setAdapter(adapter);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(fetchStatusReceiver, new IntentFilter(Fetcher.FETCH_PROVIDER_STATUS));
        LocalBroadcastManager.getInstance(this).registerReceiver(fetchCompleteReceiver, new IntentFilter(Fetcher.FETCH_PROVIDER_COMPLETE));
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(fetchStatusReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(fetchCompleteReceiver);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args)
    {
        Uri mangaPandaSeries = Provider.uri(1).buildUpon().appendPath("series").build();
        return new CursorLoader(
                this,
                mangaPandaSeries,
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data)
    {
        adapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {
        adapter.changeCursor(null);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        super.onListItemClick(l, v, position, id);
        Intent i  = new Intent(this, ChapterViewer.class);
        i.setData(Series.uri((int)id));
        startActivity(i);
    }
}
