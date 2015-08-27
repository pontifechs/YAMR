package ninja.dudley.yamr.ui.activities

import android.app.Activity
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.RelativeLayout
import ninja.dudley.yamr.R
import ninja.dudley.yamr.model.MangaElement
import ninja.dudley.yamr.ui.fragments.*
import ninja.dudley.yamr.ui.util.OrientationAware

public class Browse : Activity(), ProviderViewer.LoadSeries, SeriesViewer.LoadChapter,
        ChapterViewer.LoadPage,  Favorites.LoadSeriesAndChapter, OrientationAware.I
{

    enum class FlowType
    {
        ProviderAll,
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
                FlowType.ProviderAll ->
                {
                    val providerViewer = ProviderViewer()
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

    override fun loadSeries(series: Uri)
    {
        val transaction = getFragmentManager().beginTransaction()
        val seriesViewer = SeriesViewer.newInstance(series)
        transaction.replace(R.id.reader, seriesViewer)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun loadChapter(chapter: Uri)
    {
        val transaction = getFragmentManager().beginTransaction()
        val chapterViewer = ChapterViewer.newInstance(chapter)
        transaction.replace(R.id.reader, chapterViewer)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun loadFirstPageOfChapter(chapter: Uri)
    {
        loadPageViewer(chapter,MangaElement.UriType.Chapter)
    }

    override fun loadPage(page: Uri)
    {
        loadPageViewer(page, MangaElement.UriType.Page)
    }

    override fun loadFirstPageOfSeries(series: Uri)
    {
        loadPageViewer(series, MangaElement.UriType.Series)
    }

    fun loadPageViewer(uri: Uri, type: MangaElement.UriType)
    {
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
