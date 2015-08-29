package ninja.dudley.yamr.ui.fragments

import android.app.AlertDialog
import android.app.Fragment
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
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
import ninja.dudley.yamr.ui.util.TouchImageView
import ninja.dudley.yamr.util.Direction
import ninja.dudley.yamr.util.ProgressTracker

public fun pageViewerStatus(thiS: Any, status: Float)
{
    (thiS as PageViewer).status(status)
}

public fun pageViewerComplete(thiS: Any, page: Page)
{
    (thiS as PageViewer).complete(page)
}

public fun pageViewerFailure(thiS: Any)
{
    (thiS as PageViewer).failure()
}

public class PageViewer :
        Fragment(), TouchImageView.SwipeListener, View.OnClickListener, View.OnLongClickListener
{

    private var uri: Uri? = null
    private var type: MangaElement.UriType? = null

    private var page: Page? = null
    private var series: Series? = null
    private var progressTracker: ProgressTracker? = null

    private var direction: Direction = Direction.Next

    private var oldSystemUiVisibility: Int = 0;

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super<Fragment>.onCreate(savedInstanceState)

        if (savedInstanceState == null)
        {
            uri = Uri.parse(getArguments().getString(uriArgKey))
            type = MangaElement.UriType.valueOf(getArguments().getString(typeArgKey))
        }
        else
        {
            uri = Uri.parse(savedInstanceState.getString(uriArgKey))
            type = MangaElement.UriType.valueOf(savedInstanceState.getString(typeArgKey))
        }

        when (type)
        {
            MangaElement.UriType.Provider,
            MangaElement.UriType.Genre ->
                throw AssertionError("Invalid PageViewer Construction")
        }
    }

    public fun status(status: Float)
    {
        val loadingBar = getActivity().findViewById(R.id.page_loading_bar) as ProgressBar
        val percent = 100 * status
        loadingBar.setProgress(percent.toInt())
    }

    public fun complete(page: Page)
    {
        this.page = page
        val heritage = Heritage(getActivity().getContentResolver().query(page.heritage(), null, null, null, null))
        series = Series(getActivity().getContentResolver().query(Series.uri(heritage.seriesId), null, null, null, null))

        val imageView = getActivity().findViewById(R.id.imageView) as TouchImageView
        val d = Drawable.createFromPath(page.imagePath)
        if (d == null)
        {
            // TODO:: Handle no image or corrupt image.
        }

        imageView.setImageDrawable(d)
        val loadingBar = getActivity().findViewById(R.id.page_loading_bar) as ProgressBar
        loadingBar.setVisibility(View.INVISIBLE)

        val loadingText = getActivity().findViewById(R.id.page_loading_text) as TextView
        loadingText.setVisibility(View.VISIBLE)
        loadingText.setText("${heritage.seriesName}: ${heritage.chapterNumber}|${heritage.pageNumber}")

        val handler = Handler()
        handler.postDelayed(object : Runnable
        {
            override fun run()
            {
                loadingText.setVisibility(View.INVISIBLE)
            }
        }, 3000)


        if (series!!.favorite)
        {
            if (series!!.progressPageId == -1)
            {
                series!!.progressPageId = page.id
                series!!.progressChapterId = heritage.chapterId
            }

            if (progressTracker == null)
            {
                progressTracker = ProgressTracker(getActivity().getContentResolver(), series)
            }
            progressTracker!!.handleNextPage(page)

            if (series!!.updated)
            {
                series!!.updated = false
                getActivity().getContentResolver().update(series!!.uri(), series!!.getContentValues(), null, null)
            }
        }
    }

    public fun failure()
    {
        val loadingBar = getActivity().findViewById(R.id.page_loading_bar) as ProgressBar
        loadingBar.setVisibility(View.INVISIBLE)
        var dialog: AlertDialog
        if (direction == Direction.Next)
        {
            dialog = AlertDialog.Builder(getActivity())
                    .setTitle("E.N.D.")
                    .setMessage("You've reached the end of ${series!!.name}. Check back later for new chapters.")
                    .setNegativeButton("K.", null).create()
        }
        else
        {
            dialog = AlertDialog.Builder(getActivity())
                    .setTitle("Genesis")
                    .setMessage("You've reached the beginning of ${series!!.name}. This is as ancient as it gets.")
                    .setNegativeButton("K.", null).create()
        }
        // Wow, android. Just wow. Why the hell are you fucking with my shit?
        // See http://stackoverflow.com/questions/22794049/how-to-maintain-the-immersive-mode-in-dialogs
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialog.show()
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        val layout = inflater.inflate(R.layout.fragment_page_viewer, container, false) as RelativeLayout

        val imageView = layout.findViewById(R.id.imageView) as TouchImageView
        imageView.register(this)
        imageView.setOnClickListener(this)
        imageView.setOnLongClickListener(this)

        when (type)
        {
            MangaElement.UriType.Series ->
            {
                val fetcher = FetcherAsync.fetchPageFromSeries(getActivity().getContentResolver(), this, ::pageViewerComplete, ::pageViewerStatus)
                fetcher.execute(Series(getActivity().getContentResolver().query(uri, null, null, null, null)))
            }
            MangaElement.UriType.Chapter ->
            {
                val fetcher = FetcherAsync.fetchFirstPageFromChapter(getActivity().getContentResolver(), this, ::pageViewerComplete, ::pageViewerStatus)
                fetcher.execute(Chapter(getActivity().getContentResolver().query(uri, null, null, null, null)))
            }
            MangaElement.UriType.Page ->
            {
                val fetcher = FetcherAsync.fetchPage(getActivity().getContentResolver(), this, ::pageViewerComplete, ::pageViewerStatus)
                fetcher.execute(Page(getActivity().getContentResolver().query(uri, null, null, null, null)))
            }
        }
        return layout
    }

    override fun onResume()
    {
        super<Fragment>.onResume()

        oldSystemUiVisibility = getActivity().getWindow().getDecorView().getSystemUiVisibility()
        getActivity().getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    override fun onDetach() {
        super<Fragment>.onDetach()
        getActivity().getWindow().getDecorView().setSystemUiVisibility(oldSystemUiVisibility)
    }

    private fun nextPage()
    {
        if (page != null)
        {
            direction = Direction.Next
            val fetcher = FetcherAsync.fetchNextPage(getActivity().getContentResolver(), this, ::pageViewerComplete, ::pageViewerStatus, ::pageViewerFailure)
            fetcher.execute(page)
            val loadingBar = getActivity().findViewById(R.id.page_loading_bar) as ProgressBar
            loadingBar.setProgress(0)
            loadingBar.setVisibility(View.VISIBLE)
        }
    }

    private fun prevPage()
    {
        if (page != null)
        {
            direction = Direction.Prev
            val fetcher = FetcherAsync.fetchPrevPage(getActivity().getContentResolver(), this, ::pageViewerComplete, ::pageViewerStatus, ::pageViewerFailure)
            fetcher.execute(page)
            val loadingBar = getActivity().findViewById(R.id.page_loading_bar) as ProgressBar
            loadingBar.setProgress(0)
            loadingBar.setVisibility(View.VISIBLE)
        }
    }

    override fun onSwipeLeft()
    {
        val pref = PreferenceManager.getDefaultSharedPreferences(getActivity())
        if (pref.getBoolean(Settings.RTL_ENABLED_KEY, true))
        {
            nextPage()
        }
        else
        {
            prevPage()
        }
    }

    override fun onSwipeRight()
    {
        val pref = PreferenceManager.getDefaultSharedPreferences(getActivity())
        if (pref.getBoolean(Settings.RTL_ENABLED_KEY, true))
        {
            prevPage()
        }
        else
        {
            nextPage()
        }
    }

    override fun onClick(v: View)
    {
        nextPage()
    }

    override fun onLongClick(v: View): Boolean
    {
        if (getActivity().getActionBar()!!.isShowing())
        {
            getActivity().getActionBar()!!.hide()
        }
        else
        {
            getActivity().getActionBar()!!.show()
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super<Fragment>.onSaveInstanceState(outState)
        outState.putString(uriArgKey, page!!.uri().toString())
        outState.putString(typeArgKey, MangaElement.UriType.Page.name())
    }

    companion object
    {
        fun newInstance(uri: Uri, type: MangaElement.UriType): PageViewer
        {
            val viewer: PageViewer = PageViewer()
            val stupidAssBundle: Bundle = Bundle()
            stupidAssBundle.putString(uriArgKey, uri.toString())
            stupidAssBundle.putString(typeArgKey, type.name())
            viewer.setArguments(stupidAssBundle)
            return viewer
        }

        private val uriArgKey: String = "uri"
        private val typeArgKey: String = "type"
    }
}



