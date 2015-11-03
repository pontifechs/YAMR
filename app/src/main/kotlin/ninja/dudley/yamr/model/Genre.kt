package ninja.dudley.yamr.model

import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

import java.util.HashSet

import ninja.dudley.yamr.db.DBHelper
import ninja.dudley.yamr.db.util.Column
import ninja.dudley.yamr.db.util.Id
import ninja.dudley.yamr.db.util.Table

/**
 * Created by mdudley on 6/25/15.
 */
@Table(Genre.tableName)
public class Genre
{
    @Id
    public var id: Int = -1
    @Column(name = nameCol)
    public var name: String

    // Construction / Persistence ------------------------------------------------------------------
    private constructor(_id: Int, name: String)
    {
        this.id = _id
        this.name = name
    }

    public constructor(name: String)
    {
        this.name = name
    }

    public constructor(c: Cursor)
    {
        c.moveToFirst()
        val idCol = c.getColumnIndex(DBHelper.ID)
        val nameColIndex = c.getColumnIndex(nameCol)
        this.id = c.getInt(idCol)
        this.name = c.getString(nameColIndex)
        c.close()
    }

    public fun getContentValues(): ContentValues
    {
        val values = ContentValues()
        if (id != -1)
        {
            values.put(DBHelper.ID, id)
        }
        values.put(nameCol, name)
        return values
    }

    // Uri Handling --------------------------------------------------------------------------------
    public fun uri(): Uri
    {
        return uri(this.id)
    }

    public fun series(): Uri
    {
        return series(id)
    }

    companion object
    {
        public val tableName: String = "genre"
        public val nameCol: String = "name"

        // Uri Handling ----------------------------------------------------------------------------
        public fun baseUri(): Uri
        {
            return Uri.parse("content://" + DBHelper.AUTHORITY + "/genre")
        }

        public fun uri(id: Int): Uri
        {
            return baseUri().buildUpon().appendPath(Integer.toString(id)).build()
        }

        public fun relator(): Uri
        {
            return baseUri().buildUpon().appendPath("relator").build()
        }

        public fun series(id: Int): Uri
        {
            return uri(id).buildUpon().appendPath("series").build()
        }

        public fun all(): Uri{
            return baseUri().buildUpon().appendPath("all").build()
        }

        public fun genres(c: Cursor): Set<Genre>
        {
            val genres = HashSet<Genre>()
            while (c.moveToNext())
            {
                val idCol = c.getColumnIndex(DBHelper.ID)
                val nameColIndex = c.getColumnIndex(nameCol)
                val g = Genre(c.getInt(idCol), c.getString(nameColIndex))
                genres.add(g)
            }
            return genres
        }

        public fun SeriesGenreRelator(seriesId: Int, genreId: Int): ContentValues
        {
            val values = ContentValues()
            values.put(DBHelper.SeriesGenreEntry.COLUMN_SERIES_ID, seriesId)
            values.put(DBHelper.SeriesGenreEntry.COLUMN_GENRE_ID, genreId)
            return values
        }
    }
}
