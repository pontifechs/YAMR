package ninja.dudley.yamr.svc.fetchers

import android.content.Context
import ninja.dudley.yamr.model.Chapter
import ninja.dudley.yamr.model.Genre
import ninja.dudley.yamr.model.Page
import ninja.dudley.yamr.model.Series
import ninja.dudley.yamr.svc.FetcherSync
import ninja.dudley.yamr.ui.activities.Settings
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.*

/**
 * Created by matt on 9/16/16.
 */
class Batoto : FetcherSync
{
    val context: Context
    constructor(context: Context) : super(context)
    {
        this.context = context
    }

    override fun enumerateSeries(): List<Series>
    {
        val series = ArrayList<Series>()
        var i = 0
        while (true)
        {
            val doc = fetchUrl("http://bato.to/search_ajax?p=" + i)
            // approximately 572 pages of these at the moment
            if (hasNoMorePages(doc) || i > 1000)
            {
                break
            }
            val elements = doc.select("strong > a")
            for (seriesLink in elements)
            {
                series.add(Series(3, seriesLink.absUrl("href"), seriesLink.ownText()))
            }
            i++
        }
        return series
    }

    private fun hasNoMorePages(doc: Document): Boolean
    {
        return doc.select("td").first().ownText().equals("No (more) comics found!")
    }

    override fun fillSeries(series: Series): Series
    {
        val doc = fetchUrl(series.url)
        series.url = doc.location()
        series.thumbnailUrl = doc.select(".ipsBox div > img").first().absUrl("src")

        val rows = doc.select(".ipsBox .ipb_table tr")
        for (row in rows)
        {
            val propField = row.select("td:eq(0)").first().ownText()
            val propValSpan = row.select("td:eq(1) span").first()
            val propValLink = row.select("td:eq(1) a").first()
            val propValRaw = row.select("td:eq(1)").first()

            when (propField)
            {
                "Alt Names:" -> series.alternateName = propValSpan?.ownText()
                "Author:" -> series.author = propValLink?.ownText()
                "Artist:" -> series.artist = propValLink?.ownText()
                "Status:" -> series.complete = propValRaw?.ownText().equals("Complete")
                "Description:" -> series.description = propValRaw?.ownText()
            }
        }
        return series
    }

    override fun enumerateGenres(series: Series): List<Genre>
    {
        val doc = fetchUrl(series.url)

        val rows = doc.select(".ipsBox .ipb_table tr")
        val genres = ArrayList<Genre>()
        for (row in rows)
        {
            val propField = row.select("td:eq(0)").first().ownText()
            when (propField)
            {
                "Genres:" ->
                {
                    val imgs = row.select("img")
                    imgs.forEach {
                        val text = it.attr("alt")
                        if (!text.equals("edit"))
                        {
                            genres.add(Genre(text))
                        }
                    }
                }
            }
        }
        return genres
    }

    override fun enumerateChapters(series: Series): List<Chapter>
    {
        val doc = fetchLoggedInUrl(series.url)
        val chapters = doc.select(".row.lang_English.chapter_row a[title]")
        return chapters.map {
            val number = it.attr("title").split(" | Sort: ")[1].toFloat()
            val chapter = Chapter(series.id, it.absUrl("href"), number)
            chapter.name = it.ownText()
            chapter
        }
    }

    override fun fillChapter(chapter: Chapter): Chapter
    {
        return chapter
    }

    override fun enumeratePages(chapter: Chapter): List<Page>
    {
        verifyLoggedIn()
        val reader = Jsoup.connect("http://bato.to/areader?id=" + getId(chapter.url) + "&p=1")
                .timeout(10000)
                .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                .referrer("http://bato.to/reader")
                .cookies(cookies)
                .get()

        val pages = reader.select("#page_select option")
        return pages.map {
            val number = it.ownText().split(" ")[1].toFloat()
            Page(chapter.id, it.absUrl("value"), number)
        }
    }

    override fun fillPage(page: Page): Page
    {
        verifyLoggedIn()
        val reader = Jsoup.connect("http://bato.to/areader?id=" + getId(page.url) + "&p=" + getPage(page.url))
                .timeout(10000)
                .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                .referrer("http://bato.to/reader")
                .cookies(cookies)
                .get()
        page.imageUrl = reader.getElementById("comic_page").absUrl("src")
        return page
    }

    override fun enumerateNew(): List<Pair<Series, Chapter>>
    {
        val doc = fetchUrl("http://bato.to/")
        val rows = doc.select(".ipb_table.chapters_list tr")

        val pairs = ArrayList<Pair<Series, Chapter>>()

        var currentSeries: Series? = null
        for (i in 1..rows.size-1)
        {
            val row = rows[i]
            // Series
            if (row.children().size == 2)
            {
                val link = row.select("td:eq(1) a:eq(1)").first()
                currentSeries = Series(3, link.absUrl("href"), link.ownText())
            }
            // Chapter
            else
            {
                val link = row.select("td a[href]").first()
                val flagDiv = row.select("div[title]").first()
                if (!flagDiv.attr("title").equals("English"))
                {
                    continue
                }
                val chapter = Chapter(-1, link.absUrl("href"), -1.0f)
                pairs.add(Pair(currentSeries!!, chapter))
            }
        }
        return pairs
    }


    companion object
    {
        private var cookies: MutableMap<String, String> = HashMap()
        private var loginDate = Date(0)
        val OneHour = 1000 * 60 * 60
    }

    private fun fetchLoggedInUrl(url: String): Document
    {
        verifyLoggedIn()
        val response = Jsoup.connect(url)
                .timeout(10000)
                .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                .method(Connection.Method.GET)
                .cookies(cookies)
                .execute()
        return response.parse()
    }

    private fun login()
    {
        val forumPage = Jsoup.connect("https://bato.to/forums/")
                .method(Connection.Method.GET)
                .execute()

        val response = Jsoup.connect("https://bato.to/forums/index.php?app=core&module=global&section=login&do=process")
                .data("auth_key", "880ea6a14ea49e853634fbdc5015a024")
                .data("referer", "https://bato.to/forums/")
                .data("ips_username", Settings.batotoUsername(context))
                .data("ips_password", Settings.batotoPassword(context))
                .data("rememberMe", "1")
                .cookies(forumPage.cookies())
                .method(Connection.Method.GET)
                .followRedirects(false)
                .execute()

        if (response.statusCode() != 302)
        {
            throw IllegalArgumentException("Couldn't log in")
        }

        cookies = response.cookies()
    }

    private fun verifyLoggedIn()
    {
        val now = Date(System.currentTimeMillis())
        val duration = now.time - loginDate.time

        // If we don't have any cookies, we're definitely not logged in.
        if (cookies.isEmpty() || !cookies.containsKey("session_id") || duration > OneHour)
        {
            login()
            loginDate = now
        }
    }

    private fun getId(url: String): String
    {
        val hash = url.split("#")[1]
        return hash.split("_")[0]
    }

    private fun getPage(url: String): String
    {
        val hash = url.split("#")[1]
        return hash.split("_")[1]
    }
}
