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
    public String name;

    public static final String newUrlCol = "new_url";
    @Column(name=newUrlCol)
    public String newUrl;

    // Uri Handling --------------------------------------------------------------------------------
    public static Uri baseUri()
    {
        return Uri.parse("content://" + DBHelper.AUTHORITY + "/provider");
    }

    public static Uri uri(int id)
    {
        Uri base = baseUri();
        return base.buildUpon().appendPath(Integer.toString(id)).build();
    }
    public Uri uri()
    {
        return uri(id);
    }

    public static Uri series(int id)
    {
        return uri(id).buildUpon().appendPath("series").build();
    }
    public Uri series()
    {
        return series(id);
    }

    public static Uri all()
    {
        return baseUri().buildUpon().appendPath("all").build();
    }

    // Construction / Persistence ------------------------------------------------------------------
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
}
