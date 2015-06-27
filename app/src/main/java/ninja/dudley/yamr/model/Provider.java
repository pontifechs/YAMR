package ninja.dudley.yamr.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import ninja.dudley.yamr.db.DBHelper;

/**
 * Created by mdudley on 6/2/15.
 */
public class Provider extends MangaElement
{
    private String name;
    private String newUrl;

    public static Uri baseUri()
    {
        return Uri.parse("content://" + DBHelper.AUTHORITY + "/provider");
    }

    public static Uri uri(int id)
    {
        Uri base = baseUri();
        return base.buildUpon().appendPath(Integer.toString(id)).build();
    }

    public Provider() {}

    public Provider(Cursor c)
    {
        super(c);
        int nameCol = c.getColumnIndex(DBHelper.ProviderEntry.COLUMN_NAME);
        int newUrlCol = c.getColumnIndex(DBHelper.ProviderEntry.COLUMN_NEW_URL);
        name = c.getString(nameCol);
        newUrl = c.getString(newUrlCol);
        c.close();
    }

    @Override
    public ContentValues getContentValues()
    {
        ContentValues values = super.getContentValues();
        values.put(DBHelper.ProviderEntry.COLUMN_NAME, name);
        return values;
    }

    public Uri uri()
    {
        return uri(_id);
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getNewUrl()
    {
        return newUrl;
    }

    public void setNewUrl(String newUrl)
    {
        this.newUrl = newUrl;
    }
}
