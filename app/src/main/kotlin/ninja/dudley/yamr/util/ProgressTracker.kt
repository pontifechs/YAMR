package ninja.dudley.yamr.util

import android.content.ContentResolver

import ninja.dudley.yamr.model.Chapter
import ninja.dudley.yamr.model.Page
import ninja.dudley.yamr.model.Series

/**
 * Created by mdudley on 6/18/15.
 */
class ProgressTracker(private val resolver: ContentResolver, private val series: Series?)
{
    private var progressChapter: Chapter? = null
    private var progressPage: Page? = null

    init
    {
        if (this.series == null)
        {
            throw IllegalArgumentException("Must have a series to track progress")
        }

        val pageCursor = resolver.query(Page.uri(series.progressPageId!!), null, null, null, null)
        progressPage = Page(pageCursor)
        val chapterCursor = resolver.query(Chapter.uri(progressPage!!.chapterId), null, null, null, null)
        progressChapter = Chapter(chapterCursor)
    }

    fun handleNewPage(p: Page)
    {
        // Check if we're still on the same chapter.
        if (progressChapter!!.id == p.chapterId)
        {
            // Just need to check the numbers.
            if (p.number > progressPage!!.number)
            {
                progressPage = p
                series!!.progressPageId = progressPage!!.id
                resolver.update(series.uri(), series.getContentValues(), null, null)
            }
        }
        else
        {
            val chapterCursor = resolver.query(Chapter.uri(p.chapterId), null, null, null, null)
            val c = Chapter(chapterCursor)

            // Check it's still the same series, and then check numbering
            if (c.seriesId == progressChapter!!.seriesId && c.number > progressChapter!!.number)
            {
                progressChapter = c
                progressPage = p
                series!!.progressChapterId = progressChapter!!.id
                series.progressPageId = progressPage!!.id
                resolver.update(series.uri(), series.getContentValues(), null, null)
            }
        }// Check if the new chapter is higher numbered than the progress chapter
    }
}
