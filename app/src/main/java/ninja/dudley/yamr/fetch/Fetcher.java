package ninja.dudley.yamr.fetch;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import java.io.File;

import ninja.dudley.yamr.model.Chapter;
import ninja.dudley.yamr.model.Page;
import ninja.dudley.yamr.model.Provider;
import ninja.dudley.yamr.model.Series;

/**
 * Created by mdudley on 5/19/15.
 */
public abstract class Fetcher extends IntentService
{
    public static final String BASE = "ninja.dudley.yamr.fetch.Fetcher";
    public static final String FETCH_PROVIDER = BASE + ".FetchProvider";
    public static final String FETCH_SERIES = BASE + ".FetchSeries" ;
    public static final String FETCH_CHAPTER = BASE + ".FetchChapter";
    public static final String FETCH_PAGE = BASE + ".FetchPage";

    public static final String FETCH_PROVIDER_STATUS = FETCH_PROVIDER + ".Status";
    public static final String FETCH_PROVIDER_COMPLETE = FETCH_PROVIDER + ".Complete";
    public static final String FETCH_SERIES_STATUS = FETCH_SERIES + ".Status";
    public static final String FETCH_SERIES_COMPLETE = FETCH_SERIES + ".Complete";
    public static final String FETCH_CHAPTER_STATUS = FETCH_CHAPTER + ".Status";
    public static final String FETCH_CHAPTER_COMPLETE = FETCH_CHAPTER + ".Complete";
    public static final String FETCH_PAGE_COMPLETE = FETCH_CHAPTER + ".Complete";

    public File getStorageDirectory()
    {
        File root = Environment.getExternalStorageDirectory();
        File storage = new File(root.getAbsolutePath() + "/" + getFetcherIdentifier());
        if (!storage.exists() || !storage.mkdir())
        {
            throw new RuntimeException("Can't write to directory: " + storage.getAbsolutePath());
        }
        return storage;
    }

    public Fetcher(String name)
    {
        super(name);
    }

    public abstract String getFetcherIdentifier();

    public abstract void fetchProvider(Provider provider);

    public abstract void fetchSeries(Series series);

    public abstract void fetchChapter(Chapter chapter);

    public abstract void fetchPage(Page page);

    @Override
    protected void onHandleIntent(Intent intent)
    {
        Uri argument = intent.getData();
        switch (intent.getAction())
        {
            case FETCH_PROVIDER:
                Provider p = new Provider(getContentResolver().query(argument, null, null, null, null));
                fetchProvider(p);
                break;
            case FETCH_SERIES:
                Series s = new Series(getContentResolver().query(argument, null, null, null, null));
                fetchSeries(s);
                break;
            case FETCH_CHAPTER:
                Chapter c = new Chapter(getContentResolver().query(argument, null, null, null, null));
                fetchChapter(c);
                break;
            case FETCH_PAGE:
                Page page = new Page(getContentResolver().query(argument, null, null, null, null));
                fetchPage(page);
                break;
        }
    }
}
