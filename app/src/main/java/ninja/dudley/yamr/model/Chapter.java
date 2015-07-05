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
    private int seriesId;

    public static final String nameCol = "name";
    @Column(name=nameCol)
    private String name;

    public static final String numberCol = "number";
    @Column(name=numberCol, type= Column.Type.Real)
    private float number;

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

    public Uri uri()
    {
        return uri(id);
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
}
