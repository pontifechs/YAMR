package ninja.dudley.yamr.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.HashSet;
import java.util.Set;

import ninja.dudley.yamr.db.DBHelper;

/**
 * Created by mdudley on 6/25/15.
 */
public class Genre
{
    public static Set<Genre> genres(Cursor c)
    {
        Set<Genre> genres = new HashSet<>();
        while (c.moveToNext())
        {
            int idCol = c.getColumnIndex(DBHelper.GenreEntry._ID);
            int nameCol = c.getColumnIndex(DBHelper.GenreEntry.COLUMN_NAME);
            Genre g = new Genre(c.getInt(idCol), c.getString(nameCol));
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

    private int _id = -1;
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
            values.put(DBHelper.GenreEntry._ID, _id);
        }
        values.put(DBHelper.GenreEntry.COLUMN_NAME, name);
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
        int idCol = c.getColumnIndex(DBHelper.GenreEntry._ID);
        int nameCol = c.getColumnIndex(DBHelper.GenreEntry.COLUMN_NAME);
        this._id = c.getInt(idCol);
        this.name = c.getString(nameCol);
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
