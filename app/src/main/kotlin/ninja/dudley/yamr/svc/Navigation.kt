package ninja.dudley.yamr.svc

import android.app.IntentService
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.support.v4.content.LocalBroadcastManager
import android.util.Log

import java.util.NoSuchElementException

import ninja.dudley.yamr.model.Chapter
import ninja.dudley.yamr.model.Page
import ninja.dudley.yamr.model.Series

public class Navigation : IntentService("Navigation"), FetcherSync.NotifyStatus
{

    private var fetcher: FetcherSync? = null

    override fun notifyProviderStatus(status: Float)
    {
        // Dun care
    }

    override fun notifySeriesStatus(status: Float)
    {
        // Dun care
    }

    override fun notifyChapterStatus(status: Float)
    {
        // Dun care
    }

    override fun notifyPageStatus(status: Float)
    {
        // Do care!
        Log.d("PageStatus", "" + status)
        val i = Intent()
        i.setAction(FetcherAsync.FETCH_PAGE_STATUS)
        i.putExtra(FetcherAsync.FETCH_PAGE_STATUS, status)
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i)
    }

    override fun onHandleIntent(intent: Intent?)
    {
        if (intent != null)
        {
            fetcher = FetcherSync(getBaseContext())
            fetcher!!.register(this)
            val action = intent.getAction()
            when (action)
            {
                NEXT_PAGE ->
                {
                    val arg = page(intent.getData())
                    val next = nextPage(arg)
                    if (next != null)
                    {
                        broadcastComplete(next.uri(), NEXT_PAGE_COMPLETE)
                    }
                    else
                    {
                        broadcastComplete(null, NEXT_PAGE_DOESNT_EXIST)
                    }
                }
                PREV_PAGE ->
                {
                    val arg = page(intent.getData())
                    val prev = prevPage(arg)
                    if (prev != null)
                    {
                        broadcastComplete(prev.uri(), PREV_PAGE_COMPLETE)
                    }
                    else
                    {
                        broadcastComplete(null, PREV_PAGE_DOESNT_EXIST)
                    }
                }
                NEXT_CHAPTER ->
                {
                    val arg = chapter(intent.getData())
                    val next = nextChapter(arg)
                    if (next != null)
                    {
                        broadcastComplete(next.uri(), NEXT_CHAPTER_COMPLETE)
                    }
                    else
                    {
                        broadcastComplete(null, NEXT_CHAPTER_DOESNT_EXIST)
                    }
                }
                PREV_CHAPTER ->
                {
                    val arg = chapter(intent.getData())
                    val prev = prevChapter(arg)
                    if (prev != null)
                    {
                        broadcastComplete(prev.uri(), PREV_CHAPTER_COMPLETE)
                    }
                    else
                    {
                        broadcastComplete(null, PREV_CHAPTER_DOESNT_EXIST)
                    }
                }
                FIRST_PAGE_FROM_CHAPTER ->
                {
                    val arg = chapter(intent.getData())
                    fetcher!!.fetchChapter(arg)
                    val first = firstPageFromChapter(arg)
                    broadcastComplete(first.uri(), FIRST_PAGE_FROM_CHAPTER_COMPLETE)
                }
                FIRST_PAGE_FROM_SERIES ->
                {
                    val arg = series(intent.getData())
                    fetcher!!.fetchSeries(arg)
                    val first = firstPageFromSeries(arg)
                    broadcastComplete(first.uri(), FIRST_PAGE_FROM_SERIES_COMPLETE)
                }
                PAGE_FROM_SERIES ->
                {
                    var arg = series(intent.getData())
                    if (arg.progressPageId == -1)
                    {
                        arg = bookmarkFirstPage(arg)
                    }
                    val page = pageFromBookmark(arg)
                    broadcastComplete(page.uri(), PAGE_FROM_SERIES_COMPLETE)
                }
            }
        }
    }

    private fun broadcastComplete(uri: Uri?, action: String)
    {
        val i = Intent(action)
        i.setData(uri)
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i)
    }

    private fun page(uri: Uri): Page
    {
        return Page(getContentResolver().query(uri, null, null, null, null))
    }

    private fun chapter(uri: Uri): Chapter
    {
        return Chapter(getContentResolver().query(uri, null, null, null, null))
    }

    private fun series(uri: Uri): Series
    {
        return Series(getContentResolver().query(uri, null, null, null, null))
    }

    private fun chapterFromPage(p: Page): Chapter
    {
        val chapter = Chapter.uri(p.chapterId)
        return Chapter(getContentResolver().query(chapter, null, null, null, null))
    }

    private fun seriesFromChapter(c: Chapter): Series
    {
        val series = Series.uri(c.seriesId)
        return Series(getContentResolver().query(series, null, null, null, null))
    }

    private enum class Direction(private val `val`: String)
    {
        Previous("prev"),
        Next("next");

        override fun toString(): String
        {
            return `val`
        }
    }

    // Returns a fetched page neighboring the page specified by the given uri.
    private fun neighboringPage(currentPage: Page, direction: Direction): Page
    {
        val chapter = chapterFromPage(currentPage)
        val getNextPageUri = if (direction === Direction.Previous)
        {
            chapter.prevPage(currentPage.number)
        }
        else
        {
            chapter.nextPage(currentPage.number)
        }

        val nextPageCursor = getContentResolver().query(getNextPageUri, null, null, null, null)
        val page = Page(nextPageCursor) // Let it throw, Let it throw!!! Can't hold it back anymore!!!
        fetcher!!.fetchPage(page)
        return page
    }

    // Returns a fetched chapter neighboring the chapter specified by the given uri.
    private fun neighboringChapter(currentChapter: Chapter, direction: Direction): Chapter
    {
        val series = seriesFromChapter(currentChapter)
        val getNextChapterUri = if (direction === Direction.Previous)
            series.prevChapter(currentChapter.number)
        else
            series.nextChapter(currentChapter.number)
        val nextChapterCursor = getContentResolver().query(getNextChapterUri, null, null, null, null)
        val chapter = Chapter(nextChapterCursor) // Let it throw, Let it throw!!! Can't hold it back anymore
        fetcher!!.fetchChapter(currentChapter)
        return chapter
    }

    private fun nextPage(page: Page): Page?
    {
        val nextPage: Page
        try
        {
            nextPage = neighboringPage(page, Direction.Next)
        }
        catch (noPage: NoSuchElementException)
        {
            val wrongChapter = chapterFromPage(page)
            try
            {
                val rightChapter = nextChapter(wrongChapter)!!
                fetcher!!.fetchChapter(rightChapter)
                nextPage = firstPageFromChapter(rightChapter)
            }
            catch (noChapter: NoSuchElementException)
            {
                return null
            }
        }
        return nextPage
    }

    private fun prevPage(page: Page): Page?
    {
        val prevPage: Page
        try
        {
            prevPage = neighboringPage(page, Direction.Previous)
        }
        catch (noPage: NoSuchElementException)
        {
            val wrongChapter = chapterFromPage(page)
            try
            {
                val rightChapter = prevChapter(wrongChapter)!!
                fetcher!!.fetchChapter(rightChapter)
                prevPage = lastPageFromChapter(rightChapter)
            }
            catch (noChapter: NoSuchElementException)
            {
                return null
            }

        }

        return prevPage
    }

    private fun nextChapter(chapter: Chapter): Chapter?
    {
        return neighboringChapter(chapter, Direction.Next)
    }

    private fun prevChapter(chapter: Chapter): Chapter?
    {
        return neighboringChapter(chapter, Direction.Previous)
    }

    private fun firstPageFromChapter(chapter: Chapter): Page
    {
        val pages = getContentResolver().query(chapter.pages(), null, null, null, Page.numberCol + " asc")
        val firstPage = Page(pages)
        fetcher!!.fetchPage(firstPage)
        return firstPage
    }

    private fun lastPageFromChapter(chapter: Chapter): Page
    {
        val pages = getContentResolver().query(chapter.pages(), null, null, null, Page.numberCol + " desc")
        val lastPage = Page(pages)
        fetcher!!.fetchPage(lastPage)
        return lastPage
    }

    private fun firstChapterFromSeries(series: Series): Chapter
    {
        val chapters = getContentResolver().query(series.chapters(), null, null, null, Chapter.numberCol + " asc")
        val firstChapter = Chapter(chapters)
        fetcher!!.fetchChapter(firstChapter)
        return firstChapter
    }

    private fun firstPageFromSeries(series: Series): Page
    {
        val c = firstChapterFromSeries(series)
        fetcher!!.fetchChapter(c)
        return firstPageFromChapter(c)
    }

    private fun bookmarkFirstPage(series: Series): Series
    {
        val firstChapter = firstChapterFromSeries(series)
        val firstPage = firstPageFromSeries(series)
        series.progressChapterId = firstChapter.id
        series.progressPageId = firstPage.id
        getContentResolver().update(series.uri(), series.getContentValues(), null, null)
        return series
    }

    private fun pageFromBookmark(series: Series): Page
    {
        val pageUri = Page.uri(series.progressPageId)
        val page = page(pageUri)
        fetcher!!.fetchPage(page)
        return page
    }

    companion object
    {
        public val BASE: String = "ninja.dudley.ninja.dudley.yamr.fetch.Navigation"

        public val NEXT_PAGE: String = BASE + ".NextPage"
        public val NEXT_PAGE_COMPLETE: String = NEXT_PAGE + ".Complete"
        public val NEXT_PAGE_DOESNT_EXIST: String = NEXT_PAGE + ".DoesntExist"
        public val PREV_PAGE: String = BASE + ".PrevPage"
        public val PREV_PAGE_COMPLETE: String = PREV_PAGE + ".Complete"
        public val PREV_PAGE_DOESNT_EXIST: String = PREV_PAGE + ".DoesntExist"

        public val NEXT_CHAPTER: String = BASE + ".NextChapter"
        public val NEXT_CHAPTER_COMPLETE: String = NEXT_CHAPTER + ".Complete"
        public val NEXT_CHAPTER_DOESNT_EXIST: String = NEXT_CHAPTER + ".DoesntExist"
        public val PREV_CHAPTER: String = BASE + ".PrevChapter"
        public val PREV_CHAPTER_COMPLETE: String = PREV_CHAPTER + ".Complete"
        public val PREV_CHAPTER_DOESNT_EXIST: String = PREV_CHAPTER + ".DoesntExist"


        public val FIRST_PAGE_FROM_SERIES: String = BASE + ".FirstPageFromSeries"
        public val FIRST_PAGE_FROM_SERIES_COMPLETE: String = FIRST_PAGE_FROM_SERIES + ".Complete"
        public val FIRST_PAGE_FROM_CHAPTER: String = BASE + ".FirstPageFromChapter"
        public val FIRST_PAGE_FROM_CHAPTER_COMPLETE: String = FIRST_PAGE_FROM_CHAPTER + " .Complete"
        public val PAGE_FROM_SERIES: String = BASE + ".PageFromSeries"
        public val PAGE_FROM_SERIES_COMPLETE: String = PAGE_FROM_SERIES + ".Complete"
    }
}
