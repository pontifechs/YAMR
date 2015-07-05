package ninja.dudley.yamr.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.HashSet;
import java.util.Set;

import ninja.dudley.yamr.db.DBHelper;
import ninja.dudley.yamr.db.util.Column;
import ninja.dudley.yamr.db.util.Id;
import ninja.dudley.yamr.db.util.Table;

/**
 * Created by mdudley on 6/25/15.
 */
@Table(Genre.tableName)
public class Genre
{
    public static final String tableName = "genre";

    public static Set<Genre> genres(Cursor c)
    {
        Set<Genre> genres = new HashSet<>();
        while (c.moveToNext())
        {
            int idCol = c.getColumnIndex(DBHelper.ID);
            int nameColIndex = c.getColumnIndex(nameCol);
            Genre g = new Genre(c.getInt(idCol), c.getString(nameColIndex));
            genres.add(g);
        }
        return genres;
    }

    public static ContentValues SeriesGenreRelator(int seriesId, int genreId)
    {
        ContentValues values = new ContentValues();
        values.put(DBHelper.SeriesGenreEntry.COLUMN_SERIES_ID, seriesId);
        values.put(DBHelper.SeriesGenreEntry.COLUMN_GENRE_ID, genreId);
        return values;
    }

    @Id
    private int _id = -1;

    public static final String nameCol = "name";
    @Column(name=nameCol)
    private String name;

    public static Uri baseUri()
    {
        return Uri.parse("content://" + DBHelper.AUTHORITY + "/genre");
    }

    public static Uri uri(int id)
    {
        Uri baseUri = baseUri();
        return baseUri.buildUpon().appendPath(Integer.toString(id)).build();
    }

    public Uri uri()
    {
        return uri(this._id);
    }

    public ContentValues getContentValues()
    {
        ContentValues values = new ContentValues();
        if (_id != -1)
        {
            values.put(DBHelper.ID, _id);
        }
        values.put(nameCol, name);
        return values;
    }

    private Genre(int _id, String name)
    {
        this._id = _id;
        this.name = name;
    }

    public Genre(String name)
    {
        this.name = name;
    }

    public Genre(Cursor c)
    {
        c.moveToFirst();
        int idCol = c.getColumnIndex(DBHelper.ID);
        int nameColIndex = c.getColumnIndex(nameCol);
        this._id = c.getInt(idCol);
        this.name = c.getString(nameColIndex);
        c.close();
    }

    public int getId()
    {
        return _id;
    }

    public void setId(int _id)
    {
        this._id = _id;
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
