package ninja.dudley.yamr.svc.fetchers

import android.content.Context
import ninja.dudley.yamr.model.Chapter
import ninja.dudley.yamr.model.Genre
import ninja.dudley.yamr.model.Page
import ninja.dudley.yamr.model.Series
import ninja.dudley.yamr.svc.FetcherSync
import java.util.*

/**
 * Created by matt on 9/16/16.
 */
class MangaPanda(context: Context) : FetcherSync(context)
{
    override fun enumerateSeries(): List<Series>
    {
        val doc = fetchUrl("http://www.mangapanda.com/alphabetical")
        val elements = doc.select(".series_alpha li a[href]")
        return elements.map {
            Series(2, it.absUrl("href"), it.ownText())
        }
    }

    override fun fillSeries(series: Series): Series
    {
        val doc = fetchUrl(series.url)
        val thumb = doc.select("#mangaimg img[src]").first()
        series.thumbnailUrl = thumb.absUrl("src")

        val summary = doc.select("#readmangasum p").first()
        series.description = summary.text()

        val properties = doc.select(".propertytitle")
        for (property in properties)
        {
            val propTitle = property.text()
            val sibling = property.parent().select("td:eq(1)").first()
            when (propTitle)
            {
                "Alternate Name:" -> series.alternateName = sibling.text()
                "Status:" -> series.complete = sibling.text().contains("Ongoing")
                "Author:" -> series.author = sibling.text()
                "Artist:" -> series.artist = sibling.text()
            }
        }
        return series
    }

    override fun enumerateGenres(series: Series): List<Genre>
    {
        val doc = fetchUrl(series.url)
        val genreElements = doc.select(".genretags")
        return genreElements.map {
            Genre(it.text())
        }
    }

    override fun enumerateChapters(series: Series): List<Chapter>
    {
        val doc = fetchUrl(series.url)
        val elements = doc.select("td .chico_manga ~ a[href]")
        return elements.map {
            val text = it.text()
            val chapter = Chapter(series.id, it.absUrl("href"),
                    text.substring(text.lastIndexOf(' ')).toFloat())
            chapter.name = text
            chapter
        }
    }

    override fun fillChapter(chapter: Chapter): Chapter
    {
        return chapter
    }

    override fun enumeratePages(chapter: Chapter): List<Page>
    {
        val doc = fetchUrl(chapter.url)
        val elements = doc.select("#pageMenu option[value]")
        return elements.map {
            Page(chapter.id, it.absUrl("value"), it.text().toFloat())
        }
    }

    override fun fillPage(page: Page): Page
    {
        val doc = fetchUrl(page.url)
        page.imageUrl = doc.select("img[src]").first().absUrl("src")
        return page
    }

    override fun enumerateNew(): List<Pair<Series, Chapter>>
    {
        val doc = fetchUrl("http://www.mangapanda.com/latest")
        val elements = doc.select(".c2")
        val ret = ArrayList<Pair<Series, Chapter>>()
        for (row in elements)
        {
            val seriesElement = row.select(".chapter").first()
            val series = Series(1, seriesElement.absUrl("href"), seriesElement.text())

            val chapters = row.select(".chaptersrec")
            chapters.map {
                        Chapter(-1, it.absUrl("href"), it.text().replace(series.name, "").toFloat())
                    }
                    .mapTo(ret) { Pair(series, it) }
        }
        return ret
    }
}