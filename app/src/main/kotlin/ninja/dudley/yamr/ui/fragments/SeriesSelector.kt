package ninja.dudley.yamr.ui.fragments

import android.app.Activity
import android.app.ListFragment
import android.app.LoaderManager
import android.app.ProgressDialog
import android.content.Context
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
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
import ninja.dudley.yamr.ui.activities.Browse

// Method reference functions
fun providerViewerStatus(providerViewer: Any, status: Float)
{
    (providerViewer as SeriesSelector).status(status)
}

fun providerViewerComplete(providerViewer: Any, provider: Provider)
{
    (providerViewer as SeriesSelector).complete(provider)
}

class SeriesSelector :
        ListFragment(), LoaderManager.LoaderCallbacks<Cursor>, SearchView.OnQueryTextListener
{
    enum class FilterMode
    {
        Provider,
        Genre,
        All
    }

    private var adapter: SimpleCursorAdapter? = null

    private var loading: ProgressDialog? = null
    private var filter: String? = null

    private var parent: Browse? = null

    private var filterMode: FilterMode? = null
    private var filterUri: Uri? = null

    override fun onAttach(activity: Activity?)
    {
        super.onAttach(activity)
        this.parent = activity as Browse?
    }

    fun status(status: Float)
    {
        if (!isAdded)
        {
            return
        }
        val percent = 100 * status
        loading!!.progress = percent.toInt()
    }

    fun complete(provider: Provider)
    {
        if (!isAdded)
        {
            return
        }
        this.filterUri = provider.uri()
        loaderManager.restartLoader(0, Bundle(), this@SeriesSelector)
        adapter!!.notifyDataSetChanged()
        loading!!.dismiss()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        setHasOptionsMenu(true)

        val layout = inflater.inflate(R.layout.fragment_provider_viewer, container, false) as LinearLayout

        this.filterMode = FilterMode.valueOf(arguments.getString(FILTER_ARG_KEY))
        this.filterUri = Uri.parse(arguments.getString(PROVIDER_ARG_KEY))

        if (filterMode == FilterMode.Provider)
        {
            val provider = Provider(activity.contentResolver.query(filterUri, null, null, null, null))
            FetcherAsync.fetchProvider(provider, this, FetcherAsync.Comms(::providerViewerComplete, ::providerViewerStatus))
        }

        adapter = SimpleCursorAdapter(activity,
                                     R.layout.simple_series_item,
                                     null,
                                     arrayOf(Series.nameCol),
                                     intArrayOf(R.id.series_name), 0)
        listAdapter = adapter

        loaderManager.initLoader(0, Bundle(), this)

        if (filterMode == FilterMode.Provider)
        {
            loading = ProgressDialog(activity)
            loading!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            loading!!.setTitle("Loading Series")
            loading!!.show()
        }
        return layout
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?)
    {
        inflater!!.inflate(R.menu.menu_provider_viewer, menu)
        val item = menu!!.findItem(R.id.search)
        val sv = item.actionView as SearchView
        sv.setOnQueryTextListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
    {
        when (item!!.itemId)
        {
            R.id.search -> return true
            R.id.refresh ->
            {
                val provider = Provider(activity.contentResolver.query(filterUri, null, null, null, null))
                FetcherAsync.fetchProvider(provider, this, FetcherAsync.Comms(::providerViewerComplete, ::providerViewerStatus), behavior = FetcherSync.Behavior.ForceRefresh)

                loading!!.progress = 0
                loading!!.show()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor>
    {
        when (filterMode!!)
        {
            FilterMode.Provider, FilterMode.All ->
            {
                //TODO:: Blechhh
                val seriesUri = filterUri!!.buildUpon().appendPath("series").build()
                if (filter == null) {
                    return CursorLoader(activity, seriesUri, null, null, null, null)
                } else {
                    return CursorLoader(activity, seriesUri, null, Series.nameCol + " like ?", arrayOf("%$filter%"), null)
                }
            }
            FilterMode.Genre ->
            {
                //TODO:: Blechhh
                val genreUri = filterUri!!.buildUpon().appendPath("series").build()
                if (filter == null) {
                    return CursorLoader(activity, genreUri, null, null, null, null)
                } else {
                    return CursorLoader(activity, genreUri, null, Series.nameCol + " like ?", arrayOf("%$filter%"), null)
                }
            }
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
        super.onListItemClick(l, v, position, id)
        parent!!.loadSeries(Series.uri(id.toInt()))
        val imm: InputMethodManager  = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v!!.windowToken, 0)
    }

    override fun onQueryTextSubmit(query: String): Boolean
    {
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean
    {
        filter = if (!TextUtils.isEmpty(newText)) newText else null
        loaderManager.restartLoader(0, Bundle(), this)
        return true
    }

    companion object
    {
        private val PROVIDER_ARG_KEY: String = "ProviderUriArg"
        private val FILTER_ARG_KEY: String = "FilterArg"

        fun newInstance(filter: FilterMode, uri: Uri): SeriesSelector
        {
            val providerViewer = SeriesSelector()
            val bundle = Bundle()
            bundle.putString(PROVIDER_ARG_KEY, uri.toString())
            bundle.putString(FILTER_ARG_KEY, filter.toString())
            providerViewer.arguments = bundle
            return providerViewer
        }
    }
}
