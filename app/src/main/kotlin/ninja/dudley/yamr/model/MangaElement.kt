package ninja.dudley.yamr.model

import android.content.ContentValues
import android.database.Cursor

import java.util.NoSuchElementException

import ninja.dudley.yamr.db.DBHelper
import ninja.dudley.yamr.db.util.Column
import ninja.dudley.yamr.db.util.Id

/**
* Created by mdudley on 6/2/15. Yup.
*/
open class MangaElement
{
    enum class UriType
    {
        Provider,
        Series,
        Genre,
        Chapter,
        Page,
    }

    @Id
    val id: Int
    @Column(name = urlCol)
    var url: String
    @Column(name = fullyParsedCol)
    var fullyParsed: Boolean = false
    @Column(name = typeCol)
    var type: UriType

    constructor(url: String, type: MangaElement.UriType)
    {
        id = -1
        this.url = url;
        this.type = type;
    }

    constructor(c: Cursor)
    {
        if (c.count <= 0)
        {
            c.close()
            throw NoSuchElementException("Empty Cursor!")
        }
        if (c.position == -1)
        {
            c.moveToFirst()
        }
        id = getInt(c, DBHelper.ID)
        url = getString(c, urlCol)!!
        fullyParsed = getBool(c, fullyParsedCol)
        type = UriType.valueOf(getString(c, typeCol)!!)
    }

    open fun getContentValues(): ContentValues
    {
        val values = ContentValues()
        if (id != -1)
        {
            values.put(DBHelper.ID, id)
        }
        values.put(urlCol, url)
        values.put(fullyParsedCol, fullyParsed)
        values.put(typeCol, type.name)
        return values
    }

    protected fun getString(c: Cursor, col: String): String?
    {
        val colNum = c.getColumnIndex(col)
        return c.getString(colNum)
    }

    protected fun getInt(c: Cursor, col: String): Int
    {
        val colNum = c.getColumnIndex(col)
        if (c.isNull(colNum))
        {
            return -1
        }
        return c.getInt(colNum)
    }

    protected fun getFloat(c: Cursor, col: String): Float
    {
        val colNum = c.getColumnIndex(col)
        return c.getFloat(colNum)
    }

    protected fun getBool(c: Cursor, col: String): Boolean
    {
        val colNum = c.getColumnIndex(col)
        return c.getInt(colNum) > 0
    }

    companion object
    {
        const val urlCol: String = "url"
        const val fullyParsedCol: String = "fully_parsed"
        const val typeCol: String = "type"
    }
}
