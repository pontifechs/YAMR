package ninja.dudley.yamr.svc

import android.app.IntentService
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import ninja.dudley.yamr.model.Chapter
import ninja.dudley.yamr.model.Page
import ninja.dudley.yamr.model.Series
import ninja.dudley.yamr.svc.util.LambdaAsyncTask
import java.util.NoSuchElementException

public class Navigation(resolver: ContentResolver) : FetcherSync(resolver)
{
    private fun page(uri: Uri): Page
    {
        return Page(resolver.query(uri, null, null, null, null))
    }

    private fun chapter(uri: Uri): Chapter
    {
        return Chapter(resolver.query(uri, null, null, null, null))
    }

    private fun series(uri: Uri): Series
    {
        return Series(resolver.query(uri, null, null, null, null))
    }

    private fun chapterFromPage(p: Page): Chapter
    {
        val chapter = Chapter.uri(p.chapterId)
        return Chapter(resolver.query(chapter, null, null, null, null))
    }

    private fun seriesFromChapter(c: Chapter): Series
    {
        val series = Series.uri(c.seriesId)
        return Series(resolver.query(series, null, null, null, null))
    }

    private enum class Direction
    {
        Previous,
        Next;
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

        val nextPageCursor = resolver.query(getNextPageUri, null, null, null, null)
        val page = Page(nextPageCursor) // Let it throw, Let it throw!!! Can't hold it back anymore!!!
        fetchPage(page)
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
        val nextChapterCursor = resolver.query(getNextChapterUri, null, null, null, null)
        val chapter = Chapter(nextChapterCursor) // Let it throw, Let it throw!!! Can't hold it back anymore
        fetchChapter(currentChapter)
        return chapter
    }

    public fun nextPage(page: Page): Page?
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
                nextPage = firstPageFromChapter(rightChapter)
            }
            catch (noChapter: NoSuchElementException)
            {
                return null
            }
        }
        return nextPage
    }

    public fun prevPage(page: Page): Page?
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

    public fun firstPageFromChapter(chapter: Chapter): Page
    {
        fetchChapter(chapter)
        val pages = resolver.query(chapter.pages(), null, null, null, Page.numberCol + " asc")
        val firstPage = Page(pages)
        fetchPage(firstPage)
        return firstPage
    }

    private fun lastPageFromChapter(chapter: Chapter): Page
    {
        fetchChapter(chapter)
        val pages = resolver.query(chapter.pages(), null, null, null, Page.numberCol + " desc")
        val lastPage = Page(pages)
        fetchPage(lastPage)
        return lastPage
    }

    private fun firstChapterFromSeries(series: Series): Chapter
    {
        val chapters = resolver.query(series.chapters(), null, null, null, Chapter.numberCol + " asc")
        val firstChapter = Chapter(chapters)
        fetchChapter(firstChapter)
        return firstChapter
    }

    public fun firstPageFromSeries(series: Series): Page
    {
        val c = firstChapterFromSeries(series)
        fetchChapter(c)
        return firstPageFromChapter(c)
    }

    public fun bookmarkFirstPage(series: Series): Series
    {
        val firstChapter = firstChapterFromSeries(series)
        val firstPage = firstPageFromSeries(series)
        series.progressChapterId = firstChapter.id
        series.progressPageId = firstPage.id
        resolver.update(series.uri(), series.getContentValues(), null, null)
        return series
    }

    public fun pageFromBookmark(series: Series): Page
    {
        val pageUri = Page.uri(series.progressPageId)
        val page = page(pageUri)
        fetchPage(page)
        return page
    }
}
