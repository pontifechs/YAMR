package ninja.dudley.yamr.svc.fetchers.webcomics

import ninja.dudley.yamr.model.Chapter
import ninja.dudley.yamr.model.Genre
import ninja.dudley.yamr.model.Page
import ninja.dudley.yamr.model.Series
import ninja.dudley.yamr.svc.FetcherSync
import ninja.dudley.yamr.svc.fetchers.Webcomics

/**
 * Created by matt on 9/27/16.
 */
class QuestionableContent : Webcomics.SeriesFetcher
{
    override fun seriesUrl(): String
    {
        return "http://www.questionablecontent.net/"
    }

    override fun fillSeries(): Series
    {
        val qc = Series(4, seriesUrl(), "Questionable Content")
        qc.thumbnailUrl = "http://www.questionablecontent.net/cast/pintsize.png" // :D
        qc.description = "Questionable Content is an internet comic strip about romance and robots. It started on August 1, 2003. It updates five times a week, Monday through Friday."
        qc.artist = "Jeph Jacques"
        qc.author = "Jeph Jacques"
        qc.complete = false
        return qc
    }

    override fun enumerateGenres(): List<Genre>
    {
        return listOf(Genre("Slice of Life"), Genre("Webcomic"))
    }

    override fun enumerateChapters(series: Series): List<Chapter>
    {
        val doc = FetcherSync.fetchUrl("http://www.questionablecontent.net/archive.php")

        return doc.select(".small-12 > a[href]").map {
            val url = it.absUrl("href")
            val numberStr = url.substring(url.indexOf("=") + 1)
            val rawName = it.ownText()
            val chapter  = Chapter(series.id, url, numberStr.toFloat())
            chapter.name = rawName.substring(rawName.indexOf(":") + 2)
            chapter
        }.filter {
            it.number != 0.0f
        }
    }

    override fun enumeratePages(chapter: Chapter): List<Page>
    {
        // chapters are single-page
        return listOf(Page(chapter.id, chapter.url, 1.0f))
    }

    override fun fillPage(page: Page): Page
    {
        val doc = FetcherSync.fetchUrl(page.url)
        page.imageUrl = doc.select("#strip").first().absUrl("src")
        return page
    }

    override fun enumerateNew(): List<Chapter>
    {
        return enumerateChapters(Series(-1, "", ""))
    }
}