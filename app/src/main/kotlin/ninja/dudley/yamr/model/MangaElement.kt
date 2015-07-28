package ninja.dudley.yamr.model

import android.content.ContentValues
import android.database.Cursor

import java.util.NoSuchElementException

import ninja.dudley.yamr.db.DBHelper
import ninja.dudley.yamr.db.util.Column
import ninja.dudley.yamr.db.util.Id

/**
 * Created by mdudley on 6/2/15.
 */
public open class MangaElement
{
    enum class UriType
    {
        Provider,
        Series,
        Genre,
        Chapter,
        Page,
    }

    Id
    public val id: Int
    Column(name = urlCol)
    public var url: String
    Column(name = fullyParsedCol)
    public var fullyParsed: Boolean = false
    Column(name = typeCol)
    public var type: UriType

    public constructor(url: String, type: MangaElement.UriType)
    {
        id = -1
        this.url = url;
        this.type = type;
    }

    public constructor(c: Cursor)
    {
        if (c.getCount() <= 0)
        {
            c.close()
            throw NoSuchElementException("Empty Cursor!")
        }
        if (c.getPosition() == -1)
        {
            c.moveToFirst()
        }
        id = getInt(c, DBHelper.ID)
        url = getString(c, urlCol)!!
        fullyParsed = getBool(c, fullyParsedCol)
        type = UriType.valueOf(getString(c, typeCol)!!)
    }

    public open fun getContentValues(): ContentValues
    {
        val values = ContentValues()
        if (id != -1)
        {
            values.put(DBHelper.ID, id)
        }
        values.put(urlCol, url)
        values.put(fullyParsedCol, fullyParsed)
        values.put(typeCol, type.name())
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
        public val urlCol: String = "url"
        public val fullyParsedCol: String = "fully_parsed"
        public val typeCol: String = "type"
    }
}
