package ninja.dudley.yamr.fetch;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import java.io.File;

import ninja.dudley.yamr.db.DBHelper;
import ninja.dudley.yamr.model.Chapter;
import ninja.dudley.yamr.model.Page;
import ninja.dudley.yamr.model.Provider;
import ninja.dudley.yamr.model.Series;

/**
 * Created by mdudley on 5/19/15.
 */
public abstract class Fetcher extends IntentService
{
    public static final String FETCH_PROVIDER = "ninja.dudley.yamr.fetch.Fetcher.FetchProvider";
    public static final String FETCH_SERIES = "ninja.dudley.yamr.fetch.Fetcher.FetchSeries";
    public static final String FETCH_CHAPTER = "ninja.dudley.yamr.fetch.Fetcher.FetchChapter";
    public static final String FETCH_PAGE = "ninja.dudley.yamr.fetch.Fetcher.FetchPage";

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
//        switch(intent.getAction())
//        {
//            case FETCH_PROVIDER:
//                break;
//            case FETCH_SERIES:
//                break;
//            case FETCH_CHAPTER
//                break;
//            case FETCH_PAGE:
//                break;
//        }
        Uri series = Uri.parse("content://" + DBHelper.AUTHORITY + "/series/2000");
        Series s = new Series(getContentResolver().query(series, null,null,null,null,null));
        fetchSeries(s);
    }
}
