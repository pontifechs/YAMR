package ninja.dudley.yamr.svc

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import ninja.dudley.yamr.model.Chapter
import ninja.dudley.yamr.model.Page
import ninja.dudley.yamr.model.Series
import ninja.dudley.yamr.util.Direction
import java.util.NoSuchElementException

class Navigation : FetcherSync
{
    private val resolver: ContentResolver

    constructor(context: Context)
    : super(context)
    {
        this.resolver = context.contentResolver
    }

    private fun page(uri: Uri): Page
    {
        return Page(resolver.query(uri, null, null, null, null))
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

    // Returns a fetched page neighboring the page specified by the given uri.
    private fun neighboringPage(currentPage: Page, direction: Direction): Page
    {
        val chapter = chapterFromPage(currentPage)
        val getNextPageUri = if (direction == Direction.Prev)
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
        val getNextChapterUri = if (direction == Direction.Prev)
            series.prevChapter(currentChapter.number)
        else
            series.nextChapter(currentChapter.number)
        val nextChapterCursor = resolver.query(getNextChapterUri, null, null, null, null)
        val chapter = Chapter(nextChapterCursor) // Let it throw, Let it throw!!! Can't hold it back anymore
        fetchChapter(currentChapter)
        return chapter
    }

    fun nextPage(page: Page): Page
    {
        var nextPage: Page
        try
        {
            nextPage = neighboringPage(page, Direction.Next)
        }
        catch (noPage: NoSuchElementException)
        {
            val wrongChapter = chapterFromPage(page)
            val rightChapter = nextChapter(wrongChapter)
            nextPage = firstPageFromChapter(rightChapter)
            nextPage = fetchPage(nextPage)
        }
        return nextPage
    }

    fun prevPage(page: Page): Page
    {
        var prevPage: Page
        try
        {
            prevPage = neighboringPage(page, Direction.Prev)
        }
        catch (noPage: NoSuchElementException)
        {
            val wrongChapter = chapterFromPage(page)
            val rightChapter = prevChapter(wrongChapter)
            prevPage = lastPageFromChapter(rightChapter)
            prevPage = fetchPage(prevPage)
        }
        return prevPage
    }

    private fun nextChapter(chapter: Chapter): Chapter
    {
        return neighboringChapter(chapter, Direction.Next)
    }

    private fun prevChapter(chapter: Chapter): Chapter
    {
        return neighboringChapter(chapter, Direction.Prev)
    }

    fun firstPageFromChapter(chapter: Chapter): Page
    {
        fetchChapter(chapter)
        val pages = resolver.query(chapter.pages(), null, null, null, Page.numberCol + " asc")
        return Page(pages)
    }

    private fun lastPageFromChapter(chapter: Chapter): Page
    {
        fetchChapter(chapter)
        val pages = resolver.query(chapter.pages(), null, null, null, Page.numberCol + " desc")
        return Page(pages)
    }

    private fun firstChapterFromSeries(series: Series): Chapter
    {
        val chapters = resolver.query(series.chapters(), null, null, null, Chapter.numberCol + " asc")
        return Chapter(chapters)
    }

    fun firstPageFromSeries(series: Series): Page
    {
        val c = firstChapterFromSeries(series)
        fetchChapter(c)
        return firstPageFromChapter(c)
    }

    fun bookmarkFirstPage(series: Series): Series
    {
        val firstChapter = firstChapterFromSeries(series)
        val firstPage = firstPageFromSeries(series)
        series.progressChapterId = firstChapter.id
        series.progressPageId = firstPage.id
        resolver.update(series.uri(), series.getContentValues(), null, null)
        return series
    }

    fun pageFromBookmark(series: Series): Page
    {
        val pageUri = Page.uri(series.progressPageId!!)
        return page(pageUri)
    }

    fun fetchPageOffset(page: Page, offset: Int, direction: Direction): Page
    {
        var currentChapter = chapterFromPage(page)
        var neighboringPagesUri =
                if (direction == Direction.Next)
                    currentChapter.nextPage(page.number)
                else
                    currentChapter.prevPage(page.number)

        var neighboringPages = resolver.query(neighboringPagesUri, null, null, null, null)

        var skipped = 0
        // Until we've gone far enough, keep going
        while (skipped < offset)
        {
            if (neighboringPages.moveToNext())
            {
                ++skipped;
            }
            // Ran out of rows, try the next chapter
            else
            {
                val neighboringChapter =  if (direction == Direction.Next)
                    nextChapter(currentChapter)
                else
                    prevChapter(currentChapter)
                fetchChapter(neighboringChapter)
                currentChapter = neighboringChapter
                neighboringPagesUri =
                        if (direction == Direction.Next)
                            currentChapter.nextPage(page.number)
                        else
                            currentChapter.prevPage(page.number)
                neighboringPages = resolver.query(neighboringPagesUri, null, null, null, null)
            }
        }
        val retPage = Page(neighboringPages)
        fetchPage(retPage)
        return retPage
    }
}
