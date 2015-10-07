package ninja.dudley.yamr.ui.fragments

import android.app.Activity
import android.app.ListFragment
import android.app.LoaderManager
import android.app.ProgressDialog
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.text.TextUtils
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SearchView
import android.widget.SimpleCursorAdapter
import ninja.dudley.yamr.R
import ninja.dudley.yamr.model.Provider
import ninja.dudley.yamr.model.Series
import ninja.dudley.yamr.svc.FetcherAsync
import ninja.dudley.yamr.svc.FetcherSync

// Method reference functions
public fun providerViewerStatus(providerViewer: Any, status: Float)
{
    (providerViewer as ProviderViewer).status(status)
}

public fun providerViewerComplete(providerViewer: Any, provider: Provider)
{
    (providerViewer as ProviderViewer).complete(provider)
}

public class ProviderViewer :
        ListFragment(), LoaderManager.LoaderCallbacks<Cursor>, SearchView.OnQueryTextListener
{
    private var adapter: SimpleCursorAdapter? = null

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

    fun status(status: Float)
    {
        val percent = 100 * status
        loading!!.setProgress(percent.toInt())
    }

    fun complete(provider: Provider)
    {
        getLoaderManager().restartLoader(0, Bundle(), this@ProviderViewer)
        adapter!!.notifyDataSetChanged()
        loading!!.dismiss()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        setHasOptionsMenu(true)

        val layout = inflater.inflate(R.layout.fragment_provider_viewer, container, false) as LinearLayout

        val fetcher = FetcherAsync.fetchProvider(getActivity().getContentResolver(), this, ::providerViewerComplete, ::providerViewerStatus)
        val provider = Provider(getActivity().getContentResolver().query(Provider.uri(1), null, null, null, null))
        fetcher.execute(provider)

        adapter = SimpleCursorAdapter(getActivity(),
                                     R.layout.simple_series_item,
                                     null,
                                     arrayOf(Series.nameCol),
                                     intArrayOf(R.id.series_name), 0)
        setListAdapter(adapter)

        getLoaderManager().initLoader(0, Bundle(), this)
        loading = ProgressDialog(getActivity())
        loading!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        loading!!.setTitle("Loading Series")
        loading!!.show()
        return layout
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
                val fetcher = FetcherAsync.fetchProvider(getActivity().getContentResolver(), this, ::providerViewerComplete, ::providerViewerStatus, FetcherSync.Behavior.ForceRefresh)
                val provider = Provider(getActivity().getContentResolver().query(Provider.uri(1), null, null, null, null))
                fetcher.execute(provider)

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
            return CursorLoader(getActivity(), mangaPandaSeries, null, Series.nameCol + " like ?", arrayOf("%${filter}%"), null)
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
        val imm: InputMethodManager  = getActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v!!.getWindowToken(), 0)
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
}
