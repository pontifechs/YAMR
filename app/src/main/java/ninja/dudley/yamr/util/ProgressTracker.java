package ninja.dudley.yamr.util;

import android.content.ContentResolver;
import android.database.Cursor;

import ninja.dudley.yamr.model.Bookmark;
import ninja.dudley.yamr.model.Chapter;
import ninja.dudley.yamr.model.Page;

/**
 * Created by mdudley on 6/18/15.
 */
public class ProgressTracker
{
    private ContentResolver resolver;
    private Bookmark bookmark;
    private Chapter progressChapter;
    private Page progressPage;

    public ProgressTracker(ContentResolver resolver, Bookmark bookmark)
    {
        this.resolver = resolver;
        this.bookmark = bookmark;

        if (this.bookmark == null)
        {
            throw new IllegalArgumentException("Must have the bookmark to track progress");
        }

        Cursor pageCursor = resolver.query(Page.uri(bookmark.getPageId()), null, null, null, null);
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
                bookmark.setPageId(progressPage.getId());
                resolver.update(bookmark.uri(), bookmark.getContentValues(), null, null);
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
                bookmark.setPageId(progressPage.getId());
                resolver.update(bookmark.uri(), bookmark.getContentValues(), null, null);
            }
        }
    }

    public Bookmark getBookmark()
    {
        return bookmark;
    }

    public void setBookmark(Bookmark bookmark)
    {
        this.bookmark = bookmark;
    }
}
