package ninja.dudley.yamr.svc;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import ninja.dudley.yamr.db.DBHelper;
import ninja.dudley.yamr.model.Chapter;
import ninja.dudley.yamr.model.Genre;
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

        void notifyPageStatus(float status);

        void notifyNewStatus(float status);
    }
    protected NotifyStatus listener;
    public void register(NotifyStatus listener)
    {
        this.listener = listener;
    }

    public enum FetchBehavior
    {
        LazyFetch("LazyFetch"),
        ForceRefresh("ForceRefresh");

        private final String val;

        FetchBehavior(String val)
        {
            this.val = val;
        }

        @Override
        public String toString()
        {
            return val;
        }
    }

    public Provider fetchProvider(Provider provider)
    {
        return fetchProvider(provider, FetchBehavior.LazyFetch);
    }
    public abstract Provider fetchProvider(Provider provider, FetchBehavior behavior);

    public Series fetchSeries(Series series)
    {
        return fetchSeries(series, FetchBehavior.LazyFetch);
    }
    public abstract Series fetchSeries(Series series, FetchBehavior behavior);

    public Chapter fetchChapter(Chapter chapter)
    {
       return fetchChapter(chapter, FetchBehavior.LazyFetch);
    }
    public abstract Chapter fetchChapter(Chapter chapter, FetchBehavior behavior);

    public Page fetchPage(Page page)
    {
        return fetchPage(page, FetchBehavior.LazyFetch);
    }
    public abstract Page fetchPage(Page page, FetchBehavior behavior);

    public abstract List<Uri> fetchNew(Provider provider);

    protected boolean providerExists(String url)
    {
        Cursor c = context.getContentResolver().query(Provider.baseUri(), null, null, new String[]{url}, null);
        boolean ret = c.getCount() > 0;
        c.close();
        return ret;
    }

    protected boolean seriesExists(String url)
    {
        Cursor c = context.getContentResolver().query(Series.baseUri(), null, null, new String[]{url}, null);
        boolean ret = c.getCount() > 0;
        c.close();
        return ret;
    }

    protected boolean chapterExists(String url)
    {
        Cursor c = context.getContentResolver().query(Chapter.baseUri(), null, null, new String[]{url}, null);
        boolean ret = c.getCount() > 0;
        c.close();
        return ret;
    }

    protected boolean pageExists(String url)
    {
        Cursor c = context.getContentResolver().query(Page.baseUri(), null, null, new String[]{url}, null);
        boolean ret = c.getCount() > 0;
        c.close();
        return ret;
    }

    protected boolean genreExists(String name)
    {
        Cursor c = context.getContentResolver().query(Genre.baseUri(), null, null, new String[]{name}, null);
        boolean ret = c.getCount() > 0;
        c.close();
        return ret;
    }

    private static String stripBadCharsForFile(String file)
    {
        return file.replaceAll("[ \\\\/\\?%\\*:\\|\"<>]", ".");
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

    private void downloadImage(String imageUrl, String imagePath)
    {
        InputStream in = null;
        ByteArrayOutputStream out = null;
        int count;
        try
        {
            URL url = new URL(imageUrl);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setDoInput(true);
            conn.connect();
            in = conn.getInputStream();
            out = new ByteArrayOutputStream();

            int length = conn.getContentLength();
            int done = 0;
            byte data[] = new byte[1024];
            while ((count = in.read(data)) != -1) {
                done += count;

                if (listener != null)
                {
                    listener.notifyPageStatus(done / (float) length);
                }

                out.write(data, 0, count);
            }

            // flushing output
            out.flush();

            Bitmap bmp = BitmapFactory.decodeByteArray(out.toByteArray(), 0, length);
            FileOutputStream fileOut = new FileOutputStream(imagePath);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fileOut);
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
                "/." + stripBadCharsForFile(providerName) +
                "/" + stripBadCharsForFile(seriesName) +
                "/" + formatFloat(chapterNumber);

        File chapterDirectory = new File(chapterPath);
        chapterDirectory.mkdirs();
        String pagePath = chapterDirectory +
                "/" + formatFloat(pageNumber) + ".png";

        downloadImage(p.getImageUrl(), pagePath);

        p.setImagePath(pagePath);
        context.getContentResolver().update(p.uri(), p.getContentValues(), null, null); // Save the path off
        heritage.close();
    }

    protected String saveThumbnail(Series s)
    {
        Provider p = new Provider(context.getContentResolver().query(Provider.uri(s.getProviderId()), null, null, null, null));

        File root = Environment.getExternalStorageDirectory();
        String seriesPath = root.getAbsolutePath() +
                "/" + stripBadCharsForFile(p.getName()) +
                "/" + stripBadCharsForFile(s.getName());
        File chapterDirectory = new File(seriesPath);
        chapterDirectory.mkdirs();
        String thumbPath = chapterDirectory +
                "/thumb.png";

        s.setThumbnailPath(thumbPath);
        FileOutputStream out = null;
        try
        {
            URL url = new URL(s.getThumbnailUrl());
            Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            out = new FileOutputStream(thumbPath);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        catch (MalformedURLException e)
        {
            // TODO:: better error handling/checking
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            // Couldn't get it
            return null;
        }
        finally
        {
            try
            {
                if (out != null)
                {
                    out.close();
                }
            }
            catch (IOException e)
            {
                //TODO:: better error handling/checking
                throw new RuntimeException(e);
            }
        }
        return thumbPath;
    }
}
