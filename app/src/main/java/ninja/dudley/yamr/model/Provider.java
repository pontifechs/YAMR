package ninja.dudley.yamr.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import ninja.dudley.yamr.db.DBHelper;
import ninja.dudley.yamr.db.util.Column;
import ninja.dudley.yamr.db.util.Table;

/**
 * Created by mdudley on 6/2/15.
 */
@Table(Provider.tableName)
public class Provider extends MangaElement
{
    public static final String tableName = "provider";

    public static final String nameCol = "name";
    @Column(name=nameCol)
    private String name;

    public static final String newUrlCol = "new_url";
    @Column(name=newUrlCol)
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
        name = getString(c, nameCol);
        newUrl = getString(c, newUrlCol);
        c.close();
    }

    @Override
    public ContentValues getContentValues()
    {
        ContentValues values = super.getContentValues();
        values.put(nameCol, name);
        values.put(newUrlCol, newUrl);
        return values;
    }

    public Uri uri()
    {
        return uri(id);
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
