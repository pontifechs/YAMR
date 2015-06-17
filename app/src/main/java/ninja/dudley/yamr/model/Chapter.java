package ninja.dudley.yamr.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.List;

import ninja.dudley.yamr.db.DBHelper;

/**
 * Created by mdudley on 5/19/15.
 */
public class Chapter extends MangaElement
{
    private int seriesId;

    private String name;
    private float number;
    private List<Page> pages;

    public static Uri baseUri()
    {
        return Uri.parse("content://" + DBHelper.AUTHORITY + "/chapter");
    }

    public static Uri uri(int id)
    {
        Uri base = baseUri();
        return base.buildUpon().appendPath(Integer.toString(id)).build();
    }

    public Chapter() {}

    public Chapter(Cursor c)
    {
        super(c);
        int seriesIdCol = c.getColumnIndex(DBHelper.ChapterEntry.COLUMN_SERIES_ID);
        int nameCol = c.getColumnIndex(DBHelper.ChapterEntry.COLUMN_NAME);
        int numberCol = c.getColumnIndex(DBHelper.ChapterEntry.COLUMN_NUMBER);
        seriesId = c.getInt(seriesIdCol);
        name = c.getString(nameCol);
        number = c.getFloat(numberCol);
        c.close();
    }

    @Override
    public ContentValues getContentValues()
    {
        ContentValues values = super.getContentValues();
        values.put(DBHelper.ChapterEntry.COLUMN_SERIES_ID, seriesId);
        values.put(DBHelper.ChapterEntry.COLUMN_NAME, name);
        values.put(DBHelper.ChapterEntry.COLUMN_NUMBER, number);
        return values;
    }

    public Uri uri()
    {
        return uri(_id);
    }

    public int getSeriesId()
    {
        return seriesId;
    }

    public void setSeriesId(int seriesId)
    {
        this.seriesId = seriesId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public float getNumber()
    {
        return number;
    }

    public void setNumber(float number)
    {
        this.number = number;
    }

    public List<Page> getPages()
    {
        return pages;
    }

    public void setPages(List<Page> pages)
    {
        this.pages = pages;
    }
}
