package ninja.dudley.yamr.svc.fetchers

import android.content.Context
import ninja.dudley.yamr.model.Chapter
import ninja.dudley.yamr.model.Genre
import ninja.dudley.yamr.model.Page
import ninja.dudley.yamr.model.Series
import ninja.dudley.yamr.svc.FetcherSync
import ninja.dudley.yamr.svc.fetchers.webcomics.QuestionableContent
import java.util.*

/**
 * Created by matt on 9/27/16.
 */
class Webcomics : FetcherSync
{
    val context: Context
    val subSeriesFetchers: List<SeriesFetcher> = listOf(
            QuestionableContent()
    )
    constructor(context: Context) : super(context) {
        this.context = context
    }

    private fun findSeriesFetcher(series: Series): SeriesFetcher
    {
        return subSeriesFetchers.filter {
            series.url == it.seriesUrl()
        }[0]
    }

    override fun enumerateSeries(): List<Series>
    {
        return subSeriesFetchers.map {
            it.fillSeries()
        }
    }

    override fun fillSeries(series: Series): Series
    {
        return series
    }

    override fun enumerateGenres(series: Series): List<Genre>
    {
        return findSeriesFetcher(series).enumerateGenres()
    }

    override fun enumerateChapters(series: Series): List<Chapter>
    {
        return findSeriesFetcher(series).enumerateChapters(series)
    }

    override fun fillChapter(chapter: Chapter): Chapter
    {
        return chapter
    }

    override fun enumeratePages(chapter: Chapter): List<Page>
    {
        val series = seriesFromChapter(chapter)
        val fetcher = findSeriesFetcher(series)
        return fetcher.enumeratePages(chapter)
    }

    override fun fillPage(page: Page): Page
    {
        val series = seriesFromPage(page)
        val fetcher = findSeriesFetcher(series)
        return fetcher.fillPage(page)
    }

    override fun enumerateNew(): List<Pair<Series, Chapter>>
    {
        val ret = ArrayList<Pair<Series, Chapter>>()
        subSeriesFetchers.forEach {
            val series = seriesFromUrl(it.seriesUrl())
            ret.addAll(it.enumerateNew().map {
                Pair(series, it)
            })
        }
        return ret
    }

    // Pretty sure this is implemented somewhere else already, but whatever
    private fun seriesFromChapter(chapter: Chapter): Series
    {
        return Series(context.contentResolver.query(Series.uri(chapter.seriesId), null, null, null, null))
    }

    private fun seriesFromPage(page: Page): Series
    {
        val chapter = Chapter(context.contentResolver.query(Chapter.uri(page.chapterId), null, null, null, null))
        return seriesFromChapter(chapter)
    }

    private fun seriesFromUrl(url: String): Series
    {
        return Series(context.contentResolver.query(Series.baseUri(), null, null, arrayOf(url), null))
    }

    interface SeriesFetcher
    {
        // Must be unique across series
        fun seriesUrl(): String

        fun fillSeries(): Series

        fun enumerateGenres(): List<Genre>

        fun enumerateChapters(series: Series): List<Chapter>

        fun enumeratePages(chapter: Chapter): List<Page>

        fun fillPage(page: Page): Page

        fun enumerateNew(): List<Chapter>
    }

}