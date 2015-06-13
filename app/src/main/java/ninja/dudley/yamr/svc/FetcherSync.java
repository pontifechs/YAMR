package ninja.dudley.yamr.svc;

import android.content.Context;
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
public abstract class FetcherSync
{

    protected Context context;

    public FetcherSync(Context context)
    {
        this.context = context;
    }

    public interface NotifyStatus
    {
        void notifyProviderStatus(float status);

        void notifySeriesStatus(float status);

        void notifyChapterStatus(float status);
    }
    protected NotifyStatus listener;
    public void register(NotifyStatus listener)
    {
        this.listener = listener;
    }

    public abstract Provider fetchProvider(Provider provider);

    public abstract Series fetchSeries(Series series);

    public abstract Chapter fetchChapter(Chapter chapter);

    public abstract Page fetchPage(Page page);

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

    // TODO:: not a huge fan of this method. Will probably want to future-proof it as much as possible.
    protected void savePageImage(Page p)
    {
        Uri heritageQuery = p.uri().buildUpon().appendPath("heritage").build();
        Cursor heritage = context.getContentResolver().query(heritageQuery, null, null, null, null);
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
        context.getContentResolver().update(p.uri(), p.getContentValues(), null, null); // Save the path off
        heritage.close();
    }
}
