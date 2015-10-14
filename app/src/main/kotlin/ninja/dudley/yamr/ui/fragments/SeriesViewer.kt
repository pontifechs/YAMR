package ninja.dudley.yamr.ui.fragments

import android.app.Activity
import android.app.ListFragment
import android.app.LoaderManager
import android.app.ProgressDialog
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import ninja.dudley.yamr.R
import ninja.dudley.yamr.model.Chapter
import ninja.dudley.yamr.model.Series
import ninja.dudley.yamr.svc.FetcherAsync
import ninja.dudley.yamr.svc.FetcherSync
import ninja.dudley.yamr.ui.activities.Browse

public fun seriesViewerStatus(thiS: Any, status: Float)
{
    (thiS as SeriesViewer).status(status)
}

public fun seriesViewerComplete(thiS: Any, series: Series)
{
    (thiS as SeriesViewer).complete(series)
}


public class SeriesViewer :
        ListFragment(), LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemLongClickListener
{
    private var seriesUri: Uri? = null
    private var series: Series? = null

    private var adapter: SimpleCursorAdapter? = null

    private var loading: ProgressDialog? = null

    private var parent: Browse? = null
    override fun onAttach(activity: Activity?)
    {
        super<ListFragment>.onAttach(activity)
        this.parent = activity as Browse?
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super<ListFragment>.onCreate(savedInstanceState)
        seriesUri = Uri.parse(getArguments().getString(uriArgKey))
    }

    fun status(status: Float)
    {
        if (!isAdded())
            return
        val percent = 100 * status
        loading!!.setProgress(percent.toInt())
    }

    fun complete(series: Series)
    {
        if(!isAdded())
        {
            return
        }
        getLoaderManager().restartLoader(0, Bundle(), this@SeriesViewer)
        adapter!!.notifyDataSetChanged()
        loading?.dismiss()
        getActivity().invalidateOptionsMenu()

        val card = SeriesCard.newInstance(series)
        val transaction = getFragmentManager().beginTransaction()
        transaction.replace(R.id.list_header_container, card)
        transaction.commit()

        this.series = series;
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        setHasOptionsMenu(true)

        val layout = inflater.inflate(R.layout.fragment_series_viewer, container, false) as LinearLayout
        val list = layout.findViewById(android.R.id.list) as ListView
        val headerContainer = inflater.inflate(R.layout.list_header_container, null) as FrameLayout
        list.addHeaderView(headerContainer)
        list.setOnItemLongClickListener(this);

        series = Series(getActivity().getContentResolver().query(seriesUri, null, null, null, null))

        val seriesCard = SeriesCard.newInstance(series!!)
        val transaction = getFragmentManager().beginTransaction()
        transaction.replace(R.id.list_header_container, seriesCard)
        transaction.commit()

        val fetcher = FetcherAsync.fetchSeries(getActivity().getContentResolver(), this, ::seriesViewerComplete, ::seriesViewerStatus)
        fetcher.execute(series)

        adapter = SimpleCursorAdapter(getActivity(),
                                      R.layout.chapter_item,
                                      null,
                                      arrayOf(Chapter.nameCol, Chapter.numberCol),
                                      intArrayOf(R.id.chapter_name, R.id.chapter_number),
                                      0)
        setListAdapter(adapter)

        getLoaderManager().initLoader(0, Bundle(), this)

        loading = ProgressDialog(getActivity())
        loading!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        loading!!.setTitle("Loading Chapters for " + series!!.name)
        if (!series!!.fullyParsed)
        {
            loading!!.show()
        }
        return layout
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
                val fetcher = FetcherAsync.fetchSeries(getActivity().getContentResolver(), this,
                        ::seriesViewerComplete, ::seriesViewerStatus, FetcherSync.Behavior.ForceRefresh)
                fetcher.execute(series)

                loading!!.setProgress(0)
                loading!!.show()
                return true
            }
            R.id.reset_progress ->
            {
                series!!.progressChapterId = -1;
                series!!.progressPageId = -1;
                getActivity().getContentResolver().update(series!!.uri(), series!!.getContentValues(), null, null)
                getActivity().invalidateOptionsMenu()
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
