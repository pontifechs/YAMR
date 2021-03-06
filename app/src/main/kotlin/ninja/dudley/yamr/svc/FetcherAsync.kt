package ninja.dudley.yamr.svc

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import ninja.dudley.yamr.model.*
import ninja.dudley.yamr.svc.fetchers.Batoto
import ninja.dudley.yamr.svc.fetchers.MangaHere
import ninja.dudley.yamr.svc.fetchers.MangaPanda
import ninja.dudley.yamr.svc.fetchers.Webcomics
import ninja.dudley.yamr.util.Direction
import java.util.concurrent.PriorityBlockingQueue

/**
 * Created by mdudley on 6/11/15. Yup.
 */

class FetcherAsync: Service()
{
    private enum class RequestType
    {
        FetchProvider,
        FetchSeries,
        FetchChapter,
        FetchPage,
        FetchNextPage,
        FetchPrevPage,
        FetchFirstPageFromChapter,
        FetchPageFromSeries,
        FetchOffsetFromPage,
        FetchAllNew,
        FetchEntireChapter,
        FetchEntireSeries
    }

    class Comms<Return>
    {
        var complete: (thiS: Any, ret: Return) -> Unit
        var status: (thiS: Any, status: Float) -> Unit
        var failure: (thiS: Any, e: Exception) -> Unit

        constructor(complete: (thiS: Any, ret: Return) -> Unit = { thiS: Any, ret: Return ->},
                    status: (thiS: Any, status: Float) -> Unit = { thiS: Any, status: Float ->},
                    failure: (thiS: Any, e: Exception) -> Unit = {thiS: Any, e: Exception -> throw e})
        {
            this.complete = complete
            this.status = status
            this.failure = failure
        }
    }

    private class FetchRequest<Arg, Return>: Comparable<Any>
    {
        val arg: Arg
        val thiS: Any
        val comms: Comms<Return>
        val priority: Int
        val behavior: FetcherSync.Behavior
        val type: RequestType
        val otherArgs: Any?

        constructor(arg: Arg,
                    thiS: Any,
                    comms: Comms<Return>,
                    priority: Int,
                    behavior: FetcherSync.Behavior,
                    type: RequestType,
                    otherArgs: Any? = null)
        {
            this.arg = arg
            this.thiS = thiS
            this.comms = comms
            this.priority = priority
            this.behavior = behavior
            this.type = type
            this.otherArgs = otherArgs
        }

        override fun compareTo(other: Any): Int
        {
            if (other is FetchRequest<*,*>)
            {
                // Default integer behavior is ascending, descending is expected for priority.
                return -this.priority.compareTo(other.priority)
            }
            return 0;
        }
    }

    private val priorityQueue = PriorityBlockingQueue<FetchRequest<*, *>>()
    private val noPriorityQueue = PriorityBlockingQueue<FetchRequest<*, *>>()

    // Lifecycle Events ----------------------------------------------------------------------------
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        val ret = super.onStartCommand(intent, flags, startId)
        Fetcher = this

        val prioThread = Thread({
            while (true)
            {
                val next = priorityQueue.take()
                when (next.type)
                {
                    RequestType.FetchProvider -> { fetchProvider(next as FetchRequest<Provider, Provider>) }
                    RequestType.FetchSeries -> { fetchSeries(next as FetchRequest<Series, Series>) }
                    RequestType.FetchChapter -> { fetchChapter(next as FetchRequest<Chapter, Chapter>) }
                    RequestType.FetchPage -> { fetchPage(next as FetchRequest<Page, Page>) }
                    RequestType.FetchNextPage -> { fetchNextPage(next as FetchRequest<Page, Page>) }
                    RequestType.FetchPrevPage -> { fetchPrevPage(next as FetchRequest<Page, Page>) }
                    RequestType.FetchFirstPageFromChapter -> { fetchFirstPageFromChapter(next as FetchRequest<Chapter, Page>) }
                    RequestType.FetchPageFromSeries-> { fetchPageFromSeries(next as FetchRequest<Series, Page>) }
                    RequestType.FetchOffsetFromPage -> { fetchOffsetFromPage(next as FetchRequest<Page, Page>) }
                }
            }
        })
        prioThread.priority = 10
        prioThread.name = "HighPriorityFetch"
        prioThread.start()

        val noPrioThread = Thread({
            while (true)
            {
                val next = noPriorityQueue.take()
                when (next.type)
                {
                    RequestType.FetchAllNew -> fetchAllNew(next as FetchRequest<Unit, List<Uri>>)
                    RequestType.FetchEntireChapter -> fetchEntireChapter(next as FetchRequest<Chapter, Chapter>)
                    RequestType.FetchEntireSeries -> fetchEntireSeries(next as FetchRequest<Series, Series>)
                }
            }
        })
        noPrioThread.priority = 5
        noPrioThread.name = "LowPriorityFetch"
        noPrioThread.start()

        return ret
    }

    private fun enqueue(req: FetchRequest<*,*>)
    {
        if (req.priority == NoPriority)
        {
            noPriorityQueue.add(req)
        }
        else
        {
            priorityQueue.add(req)
        }
    }

    private fun providerFromSeries(series: Series): Provider
    {
        return Provider(baseContext.contentResolver.query(Provider.uri(series.providerId), null, null, null, null))
    }

    private fun providerFromChapter(chapter: Chapter): Provider
    {
        val series = Series(baseContext.contentResolver.query(Series.uri(chapter.seriesId), null, null, null, null))
        return providerFromSeries(series)
    }

    private fun providerFromPage(page: Page): Provider
    {
        val chapter = Chapter(baseContext.contentResolver.query(Chapter.uri(page.chapterId), null, null, null, null))
        return providerFromChapter(chapter)
    }

    private fun getProvider(element: MangaElement): Provider
    {
        when (element.type)
        {
            MangaElement.UriType.Provider ->
            {
                return element as Provider
            }
            MangaElement.UriType.Series ->
            {
                return providerFromSeries(element as Series)
            }
            MangaElement.UriType.Chapter ->
            {
                return providerFromChapter(element as Chapter)
            }
            MangaElement.UriType.Page ->
            {
                return providerFromPage(element as Page)
            }
            MangaElement.UriType.Genre ->
            {
                throw IllegalArgumentException("Can't get provider from Genre")
            }
        }
    }

    private fun fetcher(element: MangaElement): FetcherSync
    {
        val provider = getProvider(element)
        if (provider.name == "Batoto")
        {
            return Batoto(baseContext)
        }
        else if (provider.name == "MangaHere")
        {
            return MangaHere(baseContext)
        }
        else if (provider.name == "MangaPanda")
        {
            return MangaPanda(baseContext)
        }
        else if (provider.name == "Webcomics")
        {
            return Webcomics(baseContext)
        }
        throw IllegalArgumentException("how'd " + provider.name + " get in here?!")
    }

    // Fetches -------------------------------------------------------------------------------------
    private fun fetchProvider(req: FetchRequest<Provider, Provider>)
    {
        val fetcher = fetcher(req.arg)
        fetcher.register( object : FetcherSync.NotifyStatus{
            override fun notify(status: Float): Boolean
            {
                postStatus(req, status)
                return true
            }
        })
        try
        {
            val ret = fetcher.fetchProvider(req.arg, req.behavior)
            postComplete(req, ret)
        }
        catch (e: Exception)
        {
            postFailure(req, e)
        }
    }

    private fun fetchSeries(req: FetchRequest<Series, Series>)
    {
        val fetcher = fetcher(req.arg)
        fetcher.register(object : FetcherSync.NotifyStatus{
            override fun notify(status: Float): Boolean
            {
                postStatus(req, status)
                return true
            }
        })
        try
        {
            val ret = fetcher.fetchSeries(req.arg, req.behavior)
            postComplete(req, ret)
        }
        catch (e: Exception)
        {
            postFailure(req, e)
        }
    }

    private fun fetchChapter(req: FetchRequest<Chapter, Chapter>)
    {
        val fetcher = fetcher(req.arg)
        fetcher.register(object: FetcherSync.NotifyStatus {
            override fun notify(status: Float): Boolean
            {
                postStatus(req, status)
                return true
            }
        })

        try
        {
            val ret = fetcher.fetchChapter(req.arg, req.behavior)
            postComplete(req, ret)
        }
        catch (e: Exception)
        {
            postFailure(req, e)
        }
    }

    private fun fetchPage(req: FetchRequest<Page, Page>)
    {
        val fetcher = fetcher(req.arg)
        fetcher.register(object: FetcherSync.NotifyStatus {
            override fun notify(status: Float): Boolean
            {
                postStatus(req, status)
                return true
            }
        })
        try
        {
            val ret = fetcher.fetchPage(req.arg, req.behavior)
            postComplete(req, ret)
        }
        catch (e: Exception)
        {
            postFailure(req, e)
        }
    }

    private fun fetchNextPage(req: FetchRequest<Page, Page>)
    {
        val fetcher = fetcher(req.arg)
        fetcher.register(object: FetcherSync.NotifyStatus {
            override fun notify(status: Float): Boolean
            {
                postStatus(req, status)
                return true
            }
        })
        try
        {
            val navigation = Navigation(baseContext, fetcher)
            val ret = navigation.nextPage(req.arg)
            postComplete(req, ret)
        }
        catch (e: Exception)
        {
            postFailure(req, e)
        }
    }

    private fun fetchPrevPage(req: FetchRequest<Page, Page>)
    {
        val fetcher = fetcher(req.arg)
        fetcher.register(object: FetcherSync.NotifyStatus {
            override fun notify(status: Float): Boolean
            {
                postStatus(req, status)
                return true
            }
        })
        try
        {
            val navigation = Navigation(baseContext, fetcher)
            val ret = navigation.prevPage(req.arg)
            postComplete(req, ret)
        }
        catch (e: Exception)
        {
            postFailure(req, e)
        }
    }

    private fun fetchFirstPageFromChapter(req: FetchRequest<Chapter, Page>)
    {
        val fetcher = fetcher(req.arg)
        fetcher.register(object: FetcherSync.NotifyStatus {
            override fun notify(status: Float): Boolean
            {
                postStatus(req, status)
                return true
            }
        })
        try
        {
            val navigation = Navigation(baseContext, fetcher)
            val ret = navigation.firstPageFromChapter(req.arg)
            postComplete(req, ret)
        }
        catch (e: Exception)
        {
            postFailure(req, e)
        }
    }

    private fun fetchPageFromSeries(req: FetchRequest<Series, Page>)
    {
        val fetcher = fetcher(req.arg)
        fetcher.register(object: FetcherSync.NotifyStatus {
            override fun notify(status: Float): Boolean
            {
                postStatus(req, status)
                return true
            }
        })
        try
        {
            val navigation = Navigation(baseContext, fetcher)
            if (req.arg.progressPageId == -1)
            {
                navigation.bookmarkFirstPage(req.arg)
            }
            val ret = navigation.pageFromBookmark(req.arg)
            postComplete(req, ret)
        }
        catch (e: Exception)
        {
            postFailure(req, e)
        }
    }

    private fun fetchOffsetFromPage(req: FetchRequest<Page, Page>)
    {
        val fetcher = fetcher(req.arg)
        fetcher.register(object: FetcherSync.NotifyStatus {
            override fun notify(status: Float): Boolean
            {
                postStatus(req, status)
                return true
            }
        })
        try
        {
            val navigation = Navigation(baseContext, fetcher)
            val pair = req.otherArgs as Pair<Int, Direction>
            val ret = navigation.fetchPageOffset(req.arg, pair.first, pair.second)
            postComplete(req, ret)
        }
        catch (e: Exception)
        {
            postFailure(req, e)
        }
    }

    private fun fetchAllNew(req: FetchRequest<Unit, List<Uri>>)
    {
        val batoto = Batoto(baseContext)
        val mangahere = MangaHere(baseContext)
        val mangapanda = MangaPanda(baseContext)
        val webcomics = Webcomics(baseContext)

        try
        {
            val ret = mangahere.fetchNew().toMutableList()
            ret.addAll(mangapanda.fetchNew())
            ret.addAll(webcomics.fetchNew())
            ret.addAll(batoto.fetchNew())
            postComplete(req, ret)
        }
        catch (e: Exception)
        {
            postFailure(req, e)
        }
    }

    private fun fetchEntireChapter(req: FetchRequest<Chapter, Chapter>)
    {
        val fetcher = fetcher(req.arg)
        fetcher.register(object: FetcherSync.NotifyStatus
        {
            override fun notify(status: Float): Boolean
            {
                postStatus(req, status)
                return true
            }
        })

        try
        {
            val ret = fetcher.fetchEntireChapter(req.arg)
            postComplete(req, ret)
        }
        catch (e: Exception)
        {
            postFailure(req, e)
        }
    }

    private fun fetchEntireSeries(req: FetchRequest<Series, Series>)
    {
        val fetcher = fetcher(req.arg)
        fetcher.register(object: FetcherSync.NotifyStatus
        {
            override fun notify(status: Float): Boolean
            {
                postStatus(req, status)
                return true
            }
        })

        try
        {
            val ret = fetcher.fetchEntireSeries(req.arg)
            postComplete(req, ret)
        }
        catch(e: Exception)
        {
            postFailure(req, e)
        }
    }

    private fun postStatus(req: FetchRequest<*,*>, status: Float)
    {
        Handler(Looper.getMainLooper()).post {
            req.comms.status(req.thiS, status)
        }
    }

    private fun <Return> postComplete(req: FetchRequest<*,Return>, ret: Return )
    {
        Handler(Looper.getMainLooper()).post {
            req.comms.complete(req.thiS, ret)
        }
    }

    private fun postFailure(req: FetchRequest<*,*>, e: Exception)
    {
        Handler(Looper.getMainLooper()).post {
            req.comms.failure(req.thiS, e)
        }
    }

    companion object
    {
        var Fetcher: FetcherAsync? = null

        var NoPriority = 50
        var LowPriority = 100
        var MediumPriority = 500
        var HighPriority = 1000


        fun fetchProvider(provider: Provider,
                          caller: Any,
                          comms: Comms<Provider>,
                          priority: Int = LowPriority,
                          behavior: FetcherSync.Behavior = FetcherSync.Behavior.LazyFetch)
        {
            val req = FetchRequest(provider, caller, comms, priority, behavior, RequestType.FetchProvider)
            Fetcher!!.enqueue(req)
        }

        fun fetchSeries(series: Series,
                        caller: Any,
                        comms: Comms<Series>,
                        priority: Int = LowPriority,
                        behavior: FetcherSync.Behavior = FetcherSync.Behavior.LazyFetch)
        {
            val req = FetchRequest(series, caller, comms, priority, behavior, RequestType.FetchSeries)
            Fetcher!!.enqueue(req)
        }

        fun fetchChapter(chapter: Chapter,
                         caller: Any,
                         comms: Comms<Chapter>,
                         priority: Int = LowPriority,
                         behavior: FetcherSync.Behavior = FetcherSync.Behavior.LazyFetch)
        {
            val req = FetchRequest(chapter, caller, comms, priority, behavior, RequestType.FetchChapter)
            Fetcher!!.enqueue(req)
        }

        fun fetchPage(page: Page,
                      caller: Any,
                      comms: Comms<Page>,
                      priority: Int = LowPriority,
                      behavior: FetcherSync.Behavior = FetcherSync.Behavior.LazyFetch)
        {
            val req = FetchRequest(page, caller, comms, priority, behavior, RequestType.FetchPage)
            Fetcher!!.enqueue(req)
        }

        fun fetchNextPage(page: Page,
                          caller: Any,
                          comms: Comms<Page>,
                          priority: Int = LowPriority)
        {
            val req = FetchRequest(page, caller, comms, priority, FetcherSync.Behavior.LazyFetch, RequestType.FetchNextPage)
            Fetcher!!.enqueue(req)
        }

        fun fetchPrevPage(page: Page,
                          caller: Any,
                          comms: Comms<Page>,
                          priority: Int = LowPriority)
        {
            val req = FetchRequest(page, caller, comms, priority, FetcherSync.Behavior.LazyFetch, RequestType.FetchPrevPage)
            Fetcher!!.enqueue(req)
        }

        fun fetchFirstPageFromChapter(chapter: Chapter,
                                      caller: Any,
                                      comms: Comms<Page>,
                                      priority: Int = LowPriority)
        {
            val req = FetchRequest(chapter, caller, comms, priority, FetcherSync.Behavior.LazyFetch, RequestType.FetchFirstPageFromChapter)
            Fetcher!!.enqueue(req)
        }

        fun fetchPageFromSeries(series: Series,
                                caller: Any,
                                comms: Comms<Page>,
                                priority: Int = LowPriority)
        {
            val req = FetchRequest(series, caller, comms, priority, FetcherSync.Behavior.LazyFetch, RequestType.FetchPageFromSeries)
            Fetcher!!.enqueue(req)
        }

        fun fetchOffsetFromPage(page: Page,
                                offset: Int,
                                direction: Direction,
                                caller: Any,
                                comms: Comms<Page>,
                                priority: Int = LowPriority)
        {
            val req = FetchRequest(page, caller, comms, priority, FetcherSync.Behavior.LazyFetch, RequestType.FetchOffsetFromPage, Pair(offset, direction))
            Fetcher!!.enqueue(req)
        }

        fun fetchAllNew(caller: Any,
                        comms: Comms<List<Uri>>)
        {
            val req = FetchRequest(Unit, caller, comms, NoPriority, FetcherSync.Behavior.LazyFetch, RequestType.FetchAllNew)
            Fetcher!!.enqueue(req)
        }

        fun fetchEntireChapter(chapter: Chapter,
                               caller: Any,
                               comms: Comms<Chapter>)
        {
            val req = FetchRequest(chapter, caller, comms, NoPriority, FetcherSync.Behavior.LazyFetch, RequestType.FetchEntireChapter)
            Fetcher!!.enqueue(req)
        }

        fun fetchEntireSeries(series: Series,
                              caller: Any,
                              comms: Comms<Series>)
        {
            val req = FetchRequest(series, caller, comms, NoPriority, FetcherSync.Behavior.LazyFetch, RequestType.FetchEntireSeries)
            Fetcher!!.enqueue(req)
        }
    }

    override fun onBind(intent: Intent?): IBinder?
    {
        throw UnsupportedOperationException()
    }
}
