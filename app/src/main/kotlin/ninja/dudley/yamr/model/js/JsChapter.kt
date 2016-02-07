package ninja.dudley.yamr.model.js

import ninja.dudley.yamr.model.Chapter
import org.mozilla.javascript.ScriptableObject

/**
* Created by mdudley on 7/19/15. Yup.
*/
class JsChapter : ScriptableObject
{
    override fun getClassName(): String?
    {
        return "JsChapter"
    }

    private var url: String

    private var name: String? = null
    private var number: Double

    constructor(): super()
    {
        url = ""
        number = 0.0
    }

    fun jsGet_url(): String?
    {
        return url;
    }
    fun jsSet_url(url: String)
    {
        this.url = url;
    }

    fun jsGet_name(): String?
    {
        return name
    }
    fun jsSet_name(name: String?)
    {
        this.name = name
    }

    fun jsGet_number(): Double
    {
        return number
    }
    fun jsSet_number(number: Double)
    {
        this.number = number
    }

    fun unJS(seriesId: Int): Chapter
    {
        val ret = Chapter(seriesId, url, number.toFloat())
        ret.name = name
        return ret
    }
}
