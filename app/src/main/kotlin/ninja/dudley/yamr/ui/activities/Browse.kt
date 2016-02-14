package ninja.dudley.yamr.ui.activities

import android.app.Activity
import android.app.Fragment
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.RelativeLayout
import ninja.dudley.yamr.R
import ninja.dudley.yamr.model.*
import ninja.dudley.yamr.ui.fragments.*
import ninja.dudley.yamr.ui.util.OrientationAware

class Browse : Activity(), OrientationAware.I
{
    enum class FlowType
    {
        ProviderDown,
        Genre,
        Favorites,
        ContinueReading
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)
        if (savedInstanceState == null)
        {
            val b: Bundle = intent.extras
            val flow: FlowType = FlowType.valueOf(b.get(FlowKey) as String)

            val transaction = fragmentManager.beginTransaction()
            when (flow)
            {
                FlowType.ContinueReading ->
                {
                    val tracked = SeriesViewer.trackedSeries(this)
                    val pageViewer = PageViewer.newInstance(tracked!!.uri(), MangaElement.UriType.Series)
                    transaction.replace(R.id.reader, pageViewer)
                }
                FlowType.ProviderDown ->
                {
                    val providerViewer = ProviderSelector()
                    transaction.replace(R.id.reader, providerViewer)
                }
                FlowType.Genre ->
                {
                    val genreViewer = GenreSelector()
                    transaction.replace(R.id.reader, genreViewer)
                }
                FlowType.Favorites ->
                {
                    val favorites = Favorites()
                    transaction.replace(R.id.reader, favorites)
                }
            }
            transaction.commit()
        }
    }

    fun loadGenre(genre: Uri)
    {
        val transaction = fragmentManager.beginTransaction()
        val providerViewer = SeriesSelector.newInstance(SeriesSelector.FilterMode.Genre, genre)
        transaction.replace(R.id.reader, providerViewer)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    fun loadProvider(provider: Uri)
    {
        val transaction = fragmentManager.beginTransaction()
        val providerViewer = SeriesSelector.newInstance(SeriesSelector.FilterMode.Provider, provider)
        transaction.replace(R.id.reader, providerViewer)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    fun loadSeries(series: Uri)
    {
        val transaction = fragmentManager.beginTransaction()
        val seriesViewer = SeriesViewer.newInstance(series)
        transaction.replace(R.id.reader, seriesViewer)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    fun loadChapter(chapter: Uri)
    {
        val transaction = fragmentManager.beginTransaction()
        val chapterViewer = ChapterViewer.newInstance(chapter)
        transaction.replace(R.id.reader, chapterViewer)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    fun loadFirstPageOfChapter(chapter: Uri)
    {
        loadPageViewer(chapter,MangaElement.UriType.Chapter)
    }

    fun loadPage(page: Uri)
    {
        loadPageViewer(page, MangaElement.UriType.Page)
    }

    fun loadBookmarkFromSeries(series: Uri)
    {
        loadPageViewer(series, MangaElement.UriType.Series)
    }

    fun loadPageViewer(uri: Uri, type: MangaElement.UriType)
    {
        val transaction = fragmentManager.beginTransaction()
        val pageViewer = PageViewer.newInstance(uri, type)
        transaction.replace(R.id.reader, pageViewer)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    fun redraw(frag: Fragment)
    {
        fragmentManager.beginTransaction()
                .detach(frag)
                .attach(frag)
                .commitAllowingStateLoss();
    }

    override fun onConfigurationChanged(newConfig: Configuration)
    {
        OrientationAware.handleOrientationAware(this, newConfig)
        super.onConfigurationChanged(newConfig)
    }

    override fun onPortrait(newConfig: Configuration)
    {
        val layout = findViewById(R.id.reader) as RelativeLayout
        layout.removeAllViews()
    }

    override fun onLandscape(newConfig: Configuration)
    {
        val layout = findViewById(R.id.reader) as RelativeLayout
        layout.removeAllViews()
    }

    companion object
    {
        val FlowKey: String = "flow"
    }
}
