package ninja.dudley.yamr.svc;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.NoSuchElementException;

import ninja.dudley.yamr.model.Chapter;
import ninja.dudley.yamr.model.Page;
import ninja.dudley.yamr.model.Series;
import ninja.dudley.yamr.svc.fetchers.MangaPandaFetcher;

public class Navigation extends IntentService implements FetcherSync.NotifyStatus
{
    public static final String BASE = "ninja.dudley.yamr.fetch.Navigation";

    public static final String NEXT_PAGE = BASE + ".NextPage";
    public static final String NEXT_PAGE_COMPLETE = NEXT_PAGE + ".Complete";
    public static final String NEXT_PAGE_DOESNT_EXIST = NEXT_PAGE + ".DoesntExist";
    public static final String PREV_PAGE = BASE + ".PrevPage";
    public static final String PREV_PAGE_COMPLETE = PREV_PAGE + ".Complete";
    public static final String PREV_PAGE_DOESNT_EXIST = PREV_PAGE + ".DoesntExist";

    public static final String NEXT_CHAPTER = BASE + ".NextChapter";
    public static final String NEXT_CHAPTER_COMPLETE = NEXT_CHAPTER + ".Complete";
    public static final String NEXT_CHAPTER_DOESNT_EXIST = NEXT_CHAPTER + ".DoesntExist";
    public static final String PREV_CHAPTER = BASE + ".PrevChapter";
    public static final String PREV_CHAPTER_COMPLETE = PREV_CHAPTER + ".Complete";
    public static final String PREV_CHAPTER_DOESNT_EXIST = PREV_CHAPTER + ".DoesntExist";


    public static final String FIRST_PAGE_FROM_SERIES = BASE + ".FirstPageFromSeries";
    public static final String FIRST_PAGE_FROM_SERIES_COMPLETE = FIRST_PAGE_FROM_SERIES + ".Complete";
    public static final String FIRST_PAGE_FROM_CHAPTER = BASE + ".FirstPageFromChapter";
    public static final String FIRST_PAGE_FROM_CHAPTER_COMPLETE = FIRST_PAGE_FROM_CHAPTER + " .Complete";
    public static final String PAGE_FROM_SERIES = BASE + ".PageFromSeries";
    public static final String PAGE_FROM_SERIES_COMPLETE = PAGE_FROM_SERIES + ".Complete";

    public Navigation()
    {
        super("Navigation");
    }

    private FetcherSync fetcher;

    @Override
    public void notifyProviderStatus(float status)
    {
        // Dun care
    }

    @Override
    public void notifySeriesStatus(float status)
    {
        // Dun care
    }

    @Override
    public void notifyChapterStatus(float status)
    {
        // Dun care
    }

    @Override
    public void notifyPageStatus(float status)
    {
        // Do care!
        Log.d("PageStatus", "" + status);
        Intent i = new Intent();
        i.setAction(FetcherAsync.FETCH_PAGE_STATUS);
        i.putExtra(FetcherAsync.FETCH_PAGE_STATUS, status);
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
    }


    @Override
    public void notifyNewStatus(float status)
    {
        // Dun care
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        if (intent != null)
        {
            fetcher = new MangaPandaFetcher(getBaseContext());
            fetcher.register(this);
            final String action = intent.getAction();
            switch (action)
            {
                case NEXT_PAGE:
                {
                    Page arg = page(intent.getData());
                    Page next = nextPage(arg);
                    if (next != null)
                    {
                        broadcastComplete(next.uri(), NEXT_PAGE_COMPLETE);
                    }
                    else
                    {
                        broadcastComplete(null, NEXT_PAGE_DOESNT_EXIST);
                    }
                    break;
                }
                case PREV_PAGE:
                {
                    Page arg = page(intent.getData());
                    Page prev = prevPage(arg);
                    if (prev != null)
                    {
                        broadcastComplete(prev.uri(), PREV_PAGE_COMPLETE);
                    }
                    else
                    {
                        broadcastComplete(null, PREV_PAGE_DOESNT_EXIST);
                    }
                    break;
                }
                case NEXT_CHAPTER:
                {
                    Chapter arg = chapter(intent.getData());
                    Chapter next = nextChapter(arg);
                    if (next != null)
                    {
                        broadcastComplete(next.uri(), NEXT_CHAPTER_COMPLETE);
                    }
                    else
                    {
                        broadcastComplete(null, NEXT_CHAPTER_DOESNT_EXIST);
                    }
                    break;
                }
                case PREV_CHAPTER:
                {
                    Chapter arg = chapter(intent.getData());
                    Chapter prev = prevChapter(arg);
                    if (prev != null)
                    {
                        broadcastComplete(prev.uri(), PREV_CHAPTER_COMPLETE);
                    }
                    else
                    {
                        broadcastComplete(null, PREV_CHAPTER_DOESNT_EXIST);
                    }
                    break;
                }
                case FIRST_PAGE_FROM_CHAPTER:
                {
                    Chapter arg = chapter(intent.getData());
                    fetcher.fetchChapter(arg);
                    Page first = firstPageFromChapter(arg);
                    broadcastComplete(first.uri(), FIRST_PAGE_FROM_CHAPTER_COMPLETE);
                    break;
                }
                case FIRST_PAGE_FROM_SERIES:
                {
                    Series arg = series(intent.getData());
                    fetcher.fetchSeries(arg);
                    Page first = firstPageFromSeries(arg);
                    broadcastComplete(first.uri(), FIRST_PAGE_FROM_SERIES_COMPLETE);
                    break;
                }
                case PAGE_FROM_SERIES:
                {
                    Series arg = series(intent.getData());
                    if (arg.progressPageId == -1)
                    {
                        arg = bookmarkFirstPage(arg);
                    }
                    Page page = pageFromBookmark(arg);
                    broadcastComplete(page.uri(), PAGE_FROM_SERIES_COMPLETE);
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

    private Chapter chapterFromPage(Page p)
    {
        Uri chapter = Chapter.uri(p.chapterId);
        return new Chapter(getContentResolver().query(chapter, null, null, null, null));
    }

    private Series seriesFromChapter(Chapter c)
    {
        Uri series = Series.uri(c.seriesId);
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
        Uri getNextPageUri = direction == Direction.Previous
                ? chapter.prevPage(currentPage.number)
                : chapter.nextPage(currentPage.number);
        Cursor nextPageCursor = getContentResolver().query(getNextPageUri, null, null, null, null);
        Page page = new Page(nextPageCursor); // Let it throw, Let it throw!!! Can't hold it back anymore!!!
        fetcher.fetchPage(page);
        return page;
    }

    // Returns a fetched chapter neighboring the chapter specified by the given uri.
    private Chapter neighboringChapter(Chapter currentChapter, Direction direction)
    {
        Series series = seriesFromChapter(currentChapter);
        Uri getNextChapterUri = direction == Direction.Previous
                ? series.prevChapter(currentChapter.number)
                : series.nextChapter(currentChapter.number);
        Cursor nextChapterCursor = getContentResolver().query(getNextChapterUri, null, null, null, null);
        Chapter chapter= new Chapter(nextChapterCursor); // Let it throw, Let it throw!!! Can't hold it back anymore
        fetcher.fetchChapter(currentChapter);
        return chapter;
    }

    private Page nextPage(Page page)
    {
        Page nextPage;
        try
        {
            nextPage = neighboringPage(page, Direction.Next);
        }
        catch (NoSuchElementException noPage)
        {
            Chapter wrongChapter = chapterFromPage(page);
            try
            {
                Chapter rightChapter = nextChapter(wrongChapter);
                fetcher.fetchChapter(rightChapter);
                nextPage = firstPageFromChapter(rightChapter);
            }
            catch (NoSuchElementException noChapter)
            {
                return null;
            }
        }
        return nextPage;
    }

    private Page prevPage(Page page)
    {
        Page prevPage;
        try
        {
            prevPage = neighboringPage(page, Direction.Previous);
        }
        catch (NoSuchElementException noPage)
        {
            Chapter wrongChapter = chapterFromPage(page);
            try
            {
                Chapter rightChapter = prevChapter(wrongChapter);
                fetcher.fetchChapter(rightChapter);
                prevPage = lastPageFromChapter(rightChapter);
            }
            catch (NoSuchElementException noChapter)
            {
                return null;
            }
        }
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
        Cursor pages = getContentResolver().query(chapter.pages(), null, null, null, Page.numberCol + " asc");
        Page firstPage = new Page(pages);
        fetcher.fetchPage(firstPage);
        return firstPage;
    }

    private Page lastPageFromChapter(Chapter chapter)
    {
        Cursor pages = getContentResolver().query(chapter.pages(), null, null, null, Page.numberCol + " desc");
        Page lastPage = new Page(pages);
        fetcher.fetchPage(lastPage);
        return lastPage;
    }

    private Chapter firstChapterFromSeries(Series series)
    {
        Cursor chapters = getContentResolver().query(series.chapters(), null, null, null, Chapter.numberCol + " asc");
        Chapter firstChapter = new Chapter(chapters);
        fetcher.fetchChapter(firstChapter);
        return firstChapter;
    }

    private Page firstPageFromSeries(Series series)
    {
        Chapter c = firstChapterFromSeries(series);
        fetcher.fetchChapter(c);
        return firstPageFromChapter(c);
    }

    private Series bookmarkFirstPage(Series series)
    {
        Chapter firstChapter = firstChapterFromSeries(series);
        Page firstPage = firstPageFromSeries(series);
        series.progressChapterId = firstChapter.id;
        series.progressPageId = firstPage.id;
        getContentResolver().update(series.uri(), series.getContentValues(), null, null);
        return series;
    }

    private Page pageFromBookmark(Series series)
    {
        Uri pageUri = Page.uri(series.progressPageId);
        Page page = page(pageUri);
        fetcher.fetchPage(page);
        return page;
    }
}
