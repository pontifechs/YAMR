package ninja.dudley.yamr.model.js

import ninja.dudley.yamr.model.Page
import org.mozilla.javascript.ScriptableObject

/**
* Created by mdudley on 7/21/15. Yup.
*/
public class JsPage : ScriptableObject
{
    override fun getClassName(): String?
    {
        return "JsPage"
    }

    private var url: String
    private var number: Double

    private var imageUrl: String? = null

    public constructor() : super()
    {
        // Kinda hate you, Rhino.
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

    public fun jsGet_number(): Double
    {
        return number
    }
    public fun jsSet_number(number: Double)
    {
        this.number = number
    }

    public fun jsGet_imageUrl(): String?
    {
        return imageUrl
    }
    public fun jsSet_imageUrl(imageUrl: String?)
    {
        this.imageUrl = imageUrl
    }

    public fun unJS(chapterId: Int): Page
    {
        val ret = Page(chapterId, url, number.toFloat())
        ret.imageUrl = imageUrl
        return ret
    }
}
