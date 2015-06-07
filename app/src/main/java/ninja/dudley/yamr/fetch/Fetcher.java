package ninja.dudley.yamr.fetch;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

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

    public Fetcher(String name)
    {
        super(name);
    }

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

    private static String stripBadCharsForFile(String file)
    {
        return file.replaceAll("[\\\\/\\?%\\*:\\|\"<>]", ".");
    }

    private static String formatFloat(float f)
    {
        if (f == (int) f)
        {
            return String.format("%d", (int)f);
        }
        else
        {
            return String.format("%s", f);
        }
    }

    // TODO:: not a huge fan of this method
    protected void savePageImage(Page p)
    {
        Uri heritageQuery = p.uri().buildUpon().appendPath("heritage").build();
        Cursor heritage = getContentResolver().query(heritageQuery, null, null, null, null);
        heritage.moveToFirst();

        int providerNameCol = heritage.getColumnIndex(DBHelper.PageHeritageViewEntry.COLUMN_PROVIDER_NAME);
        int seriesNameCol = heritage.getColumnIndex(DBHelper.PageHeritageViewEntry.COLUMN_SERIES_NAME);
        int chapterNumberCol = heritage.getColumnIndex(DBHelper.PageHeritageViewEntry.COLUMN_CHAPTER_NUMBER);
        int pageNumberCol = heritage.getColumnIndex(DBHelper.PageHeritageViewEntry.COLUMN_PAGE_NUMBER);

        String providerName = heritage.getString(providerNameCol);
        String seriesName = heritage.getString(seriesNameCol);
        float chapterNumber = heritage.getFloat(chapterNumberCol);
        float pageNumber = heritage.getFloat(pageNumberCol);

        File root = Environment.getExternalStorageDirectory();
        String chapterPath = root.getAbsolutePath() +
                "/" + stripBadCharsForFile(providerName) +
                "/" + stripBadCharsForFile(seriesName) +
                "/" + formatFloat(chapterNumber);

        File chapterDirectory = new File(chapterPath);
        chapterDirectory.mkdirs();
        String pagePath = chapterDirectory +
                "/" + formatFloat(pageNumber) + ".png";

        FileOutputStream out = null;
        try
        {
            URL url = new URL(p.getImageUrl());
            Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            out = new FileOutputStream(pagePath);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        catch (MalformedURLException e)
        {
            // TODO:: better error handling/checking
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
             // TODO:: better error handling/checking
            throw new RuntimeException(e);
        }
        finally
        {
            try
            {
                out.close();
            }
            catch (IOException e)
            {
                //TODO:: better error handling/checking
                throw new RuntimeException(e);
            }
        }
        p.setImagePath(pagePath);
        getContentResolver().update(p.uri(), p.getContentValues(), null, null); // Save the path off
        heritage.close();
    }
}
