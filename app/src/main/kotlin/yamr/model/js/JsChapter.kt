package yamr.model.js

import ninja.dudley.yamr.model.MangaElement
import org.mozilla.javascript.ScriptableObject

/**
 * Created by mdudley on 7/19/15.
 */

public class JsChapter : ScriptableObject
{
    override fun getClassName(): String?
    {
        return "JsSeries"
    }

    private var id: Int = -1
    private var url: String
    private var fullyParsed: Boolean = false
    private var type: MangaElement.UriType? = null

    private var name: String? = null
    private var number: Float

    private constructor(url: String, number: Float) : super()
    {
        this.url = url
        this.number = number
    }

    public fun jsConstructor(url: String, number: Float): JsChapter
    {
        return JsChapter(url, number)
    }


}

