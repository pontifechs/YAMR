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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;

import ninja.dudley.yamr.R;
import ninja.dudley.yamr.coms.LoadSeries;
import ninja.dudley.yamr.db.DBHelper;
import ninja.dudley.yamr.model.Provider;
import ninja.dudley.yamr.model.Series;
import ninja.dudley.yamr.svc.FetcherAsync;

public class ProviderViewer extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor>, SearchView.OnQueryTextListener
{
    private SimpleCursorAdapter adapter;

    private BroadcastReceiver fetchStatusReceiver;
    private BroadcastReceiver fetchCompleteReceiver;

    private LoadSeries parent;

    private ProgressDialog loading;

    private String filter;

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        this.parent = (LoadSeries) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);

        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_provider_viewer, container, false);

        fetchStatusReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                float percent = 100 * intent.getFloatExtra(FetcherAsync.FETCH_PROVIDER_STATUS, 0.0f);
                loading.setProgress((int) percent);
            }
        };

        fetchCompleteReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                getLoaderManager().restartLoader(0, null, ProviderViewer.this);
                adapter.notifyDataSetChanged();
                loading.dismiss();
            }
        };

        Intent i = new Intent(getActivity(), FetcherAsync.class);
        i.setAction(FetcherAsync.FETCH_PROVIDER);
        i.setData(Provider.uri(1));   // Hard-code to the first (mangapanda) for now.
        getActivity().startService(i);

        adapter = new SimpleCursorAdapter(
                getActivity(),
                R.layout.series_item,
                null,
                new String[]{DBHelper.SeriesEntry.COLUMN_NAME},
                new int[]{R.id.series_name},
                0
        );
        setListAdapter(adapter);

        getLoaderManager().initLoader(0, null, this);
        loading = new ProgressDialog(getActivity());
        loading.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        loading.setTitle("Loading Series");
        loading.show();
        return layout;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        IntentFilter completeFilter = new IntentFilter(FetcherAsync.FETCH_PROVIDER_COMPLETE);
        try
        {
            completeFilter.addDataType(getActivity().getContentResolver().getType(Provider.baseUri()));
        }
        catch (IntentFilter.MalformedMimeTypeException e)
        {
            // I'm a little more OK with this, as Provider.baseUri() is static.
            throw new AssertionError(e);
        }
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(fetchCompleteReceiver, completeFilter);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(fetchStatusReceiver, new IntentFilter(FetcherAsync.FETCH_PROVIDER_STATUS));
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
        inflater.inflate(R.menu.menu_provider_viewer, menu);
        MenuItem item = menu.findItem(R.id.search);
        SearchView sv = (SearchView) item.getActionView();
        sv.setOnQueryTextListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.search:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args)
    {
        Uri mangaPandaSeries = Provider.uri(1).buildUpon().appendPath("series").build();
        if (filter == null)
        {
            return new CursorLoader(
                    getActivity(),
                    mangaPandaSeries,
                    null,
                    null,
                    null,
                    null
            );
        }
        else
        {
            return new CursorLoader(
                    getActivity(),
                    mangaPandaSeries,
                    null,
                    DBHelper.SeriesEntry.COLUMN_NAME + " like ?",
                    new String[]{'%' + filter + '%'},
                    null
            );
        }
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
        super.onListItemClick(l, v, position, id);
        parent.loadSeries(Series.uri((int) id));
    }

    @Override
    public boolean onQueryTextSubmit(String query)
    {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText)
    {
        filter = !TextUtils.isEmpty(newText) ? newText : null;
        getLoaderManager().restartLoader(0, null, this);
        return true;
    }
}
