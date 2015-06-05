package ninja.dudley.yamr;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import ninja.dudley.yamr.db.DBHelper;
import ninja.dudley.yamr.model.Page;
import ninja.dudley.yamr.model.Series;


public class ChapterViewer extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>
{

    private Series series;

    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_viewer);

        Intent intent = getIntent();
        series = new Series(getContentResolver().query(intent.getData(), null, null, null, null));

        TextView title = (TextView) findViewById(R.id.textView);
        title.setText(series.getName());

        adapter = new SimpleCursorAdapter(
                this,
                R.layout.chapter_item,
                null,
                new String[]{DBHelper.SeriesEntry.COLUMN_NAME},
                new int[]{R.id.chapter_name},
                0
        );
        getListView().setAdapter(adapter);

        getLoaderManager().initLoader(0, null, this);
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
        i.setData(Page.uri((int)id));
        startActivity(i);
    }
}
