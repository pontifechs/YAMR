package ninja.dudley.yamr.model;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.NoSuchElementException;

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
        if (c.getCount() <= 0)
        {
            c.close();
            throw new NoSuchElementException("Empty Cursor!");
        }
        if (c.getPosition() == -1)
        {
            c.moveToFirst();
        }
        _id = getInt(c, DBHelper.MangaElementEntry._ID);
        url = getString(c, DBHelper.MangaElementEntry.COLUMN_URL);
        fullyParsed = getBool(c, DBHelper.MangaElementEntry.COLUMN_FULLY_PARSED);
        // Can't close yet, others need the cursor still
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

    protected String getString(Cursor c, String col)
    {
        int colNum = c.getColumnIndex(col);
        return c.getString(colNum);
    }

    protected int getInt(Cursor c, String col)
    {
        int colNum = c.getColumnIndex(col);
        if (c.isNull(colNum))
        {
            return -1;
        }
        return c.getInt(colNum);
    }

    protected float getFloat(Cursor c, String col)
    {
        int colNum = c.getColumnIndex(col);
        return c.getFloat(colNum);
    }

    protected boolean getBool(Cursor c, String col)
    {
        int colNum = c.getColumnIndex(col);
        return c.getInt(colNum) > 0;
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
