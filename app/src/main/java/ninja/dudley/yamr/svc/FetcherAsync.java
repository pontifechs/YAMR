package ninja.dudley.yamr.svc;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;

import ninja.dudley.yamr.model.Chapter;
import ninja.dudley.yamr.model.Page;
import ninja.dudley.yamr.model.Provider;
import ninja.dudley.yamr.model.Series;
import ninja.dudley.yamr.svc.fetchers.MangaPandaFetcher;

/**
 * Created by mdudley on 6/11/15.
 */
public class FetcherAsync extends IntentService implements FetcherSync.NotifyStatus
{
    public static final String BASE = "ninja.dudley.yamr.fetch.FetcherSync";

    public static final String FETCH_BEHAVIOR = BASE + ".FetchBehavior";

    public static final String FETCH_PROVIDER = BASE + ".FetchProvider";
    public static final String FETCH_SERIES = BASE + ".FetchSeries";
    public static final String FETCH_CHAPTER = BASE + ".FetchChapter";
    public static final String FETCH_PAGE = BASE + ".FetchPage";
    public static final String FETCH_NEW = BASE + ".FetchStarter";

    public static final String FETCH_PROVIDER_STATUS = FETCH_PROVIDER + ".Status";
    public static final String FETCH_PROVIDER_COMPLETE = FETCH_PROVIDER + ".Complete";
    public static final String FETCH_SERIES_STATUS = FETCH_SERIES + ".Status";
    public static final String FETCH_SERIES_COMPLETE = FETCH_SERIES + ".Complete";
    public static final String FETCH_CHAPTER_STATUS = FETCH_CHAPTER + ".Status";
    public static final String FETCH_CHAPTER_COMPLETE = FETCH_CHAPTER + ".Complete";
    public static final String FETCH_PAGE_STATUS = FETCH_PAGE + ".Status";
    public static final String FETCH_PAGE_COMPLETE = FETCH_PAGE + ".Complete";
    public static final String FETCH_NEW_STATUS = FETCH_NEW + ".Status";
    public static final String FETCH_NEW_COMPLETE = FETCH_NEW + ".Complete";

    public FetcherAsync()
    {
        super("Fetcher");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        FetcherSync fetcher = new MangaPandaFetcher(getBaseContext());
        fetcher.register(this);
        Uri argument = intent.getData();

        FetcherSync.FetchBehavior behavior;
        try
        {
            behavior = FetcherSync.FetchBehavior.valueOf(intent.getStringExtra(FETCH_BEHAVIOR));
        }
        catch (IllegalArgumentException|NullPointerException e)
        {
            behavior = FetcherSync.FetchBehavior.LazyFetch;
        }

        switch (intent.getAction())
        {
            case FETCH_PROVIDER:
            {
                Provider p = new Provider(getContentResolver().query(argument, null, null, null, null));
                fetcher.fetchProvider(p, behavior);
                Intent complete = new Intent(FETCH_PROVIDER_COMPLETE);
                complete.setData(p.uri());
                LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(complete);
                break;
            }
            case FETCH_SERIES:
            {
                Series s = new Series(getContentResolver().query(argument, null, null, null, null));
                fetcher.fetchSeries(s, behavior);
                Intent complete = new Intent(FETCH_SERIES_COMPLETE);
                complete.setData(s.uri());
                LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(complete);
                break;
            }
            case FETCH_CHAPTER:
            {
                Chapter c = new Chapter(getContentResolver().query(argument, null, null, null, null));
                fetcher.fetchChapter(c, behavior);
                Intent complete = new Intent(FETCH_CHAPTER_COMPLETE);
                complete.setData(c.uri());
                LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(complete);
                break;
            }
            case FETCH_PAGE:
            {
                Page page = new Page(getContentResolver().query(argument, null, null, null, null));
                fetcher.fetchPage(page, behavior);
                Intent complete = new Intent(FETCH_PAGE_COMPLETE);
                complete.setData(page.uri());
                LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(complete);
                break;
            }

            case FETCH_NEW:
            {
                Provider p = new Provider(getContentResolver().query(argument, null, null, null, null));
                List<Uri> newChapters = fetcher.fetchNew(p);
                Intent complete = new Intent(FETCH_NEW_COMPLETE);
                ArrayList<String> uriStrings = new ArrayList<>();
                for (Uri u : newChapters)
                {
                    uriStrings.add(u.toString());
                }
                complete.putStringArrayListExtra(FETCH_NEW_COMPLETE, uriStrings);

                sendBroadcast(complete);
                break;
            }
        }
    }

    @Override
    public void notifyProviderStatus(float status)
    {
        Intent i = new Intent(FETCH_PROVIDER_STATUS);
        i.putExtra(FETCH_PROVIDER_STATUS, status);
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
    }

    @Override
    public void notifySeriesStatus(float status)
    {
        Intent i = new Intent(FETCH_SERIES_STATUS);
        i.putExtra(FETCH_SERIES_STATUS, status);
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
    }

    @Override
    public void notifyChapterStatus(float status)
    {
        Intent i = new Intent(FETCH_CHAPTER_STATUS);
        i.putExtra(FETCH_CHAPTER_STATUS, status);
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
    }

    @Override
    public void notifyPageStatus(float status)
    {
        Intent i = new Intent(FETCH_PAGE_STATUS);
        i.putExtra(FETCH_PAGE_STATUS, status);
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
    }

    @Override
    public void notifyNewStatus(float status)
    {
        Intent i = new Intent(FETCH_NEW_STATUS);
        i.putExtra(FETCH_NEW_STATUS, status);
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
    }
}
