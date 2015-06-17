package ninja.dudley.yamr.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.NoSuchElementException;

import ninja.dudley.yamr.db.DBHelper;

/**
 * Created by mdudley on 6/15/15.
 */
public class Bookmark
{
    private int _id = -1;

    private int seriesId;
    private int pageId;
    private String note;

    public static Uri baseUri()
    {
        return Uri.parse("content://" + DBHelper.AUTHORITY + "/bookmark");
    }

    public static Uri uri(int id)
    {
        Uri base = baseUri();
        return base.buildUpon().appendPath(Integer.toString(id)).build();
    }

    public Bookmark(int seriesId)
    {
        this.seriesId = seriesId;
    }

    public Bookmark(Cursor c)
    {
        if (c.getCount() <= 0)
        {
            c.close();
            throw new NoSuchElementException("Empty Cursor!");
        }
        c.moveToFirst();
        int idCol = c.getColumnIndex(DBHelper.BookmarkEntry._ID);
        int seriesIdCol = c.getColumnIndex(DBHelper.BookmarkEntry.COLUMN_SERIES_ID);
        int pageProgressIdCol = c.getColumnIndex(DBHelper.BookmarkEntry.COLUMN_PAGE_ID);
        int noteCol = c.getColumnIndex(DBHelper.BookmarkEntry.COLUMN_NOTE);
        _id = c.getInt(idCol);
        seriesId = c.getInt(seriesIdCol);
        pageId = c.getInt(pageProgressIdCol);
        note = c.getString(noteCol);
        c.close();
    }

    public Uri uri()
    {
        return uri(this.seriesId);
    }

    public ContentValues getContentValues()
    {
        ContentValues values = new ContentValues();
        values.put(DBHelper.BookmarkEntry._ID, _id);
        values.put(DBHelper.BookmarkEntry.COLUMN_SERIES_ID, seriesId);
        values.put(DBHelper.BookmarkEntry.COLUMN_PAGE_ID, pageId);
        values.put(DBHelper.BookmarkEntry.COLUMN_NOTE, note);
        return values;
    }

    public int getId()
    {
        return _id;
    }

    public void setId(int _id)
    {
        this._id = _id;
    }

    public int getSeriesId()
    {
        return seriesId;
    }

    public void setSeriesId(int seriesId)
    {
        this.seriesId = seriesId;
    }

    public int getPageId()
    {
        return pageId;
    }

    public void setPageId(int pageId)
    {
        this.pageId = pageId;
    }

    public String getNote()
    {
        return note;
    }

    public void setNote(String note)
    {
        this.note = note;
    }
}
