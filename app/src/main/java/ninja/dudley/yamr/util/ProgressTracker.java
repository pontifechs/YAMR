package ninja.dudley.yamr.util;

import android.content.ContentResolver;
import android.database.Cursor;

import ninja.dudley.yamr.model.Chapter;
import ninja.dudley.yamr.model.Page;
import ninja.dudley.yamr.model.Series;

/**
 * Created by mdudley on 6/18/15.
 */
public class ProgressTracker
{
    private ContentResolver resolver;
    private Series series;
    private Chapter progressChapter;
    private Page progressPage;

    public ProgressTracker(ContentResolver resolver, Series series)
    {
        this.resolver = resolver;
        this.series= series;

        if (this.series == null)
        {
            throw new IllegalArgumentException("Must have the bookmark to track progress");
        }

        Cursor pageCursor = resolver.query(Page.uri(series.getProgressPageId()), null, null, null, null);
        progressPage = new Page(pageCursor);
        Cursor chapterCursor = resolver.query(Chapter.uri(progressPage.getChapterId()), null, null, null, null);
        progressChapter = new Chapter(chapterCursor);
    }

    public void handleNextPage(Page p)
    {
        // Check if we're still on the same chapter.
        if (progressChapter.getId() == p.getChapterId())
        {
            // Just need to check the numbers.
            if (p.getNumber() > progressPage.getNumber())
            {
                progressPage = p;
                series.setProgressPageId(progressPage.getId());
                resolver.update(series.uri(), series.getContentValues(), null, null);
            }
        }
        // Check if the new chapter is higher numbered than the progress chapter
        else
        {
            Cursor chapterCursor = resolver.query(Chapter.uri(p.getChapterId()), null, null, null, null);
            Chapter c = new Chapter(chapterCursor);

            // Check it's still the same series, and then check numbering
            if (c.getSeriesId() == progressChapter.getSeriesId() && c.getNumber() > progressChapter.getNumber())
            {
                progressChapter = c;
                progressPage = p;
                series.setProgressChapterId(progressChapter.getId());
                series.setProgressPageId(progressPage.getId());
                resolver.update(series.uri(), series.getContentValues(), null, null);
            }
        }
    }
}
