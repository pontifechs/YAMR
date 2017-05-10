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
class MangaHere(context: Context) : FetcherSync(context)
{
    override fun enumerateSeries(): List<Series>
    {
        val doc = fetchUrl("http://www.mangahere.co/mangalist/")
        val elements = doc.select(".manga_info[href]")
        return elements.map {
            Series(1, it.absUrl("href"), it.ownText())
        }
    }

    override fun fillSeries(series: Series): Series
    {
        val doc = fetchUrl(series.url)
        series.thumbnailUrl = doc.select(".img[src]").first().absUrl("src")
        series.description = doc.select("#show").first().ownText()

        val propertyLabels = doc.select(".detail_topText label")
        for (property in propertyLabels)
        {
            val propTitle = property.ownText()
            val propValue = property.parent().ownText()
            if (propTitle == "Alternative Name:")
            {
                series.alternateName = propValue
            }
            else if (propTitle == "Status:")
            {
                series.complete = propValue == "Completed"
            }
            else if (propTitle == "Author(s):")
            {
                try
                {
                    val authorLink = property.parent().child(1)
                    series.author = authorLink.ownText()
                }
                catch(e: Exception)
                {
                    series.author = propValue
                }
            }
            else if (propTitle == "Artist(s):")
            {
                try
                {
                    val artistLink = property.parent().child(1)
                    series.artist = artistLink.ownText()
                }
                catch(e: Exception)
                {
                    series.artist = propValue
                }
            }
        }
        return series
    }

    override fun enumerateGenres(series: Series): List<Genre>
    {
        val doc = fetchUrl(series.url)
        val genres = ArrayList<Genre>()
        val propertyLabels = doc.select(".detail_topText label")
        for (property in propertyLabels)
        {
            val propTitle = property.ownText()
            val propValue = property.parent().ownText()
            if (propTitle == "Genre(s):")
            {
                propValue.split(", ").mapTo(genres) { Genre(it) }
            }
        }
        return genres
    }

    override fun enumerateChapters(series: Series): List<Chapter>
    {
        val doc = fetchUrl(series.url)
        val elements = doc.select(".detail_list a.color_0077")
        return elements.map {
            val elementText = it.ownText()
            val number = elementText.substring(elementText.lastIndexOf(' ')).toFloat()
            val chapter = Chapter(series.id, it.absUrl("href"), number)
            chapter.name = elementText
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
        val elements = doc.select(".wid60").first().select("option")
        return elements.map {
            Page(chapter.id, it.absUrl("value"), it.text().toFloat())
        }
    }

    override fun fillPage(page: Page): Page
    {
        val doc = fetchUrl(page.url)
        page.imageUrl = doc.select("#image").first().absUrl("src")
        return page
    }

    override fun enumerateNew(): List<Pair<Series, Chapter>>
    {
        val doc = fetchUrl("http://www.mangahere.co/latest/")
        val elements = doc.select(".manga_updates dl")
        val ret = ArrayList<Pair<Series, Chapter>>()
        for (row in elements)
        {
            val seriesElement = row.select("dt a").first()
            val series = Series(1, seriesElement.absUrl("href"), seriesElement.text())

            val chapters = row.select("dd a")
            chapters.map {
                        Chapter(-1, it.absUrl("href"), it.text().replace(series.name, "").toFloat())
                    }
                    .mapTo(ret) { Pair(series, it) }
        }
        return ret
    }
}

