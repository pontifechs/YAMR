package ninja.dudley.yamr.model

import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import ninja.dudley.yamr.db.DBHelper
import ninja.dudley.yamr.db.util.Column
import ninja.dudley.yamr.db.util.Table
import ninja.dudley.yamr.db.util.Unique

/**
* Created by mdudley on 6/2/15. Yup.
*/
@Table(Provider.tableName)
class Provider : MangaElement
{
    @Unique
    @Column(name = nameCol)
    val name: String

    // Construction / Persistence ------------------------------------------------------------------
    constructor(c: Cursor, closeAfter: Boolean = true) : super(c)
    {
        if (type != MangaElement.UriType.Provider)
        {
            throw IllegalArgumentException("Attempted to make a provider from a $type")
        }
        name = getString(c, nameCol)!!
        if (closeAfter)
        {
            c.close()
        }
    }

    override fun getContentValues(): ContentValues
    {
        val values = super.getContentValues()
        values.put(nameCol, name)
        return values
    }

    // Uri Handling --------------------------------------------------------------------------------
    fun uri(): Uri
    {
        return uri(id)
    }

    fun series(): Uri
    {
        return series(id)
    }

    companion object
    {
        const val tableName: String = "provider"
        const val nameCol: String = "name"

        // Uri Handling ----------------------------------------------------------------------------
        fun baseUri(): Uri
        {
            return Uri.parse("content://" + DBHelper.AUTHORITY + "/provider")
        }

        fun uri(id: Int): Uri
        {
            val base = baseUri()
            return base.buildUpon().appendPath(Integer.toString(id)).build()
        }

        fun series(id: Int): Uri
        {
            return uri(id).buildUpon().appendPath("series").build()
        }

        fun all(): Uri
        {
            return baseUri().buildUpon().appendPath("all").build()
        }
    }
}
