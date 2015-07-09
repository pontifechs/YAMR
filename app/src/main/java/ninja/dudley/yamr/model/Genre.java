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
    public int id = -1;

    public static final String nameCol = "name";
    @Column(name = nameCol)
    public String name;


    // Uri Handling --------------------------------------------------------------------------------
    public static Uri baseUri()
    {
        return Uri.parse("content://" + DBHelper.AUTHORITY + "/genre");
    }

    public static Uri uri(int id)
    {
        return baseUri().buildUpon().appendPath(Integer.toString(id)).build();
    }

    public Uri uri()
    {
        return uri(this.id);
    }

    public static Uri relator()
    {
        return baseUri().buildUpon().appendPath("relator").build();
    }

    public static Uri series(int id)
    {
        return uri(id).buildUpon().appendPath("series").build();
    }

    public Uri series()
    {
        return series(id);
    }


    // Construction / Persistence ------------------------------------------------------------------
    private Genre(int _id, String name)
    {
        this.id = _id;
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
        this.id = c.getInt(idCol);
        this.name = c.getString(nameColIndex);
        c.close();
    }

    public ContentValues getContentValues()
    {
        ContentValues values = new ContentValues();
        if (id != -1)
        {
            values.put(DBHelper.ID, id);
        }
        values.put(nameCol, name);
        return values;
    }
}
