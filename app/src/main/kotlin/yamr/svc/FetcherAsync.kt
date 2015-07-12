package ninja.dudley.yamr.svc

import android.app.IntentService
import android.content.Intent
import android.net.Uri
import android.support.v4.content.LocalBroadcastManager

import java.util.ArrayList

import ninja.dudley.yamr.model.Chapter
import ninja.dudley.yamr.model.Page
import ninja.dudley.yamr.model.Provider
import ninja.dudley.yamr.model.Series
import ninja.dudley.yamr.svc.fetchers.MangaPandaFetcher

/**
 * Created by mdudley on 6/11/15.
 */
public class FetcherAsync : IntentService("Fetcher"), FetcherSync.NotifyStatus
{

    override fun onHandleIntent(intent: Intent)
    {
        val fetcher = MangaPandaFetcher(getBaseContext())
        fetcher.register(this)
        val argument = intent.getData()

        val behavior: FetcherSync.FetchBehavior
        try
        {
            behavior = FetcherSync.FetchBehavior.valueOf(intent.getStringExtra(FETCH_BEHAVIOR))
        }
        catch (e: IllegalArgumentException)
        {
            behavior = FetcherSync.FetchBehavior.LazyFetch
        }
        catch (e: NullPointerException)
        {
            behavior = FetcherSync.FetchBehavior.LazyFetch
        }


        when (intent.getAction())
        {
            FETCH_PROVIDER ->
            {
                val p = Provider(getContentResolver().query(argument, null, null, null, null))
                fetcher.fetchProvider(p, behavior)
                val complete = Intent(FETCH_PROVIDER_COMPLETE)
                complete.setData(p.uri())
                LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(complete)
            }
            FETCH_SERIES ->
            {
                val s = Series(getContentResolver().query(argument, null, null, null, null))
                fetcher.fetchSeries(s, behavior)
                val complete = Intent(FETCH_SERIES_COMPLETE)
                complete.setData(s.uri())
                LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(complete)
            }
            FETCH_CHAPTER ->
            {
                val c = Chapter(getContentResolver().query(argument, null, null, null, null))
                fetcher.fetchChapter(c, behavior)
                val complete = Intent(FETCH_CHAPTER_COMPLETE)
                complete.setData(c.uri())
                LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(complete)
            }
            FETCH_PAGE ->
            {
                val page = Page(getContentResolver().query(argument, null, null, null, null))
                fetcher.fetchPage(page, behavior)
                val complete = Intent(FETCH_PAGE_COMPLETE)
                complete.setData(page.uri())
                LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(complete)
            }

            FETCH_NEW ->
            {
                val p = Provider(getContentResolver().query(argument, null, null, null, null))
                val newChapters = fetcher.fetchNew(p)
                val complete = Intent(FETCH_NEW_COMPLETE)
                val uriStrings = ArrayList<String>()
                for (u in newChapters)
                {
                    uriStrings.add(u.toString())
                }
                complete.putStringArrayListExtra(FETCH_NEW_COMPLETE, uriStrings)
                sendBroadcast(complete)
            }
        }
    }

    override fun notifyProviderStatus(status: Float)
    {
        val i = Intent(FETCH_PROVIDER_STATUS)
        i.putExtra(FETCH_PROVIDER_STATUS, status)
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i)
    }

    override fun notifySeriesStatus(status: Float)
    {
        val i = Intent(FETCH_SERIES_STATUS)
        i.putExtra(FETCH_SERIES_STATUS, status)
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i)
    }

    override fun notifyChapterStatus(status: Float)
    {
        val i = Intent(FETCH_CHAPTER_STATUS)
        i.putExtra(FETCH_CHAPTER_STATUS, status)
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i)
    }

    override fun notifyPageStatus(status: Float)
    {
        val i = Intent(FETCH_PAGE_STATUS)
        i.putExtra(FETCH_PAGE_STATUS, status)
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i)
    }

    override fun notifyNewStatus(status: Float)
    {
        val i = Intent(FETCH_NEW_STATUS)
        i.putExtra(FETCH_NEW_STATUS, status)
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i)
    }

    companion object
    {
        public val BASE: String = "ninja.dudley.yamr.fetch.FetcherSync"

        public val FETCH_BEHAVIOR: String = BASE + ".FetchBehavior"

        public val FETCH_PROVIDER: String = BASE + ".FetchProvider"
        public val FETCH_SERIES: String = BASE + ".FetchSeries"
        public val FETCH_CHAPTER: String = BASE + ".FetchChapter"
        public val FETCH_PAGE: String = BASE + ".FetchPage"
        public val FETCH_NEW: String = BASE + ".FetchStarter"

        public val FETCH_PROVIDER_STATUS: String = FETCH_PROVIDER + ".Status"
        public val FETCH_PROVIDER_COMPLETE: String = FETCH_PROVIDER + ".Complete"
        public val FETCH_SERIES_STATUS: String = FETCH_SERIES + ".Status"
        public val FETCH_SERIES_COMPLETE: String = FETCH_SERIES + ".Complete"
        public val FETCH_CHAPTER_STATUS: String = FETCH_CHAPTER + ".Status"
        public val FETCH_CHAPTER_COMPLETE: String = FETCH_CHAPTER + ".Complete"
        public val FETCH_PAGE_STATUS: String = FETCH_PAGE + ".Status"
        public val FETCH_PAGE_COMPLETE: String = FETCH_PAGE + ".Complete"
        public val FETCH_NEW_STATUS: String = FETCH_NEW + ".Status"
        public val FETCH_NEW_COMPLETE: String = FETCH_NEW + ".Complete"
    }
}
