package ninja.dudley.yamr.model.js;

import ninja.dudley.yamr.model.Series
import org.mozilla.javascript.ScriptableObject

/**
* Created by mdudley on 7/19/15. Yup.
*/
class JsSeries : ScriptableObject
{
    override fun getClassName(): String?
    {
        return "JsSeries"
    }

    private var url: String

    private var name: String
    private var description: String? = null
    private var alternateName: String? = null
    private var complete: Boolean = false
    private var author: String? = null
    private var artist: String? = null
    private var thumbnailUrl: String? = null

    constructor() : super()
    {
        this.url = ""
        this.name = ""
    }

    fun jsGet_url(): String?
    {
        return url;
    }
    fun jsSet_url(url: String)
    {
        this.url = url;
    }

    fun jsGet_name(): String
    {
        return name
    }
    fun jsSet_name(name: String)
    {
        this.name = name
    }

    fun jsGet_description(): String?
    {
        return description
    }
    fun jsSet_description(description: String?)
    {
        this.description = description
    }

    fun jsGet_alternateName(): String?
    {
        return alternateName
    }
    fun jsSet_alternateName(alternateName: String?)
    {
        this.alternateName = alternateName
    }

    fun jsGet_complete(): Boolean
    {
        return complete
    }
    fun jsSet_complete(complete: Boolean)
    {
        this.complete = complete
    }

    fun jsGet_author(): String?
    {
        return author
    }
    fun jsSet_author(author: String?)
    {
        this.author = author
    }

    fun jsGet_artist(): String?
    {
        return artist
    }
    fun jsSet_artist(artist: String?)
    {
        this.artist = artist
    }

    fun jsGet_thumbnailUrl(): String?
    {
        return thumbnailUrl
    }
    fun jsSet_thumbnailUrl(thumbnailUrl: String?)
    {
        this.thumbnailUrl = thumbnailUrl
    }

    fun unJS(providerId: Int): Series
    {
        val ret = Series(providerId, url, name)
        ret.description = description
        ret.alternateName = alternateName
        ret.complete = complete
        ret.author = author
        ret.artist = artist
        ret.thumbnailUrl = thumbnailUrl
        return ret;
    }
}
