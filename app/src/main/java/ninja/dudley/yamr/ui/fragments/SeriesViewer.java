package ninja.dudley.yamr.ui.fragments;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import ninja.dudley.yamr.R;
import ninja.dudley.yamr.coms.LoadChapter;
import ninja.dudley.yamr.db.DBHelper;
import ninja.dudley.yamr.model.Chapter;
import ninja.dudley.yamr.model.Series;
import ninja.dudley.yamr.svc.FetcherAsync;


public class SeriesViewer extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>
{
    private Series series;

    private SimpleCursorAdapter adapter;

    private BroadcastReceiver fetchStatusReceiver;
    private BroadcastReceiver fetchCompleteReceiver;

    private LoadChapter parent;

    private ProgressDialog loading;

    public static final String ArgumentKey = "series";

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        this.parent = (LoadChapter) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);

        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_series_viewer, container, false);

        fetchStatusReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                float percent = 100 * intent.getFloatExtra(FetcherAsync.FETCH_SERIES_STATUS, 0.0f);
                loading.setProgress((int)percent);
            }
        };

        fetchCompleteReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                getLoaderManager().restartLoader(0, null, SeriesViewer.this);
                adapter.notifyDataSetChanged();
                if (loading != null)
                {
                    loading.dismiss();
                }
                getActivity().invalidateOptionsMenu();
            }
        };

        Uri seriesUri = getArguments().getParcelable(ArgumentKey);

        series = new Series(getActivity().getContentResolver().query(seriesUri, null, null, null, null));

        Intent fetchSeries = new Intent(getActivity(), FetcherAsync.class);
        fetchSeries.setAction(FetcherAsync.FETCH_SERIES);
        fetchSeries.setData(series.uri());
        getActivity().startService(fetchSeries);


        TextView title = (TextView) layout.findViewById(R.id.textView);
        title.setText(series.getName());

        adapter = new SimpleCursorAdapter(
                getActivity(),
                R.layout.chapter_item,
                null,
                new String[]{DBHelper.ChapterEntry.COLUMN_NAME, DBHelper.ChapterEntry.COLUMN_NUMBER},
                new int[]{R.id.chapter_name, R.id.chapter_number},
                0
        );
        setListAdapter(adapter);

        getLoaderManager().initLoader(0, null, this);

        if (!series.isFullyParsed())
        {
            loading = new ProgressDialog(getActivity());
            loading.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            loading.setTitle("Loading Chapters for " + series.getName());
            loading.show();
        }
        return layout;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(fetchStatusReceiver, new IntentFilter(FetcherAsync.FETCH_SERIES_STATUS));
        IntentFilter completeFilter = new IntentFilter(FetcherAsync.FETCH_SERIES_COMPLETE);
        try
        {
            completeFilter.addDataType(getActivity().getContentResolver().getType(Series.baseUri()));
        }
        catch (IntentFilter.MalformedMimeTypeException e)
        {
            throw new AssertionError(e);
        }
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(fetchCompleteReceiver, completeFilter);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(fetchStatusReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(fetchCompleteReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.menu_series_viewer, menu);
        if (series != null && series.isFavorite())
        {
            menu.findItem(R.id.favorite).setIcon(R.drawable.ic_favorite_border_white_48dp);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.favorite:
                if (series.isFavorite())
                {
                    series.setFavorite(false);
                }
                else
                {
                    series.setFavorite(true);
                }
                getActivity().getContentResolver().update(series.uri(), series.getContentValues(), null, null);
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args)
    {
        return new CursorLoader(
                getActivity(),
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
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        parent.loadChapter(Chapter.uri((int) id));
    }
}
