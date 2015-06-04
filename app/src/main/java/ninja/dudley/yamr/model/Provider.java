package ninja.dudley.yamr.model;

import android.content.ContentValues;
import android.database.Cursor;

import ninja.dudley.yamr.db.DBHelper;

/**
 * Created by mdudley on 6/2/15.
 */
public class Provider extends MangaElement
{
    private String name;

    public Provider() {}

    public Provider(Cursor c)
    {
        super(c);
        int nameCol = c.getColumnIndex(DBHelper.ProviderEntry.COLUMN_NAME);
        name = c.getString(nameCol);
    }

    @Override
    public ContentValues getContentValues()
    {
        ContentValues values = super.getContentValues();
        values.put(DBHelper.ProviderEntry.COLUMN_NAME, name);
        return values;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
