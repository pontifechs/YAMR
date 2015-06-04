package ninja.dudley.yamr.model;

import android.content.ContentValues;
import android.database.Cursor;

import ninja.dudley.yamr.db.DBHelper;

/**
 * Created by mdudley on 6/2/15.
 */
public class MangaElement
{
    protected int _id = -1;
    protected String url;
    protected boolean fullyParsed = false;

    public MangaElement() {}

    // Provider IO
    public MangaElement(Cursor c)
    {
        c.moveToFirst();
        int _idCol = c.getColumnIndex(DBHelper.MangaElementEntry._ID);
        int urlCol = c.getColumnIndex(DBHelper.MangaElementEntry.COLUMN_URL);
        int fullyParsedCol = c.getColumnIndex(DBHelper.MangaElementEntry.COLUMN_FULLY_PARSED);
        _id = c.getInt(_idCol);
        url = c.getString(urlCol);
        fullyParsed = c.getInt(fullyParsedCol) > 0;
    }

    public ContentValues getContentValues()
    {
        ContentValues values = new ContentValues();
        if (_id != -1)
        {
            values.put(DBHelper.MangaElementEntry._ID, _id);
        }
        values.put(DBHelper.MangaElementEntry.COLUMN_URL, url);
        values.put(DBHelper.MangaElementEntry.COLUMN_FULLY_PARSED, fullyParsed);
        return values;
    }

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        this._id = id;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public boolean isFullyParsed()
    {
        return fullyParsed;
    }

    public void setFullyParsed(boolean fullyParsed)
    {
        this.fullyParsed = fullyParsed;
    }
}
