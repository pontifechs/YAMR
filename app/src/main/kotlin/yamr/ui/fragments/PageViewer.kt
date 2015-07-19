package ninja.dudley.yamr.ui.fragments

import android.app.AlertDialog
import android.app.Fragment
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import ninja.dudley.yamr.R
import ninja.dudley.yamr.model.MangaElement
import ninja.dudley.yamr.model.Page
import ninja.dudley.yamr.model.Series
import ninja.dudley.yamr.svc.FetcherAsync
import ninja.dudley.yamr.svc.Navigation
import ninja.dudley.yamr.ui.util.TouchImageView
import ninja.dudley.yamr.util.ProgressTracker
import yamr.model.Heritage

public class PageViewer(private val uri: Uri, private val type: MangaElement.UriType) : Fragment(), TouchImageView.SwipeListener, View.OnClickListener, View.OnLongClickListener
{
    private var page: Page? = null
    private var series: Series? = null
    private var progressTracker: ProgressTracker? = null

    private var loadPageCompleteReceiver: BroadcastReceiver? = null
    private var prevPageFailedReceiver: BroadcastReceiver? = null
    private var nextPageFailedReceiver: BroadcastReceiver? = null
    private var pageLoadStatusReceiver: BroadcastReceiver? = null

    init
    {
        when (type)
        {
            MangaElement.UriType.Provider,
            MangaElement.UriType.Genre->
                throw AssertionError("Invalid PageViewer Construction")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        // Go full-screen
        //        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        //        getActivity().getActionBar().setBackgroundDrawable(new ColorDrawable(R.color.black_overlay));
        getActivity().getActionBar()!!.hide()

        val layout = inflater.inflate(R.layout.fragment_page_viewer, container, false) as RelativeLayout

        loadPageCompleteReceiver = object : BroadcastReceiver()
        {
            override fun onReceive(context: Context, intent: Intent)
            {
                page = Page(getActivity().getContentResolver().query(intent.getData(), null, null, null, null))
                val heritage = Heritage(getActivity().getContentResolver().query(page!!.heritage(), null, null, null, null))
                series = Series(getActivity().getContentResolver().query(Series.uri(heritage.seriesId), null, null, null, null))

                val imageView = getActivity().findViewById(R.id.imageView) as TouchImageView
                val d = Drawable.createFromPath(page!!.imagePath)
                if (d == null)
                {
                    // TODO:: Handle no image or corrupt image.
                }

                imageView.setImageDrawable(d)
                val loadingBar = getActivity().findViewById(R.id.page_loading) as ProgressBar
                loadingBar.setVisibility(View.INVISIBLE)

                if (series!!.favorite)
                {
                    if (series!!.progressPageId == -1)
                    {
                        series!!.progressPageId = page!!.id
                        series!!.progressChapterId = heritage.chapterId
                    }

                    if (progressTracker == null)
                    {
                        progressTracker = ProgressTracker(getActivity().getContentResolver(), series)
                    }
                    progressTracker!!.handleNextPage(page!!)
                }
            }
        }

        nextPageFailedReceiver = object : BroadcastReceiver()
        {
            override fun onReceive(context: Context, intent: Intent)
            {
                val loadingBar = getActivity().findViewById(R.id.page_loading) as ProgressBar
                loadingBar.setVisibility(View.INVISIBLE)
                // TODO:: Make sure series is available for this
                AlertDialog.Builder(getActivity()).setTitle("E.N.D.").setMessage("You've reached the end of  + series.getName() + . Check back later for new chapters.").setNegativeButton("K.", null).show()
            }
        }

        prevPageFailedReceiver = object : BroadcastReceiver()
        {
            override fun onReceive(context: Context, intent: Intent)
            {
                val loadingBar = getActivity().findViewById(R.id.page_loading) as ProgressBar
                loadingBar.setVisibility(View.INVISIBLE)
                // TODO:: Make sure series is available for this
                AlertDialog.Builder(getActivity()).setTitle("Genesis").setMessage("You've reached the beginning of  + series.getName() + . Nothing older").setNegativeButton("K.", null).show()
            }
        }

        pageLoadStatusReceiver = object : BroadcastReceiver()
        {
            override fun onReceive(context: Context, intent: Intent)
            {
                val loadingBar = getActivity().findViewById(R.id.page_loading) as ProgressBar
                val percent = 100 * intent.getFloatExtra(FetcherAsync.FETCH_PAGE_STATUS, 0.0f)
                loadingBar.setProgress(percent.toInt())
            }
        }

        val imageView = layout.findViewById(R.id.imageView) as TouchImageView
        imageView.register(this)
        imageView.setOnClickListener(this)
        imageView.setOnLongClickListener(this)

        when (type)
        {
            MangaElement.UriType.Series ->
            {
                val fetchPage = Intent(getActivity(), javaClass<Navigation>())
                fetchPage.setAction(Navigation.PAGE_FROM_SERIES)
                fetchPage.setData(uri)
                getActivity().startService(fetchPage)
            }
            MangaElement.UriType.Chapter ->
            {
                val fetchChapter = Intent(getActivity(), javaClass<Navigation>())
                fetchChapter.setAction(Navigation.FIRST_PAGE_FROM_CHAPTER)
                fetchChapter.setData(uri)
                getActivity().startService(fetchChapter)
            }
            MangaElement.UriType.Page ->
            {
                val fetchPage = Intent(getActivity(), javaClass<FetcherAsync>())
                fetchPage.setAction(FetcherAsync.FETCH_PAGE)
                fetchPage.setData(uri)
                getActivity().startService(fetchPage)
            }
        }
        return layout
    }

    override fun onResume()
    {
        super<Fragment>.onPause()
        val pageCompleteFilter = IntentFilter()
        pageCompleteFilter.addAction(Navigation.FIRST_PAGE_FROM_CHAPTER_COMPLETE)
        pageCompleteFilter.addAction(Navigation.NEXT_PAGE_COMPLETE)
        pageCompleteFilter.addAction(Navigation.PREV_PAGE_COMPLETE)
        pageCompleteFilter.addAction(Navigation.PAGE_FROM_SERIES_COMPLETE)
        pageCompleteFilter.addAction(FetcherAsync.FETCH_PAGE_COMPLETE)
        try
        {
            pageCompleteFilter.addDataType(getActivity().getContentResolver().getType(Page.baseUri()))
        }
        catch (e: IntentFilter.MalformedMimeTypeException)
        {
            // I'm a little more OK with this, as Provider.baseUri() is static.
            throw AssertionError(e)
        }

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(loadPageCompleteReceiver, pageCompleteFilter)

        val prevFailedFilter = IntentFilter()
        prevFailedFilter.addAction(Navigation.PREV_PAGE_DOESNT_EXIST)
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(prevPageFailedReceiver, prevFailedFilter)

        val nextFailedFilter = IntentFilter()
        nextFailedFilter.addAction(Navigation.NEXT_PAGE_DOESNT_EXIST)
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(nextPageFailedReceiver, nextFailedFilter)
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(pageLoadStatusReceiver, IntentFilter(FetcherAsync.FETCH_PAGE_STATUS))
    }

    override fun onPause()
    {
        super<Fragment>.onResume()
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(loadPageCompleteReceiver)
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(prevPageFailedReceiver)
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(nextPageFailedReceiver)
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(pageLoadStatusReceiver)
        getActivity().getActionBar()!!.show()
    }

    private fun nextPage()
    {
        if (page != null)
        {
            val nextIntent = Intent(getActivity(), javaClass<Navigation>())
            nextIntent.setAction(Navigation.NEXT_PAGE)
            nextIntent.setData(page!!.uri())
            getActivity().startService(nextIntent)
            val loadingBar = getActivity().findViewById(R.id.page_loading) as ProgressBar
            loadingBar.setProgress(0)
            loadingBar.setVisibility(View.VISIBLE)
        }
    }

    private fun prevPage()
    {
        if (page != null)
        {
            val nextIntent = Intent(getActivity(), javaClass<Navigation>())
            nextIntent.setAction(Navigation.PREV_PAGE)
            nextIntent.setData(page!!.uri())
            getActivity().startService(nextIntent)
            val loadingBar = getActivity().findViewById(R.id.page_loading) as ProgressBar
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
}



