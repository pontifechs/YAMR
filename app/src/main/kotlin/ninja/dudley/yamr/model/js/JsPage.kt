package ninja.dudley.yamr.model.js

import ninja.dudley.yamr.model.Page
import org.mozilla.javascript.ScriptableObject

/**
* Created by mdudley on 7/21/15. Yup.
*/
class JsPage : ScriptableObject
{
    override fun getClassName(): String?
    {
        return "JsPage"
    }

    private var url: String
    private var number: Double

    private var imageUrl: String? = null

    constructor() : super()
    {
        // Kinda hate you, Rhino.
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

    fun jsGet_number(): Double
    {
        return number
    }
    fun jsSet_number(number: Double)
    {
        this.number = number
    }

    fun jsGet_imageUrl(): String?
    {
        return imageUrl
    }
    fun jsSet_imageUrl(imageUrl: String?)
    {
        this.imageUrl = imageUrl
    }

    fun unJS(chapterId: Int): Page
    {
        val ret = Page(chapterId, url, number.toFloat())
        ret.imageUrl = imageUrl
        return ret
    }
}
