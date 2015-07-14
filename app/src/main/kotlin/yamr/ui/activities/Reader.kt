package ninja.dudley.yamr.ui.activities

import android.app.Activity
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.RelativeLayout
import ninja.dudley.yamr.R
import ninja.dudley.yamr.ui.fragments.ChapterViewer
import ninja.dudley.yamr.ui.fragments.PageViewer
import ninja.dudley.yamr.ui.fragments.ProviderViewer
import ninja.dudley.yamr.ui.fragments.SeriesViewer
import ninja.dudley.yamr.ui.util.OrientationAware

public class Reader : Activity(), ProviderViewer.LoadSeries, SeriesViewer.LoadChapter, ChapterViewer.LoadPage,  OrientationAware.I
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super<Activity>.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)
        if (savedInstanceState == null)
        {
            val transaction = getFragmentManager().beginTransaction()
            val providerViewer = ProviderViewer()
            transaction.replace(R.id.reader, providerViewer)
            transaction.commit()
        }
    }

    override fun loadSeries(series: Uri)
    {
        val transaction = getFragmentManager().beginTransaction()
        val seriesViewer = SeriesViewer()
        val args = Bundle()
        args.putParcelable(SeriesViewer.ArgumentKey, series)
        seriesViewer.setArguments(args)
        transaction.replace(R.id.reader, seriesViewer)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun loadChapter(chapter: Uri)
    {
        val transaction = getFragmentManager().beginTransaction()
        val chapterViewer = ChapterViewer(chapter)
        transaction.replace(R.id.reader, chapterViewer)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun loadFirstPageOfChapter(chapter: Uri)
    {
        val transaction = getFragmentManager().beginTransaction()
        val pageViewer = PageViewer()
        val args = Bundle()
        args.putParcelable(PageViewer.ChapterArgumentKey, chapter)
        pageViewer.setArguments(args)
        transaction.replace(R.id.reader, pageViewer)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun loadPage(page: Uri)
    {
        val transaction = getFragmentManager().beginTransaction()
        val pageViewer = PageViewer()
        val args = Bundle()
        args.putParcelable(PageViewer.PageArgumentKey, page)
        pageViewer.setArguments(args)
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

    override fun onSaveInstanceState(outState: Bundle)
    {
        super<Activity>.onSaveInstanceState(outState)
    }
}
