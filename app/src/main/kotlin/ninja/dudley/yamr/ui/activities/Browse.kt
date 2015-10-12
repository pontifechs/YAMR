package ninja.dudley.yamr.ui.activities

import android.app.Activity
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.RelativeLayout
import ninja.dudley.yamr.R
import ninja.dudley.yamr.model.*
import ninja.dudley.yamr.ui.fragments.*
import ninja.dudley.yamr.ui.util.OrientationAware

public class Browse : Activity(), OrientationAware.I
{
    public var provider: Provider? = null

    enum class FlowType
    {
        ProviderDown,
        Favorites
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super<Activity>.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)
        if (savedInstanceState == null)
        {
            val b: Bundle = getIntent().getExtras()
            val flow: FlowType = FlowType.valueOf(b.get(FlowKey) as String)

            val transaction = getFragmentManager().beginTransaction()
            when (flow)
            {
                FlowType.ProviderDown ->
                {
                    val providerViewer = ProviderSelector()
                    transaction.replace(R.id.reader, providerViewer)
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

    //TODO:: Generify heritage?
    fun providerFromSeries(series: Uri)
    {
        val fetchedSeries = Series(getContentResolver().query(series, null, null, null, null))
        this.provider = Provider(getContentResolver().query(Provider.uri(fetchedSeries.providerId), null, null, null, null))
    }

    fun providerFromChapter(chapter: Uri)
    {
        val fetchedChapter = Chapter(getContentResolver().query(chapter, null, null, null, null))
        return providerFromSeries(Series.uri(fetchedChapter.seriesId))
    }

    fun providerFromPage(page: Uri)
    {
        val fetchedPage = Page(getContentResolver().query(page, null, null, null, null))
        return providerFromSeries(Chapter.uri(fetchedPage.chapterId))
    }


    fun loadProvider(provider: Uri)
    {
        this.provider = Provider(getContentResolver().query(provider, null, null, null, null))
        val transaction = getFragmentManager().beginTransaction()
        val providerViewer = ProviderViewer.newInstance(provider)
        transaction.replace(R.id.reader, providerViewer)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    fun loadSeries(series: Uri)
    {
        providerFromSeries(series)
        val transaction = getFragmentManager().beginTransaction()
        val seriesViewer = SeriesViewer.newInstance(series)
        transaction.replace(R.id.reader, seriesViewer)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    fun loadChapter(chapter: Uri)
    {
        providerFromChapter(chapter)
        val transaction = getFragmentManager().beginTransaction()
        val chapterViewer = ChapterViewer.newInstance(chapter)
        transaction.replace(R.id.reader, chapterViewer)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    fun loadFirstPageOfChapter(chapter: Uri)
    {
        providerFromChapter(chapter)
        loadPageViewer(chapter,MangaElement.UriType.Chapter)
    }

    fun loadPage(page: Uri)
    {
        providerFromPage(page)
        loadPageViewer(page, MangaElement.UriType.Page)
    }

    fun loadBookmarkFromSeries(series: Uri)
    {
        providerFromSeries(series)
        loadPageViewer(series, MangaElement.UriType.Series)
    }

    fun loadPageViewer(uri: Uri, type: MangaElement.UriType)
    {
        when (type)
        {
            MangaElement.UriType.Series -> {
                providerFromSeries(uri)
            }
            MangaElement.UriType.Chapter -> {
                providerFromChapter(uri)
            }
            MangaElement.UriType.Page -> {
                providerFromPage(uri)
            }
        }
        val transaction = getFragmentManager().beginTransaction()
        val pageViewer = PageViewer.newInstance(uri, type)
        transaction.replace(R.id.reader, pageViewer)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onConfigurationChanged(newConfig: Configuration)
    {
        OrientationAware.handleOrientationAware(this, newConfig)
        super<Activity>.onConfigurationChanged(newConfig)
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
        public val FlowKey: String = "flow"
    }
}
