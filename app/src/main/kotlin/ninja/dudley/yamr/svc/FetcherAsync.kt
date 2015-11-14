package ninja.dudley.yamr.svc

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import ninja.dudley.yamr.model.*
import ninja.dudley.yamr.util.Direction
import java.util.concurrent.PriorityBlockingQueue

/**
* Created by mdudley on 6/11/15. Yup.
*/

private fun failureNop(thiS: Any) {}

private fun statusNop(thiS: Any, status: Float) {}

public class FetcherAsync: Service()
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
        FetchEntireSeries,
        FetchAllSeries
    }

    public class Comms<Return>
    {
        public var complete: (thiS: Any, ret: Return) -> Unit
        public var status: (thiS: Any, status: Float) -> Unit
        public var failure: (thiS: Any, e: Exception) -> Unit

        public constructor(complete: (thiS: Any, ret: Return) -> Unit = { thiS: Any, ret: Return ->},
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
        public val arg: Arg
        public val thiS: Any
        public val comms: Comms<Return>
        public val priority: Int
        public val behavior: FetcherSync.Behavior
        public val type: RequestType
        public val otherArgs: Any?

        public constructor(arg: Arg,
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
                    RequestType.FetchAllSeries -> fetchAllSeries(next as FetchRequest<Unit, Unit>)
                }
            }
        })
        noPrioThread.priority = 5
        noPrioThread.start()

        return ret
    }

    public fun enqueue(req: FetchRequest<*,*>)
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

    // Fetches -------------------------------------------------------------------------------------
    private fun fetchProvider(req: FetchRequest<Provider, Provider>)
    {
        val fetcher = FetcherSync(contentResolver)
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
        val fetcher = FetcherSync(contentResolver)
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
        val fetcher = FetcherSync(contentResolver)
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
        val fetcher = FetcherSync(contentResolver)
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
        val fetcher = Navigation(contentResolver)
        fetcher.register(object: FetcherSync.NotifyStatus {
            override fun notify(status: Float): Boolean
            {
                postStatus(req, status)
                return true
            }
        })
        try
        {
            val ret = fetcher.nextPage(req.arg)
            postComplete(req, ret)
        }
        catch (e: Exception)
        {
            postFailure(req, e)
        }
    }

    private fun fetchPrevPage(req: FetchRequest<Page, Page>)
    {
        val fetcher = Navigation(contentResolver)
        fetcher.register(object: FetcherSync.NotifyStatus {
            override fun notify(status: Float): Boolean
            {
                postStatus(req, status)
                return true
            }
        })
        try
        {
            val ret = fetcher.prevPage(req.arg)
            postComplete(req, ret)
        }
        catch (e: Exception)
        {
            postFailure(req, e)
        }
    }

    private fun fetchFirstPageFromChapter(req: FetchRequest<Chapter, Page>)
    {
        val fetcher = Navigation(contentResolver)
        fetcher.register(object: FetcherSync.NotifyStatus {
            override fun notify(status: Float): Boolean
            {
                postStatus(req, status)
                return true
            }
        })
        try
        {
            val ret = fetcher.firstPageFromChapter(req.arg)
            postComplete(req, ret)
        }
        catch (e: Exception)
        {
            postFailure(req, e)
        }
    }

    private fun fetchPageFromSeries(req: FetchRequest<Series, Page>)
    {
        val fetcher = Navigation(contentResolver)
        fetcher.register(object: FetcherSync.NotifyStatus {
            override fun notify(status: Float): Boolean
            {
                postStatus(req, status)
                return true
            }
        })
        try
        {
            if (req.arg.progressPageId == -1)
            {
                fetcher.bookmarkFirstPage(req.arg)
            }
            val ret = fetcher.pageFromBookmark(req.arg)
            postComplete(req, ret)
        }
        catch (e: Exception)
        {
            postFailure(req, e)
        }
    }

    private fun fetchOffsetFromPage(req: FetchRequest<Page, Page>)
    {
        val fetcher = Navigation(contentResolver)
        fetcher.register(object: FetcherSync.NotifyStatus {
            override fun notify(status: Float): Boolean
            {
                postStatus(req, status)
                return true
            }
        })
        try
        {
            val pair = req.otherArgs as Pair<Int, Direction>
            val ret = fetcher.fetchPageOffset(req.arg, pair.first, pair.second)
            postComplete(req, ret)
        }
        catch (e: Exception)
        {
            postFailure(req, e)
        }
    }

    private fun fetchAllNew(req: FetchRequest<Unit, List<Uri>>)
    {
        val fetcher = FetcherSync(contentResolver)
        try
        {
            val ret = fetcher.fetchAllNew()
            postComplete(req, ret)
        }
        catch (e: Exception)
        {
            postFailure(req, e)
        }
    }

    private fun fetchEntireChapter(req: FetchRequest<Chapter, Chapter>)
    {
        val fetcher = FetcherSync(contentResolver)
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
        val fetcher = FetcherSync(contentResolver)
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

    private fun fetchAllSeries(req: FetchRequest<Unit, Unit>)
    {
        val fetcher = FetcherSync(contentResolver)
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
            fetcher.fetchAllSeries()
            postComplete(req, Unit)
        }
        catch (e: Exception)
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

        public var NoPriority = 50
        public var LowPriority = 100
        public var MediumPriority = 500
        public var HighPriority = 1000


        public fun fetchProvider(provider: Provider,
                                 caller: Any,
                                 comms: Comms<Provider>,
                                 priority: Int = LowPriority,
                                 behavior: FetcherSync.Behavior = FetcherSync.Behavior.LazyFetch)
        {
            val req = FetchRequest(provider, caller, comms, priority, behavior, RequestType.FetchProvider)
            Fetcher!!.enqueue(req)
        }

        public fun fetchSeries(series: Series,
                               caller: Any,
                               comms: Comms<Series>,
                               priority: Int = LowPriority,
                               behavior: FetcherSync.Behavior = FetcherSync.Behavior.LazyFetch)
        {
            val req = FetchRequest(series, caller, comms, priority, behavior, RequestType.FetchSeries)
            Fetcher!!.enqueue(req)
        }

        public fun fetchChapter(chapter: Chapter,
                                caller: Any,
                                comms: Comms<Chapter>,
                                priority: Int = LowPriority,
                                behavior: FetcherSync.Behavior = FetcherSync.Behavior.LazyFetch)
        {
            val req = FetchRequest(chapter, caller, comms, priority, behavior, RequestType.FetchChapter)
            Fetcher!!.enqueue(req)
        }

        public fun fetchPage(page: Page,
                             caller: Any,
                             comms: Comms<Page>,
                             priority: Int = LowPriority,
                             behavior: FetcherSync.Behavior = FetcherSync.Behavior.LazyFetch)
        {
            val req = FetchRequest(page, caller, comms, priority, behavior, RequestType.FetchPage)
            Fetcher!!.enqueue(req)
        }

        public fun fetchNextPage(page: Page,
                                 caller: Any,
                                 comms: Comms<Page>,
                                 priority: Int = LowPriority)
        {
            val req = FetchRequest(page, caller, comms, priority, FetcherSync.Behavior.LazyFetch, RequestType.FetchNextPage)
            Fetcher!!.enqueue(req)
        }

        public fun fetchPrevPage(page: Page,
                                 caller: Any,
                                 comms: Comms<Page>,
                                 priority: Int = LowPriority)
        {
            val req = FetchRequest(page, caller, comms, priority, FetcherSync.Behavior.LazyFetch, RequestType.FetchPrevPage)
            Fetcher!!.enqueue(req)
        }

        public fun fetchFirstPageFromChapter(chapter: Chapter,
                                             caller: Any,
                                             comms: Comms<Page>,
                                             priority: Int = LowPriority)
        {
            val req = FetchRequest(chapter, caller, comms, priority, FetcherSync.Behavior.LazyFetch, RequestType.FetchFirstPageFromChapter)
            Fetcher!!.enqueue(req)
        }

        public fun fetchPageFromSeries(series: Series,
                                       caller: Any,
                                       comms: Comms<Page>,
                                       priority: Int = LowPriority)
        {
            val req = FetchRequest(series, caller, comms, priority, FetcherSync.Behavior.LazyFetch, RequestType.FetchPageFromSeries)
            Fetcher!!.enqueue(req)
        }

        public fun fetchOffsetFromPage(page: Page,
                                       offset: Int,
                                       direction: Direction,
                                       caller: Any,
                                       comms: Comms<Page>,
                                       priority: Int = LowPriority)
        {
            val req = FetchRequest(page, caller, comms, priority, FetcherSync.Behavior.LazyFetch, RequestType.FetchOffsetFromPage, Pair(offset, direction))
            Fetcher!!.enqueue(req)
        }

        public fun fetchAllNew(caller: Any,
                               comms: Comms<List<Uri>>)
        {
            val req = FetchRequest(Unit, caller, comms, NoPriority, FetcherSync.Behavior.LazyFetch, RequestType.FetchAllNew)
            Fetcher!!.enqueue(req)
        }

        public fun fetchEntireChapter(chapter: Chapter,
                                      caller: Any,
                                      comms: Comms<Chapter>)
        {
            val req = FetchRequest(chapter, caller, comms, NoPriority, FetcherSync.Behavior.LazyFetch, RequestType.FetchEntireChapter)
            Fetcher!!.enqueue(req)
        }

        public fun fetchEntireSeries(series: Series,
                                     caller: Any,
                                     comms: Comms<Series>)
        {
            val req = FetchRequest(series, caller, comms, NoPriority, FetcherSync.Behavior.LazyFetch, RequestType.FetchEntireSeries)
            Fetcher!!.enqueue(req)
        }

        public fun fetchAllSeries(caller: Any,
                                  comms: Comms<Series>)
        {
            val req = FetchRequest(Unit, caller, comms, NoPriority, FetcherSync.Behavior.LazyFetch, RequestType.FetchAllSeries)
            Fetcher!!.enqueue(req)
        }
    }

    override fun onBind(intent: Intent?): IBinder?
    {
        throw UnsupportedOperationException()
    }
}
