package ninja.dudley.yamr.ui.activities;

import android.app.FragmentTransaction;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import ninja.dudley.yamr.R;
import ninja.dudley.yamr.db.DBHelper;
import ninja.dudley.yamr.model.Bookmark;
import ninja.dudley.yamr.model.Series;
import ninja.dudley.yamr.ui.fragments.PageViewer;

public class Favorites extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>
{
    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        adapter = new SimpleCursorAdapter(
                this,
                R.layout.series_item,
                null,
                new String[]{DBHelper.SeriesEntry.COLUMN_NAME},
                new int[]{R.id.series_name},
                0
        );
        setListAdapter(adapter);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_favorites, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        Uri bookmarkUri = Bookmark.uri((int) id);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        PageViewer pageViewer = new PageViewer();
        Bundle args = new Bundle();
        args.putParcelable(PageViewer.BookmarkArgumentKey, bookmarkUri);
        pageViewer.setArguments(args);
        transaction.replace(R.id.favorites, pageViewer);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args)
    {
        Uri favorites = Series.baseUri().buildUpon().appendPath("favorites").build();
        return new CursorLoader(
                this,
                favorites,
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
}
