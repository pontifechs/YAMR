package ninja.dudley.yamr.model;


import android.content.ContentValues;
import android.database.Cursor;

import java.util.List;

import ninja.dudley.yamr.db.DBHelper;

/**
 * Created by mdudley on 5/19/15.
 */
public class Series extends MangaElement
{
    private int providerId;

    private String name;
    private List<Chapter> chapters;

    public Series() {}

    public Series(Cursor c)
    {
        super(c);
        int providerIdCol = c.getColumnIndex(DBHelper.SeriesEntry.COLUMN_PROVIDER_ID);
        int nameCol = c.getColumnIndex(DBHelper.SeriesEntry.COLUMN_NAME);
        providerId = c.getInt(providerIdCol);
        name = c.getString(nameCol);
    }

    @Override
    public ContentValues getContentValues()
    {
        ContentValues values = super.getContentValues();
        values.put(DBHelper.SeriesEntry.COLUMN_PROVIDER_ID, providerId);
        values.put(DBHelper.SeriesEntry.COLUMN_NAME, name);
        return values;
    }

    public int getProviderId()
    {
        return providerId;
    }

    public void setProviderId(int providerId)
    {
        this.providerId = providerId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public List<Chapter> getChapters()
    {
        return chapters;
    }

    public void setChapters(List<Chapter> chapters)
    {
        this.chapters = chapters;
    }
}
