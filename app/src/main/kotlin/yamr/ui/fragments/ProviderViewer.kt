package ninja.dudley.yamr.ui.fragments

import android.app.Activity
import android.app.ListFragment
import android.app.LoaderManager
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.CursorLoader
import android.content.Intent
import android.content.IntentFilter
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SearchView
import android.widget.SimpleCursorAdapter

import ninja.dudley.yamr.R
import ninja.dudley.yamr.model.Provider
import ninja.dudley.yamr.model.Series
import ninja.dudley.yamr.svc.FetcherAsync
import ninja.dudley.yamr.svc.FetcherSync

public class ProviderViewer : ListFragment(), LoaderManager.LoaderCallbacks<Cursor>, SearchView.OnQueryTextListener
{
    private var adapter: SimpleCursorAdapter? = null

    private var fetchStatusReceiver: BroadcastReceiver? = null
    private var fetchCompleteReceiver: BroadcastReceiver? = null

    private var loading: ProgressDialog? = null
    private var filter: String? = null

    public interface LoadSeries
    {
        public fun loadSeries(series: Uri)
    }

    private var parent: LoadSeries? = null
    override fun onAttach(activity: Activity?)
    {
        super<ListFragment>.onAttach(activity)

        this.parent = activity as LoadSeries?
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        setHasOptionsMenu(true)

        val layout = inflater.inflate(R.layout.fragment_provider_viewer, container, false) as LinearLayout

        fetchStatusReceiver = object : BroadcastReceiver()
        {
            override fun onReceive(context: Context, intent: Intent)
            {
                val percent = 100 * intent.getFloatExtra(FetcherAsync.FETCH_PROVIDER_STATUS, 0.0f)
                loading!!.setProgress(percent.toInt())
            }
        }

        fetchCompleteReceiver = object : BroadcastReceiver()
        {
            override fun onReceive(context: Context, intent: Intent)
            {
                getLoaderManager().restartLoader(0, Bundle(), this@ProviderViewer)
                adapter!!.notifyDataSetChanged()
                loading!!.dismiss()
            }
        }

        val i = Intent(getActivity(), javaClass<FetcherAsync>())
        i.setAction(FetcherAsync.FETCH_PROVIDER)
        i.setData(Provider.uri(1))   // Hard-code to the first (mangapanda) for now.
        getActivity().startService(i)

        adapter = SimpleCursorAdapter(getActivity(), R.layout.simple_series_item, null, arrayOf(Series.nameCol), intArrayOf(R.id.series_name), 0)
        setListAdapter(adapter)

        getLoaderManager().initLoader(0, Bundle(), this)
        loading = ProgressDialog(getActivity())
        loading!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        loading!!.setTitle("Loading Series")
        loading!!.show()
        return layout
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        outState.putString(filterArg, filter)
        super<ListFragment>.onSaveInstanceState(outState)
    }

    override fun onResume()
    {
        super<ListFragment>.onResume()
        val completeFilter = IntentFilter(FetcherAsync.FETCH_PROVIDER_COMPLETE)
        try
        {
            completeFilter.addDataType(getActivity().getContentResolver().getType(Provider.baseUri()))
        }
        catch (e: IntentFilter.MalformedMimeTypeException)
        {
            // I'm a little more OK with this, as Provider.baseUri() is static.
            throw AssertionError(e)
        }

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(fetchCompleteReceiver, completeFilter)
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(fetchStatusReceiver, IntentFilter(FetcherAsync.FETCH_PROVIDER_STATUS))
    }

    override fun onPause()
    {
        super<ListFragment>.onPause()
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(fetchStatusReceiver)
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(fetchCompleteReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?)
    {
        inflater!!.inflate(R.menu.menu_provider_viewer, menu)
        val item = menu!!.findItem(R.id.search)
        val sv = item.getActionView() as SearchView
        sv.setOnQueryTextListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
    {
        when (item!!.getItemId())
        {
            R.id.search -> return true
            R.id.refresh ->
            {
                val i = Intent(getActivity(), javaClass<FetcherAsync>())
                i.setAction(FetcherAsync.FETCH_PROVIDER)
                i.setData(Provider.uri(1))   // Hard-code to the first (mangapanda) for now.
                i.putExtra(FetcherAsync.FETCH_BEHAVIOR, FetcherSync.FetchBehavior.ForceRefresh.toString())
                getActivity().startService(i)
                loading!!.setProgress(0)
                loading!!.show()
                return true
            }
            else -> return super<ListFragment>.onOptionsItemSelected(item)
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor>
    {
        val mangaPandaSeries = Provider.series(1)
        if (filter == null)
        {
            return CursorLoader(getActivity(), mangaPandaSeries, null, null, null, null)
        }
        else
        {
            return CursorLoader(getActivity(), mangaPandaSeries, null, Series.nameCol + " like ?", arrayOf<String>("%${filter}%"), null)
        }
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor)
    {
        adapter!!.changeCursor(data)
    }

    override fun onLoaderReset(loader: Loader<Cursor>)
    {
        adapter!!.changeCursor(null)
    }

    override fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long)
    {
        super<ListFragment>.onListItemClick(l, v, position, id)
        parent!!.loadSeries(Series.uri(id.toInt()))
    }

    override fun onQueryTextSubmit(query: String): Boolean
    {
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean
    {
        filter = if (!TextUtils.isEmpty(newText)) newText else null
        getLoaderManager().restartLoader(0, Bundle(), this)
        return true
    }

    companion object
    {

        private val filterArg = "filter"
    }
}
