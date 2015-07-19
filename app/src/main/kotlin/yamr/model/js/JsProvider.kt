package yamr.model.js

import ninja.dudley.yamr.model.MangaElement
import ninja.dudley.yamr.model.Provider
import org.mozilla.javascript.ScriptableObject

/**
 * Created by mdudley on 7/19/15.
 */
public open class JsProvider : ScriptableObject()
{
    override fun getClassName(): String?
    {
        return "JsProvider"
    }

    private var id: Int = -1
    private var url: String? = null
    private var fullyParsed: Boolean = false
    private var type: MangaElement.UriType? = null

    private var name: String? = null
    private var newUrl: String? = null

    public fun jsConstructor() {}

    public fun jsGet_id(): Int
    {
        return id
    }
    public fun jsSet_id(id: Int)
    {
        this.id = id
    }

    public fun jsGet_url(): String?
    {
        return url;
    }
    public fun jsSet_url(url: String)
    {
        this.url = url;
    }

    public fun jsGet_fullyParsed(): Boolean
    {
        return fullyParsed;
    }
    public fun jsSet_fullyParsed(fullyParsed: Boolean)
    {
        this.fullyParsed = fullyParsed
    }

    public fun jsGet_type(): MangaElement.UriType?
    {
        return type
    }
    public fun jsSet_type(type: MangaElement.UriType)
    {
        this.type = type
    }

    public fun jsGet_name(): String?
    {
        return name
    }
    public fun jsSet_name(name: String?)
    {
        this.name = name
    }

    public fun jsGet_newUrl(): String?
    {
        return newUrl
    }
    public fun jsSet_newUrl(newUrl: String?)
    {
        this.newUrl = newUrl
    }
}

