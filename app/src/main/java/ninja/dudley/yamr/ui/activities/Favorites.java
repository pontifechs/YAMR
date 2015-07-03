package ninja.dudley.yamr.ui.activities;

import android.app.FragmentTransaction;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import ninja.dudley.yamr.R;
import ninja.dudley.yamr.model.Chapter;
import ninja.dudley.yamr.model.Page;
import ninja.dudley.yamr.model.Series;
import ninja.dudley.yamr.ui.fragments.PageViewer;

public class Favorites extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>
{

    private ThumbSeriesAdapter adapter;
    public class ThumbSeriesAdapter extends SimpleCursorAdapter
    {
        private final LayoutInflater inflater;

        public ThumbSeriesAdapter()
        {
            super(Favorites.this, 0, null, new String[]{}, new int[]{}, 0);
            inflater = LayoutInflater.from(Favorites.this);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent)
        {
            return inflater.inflate(R.layout.thumb_series_item, null);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor)
        {
            super.bindView(view, context, cursor);
            Series series = new Series(cursor, false);
            ImageView thumb = (ImageView) view.findViewById(R.id.seriesThumbnail);
            TextView name = (TextView) view.findViewById(R.id.seriesName);
            TextView progress = (TextView) view.findViewById(R.id.seriesProgress);

            if (series.getThumbnailPath() != null)
            {
                Drawable d = Drawable.createFromPath(series.getThumbnailPath());
                thumb.setImageDrawable(d);
            }

            name.setText(series.getName());

            if (series.getProgressPageId() != -1)
            {
                Page p = new Page(context.getContentResolver().query(Page.uri(series.getProgressPageId()), null, null, null, null));
                Chapter c = new Chapter(context.getContentResolver().query(Chapter.uri(p.getChapterId()), null, null, null, null));

                progress.setText("Chapter: " + c.getNumber() + ", Page: " + p.getNumber());
            }
            else
            {
                progress.setText("Not Started");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        adapter = new ThumbSeriesAdapter();
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
        Uri bookmarkUri = Series.uri((int) id);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        PageViewer pageViewer = new PageViewer();
        Bundle args = new Bundle();
        args.putParcelable(PageViewer.SeriesArgumentKey, bookmarkUri);
        pageViewer.setArguments(args);
        transaction.replace(R.id.favorites, pageViewer);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args)
    {
        Uri favorites = Series.baseUri().buildUpon().appendPath("favorites").build();
        return new CursorLoader(this, favorites, null, null, null, null);
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
