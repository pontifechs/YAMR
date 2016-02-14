package ninja.dudley.yamr.ui.fragments

import android.app.*
import android.content.Context
import android.content.CursorLoader
import android.content.DialogInterface
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import ninja.dudley.yamr.R
import ninja.dudley.yamr.model.Chapter
import ninja.dudley.yamr.model.Page
import ninja.dudley.yamr.model.Series
import ninja.dudley.yamr.svc.FetcherAsync
import ninja.dudley.yamr.svc.FetcherSync
import ninja.dudley.yamr.ui.activities.Browse
import ninja.dudley.yamr.ui.notifications.FetchAllProgress
import java.io.File

fun seriesViewerStatus(thiS: Any, status: Float)
{
    (thiS as SeriesViewer).status(status)
}

fun seriesViewerComplete(thiS: Any, series: Series)
{
    (thiS as SeriesViewer).complete(series)
}


class SeriesViewer :
        ListFragment(), LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemLongClickListener
{
    private var seriesUri: Uri? = null
    private var series: Series? = null

    private var adapter: SimpleCursorAdapter? = null

    private var loading: ProgressDialog? = null

    private var parent: Browse? = null
    override fun onAttach(activity: Activity?)
    {
        super.onAttach(activity)
        this.parent = activity as Browse?
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        seriesUri = Uri.parse(arguments.getString(uriArgKey))
    }

    fun status(status: Float)
    {
        if (!isAdded)
            return
        val percent = 100 * status
        loading!!.progress = percent.toInt()
    }

    fun complete(series: Series)
    {
        if(!isAdded)
        {
            return
        }
        loaderManager.restartLoader(0, Bundle(), this@SeriesViewer)
        adapter!!.notifyDataSetChanged()
        loading?.dismiss()
        activity.invalidateOptionsMenu()

        val card = SeriesCard.newInstance(series)
        val transaction = fragmentManager.beginTransaction()
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
        list.onItemLongClickListener = this;

        series = Series(activity.contentResolver.query(seriesUri, null, null, null, null))

        val seriesCard = SeriesCard.newInstance(series!!)
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.list_header_container, seriesCard)
        transaction.commit()

        FetcherAsync.fetchSeries(series!!, this, FetcherAsync.Comms(::seriesViewerComplete, ::seriesViewerStatus))

        adapter = SimpleCursorAdapter(activity,
                                      R.layout.chapter_item,
                                      null,
                                      arrayOf(Chapter.nameCol, Chapter.numberCol),
                                      intArrayOf(R.id.chapter_name, R.id.chapter_number),
                                      0)
        listAdapter = adapter

        loaderManager.initLoader(0, Bundle(), this)

        loading = ProgressDialog(activity)
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

        val trackedSeries = trackedSeries(activity)
        if (series != null &&  trackedSeries != null && series!!.uri() == trackedSeries.uri())
        {
            menu!!.findItem(R.id.read).setIcon(R.drawable.ic_label_outline_white_48dp)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
    {
        when (item!!.itemId)
        {
            R.id.read ->
            {
                trackSeries(series!!)

                activity.invalidateOptionsMenu()
                return true
            }
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
                activity.contentResolver.update(series!!.uri(), series!!.getContentValues(), null, null)
                activity.invalidateOptionsMenu()
                return true
            }
            R.id.refresh ->
            {
                FetcherAsync.fetchSeries(series!!, this, FetcherAsync.Comms(::seriesViewerComplete, ::seriesViewerStatus), behavior = FetcherSync.Behavior.ForceRefresh)

                loading!!.progress = 0
                loading!!.show()
                return true
            }
            R.id.reset_progress ->
            {
                series!!.progressChapterId = -1;
                series!!.progressPageId = -1;
                activity.contentResolver.update(series!!.uri(), series!!.getContentValues(), null, null)
                activity.invalidateOptionsMenu()
                return true
            }
            R.id.fetch_all ->
            {
                FetcherAsync.fetchEntireSeries(series!!, this,
                        FetcherAsync.Comms(
                                { thiS, series -> FetchAllProgress.notify(activity, "Fetching ${series.name} Complete!", 1.0f) },
                                { thiS, status -> FetchAllProgress.notify(activity, "Fetching ${series!!.name}", status) }))
                return true
            }
            R.id.delete ->
            {
                AlertDialog.Builder(activity)
                        .setTitle("Really Delete?")
                        .setMessage("This will delete all the images in this series. They can be re-downloaded")
                        .setPositiveButton("OK!", { dialogInterface: DialogInterface?, i: Int ->
                            val chapters = activity.contentResolver.query(series!!.chapters(), null, null, null, null)
                            while (chapters.moveToNext())
                            {
                                val chapter = Chapter(chapters, false)
                                val pages = activity.contentResolver.query(chapter.pages(), null, null, null, null)
                                while (pages.moveToNext())
                                {
                                    val page = Page(pages, false)
                                    if (page.imagePath.isNullOrEmpty())
                                    {
                                        continue;
                                    }
                                    val image = File(page.imagePath)
                                    image.delete()
                                    page.imagePath = null
                                    page.fullyParsed = false
                                    activity.contentResolver.update(page.uri(), page.getContentValues(), null, null)
                                }
                                pages.close()
                            }
                            parent!!.redraw(this)
                        })
                        .setNegativeButton("JK.", null).create().show()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor>
    {
        return CursorLoader(activity, series!!.chapters(), null, null, null, null)
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

    private fun trackSeries(series: Series)
    {
        val prefs = activity.getSharedPreferences(TrackedSeriesKey, Context.MODE_PRIVATE).edit()
        prefs.putString(TrackedSeriesKey, series.uri().toString())
        prefs.commit()
    }

    companion object
    {
        fun newInstance(uri: Uri): SeriesViewer
        {
            val seriesViewer: SeriesViewer = SeriesViewer()
            val bundle: Bundle = Bundle()
            bundle.putString(uriArgKey, uri.toString())
            seriesViewer.arguments = bundle
            return seriesViewer
        }

        private val uriArgKey: String = "fuckandroid"

        private val TrackedSeriesKey: String = "trackedSeriesKey"
        fun trackedSeries(context: Context): Series?
        {
            val prefs = context.getSharedPreferences(TrackedSeriesKey, Context.MODE_PRIVATE)
            val storedUri = prefs.getString(TrackedSeriesKey, null) ?: return null
            return Series(context.contentResolver.query(Uri.parse(storedUri), null, null, null, null))
        }
    }
}
