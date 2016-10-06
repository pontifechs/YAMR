package ninja.dudley.yamr.ui.fragments

import android.app.Activity
import android.app.AlertDialog
import android.app.Fragment
import android.content.DialogInterface
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.net.Uri
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
import ninja.dudley.yamr.ui.activities.Browse
import ninja.dudley.yamr.ui.activities.Settings
import ninja.dudley.yamr.ui.util.TouchImageView
import ninja.dudley.yamr.util.Direction
import ninja.dudley.yamr.util.ProgressTracker
import org.acra.ACRA
import java.util.*

private fun pageViewerStatus(thiS: Any, status: Float)
{
    (thiS as PageViewer).status(status)
}

private fun pageViewerPageAcquired(thiS: Any, page: Page)
{
    (thiS as PageViewer).pageAcquired(page)
}

private fun pageViewerPageComplete(thiS: Any, page: Page)
{
    (thiS as PageViewer).pageComplete(page)
}

private fun pageViewerFailure(thiS: Any, e: Exception)
{
    (thiS as PageViewer).failure(e)
}

private fun pageCompleteNop(thiS: Any, page: Page) {}

private fun pageStatusNop(thiS: Any, status: Float) {}

private fun pageFailNop(thiS: Any, e: Exception) {}

class PageViewer : Fragment(), TouchImageView.SwipeListener
{
    // The page that is currently being displayed
    private var page: Page? = null

    private var progressTracker: ProgressTracker? = null

    private var parent: Browse? = null

    private var readDirection: Direction = Direction.Next

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
                FetcherAsync.fetchPageFromSeries(series, this,
                        FetcherAsync.Comms(::pageViewerPageAcquired, ::pageViewerStatus))
            }
            MangaElement.UriType.Chapter -> {
                val chapter = Chapter(fetchUri(uri))
                FetcherAsync.fetchFirstPageFromChapter(chapter, this,
                        FetcherAsync.Comms(::pageViewerPageAcquired, ::pageViewerStatus))
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
    }

    // Page Loading --------------------------------------------------------------------------------
    fun status(status: Float)
    {
        if (!isAdded)
        {
            return
        }
        resetLoadingBar()
        val loadingBar = activity.findViewById(R.id.page_loading_bar) as ProgressBar
        val percent = 100 * status
        loadingBar.progress = percent.toInt()
    }

    fun pageAcquired(page: Page)
    {
        if (!isAdded)
        {
            return
        }
        this.page = page
        initProgressTracker(page)
        fetchPage()
    }

    fun pageComplete(page: Page)
    {
        if (!isAdded)
        {
            return
        }
        this.page = page
        hideLoadingBar()
        val touchImageView = activity.findViewById(R.id.imageView) as TouchImageView
        val d: Drawable
        if (page.imagePath == null)
        {
            d = resources.getDrawable(R.drawable.panic, null)
        }
        else
        {
            d = Drawable.createFromPath(page.imagePath)
        }
        touchImageView.setImageDrawable(d)
        progressTracker?.handleNewPage(page)

        val chapter = Chapter(activity.contentResolver.query(Chapter.uri(page.chapterId), null, null, null, null))
        showLoadingText("Chapter ${chapter.number}, Page ${page.number}")
    }

    fun failure(e: Exception)
    {
        if (!isAdded)
        {
            return
        }

        val heritage = Heritage(activity.contentResolver.query(page!!.heritage(), null, null, null, null))
        val series = Series(activity.contentResolver.query(Series.uri(heritage.seriesId), null, null, null, null))

        val loadingBar = activity.findViewById(R.id.page_loading_bar) as ProgressBar
        loadingBar.visibility = View.INVISIBLE
        val dialog: AlertDialog

        if (e !is NoSuchElementException)
        {
            dialog = AlertDialog.Builder(activity)
                    .setTitle("Something went wrong!")
                    .setMessage(e.message)
                    .setPositiveButton("Report!", { dialogInterface: DialogInterface?, i: Int ->
                        val reporter = ACRA.getErrorReporter()
                        reporter.handleSilentException(e)
                    })
                    .setNegativeButton("K.", null).create()
        }
        else if (readDirection == Direction.Next)
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
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        dialog.show()
    }

    // State Changes -------------------------------------------------------------------------------
    private fun hideLoadingBar()
    {
        val loadingBar = activity?.findViewById(R.id.page_loading_bar) as ProgressBar?
        loadingBar?.visibility = View.INVISIBLE
    }

    private fun resetLoadingBar()
    {
        val loadingBar = activity.findViewById(R.id.page_loading_bar) as ProgressBar
        loadingBar.progress = 0
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
        FetcherAsync.fetchPage(page!!, this, FetcherAsync.Comms(::pageViewerPageComplete, ::pageViewerStatus))
        if (Settings.preFetchEnabled(activity))
        {
            loadPreFetches(FetcherAsync.LowPriority)
        }
    }

    private fun changePage(direction: Direction)
    {
        resetLoadingBar()
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
                FetcherAsync.fetchNextPage(page!!, this, FetcherAsync.Comms(::pageViewerPageComplete, ::pageViewerStatus, ::pageViewerFailure), priority = FetcherAsync.HighPriority)
            }
            Direction.Prev ->
            {
                FetcherAsync.fetchPrevPage(page!!, this, FetcherAsync.Comms(::pageViewerPageComplete, ::pageViewerStatus, ::pageViewerFailure), priority = FetcherAsync.HighPriority)
            }
        }
    }

    private fun changePagePreFetch(direction: Direction)
    {
        val oldDirection = readDirection
        readDirection = direction

        if (direction == oldDirection)
        {
            // Complete the requested Fetch
            changePageSimple(direction)

            // Request the furthest out fetch.
            FetcherAsync.fetchOffsetFromPage(page!!, Settings.preFetchSize(activity), direction, activity.contentResolver, FetcherAsync.Comms(::pageCompleteNop, ::pageStatusNop, ::pageFailNop))
        }
        else
        {
            when(direction)
            {
                Direction.Next ->
                {
                    FetcherAsync.fetchNextPage(page!!, this, FetcherAsync.Comms(::pageViewerPageComplete, ::pageViewerStatus, ::pageViewerFailure), FetcherAsync.HighPriority)
                    loadPreFetches(FetcherAsync.MediumPriority)
                }
                Direction.Prev ->
                {
                    FetcherAsync.fetchPrevPage(page!!, this, FetcherAsync.Comms(::pageViewerPageComplete, ::pageViewerStatus, ::pageViewerFailure), FetcherAsync.HighPriority)
                    loadPreFetches(FetcherAsync.MediumPriority)
                }
            }
        }
    }

    private fun loadPreFetches(priority: Int)
    {
        for (i in 1..Settings.preFetchSize(activity))
        {
            FetcherAsync.fetchOffsetFromPage(page!!, i, readDirection, this, FetcherAsync.Comms(::pageCompleteNop, ::pageStatusNop, ::pageFailNop), priority - i)
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
            bundle.putString(TYPE_ARG_KEY, type.name)
            pageViewer.arguments = bundle
            return pageViewer
        }
    }
}
