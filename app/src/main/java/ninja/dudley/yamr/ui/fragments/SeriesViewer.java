package ninja.dudley.yamr.ui.fragments;

import android.app.Activity;
import android.app.ListFragment;
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
import android.view.LayoutInflater;
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
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_series_viewer, container, false);

        fetchStatusReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                float percent = intent.getFloatExtra(FetcherAsync.FETCH_PROVIDER_STATUS, 0.0f);
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
            // I'm a little more OK with this, as Provider.baseUri() is static.
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
