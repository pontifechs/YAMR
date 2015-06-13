package ninja.dudley.yamr.svc;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;

import ninja.dudley.yamr.model.Chapter;
import ninja.dudley.yamr.model.Page;
import ninja.dudley.yamr.model.Series;
import ninja.dudley.yamr.svc.fetchers.MangaPandaFetcher;

public class Paging extends IntentService
{
    public static final String BASE = "ninja.dudley.yamr.fetch.Paging";

    public static final String NEXT_PAGE = BASE + ".NextPage";
    public static final String NEXT_PAGE_COMPLETE = NEXT_PAGE + ".Complete";
    public static final String PREV_PAGE = BASE + ".PrevPage";
    public static final String PREV_PAGE_COMPLETE = PREV_PAGE + ".Complete";

    public static final String NEXT_CHAPTER = BASE + ".NextChapter";
    public static final String NEXT_CHAPTER_COMPLETE = NEXT_CHAPTER + ".Complete";
    public static final String PREV_CHAPTER = BASE + ".PrevChapter";
    public static final String PREV_CHAPTER_COMPLETE = PREV_CHAPTER + ".Complete";

    public Paging()
    {
        super("Paging");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        if (intent != null)
        {
            final String action = intent.getAction();
            Page p = new Page(getContentResolver().query(intent.getData(), null, null, null, null));
            switch (action)
            {
                case NEXT_PAGE:
                    nextPage(p);
                    break;
                case PREV_PAGE:
                    prevPage(p);
                    break;
                case NEXT_CHAPTER:
                    nextChapter(p);
                    break;
                case PREV_CHAPTER:
                    prevChapter(p);
                    break;
            }
        }
    }

    // TODO:: Clean this up
    private void nextPage(Page p)
    {
        FetcherSync fetcher = new MangaPandaFetcher(getBaseContext());
        Uri getNextPageUri = Chapter.uri(p.getChapterId())
                .buildUpon().appendPath("pages").appendPath(Float.toString(p.getNumber())).build();
        Cursor nextPageCursor = getContentResolver().query(getNextPageUri,
                null,
                ">",
                null,
                null);
        // Did it exist?
        if (nextPageCursor.getCount() > 0)
        {
            nextPageCursor.moveToFirst();
            Page nextPage = new Page(nextPageCursor);
            fetcher.fetchPage(nextPage);
            Intent i = new Intent(NEXT_PAGE_COMPLETE);
            i.setData(nextPage.uri());
            LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
        }
        // Didn't exist. Go get the next chapter and it's first page
        else
        {
            Uri wrongChapterUri = Chapter.uri(p.getChapterId());
            Chapter wrongChapter = new Chapter(getContentResolver().query(wrongChapterUri, null, null, null, null));
            fetcher.fetchChapter(wrongChapter);
            Uri seriesUri = Series.uri(wrongChapter.getSeriesId());
            Series series = new Series(getContentResolver().query(seriesUri, null, null, null, null));
            Uri getNextChapterUri = series.uri().buildUpon().appendPath("chapters")
                    .appendPath(Float.toString(wrongChapter.getNumber())).build();
            Cursor nextChapterCursor = getContentResolver().query(getNextChapterUri,
                    null,
                    ">",
                    null,
                    null);
            // Did it exist?
            if (nextChapterCursor.getCount() > 0)
            {
                nextChapterCursor.moveToFirst();
                Chapter nextChapter = new Chapter(nextChapterCursor);
                fetcher.fetchChapter(nextChapter);
                Uri pagesQuery = nextChapter.uri().buildUpon().appendPath("pages").build();
                Cursor pages = getContentResolver().query(pagesQuery, null, null, null, null);
                Page firstPage = new Page(pages);
                fetcher.fetchPage(firstPage);
                Intent i = new Intent(NEXT_PAGE_COMPLETE);
                i.setData(firstPage.uri());
                LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
            }
            // Ran off the end.
            // TODO:: Figure out the proper way to handle this.
            else
            {
                throw new AssertionError("All Done!");
            }
        }
    }

    private void prevPage(Page p)
    {
        FetcherSync fetcher = new MangaPandaFetcher(getBaseContext());
        Uri getNextPageUri = Chapter.uri(p.getChapterId())
                .buildUpon().appendPath("pages").appendPath(Float.toString(p.getNumber())).build();
        Cursor nextPageCursor = getContentResolver().query(getNextPageUri,
                null,
                "<",
                null,
                null);
        // Did it exist?
        if (nextPageCursor.getCount() > 0)
        {
            nextPageCursor.moveToFirst();
            Page nextPage = new Page(nextPageCursor);
            fetcher.fetchPage(nextPage);
            Intent i = new Intent(PREV_PAGE_COMPLETE);
            i.setData(nextPage.uri());
            LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
        }
        // Didn't exist. Go get the next chapter and it's first page
        else
        {
            Uri wrongChapterUri = Chapter.uri(p.getChapterId());
            Chapter wrongChapter = new Chapter(getContentResolver().query(wrongChapterUri, null, null, null, null));
            fetcher.fetchChapter(wrongChapter);
            Uri seriesUri = Series.uri(wrongChapter.getSeriesId());
            Series series = new Series(getContentResolver().query(seriesUri, null, null, null, null));
            Uri getNextChapterUri = series.uri().buildUpon().appendPath("chapters")
                    .appendPath(Float.toString(wrongChapter.getNumber())).build();
            Cursor nextChapterCursor = getContentResolver().query(getNextChapterUri,
                    null,
                    "<",
                    null,
                    null);
            // Did it exist?
            if (nextChapterCursor.getCount() > 0)
            {
                nextChapterCursor.moveToFirst();
                Chapter nextChapter = new Chapter(nextChapterCursor);
                fetcher.fetchChapter(nextChapter);
                Uri pagesQuery = nextChapter.uri().buildUpon().appendPath("pages").build();
                Cursor pages = getContentResolver().query(pagesQuery, null, null, null, null);
                Page firstPage = new Page(pages);
                fetcher.fetchPage(firstPage);
                Intent i = new Intent(PREV_PAGE_COMPLETE);
                i.setData(firstPage.uri());
                LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
            }
            // Ran off the end.
            // TODO:: Figure out the proper way to handle this.
            else
            {
                throw new AssertionError("All Done!");
            }
        }
    }

    private void nextChapter(Page p)
    {
        throw new AssertionError("Not Implemented");
    }

    private void prevChapter(Page p)
    {
        throw new AssertionError("Not Implemented");
    }

    private Chapter chapterFromPage(Page p)
    {
        Uri chapter = Chapter.uri(p.getChapterId());
        return new Chapter(getContentResolver().query(chapter, null, null, null, null));
    }
}
