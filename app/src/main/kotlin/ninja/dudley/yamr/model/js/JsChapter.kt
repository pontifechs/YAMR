package ninja.dudley.yamr.model.js

import ninja.dudley.yamr.model.Chapter
import org.mozilla.javascript.ScriptableObject

/**
 * Created by mdudley on 7/19/15.
 */
public class JsChapter : ScriptableObject
{
    override fun getClassName(): String?
    {
        return "JsChapter"
    }

    private var url: String

    private var name: String? = null
    private var number: Double

    public constructor(): super()
    {
        url = ""
        number = 0.0
    }

    public fun jsGet_url(): String?
    {
        return url;
    }
    public fun jsSet_url(url: String)
    {
        this.url = url;
    }

    public fun jsGet_name(): String?
    {
        return name
    }
    public fun jsSet_name(name: String?)
    {
        this.name = name
    }

    public fun jsGet_number(): Double
    {
        return number
    }
    public fun jsSet_number(number: Double)
    {
        this.number = number
    }

    public fun unJS(seriesId: Int): Chapter
    {
        val ret = Chapter(seriesId, url, number.toFloat())
        ret.name = name
        return ret
    }
}
