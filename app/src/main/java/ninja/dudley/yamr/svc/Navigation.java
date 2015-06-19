package ninja.dudley.yamr.svc;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;

import java.util.NoSuchElementException;

import ninja.dudley.yamr.db.DBHelper;
import ninja.dudley.yamr.model.Bookmark;
import ninja.dudley.yamr.model.Chapter;
import ninja.dudley.yamr.model.Page;
import ninja.dudley.yamr.model.Series;
import ninja.dudley.yamr.svc.fetchers.MangaPandaFetcher;

public class Navigation extends IntentService
{
    public static final String BASE = "ninja.dudley.yamr.fetch.Navigation";

    public static final String NEXT_PAGE = BASE + ".NextPage";
    public static final String NEXT_PAGE_COMPLETE = NEXT_PAGE + ".Complete";
    public static final String PREV_PAGE = BASE + ".PrevPage";
    public static final String PREV_PAGE_COMPLETE = PREV_PAGE + ".Complete";

    public static final String NEXT_CHAPTER = BASE + ".NextChapter";
    public static final String NEXT_CHAPTER_COMPLETE = NEXT_CHAPTER + ".Complete";
    public static final String PREV_CHAPTER = BASE + ".PrevChapter";
    public static final String PREV_CHAPTER_COMPLETE = PREV_CHAPTER + ".Complete";

    public static final String FIRST_PAGE_FROM_SERIES = BASE + ".FirstPageFromSeries";
    public static final String FIRST_PAGE_FROM_SERIES_COMPLETE = FIRST_PAGE_FROM_SERIES + ".Complete";
    public static final String FIRST_PAGE_FROM_CHAPTER = BASE + ".FirstPageFromChapter";
    public static final String FIRST_PAGE_FROM_CHAPTER_COMPLETE = FIRST_PAGE_FROM_CHAPTER + " .Complete";
    public static final String PAGE_FROM_BOOKMARK = BASE + ".FirstPageFromBookmark";
    public static final String PAGE_FROM_BOOKMARK_COMPLETE = PAGE_FROM_BOOKMARK + ".Complete";


    public Navigation()
    {
        super("Navigation");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        if (intent != null)
        {
            final String action = intent.getAction();
            switch (action)
            {
                case NEXT_PAGE:
                {
                    Page arg = page(intent.getData());
                    Page next = nextPage(arg);
                    broadcastComplete(next.uri(), NEXT_PAGE_COMPLETE);
                    break;
                }
                case PREV_PAGE:
                {
                    Page arg = page(intent.getData());
                    Page prev = prevPage(arg);
                    broadcastComplete(prev.uri(), PREV_PAGE_COMPLETE);
                    break;
                }
                case NEXT_CHAPTER:
                {
                    Chapter arg = chapter(intent.getData());
                    Chapter next = nextChapter(arg);
                    broadcastComplete(next.uri(), NEXT_CHAPTER_COMPLETE);
                    break;
                }
                case PREV_CHAPTER:
                {
                    Chapter arg = chapter(intent.getData());
                    Chapter prev = prevChapter(arg);
                    broadcastComplete(prev.uri(), PREV_CHAPTER_COMPLETE);
                    break;
                }
                case FIRST_PAGE_FROM_CHAPTER:
                {
                    Chapter arg = chapter(intent.getData());
                    new MangaPandaFetcher(getBaseContext()).fetchChapter(arg);
                    Page first = firstPageFromChapter(arg);
                    broadcastComplete(first.uri(), FIRST_PAGE_FROM_CHAPTER_COMPLETE);
                    break;
                }
                case FIRST_PAGE_FROM_SERIES:
                {
                    Series arg = series(intent.getData());
                    new MangaPandaFetcher(getBaseContext()).fetchSeries(arg);
                    Page first = firstPageFromSeries(arg);
                    broadcastComplete(first.uri(), FIRST_PAGE_FROM_SERIES_COMPLETE);
                    break;
                }
                case PAGE_FROM_BOOKMARK:
                {
                    Bookmark arg;
                    try
                    {
                        arg = bookmark(intent.getData());
                    }
                    catch (NoSuchElementException e)
                    {
                        Series s = series(Series.uri(Integer.parseInt(intent.getData().getLastPathSegment())));
                        arg = bookmarkFirstPage(s);
                    }
                    Page page = pageFromBookmark(arg);
                    broadcastComplete(page.uri(), PAGE_FROM_BOOKMARK_COMPLETE);
                    break;
                }
            }
        }
    }

    private void broadcastComplete(Uri uri, String action)
    {
        Intent i = new Intent(action);
        i.setData(uri);
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
    }

    private Page page(Uri uri)
    {
        return new Page(getContentResolver().query(uri, null, null, null, null));
    }

    private Chapter chapter(Uri uri)
    {
        return new Chapter(getContentResolver().query(uri, null, null, null, null));
    }

    private Series series(Uri uri)
    {
        return new Series(getContentResolver().query(uri, null, null, null, null));
    }

    private Bookmark bookmark(Uri uri)
    {
        return new Bookmark(getContentResolver().query(uri, null, null, null, null));
    }

    private Chapter chapterFromPage(Page p)
    {
        Uri chapter = Chapter.uri(p.getChapterId());
        return new Chapter(getContentResolver().query(chapter, null, null, null, null));
    }

    private Series seriesFromChapter(Chapter c)
    {
        Uri series = Series.uri(c.getSeriesId());
        return new Series(getContentResolver().query(series, null, null, null, null));
    }

    private enum Direction
    {
        Previous("prev"),
        Next("next");

        private final String val;

        Direction(final String val)
        {
            this.val = val;
        }

        @Override
        public String toString()
        {
            return val;
        }
    }

    // Returns a fetched page neighboring the page specified by the given uri.
    private Page neighboringPage(Page currentPage, Direction direction)
    {
        Chapter chapter = chapterFromPage(currentPage);
        Uri getNextPageUri = chapter.uri().buildUpon().appendPath("pages")
                .appendPath(Float.toString(currentPage.getNumber())).appendPath(direction.toString()).build();
        Cursor nextPageCursor = getContentResolver().query(getNextPageUri, null, null, null, null);
        Page page = new Page(nextPageCursor); // Let it throw, Let it throw!!! Can't hold it back anymore!!!
        new MangaPandaFetcher(getBaseContext()).fetchPage(page);
        return page;
    }

    // Returns a fetched chapter neighboring the chapter specified by the given uri.
    private Chapter neighboringChapter(Chapter currentChapter, Direction direction)
    {
        Series series = seriesFromChapter(currentChapter);
        Uri getNextChapterUri = series.uri().buildUpon().appendPath("chapters")
                .appendPath(Float.toString(currentChapter.getNumber())).appendPath(direction.toString()).build();
        Cursor nextChapterCursor = getContentResolver().query(getNextChapterUri, null, null, null, null);
        Chapter chapter= new Chapter(nextChapterCursor); // Let it throw, Let it throw!!! Can't hold it back anymore
        new MangaPandaFetcher(getBaseContext()).fetchChapter(currentChapter);
        return chapter;
    }

    private Page nextPage(Page page)
    {
        Page nextPage;
        try
        {
            nextPage = neighboringPage(page, Direction.Next);
        }
        catch (NoSuchElementException e)
        {
            Chapter wrongChapter = chapterFromPage(page);
            nextPage = firstPageFromChapter(wrongChapter);
        }

        Intent i = new Intent(NEXT_PAGE_COMPLETE);
        i.setData(nextPage.uri());
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
        return nextPage;
    }

    private Page prevPage(Page page)
    {
        Page prevPage;
        try
        {
            prevPage = neighboringPage(page, Direction.Previous);
        }
        catch (NoSuchElementException e)
        {
            Chapter wrongChapter = chapterFromPage(page);
            prevPage = lastPageFromChapter(wrongChapter);
        }

        Intent i = new Intent(PREV_PAGE_COMPLETE);
        i.setData(prevPage.uri());
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
        return prevPage;
    }

    private Chapter nextChapter(Chapter chapter)
    {
        return neighboringChapter(chapter, Direction.Next);
    }

    private Chapter prevChapter(Chapter chapter)
    {
        return neighboringChapter(chapter, Direction.Previous);
    }

    private Page firstPageFromChapter(Chapter chapter)
    {
        Uri pagesQuery = chapter.uri().buildUpon().appendPath("pages").build();
        Cursor pages = getContentResolver().query(pagesQuery, null, null, null, DBHelper.PageEntry.COLUMN_NUMBER + " asc");
        Page firstPage = new Page(pages);
        new MangaPandaFetcher(getBaseContext()).fetchPage(firstPage);
        return firstPage;
    }

    private Page lastPageFromChapter(Chapter chapter)
    {
        Uri pagesQuery = chapter.uri().buildUpon().appendPath("pages").build();
        Cursor pages = getContentResolver().query(pagesQuery, null, null, null, DBHelper.PageEntry.COLUMN_NUMBER + " desc");
        Page lastPage = new Page(pages);
        new MangaPandaFetcher(getBaseContext()).fetchPage(lastPage);
        return lastPage;
    }

    private Chapter firstChapterFromSeries(Series series)
    {
        Uri chaptersQuery = series.uri().buildUpon().appendPath("chapters").build();
        Cursor chapters = getContentResolver().query(chaptersQuery, null, null, null, DBHelper.ChapterEntry.COLUMN_NUMBER + " asc");
        Chapter firstChapter = new Chapter(chapters);
        new MangaPandaFetcher(getBaseContext()).fetchChapter(firstChapter);
        return firstChapter;
    }

    private Page firstPageFromSeries(Series series)
    {
        Chapter c = firstChapterFromSeries(series);
        new MangaPandaFetcher(getBaseContext()).fetchChapter(c);
        return firstPageFromChapter(c);
    }

    private Bookmark bookmarkFromSeries(Series series)
    {
        Uri bookmarkQuery = Uri.withAppendedPath(Bookmark.baseUri(), Integer.toString(series.getId()));
        Cursor bookmarkCursor = getContentResolver().query(bookmarkQuery, null, null, null, null);
        return new Bookmark(bookmarkCursor);
    }

    private Bookmark bookmarkFirstPage(Series series)
    {
        Bookmark bookmark = new Bookmark(series.getId());
        Page firstPage = firstPageFromSeries(series);
        bookmark.setPageId(firstPage.getId());
        getContentResolver().insert(bookmark.uri(), bookmark.getContentValues());
        return bookmark;
    }

    private Page pageFromBookmark(Bookmark bookmark)
    {
        Uri pageUri = Page.uri(bookmark.getPageId());
        Page page = page(pageUri);
        new MangaPandaFetcher(getBaseContext()).fetchPage(page);
        return page;
    }
}
