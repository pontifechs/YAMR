package ninja.dudley.yamr.model;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.NoSuchElementException;

import ninja.dudley.yamr.db.DBHelper;
import ninja.dudley.yamr.db.util.Column;
import ninja.dudley.yamr.db.util.Id;

/**
 * Created by mdudley on 6/2/15.
 */
public class MangaElement
{
    @Id
    protected int id = -1;

    public static final String urlCol= "url";
    @Column(name=urlCol)
    protected String url;

    public static final String fullyParsedCol = "fully_parsed";
    @Column(name=fullyParsedCol)
    protected boolean fullyParsed = false;

    public MangaElement() {}
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
        id = getInt(c, DBHelper.ID);
        url = getString(c, urlCol);
        fullyParsed = getBool(c, fullyParsedCol);
        // Can't close yet, others need the cursor still
    }

    public ContentValues getContentValues()
    {
        ContentValues values = new ContentValues();
        if (id != -1)
        {
            values.put(DBHelper.ID , id);
        }
        values.put(urlCol, url);
        values.put(fullyParsedCol, fullyParsed);
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
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
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
