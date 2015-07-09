package ninja.dudley.yamr.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import ninja.dudley.yamr.db.DBHelper;
import ninja.dudley.yamr.db.util.Column;
import ninja.dudley.yamr.db.util.ForeignKey;
import ninja.dudley.yamr.db.util.Table;

/**
 * Created by mdudley on 5/19/15.
 */
@Table(Chapter.tableName)
public class Chapter extends MangaElement
{
    public static final String tableName = "chapter";

    public static final String seriesIdCol = Series.tableName + DBHelper.ID;
    @ForeignKey(value=Series.class, name=seriesIdCol)
    public final int seriesId;

    public static final String nameCol = "name";
    @Column(name=nameCol)
    public String name;

    public static final String numberCol = "number";
    @Column(name=numberCol, type= Column.Type.Real)
    public float number;


    // Uri Handling --------------------------------------------------------------------------------
    public static Uri baseUri()
    {
        return Uri.parse("content://" + DBHelper.AUTHORITY + "/chapter");
    }

    public static Uri uri(int id)
    {
        return baseUri().buildUpon().appendPath(Integer.toString(id)).build();
    }
    public Uri uri()
    {
        return uri(id);
    }

    public static Uri pages(int id)
    {
        return uri(id).buildUpon().appendPath("pages").build();
    }
    public Uri pages()
    {
        return pages(id);
    }

    public static Uri prevPage(int id, float num)
    {
        return pages(id).buildUpon().appendPath(Float.toString(num)).appendPath("prev").build();
    }
    public Uri prevPage(float num)
    {
        return prevPage(id, num);
    }

    public static Uri nextPage(int id, float num)
    {
        return pages(id).buildUpon().appendPath(Float.toString(num)).appendPath("next").build();
    }
    public Uri nextPage(float num)
    {
        return nextPage(id, num);
    }


    // Construction / Persistence ------------------------------------------------------------------
    public Chapter() { seriesId = -1; }
    public Chapter(int seriesId) { this.seriesId = seriesId; }
    public Chapter(Cursor c)
    {
        super(c);
        seriesId = getInt(c, seriesIdCol);
        name = getString(c, nameCol);
        number = getFloat(c, numberCol);
        c.close();
    }

    @Override
    public ContentValues getContentValues()
    {
        ContentValues values = super.getContentValues();
        values.put(seriesIdCol, seriesId);
        values.put(nameCol, name);
        values.put(numberCol, number);
        return values;
    }
}
