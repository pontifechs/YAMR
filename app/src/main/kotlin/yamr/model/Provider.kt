package ninja.dudley.yamr.model

import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

import ninja.dudley.yamr.db.DBHelper
import ninja.dudley.yamr.db.util.Column
import ninja.dudley.yamr.db.util.Table

/**
 * Created by mdudley on 6/2/15.
 */
Table(Provider.tableName)
public class Provider : MangaElement
{
    Column(name = nameCol)
    public var name: String
    Column(name = newUrlCol)
    public var newUrl: String

    public fun uri(): Uri
    {
        return uri(id)
    }

    public fun series(): Uri
    {
        return series(id)
    }

    // Construction / Persistence ------------------------------------------------------------------

    public constructor(c: Cursor) : super(c)
    {
        name = getString(c, nameCol)!!
        newUrl = getString(c, newUrlCol)!!
        c.close()
    }

    override fun getContentValues(): ContentValues
    {
        val values = super.getContentValues()
        values.put(nameCol, name)
        values.put(newUrlCol, newUrl)
        return values
    }

    companion object
    {
        public val tableName: String = "provider"
        public val nameCol: String = "name"
        public val newUrlCol: String = "new_url"

        // Uri Handling --------------------------------------------------------------------------------
        public fun baseUri(): Uri
        {
            return Uri.parse("content://" + DBHelper.AUTHORITY + "/provider")
        }

        public fun uri(id: Int): Uri
        {
            val base = baseUri()
            return base.buildUpon().appendPath(Integer.toString(id)).build()
        }

        public fun series(id: Int): Uri
        {
            return uri(id).buildUpon().appendPath("series").build()
        }

        public fun all(): Uri
        {
            return baseUri().buildUpon().appendPath("all").build()
        }
    }
}
