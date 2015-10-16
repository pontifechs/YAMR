package ninja.dudley.yamr.ui.fragments

import android.app.Activity
import android.app.AlertDialog
import android.app.Fragment
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import ninja.dudley.yamr.R
import ninja.dudley.yamr.model.*
import ninja.dudley.yamr.svc.FetcherAsync
import ninja.dudley.yamr.svc.util.LambdaAsyncTask
import ninja.dudley.yamr.ui.activities.Browse
import ninja.dudley.yamr.ui.activities.Settings
import ninja.dudley.yamr.ui.util.TouchImageView
import ninja.dudley.yamr.util.Direction
import ninja.dudley.yamr.util.ProgressTracker
import java.util.ArrayList

public fun pageViewerStatus(thiS: Any, status: Float)
{
    (thiS as PageViewer).status(status)
}

public fun pageViewerPageAcquired(thiS: Any, page: Page)
{
    (thiS as PageViewer).pageAcquired(page)
}

public fun pageViewerPageComplete(thiS: Any, page: Page)
{
    (thiS as PageViewer).pageComplete(page)
}

public fun pageViewerFailure(thiS: Any)
{
    (thiS as PageViewer).failure()
}

public class PageViewer : Fragment(), TouchImageView.SwipeListener
{
    // The page that is currently being displayed
    private var page: Page? = null

    private var progressTracker: ProgressTracker? = null

    private var parent: Browse? = null

    // Lifecycle Methods ---------------------------------------------------------------------------
    override fun onAttach(activity: Activity?)
    {
        super.onAttach(activity)
        parent = activity as Browse?
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        fun fetchUri(uri: Uri): Cursor
        {
            return activity.contentResolver.query(uri, null, null, null, null)
        }

        // Kick off the fetch.
        val uri = Uri.parse(arguments.getString(URI_ARG_KEY))
        val type = MangaElement.UriType.valueOf(arguments.getString(TYPE_ARG_KEY))


        when (type)
        {
            MangaElement.UriType.Series -> {
                val series = Series(fetchUri(uri))
                FetcherAsync.fetchPageFromSeries(activity.contentResolver, this,
                        ::pageViewerPageAcquired, ::pageViewerStatus).execute(series)
            }
            MangaElement.UriType.Chapter -> {
                val chapter = Chapter(fetchUri(uri))
                FetcherAsync.fetchFirstPageFromChapter(activity.contentResolver, this,
                        ::pageViewerPageAcquired, ::pageViewerStatus).execute(chapter)
            }
            MangaElement.UriType.Page -> {
                this.page = Page(fetchUri(uri))
                initProgressTracker(this.page!!)
                fetchPage()
            }
            else -> { throw UnsupportedOperationException("Can't start pageviewer with whatever type you passed, you dink.")}
        }

        val layout = inflater.inflate(R.layout.fragment_page_viewer, container, false) as RelativeLayout

        val touchView = layout.findViewById(R.id.imageView) as TouchImageView
        touchView.register(this)

        return layout
    }

    override fun onResume()
    {
        super.onResume() // Sigh....
        goFullscreen()
    }

    override fun onPause()
    {
        super.onPause() // Sigh....
        leaveFullscreen()
        preFetches.forEach{ it.cancel(true) }
    }

    // Page Loading --------------------------------------------------------------------------------
    public fun status(status: Float)
    {
        if (!isAdded)
        {
            return
        }
        showLoadingBar()
        val loadingBar = activity.findViewById(R.id.page_loading_bar) as ProgressBar
        val percent = 100 * status;
        loadingBar.progress = percent.toInt();
    }

    public fun pageAcquired(page: Page)
    {
        if (!isAdded)
        {
            return
        }
        this.page = page;
        initProgressTracker(page)
        fetchPage()
    }

    public fun pageComplete(page: Page)
    {
        if (!isAdded)
        {
            return
        }
        this.page = page
        hideLoadingBar()
        val touchImageView = activity.findViewById(R.id.imageView) as TouchImageView
        val d = Drawable.createFromPath(page.imagePath)
        touchImageView.setImageDrawable(d)
        progressTracker?.handleNewPage(page)

        val chapter = Chapter(activity.contentResolver.query(Chapter.uri(page.chapterId), null, null, null, null))
        showLoadingText("Chapter ${chapter.number}, Page ${page.number}")
    }

    public fun failure()
    {
        if (!isAdded)
        {
            return
        }
        val heritage = Heritage(activity.contentResolver.query(page!!.heritage(), null, null, null, null))
        val series = Series(activity.contentResolver.query(Series.uri(heritage.seriesId), null, null, null, null))

        val loadingBar = activity.findViewById(R.id.page_loading_bar) as ProgressBar
        loadingBar.visibility = View.INVISIBLE
        var dialog: AlertDialog
        if (readDirection == Direction.Next)
        {
            dialog = AlertDialog.Builder(activity)
                    .setTitle("E.N.D.")
                    .setMessage("You've reached the end of ${series.name}. Check back later for new chapters.")
                    .setNegativeButton("K.", null).create()
        }
        else
        {
            dialog = AlertDialog.Builder(activity)
                    .setTitle("Genesis")
                    .setMessage("You've reached the beginning of ${series.name}. This is as ancient as it gets.")
                    .setNegativeButton("K.", null).create()
        }
        // Wow, android. Just wow. Why the hell are you fucking with my shit?
        // See http://stackoverflow.com/questions/22794049/how-to-maintain-the-immersive-mode-in-dialogs
        dialog.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialog.show()
    }

    // State Changes -------------------------------------------------------------------------------
    private fun hideLoadingBar()
    {
        val loadingBar = activity?.findViewById(R.id.page_loading_bar) as ProgressBar?
        loadingBar?.visibility = View.INVISIBLE
    }

    private fun showLoadingBar()
    {
        val loadingBar = activity.findViewById(R.id.page_loading_bar) as ProgressBar
        loadingBar.visibility = View.VISIBLE
    }

    private fun hideLoadingText()
    {
        val loadingText = activity?.findViewById(R.id.page_loading_text) as TextView?
        loadingText?.visibility = View.INVISIBLE
    }

    private fun showLoadingText(text: String)
    {
        val loadingText = activity.findViewById(R.id.page_loading_text) as TextView
        loadingText.text = text
        loadingText.visibility = View.VISIBLE

        val handler = Handler()
        handler.postDelayed({this.hideLoadingText()}, 3000)
    }

    private var oldSystemUiVisibility: Int = -1
    private fun goFullscreen()
    {
        oldSystemUiVisibility = activity.window.decorView.systemUiVisibility
        activity.window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    private fun leaveFullscreen()
    {
        activity.window.decorView.systemUiVisibility = oldSystemUiVisibility
    }

    // Pre-Fetching and Navigation -----------------------------------------------------------------
    // Fetch the current page
    private fun fetchPage()
    {
        FetcherAsync.fetchPage(activity.contentResolver, this, ::pageViewerPageComplete,
                ::pageViewerStatus).execute(page)
        if (Settings.preFetchEnabled(activity))
        {
            loadPreFetches()
        }
    }

    private fun changePage(direction: Direction)
    {
        showLoadingBar()
        if (Settings.preFetchEnabled(activity))
        {
            changePagePreFetch(direction)
        }
        else
        {
            changePageSimple(direction)
        }
    }

    // Simple fetch prev or next based on direction
    private fun changePageSimple(direction: Direction)
    {
        when(direction)
        {
            Direction.Next ->
            {
                FetcherAsync.fetchNextPage(activity.contentResolver, this,
                        ::pageViewerPageComplete, ::pageViewerStatus, ::pageViewerFailure).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, page)
            }
            Direction.Prev ->
            {
                FetcherAsync.fetchPrevPage(activity.contentResolver, this,
                        ::pageViewerPageComplete, ::pageViewerStatus, ::pageViewerFailure).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, page)
            }
        }
    }

    private var preFetches: MutableList<LambdaAsyncTask<Page, Float, Page>> = ArrayList()
    private var readDirection: Direction = Direction.Next
    private fun changePagePreFetch(direction: Direction)
    {
        val oldDirection = readDirection
        readDirection = direction

        if (direction == oldDirection)
        {
            val nextFetch = preFetches.get(0)
            if (!nextFetch.finished)
            {
                nextFetch.complete = ::pageViewerPageComplete
                nextFetch.progress = ::pageViewerStatus
            }
            else
            {
                changePageSimple(direction)
            }
            // Slide the fetches down, and fetch the end
            for (i in 1..preFetches.size()-1)
            {
                preFetches.set(i-1, preFetches.get(i))
            }

            val endFetch = FetcherAsync.fetchOffsetFromPage(preFetches.size(), direction,
                    activity.contentResolver, this, null, null)
            endFetch.execute(page)
            preFetches.set(preFetches.size()-1, endFetch)
        }
        else
        {
            // Send us back to page acquired with the new direction, and we'll re-initialize the prefetches the same way
            when(direction)
            {
                Direction.Next ->
                {
                    FetcherAsync.fetchNextPage(activity.contentResolver, this, ::pageViewerPageAcquired,
                            ::pageViewerStatus, ::pageViewerFailure).execute(page)
                }
                Direction.Prev ->
                {
                     FetcherAsync.fetchPrevPage(activity.contentResolver, this, ::pageViewerPageAcquired,
                            ::pageViewerStatus, ::pageViewerFailure).execute(page)
                }
            }
        }
    }

    private fun loadPreFetches()
    {
        // Cancel any existing prefetches
        preFetches.forEach { it.cancel(true) }

        // Recreate the list
        val preFetchSize = Settings.preFetchSize(activity)
        preFetches = ArrayList<LambdaAsyncTask<Page, Float, Page>>(preFetchSize)

        for (i in 1..preFetchSize)
        {
            val nextFetch = FetcherAsync.fetchOffsetFromPage(i, readDirection,
                    activity.contentResolver, this, null, null)
            nextFetch.execute(page)
            preFetches.add(nextFetch)
        }
    }

    private fun initProgressTracker(page: Page)
    {
        val heritage = Heritage(activity.contentResolver.query(page.heritage(), null, null, null, null))
        val series = Series(activity.contentResolver.query(Series.uri(heritage.seriesId), null, null, null, null))
        if (series.progressPageId == -1)
        {
            series.progressPageId = page.id
            series.progressChapterId = heritage.chapterId
        }

        progressTracker = ProgressTracker(activity.contentResolver, series)
        progressTracker!!.handleNewPage(page)

        if (series.updated)
        {
            series.updated = false
            activity.contentResolver.update(series.uri(), series.getContentValues(), null, null)
        }
    }

    override fun onSwipeLeft()
    {
        if (Settings.rtlEnabled(activity))
        {
            changePage(Direction.Prev)
        }
        else
        {
            changePage(Direction.Next)
        }
    }

    override fun onSwipeRight()
    {
        if (Settings.rtlEnabled(activity))
        {
            changePage(Direction.Next)
        }
        else
        {
            changePage(Direction.Prev)
        }
    }

    companion object
    {
        private val URI_ARG_KEY: String = "URI_ARG"
        private val TYPE_ARG_KEY: String = "TYPE_ARG"

        fun newInstance(uri: Uri, type: MangaElement.UriType): PageViewer
        {
            val pageViewer = PageViewer()
            val bundle = Bundle()
            bundle.putString(URI_ARG_KEY, uri.toString())
            bundle.putString(TYPE_ARG_KEY, type.name())
            pageViewer.arguments = bundle
            return pageViewer
        }
    }
}
