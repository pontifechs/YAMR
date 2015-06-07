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
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import ninja.dudley.yamr.db.DBHelper;
import ninja.dudley.yamr.fetch.Fetcher;
import ninja.dudley.yamr.fetch.impl.MangaPandaFetcher;
import ninja.dudley.yamr.model.Chapter;
import ninja.dudley.yamr.model.Series;


public class SeriesViewer extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>
{

    private Series series;

    private SimpleCursorAdapter adapter;

    private BroadcastReceiver fetchStatusReceiver;
    private BroadcastReceiver fetchCompleteReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_viewer);

        fetchStatusReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                float percent = intent.getFloatExtra(Fetcher.FETCH_PROVIDER_STATUS, 0.0f);
                int grey = (int) (percent * 255);
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

        series = new Series(getContentResolver().query(getIntent().getData(), null, null, null, null));

        Intent fetchSeries = new Intent(this, MangaPandaFetcher.class);
        fetchSeries.setAction(Fetcher.FETCH_SERIES);
        fetchSeries.setData(series.uri());
        startService(fetchSeries);


        TextView title = (TextView) findViewById(R.id.textView);
        title.setText(series.getName());

        adapter = new SimpleCursorAdapter(
                this,
                R.layout.chapter_item,
                null,
                new String[]{DBHelper.ChapterEntry.COLUMN_NAME, DBHelper.ChapterEntry.COLUMN_NUMBER},
                new int[]{R.id.chapter_name, R.id.chapter_number},
                0
        );
        getListView().setAdapter(adapter);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(fetchStatusReceiver, new IntentFilter(Fetcher.FETCH_SERIES_STATUS));
        IntentFilter completeFilter = new IntentFilter(Fetcher.FETCH_SERIES_COMPLETE);
        try
        {
            completeFilter.addDataType(getContentResolver().getType(Series.baseUri()));
        }
        catch (IntentFilter.MalformedMimeTypeException e)
        {
            // I'm a little more OK with this, as Provider.baseUri() is static.
            throw new AssertionError(e);
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(fetchCompleteReceiver, completeFilter);
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
        return new CursorLoader(
                this,
                series.uri().buildUpon().appendPath("chapters").build(),
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
        Intent i = new Intent(this, PageViewer.class);
        i.setData(Chapter.uri((int) id));
        startActivity(i);
    }
}
