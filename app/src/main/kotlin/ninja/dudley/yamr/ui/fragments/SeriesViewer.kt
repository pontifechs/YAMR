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
import android.view.*
import android.widget.*
import ninja.dudley.yamr.R
import ninja.dudley.yamr.model.Chapter
import ninja.dudley.yamr.model.Series
import ninja.dudley.yamr.svc.FetcherAsync
import ninja.dudley.yamr.svc.FetcherSync

public class SeriesViewer : ListFragment(), LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemLongClickListener
{
    private var seriesUri: Uri? = null
    private var series: Series? = null

    private var adapter: SimpleCursorAdapter? = null

    private var fetchStatusReceiver: BroadcastReceiver? = null
    private var fetchCompleteReceiver: BroadcastReceiver? = null

    private var loading: ProgressDialog? = null

    public interface LoadChapter
    {
        public fun loadFirstPageOfChapter(chapter: Uri)

        public fun loadChapter(chapter: Uri)
    }

    private var parent: LoadChapter? = null
    override fun onAttach(activity: Activity?)
    {
        super<ListFragment>.onAttach(activity)
        this.parent = activity as LoadChapter?
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super<ListFragment>.onCreate(savedInstanceState)
        seriesUri = Uri.parse(getArguments().getString(uriArgKey))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        setHasOptionsMenu(true)

        val layout = inflater.inflate(R.layout.fragment_series_viewer, container, false) as LinearLayout
        val list = layout.findViewById(android.R.id.list) as ListView
        val headerContainer = inflater.inflate(R.layout.list_header_container, null) as FrameLayout
        list.addHeaderView(headerContainer)
        list.setOnItemLongClickListener(this);

        fetchStatusReceiver = object : BroadcastReceiver()
        {
            override fun onReceive(context: Context, intent: Intent)
            {
                val percent = 100 * intent.getFloatExtra(FetcherAsync.FETCH_SERIES_STATUS, 0.0f)
                loading!!.setProgress(percent.toInt())
            }
        }

        fetchCompleteReceiver = object : BroadcastReceiver()
        {
            override fun onReceive(context: Context, intent: Intent)
            {
                getLoaderManager().restartLoader(0, Bundle(), this@SeriesViewer)
                adapter!!.notifyDataSetChanged()
                if (loading != null)
                {
                    loading!!.dismiss()
                }
                getActivity().invalidateOptionsMenu()
                series = Series(getActivity().getContentResolver().query(intent.getData(), null, null, null, null))

                val card = SeriesCard.newInstance(series!!)
                val transaction = getFragmentManager().beginTransaction()
                transaction.replace(R.id.list_header_container, card)
                transaction.commit()
            }
        }

        series = Series(getActivity().getContentResolver().query(seriesUri, null, null, null, null))

        val seriesCard = SeriesCard.newInstance(series!!)
        val transaction = getFragmentManager().beginTransaction()
        transaction.replace(R.id.list_header_container, seriesCard)
        transaction.commit()

        val fetchSeries = Intent(getActivity(), javaClass<FetcherAsync>())
        fetchSeries.setAction(FetcherAsync.FETCH_SERIES)
        fetchSeries.setData(series!!.uri())
        getActivity().startService(fetchSeries)

        adapter = SimpleCursorAdapter(getActivity(), R.layout.chapter_item, null, arrayOf(Chapter.nameCol, Chapter.numberCol), intArrayOf(R.id.chapter_name, R.id.chapter_number), 0)
        setListAdapter(adapter)

        getLoaderManager().initLoader(0, Bundle(), this)

        loading = ProgressDialog(getActivity())
        loading!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        loading!!.setTitle("Loading Chapters for " + series!!.name!!)
        if (!series!!.fullyParsed)
        {
            loading!!.show()
        }
        return layout
    }

    override fun onResume()
    {
        super<ListFragment>.onResume()
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(fetchStatusReceiver, IntentFilter(FetcherAsync.FETCH_SERIES_STATUS))
        val completeFilter = IntentFilter(FetcherAsync.FETCH_SERIES_COMPLETE)
        try
        {
            completeFilter.addDataType(getActivity().getContentResolver().getType(Series.baseUri()))
        }
        catch (e: IntentFilter.MalformedMimeTypeException)
        {
            throw AssertionError(e)
        }

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(fetchCompleteReceiver, completeFilter)
    }

    override fun onPause()
    {
        super<ListFragment>.onPause()
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(fetchStatusReceiver)
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(fetchCompleteReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?)
    {
        inflater!!.inflate(R.menu.menu_series_viewer, menu)
        if (series != null && series!!.favorite)
        {
            menu!!.findItem(R.id.favorite).setIcon(R.drawable.ic_favorite_border_white_48dp)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
    {
        when (item!!.getItemId())
        {
            R.id.favorite ->
            {
                if (series!!.favorite)
                {
                    series!!.favorite = false
                }
                else
                {
                    series!!.favorite = true
                }
                getActivity().getContentResolver().update(series!!.uri(), series!!.getContentValues(), null, null)
                getActivity().invalidateOptionsMenu()
                return true
            }
            R.id.refresh ->
            {
                val i = Intent(getActivity(), javaClass<FetcherAsync>())
                i.setAction(FetcherAsync.FETCH_SERIES)
                i.setData(series!!.uri())
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
        return CursorLoader(getActivity(), series!!.chapters(), null, null, null, null)
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
        parent!!.loadFirstPageOfChapter(Chapter.uri(id.toInt()))
    }

    override fun onItemLongClick(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long): Boolean
    {
        parent!!.loadChapter(Chapter.uri(id.toInt()))
        return false;
    }

    companion object
    {
        fun newInstance(uri: Uri): SeriesViewer
        {
            val seriesViewer: SeriesViewer = SeriesViewer()
            val bundle: Bundle = Bundle()
            bundle.putString(uriArgKey, uri.toString())
            seriesViewer.setArguments(bundle)
            return seriesViewer
        }

        private val uriArgKey: String = "fuckandroid"
    }
}
