package yamr.model.js;

import ninja.dudley.yamr.model.MangaElement;
import ninja.dudley.yamr.model.Series
import org.mozilla.javascript.ScriptableObject

/**
 * Created by mdudley on 7/19/15.
 */
public class JsSeries : ScriptableObject
{
    override fun getClassName(): String?
    {
        return "JsSeries"
    }

    private var id: Int = -1
    private var url: String
    private var fullyParsed: Boolean = false
    private var type: MangaElement.UriType? = null

    private var name: String
    private var description: String? = null
    private var alternateName: String? = null
    private var complete: Boolean = false
    private var author: String? = null
    private var artist: String? = null
    private var thumbnailUrl: String? = null

    private constructor(url: String, name: String)
    {
        this.url = url
        this.name = name
    }

    public fun jsConstructor(url: String, name: String): JsSeries
    {
        return JsSeries(url, name)
    }

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

    public fun jsGet_description(): String?
    {
        return description
    }
    public fun jsSet_description(description: String?)
    {
        this.description = description
    }

    public fun jsGet_alternateName(): String?
    {
        return alternateName
    }
    public fun jsSet_alternateName(alternateName: String?)
    {
        this.alternateName = alternateName
    }

    public fun jsGet_complete(): Boolean
    {
        return complete
    }
    public fun jsSet_complete(complete: Boolean)
    {
        this.complete = complete
    }

    public fun jsGet_author(): String?
    {
        return author
    }
    public fun jsSet_author(author: String?)
    {
        this.author = author
    }

    public fun jsGet_artist(): String?
    {
        return artist
    }
    public fun jsSet_artist(artist: String?)
    {
        this.artist = artist
    }

    public fun jsGet_thumbnailUrl(): String?
    {
        return thumbnailUrl
    }
    public fun jsSet_thumbnailUrl(thumbnailUrl: String?)
    {
        this.thumbnailUrl = thumbnailUrl
    }

    public fun unJS(providerId: Int): Series
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
