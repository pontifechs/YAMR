package ninja.dudley.yamr.svc

import android.app.Activity
import android.app.Service
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import ninja.dudley.yamr.model.*
import ninja.dudley.yamr.svc.util.LambdaAsyncTask
import ninja.dudley.yamr.util.Direction
import java.util.*
import java.util.concurrent.PriorityBlockingQueue

/**
 * Created by mdudley on 6/11/15.
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
        FetchNew,
    }

    public class FetchRequest<Arg, Return>: Comparable<Any>
    {
        public val arg: Arg
        public val thiS: Any
        public var complete: (thiS: Any, ret: Return) -> Unit
        public var status: (thiS: Any, status: Float) -> Unit
        public var failure: (thiS: Any) -> Unit
        public val priority: Int
        public val behavior: FetcherSync.Behavior
        public val type: RequestType
        public val otherArgs: Any?

        public constructor(arg: Arg,
                           thiS: Any,
                           complete: (thiS: Any, ret: Return) -> Unit,
                           status: (thiS: Any, status: Float) -> Unit,
                           failure: (thiS: Any) -> Unit,
                           priority: Int,
                           behavior: FetcherSync.Behavior,
                           type: RequestType,
                           otherArgs: Any? = null)
        {
            this.arg = arg
            this.thiS = thiS
            this.complete = complete
            this.status = status
            this.failure = failure
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

    private val queue = PriorityBlockingQueue<FetchRequest<*, *>>()
    private var inFlight: FetchRequest<*, *>? = null

    // Lifecycle Events ----------------------------------------------------------------------------
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        val ret = super.onStartCommand(intent, flags, startId)
        Fetcher = this

        val thread = Thread({
            while (true)
            {
                val next = queue.take()
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
                    RequestType.FetchNew -> { fetchNew(next as FetchRequest<Provider, List<Uri>>) }
                }
            }
        })
        thread.start()

        return ret
    }

    // Fetches -------------------------------------------------------------------------------------
    private fun fetchProvider(req: FetchRequest<Provider, Provider>)
    {
        val fetcher = FetcherSync(contentResolver)
        fetcher.register( object : FetcherSync.NotifyStatus{
            override fun notify(status: Float): Boolean
            {
                Handler(Looper.getMainLooper()).post {
                    req.status(req.thiS, status)
                }
                return true
            }
        })

        val ret = fetcher.fetchProvider(req.arg, req.behavior)
        Handler(Looper.getMainLooper()).post {
            req.complete(req.thiS, ret)
        }
    }

    private fun fetchSeries(req: FetchRequest<Series, Series>)
    {
        val fetcher = FetcherSync(contentResolver)
        fetcher.register(object : FetcherSync.NotifyStatus{
            override fun notify(status: Float): Boolean
            {
                Handler(Looper.getMainLooper()).post {
                    req.status(req.thiS, status)
                }
                return true
            }
        })
        val ret = fetcher.fetchSeries(req.arg, req.behavior)
        Handler(Looper.getMainLooper()).post {
            req.complete(req.thiS, ret)
        }
    }

    private fun fetchChapter(req: FetchRequest<Chapter, Chapter>)
    {
        val fetcher = FetcherSync(contentResolver)
        fetcher.register(object: FetcherSync.NotifyStatus {
            override fun notify(status: Float): Boolean
            {
                Handler(Looper.getMainLooper()).post {
                    req.status(req.thiS, status)
                }
                return true
            }
        })
        val ret = fetcher.fetchChapter(req.arg, req.behavior)
        Handler(Looper.getMainLooper()).post {
            req.complete(req.thiS, ret)
        }
    }

    private fun fetchPage(req: FetchRequest<Page, Page>)
    {
        val fetcher = FetcherSync(contentResolver)
        fetcher.register(object: FetcherSync.NotifyStatus {
            override fun notify(status: Float): Boolean
            {
                Handler(Looper.getMainLooper()).post {
                    req.status(req.thiS, status)
                }
                return true
            }
        })
        val ret = fetcher.fetchPage(req.arg, req.behavior)
        Handler(Looper.getMainLooper()).post {
            req.complete(req.thiS, ret)
        }
    }

    private fun fetchNextPage(req: FetchRequest<Page, Page>)
    {
        val fetcher = Navigation(contentResolver)
        fetcher.register(object: FetcherSync.NotifyStatus {
            override fun notify(status: Float): Boolean
            {
                Handler(Looper.getMainLooper()).post {
                    req.status(req.thiS, status)
                }
                return true
            }
        })
        try
        {
            val ret = fetcher.nextPage(req.arg)
            Handler(Looper.getMainLooper()).post {
                req.complete(req.thiS, ret)
            }
        }
        catch (e: NoSuchElementException)
        {
            Handler(Looper.getMainLooper()).post {
                req.failure(req.thiS)
            }
        }
    }

    private fun fetchPrevPage(req: FetchRequest<Page, Page>)
    {
        val fetcher = Navigation(contentResolver)
        fetcher.register(object: FetcherSync.NotifyStatus {
            override fun notify(status: Float): Boolean
            {
                Handler(Looper.getMainLooper()).post {
                    req.status(req.thiS, status)
                }
                return true
            }
        })
        try
        {
            val ret = fetcher.prevPage(req.arg)
            Handler(Looper.getMainLooper()).post {
                req.complete(req.thiS, ret)
            }
        }
        catch (e: NoSuchElementException)
        {
            Handler(Looper.getMainLooper()).post {
                req.failure(req.thiS)
            }
        }
    }

    private fun fetchFirstPageFromChapter(req: FetchRequest<Chapter, Page>)
    {
        val fetcher = Navigation(contentResolver)
        fetcher.register(object: FetcherSync.NotifyStatus {
            override fun notify(status: Float): Boolean
            {
                Handler(Looper.getMainLooper()).post {
                    req.status(req.thiS, status)
                }
                return true
            }
        })
        try
        {
            val ret = fetcher.firstPageFromChapter(req.arg)
            Handler(Looper.getMainLooper()).post {
                req.complete(req.thiS, ret)
            }
        }
        catch (e: NoSuchElementException)
        {
            Handler(Looper.getMainLooper()).post {
                req.failure(req.thiS)
            }
        }
    }

    private fun fetchPageFromSeries(req: FetchRequest<Series, Page>)
    {
        val fetcher = Navigation(contentResolver)
        fetcher.register(object: FetcherSync.NotifyStatus {
            override fun notify(status: Float): Boolean
            {
                Handler(Looper.getMainLooper()).post {
                    req.status(req.thiS, status)
                }
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
            Handler(Looper.getMainLooper()).post {
                req.complete(req.thiS, ret)
            }
        }
        catch (e: NoSuchElementException)
        {
            Handler(Looper.getMainLooper()).post {
                req.failure(req.thiS)
            }
        }
    }

    private fun fetchOffsetFromPage(req: FetchRequest<Page, Page>)
    {
        val fetcher = Navigation(contentResolver)
        fetcher.register(object: FetcherSync.NotifyStatus {
            override fun notify(status: Float): Boolean
            {
                Handler(Looper.getMainLooper()).post {
                    req.status(req.thiS, status)
                }
                return true
            }
        })
        try
        {
            val pair = req.otherArgs as Pair<Int, Direction>
            val ret = fetcher.fetchPageOffset(req.arg, pair.first, pair.second)
            Handler(Looper.getMainLooper()).post {
                req.complete(req.thiS, ret)
            }
        }
        catch (e: NoSuchElementException)
        {
            Handler(Looper.getMainLooper()).post {
                req.failure(req.thiS)
            }
        }
    }

    private fun fetchNew(req: FetchRequest<Provider, List<Uri>>)
    {
        val fetcher = FetcherSync(contentResolver)
        val ret = fetcher.fetchNew(req.arg)
        Handler(Looper.getMainLooper()).post {
            req.complete(req.thiS, ret)
        }
    }

    companion object
    {
        var Fetcher: FetcherAsync? = null

        public var LowPriority = 100
        public var MediumPriority = 500
        public var HighPriority = 1000


        public fun fetchProvider(provider: Provider,
                                 caller: Any,
                                 complete: (thiS: Any, provider: Provider) -> Unit,
                                 progress: (thiS: Any, progress: Float) -> Unit,
                                 priority: Int = LowPriority,
                                 behavior: FetcherSync.Behavior = FetcherSync.Behavior.LazyFetch)
        {
            val req = FetchRequest(provider, caller, complete, progress, ::failureNop, priority, behavior, RequestType.FetchProvider)
            Fetcher!!.queue.add(req)
        }

        public fun fetchSeries(series: Series,
                               caller: Any,
                               complete: (thiS: Any, provider: Series) -> Unit,
                               progress: (thiS: Any, progress: Float) -> Unit,
                               priority: Int = LowPriority,
                               behavior: FetcherSync.Behavior = FetcherSync.Behavior.LazyFetch)
        {
            val req = FetchRequest(series, caller, complete, progress, ::failureNop, priority, behavior, RequestType.FetchSeries)
            Fetcher!!.queue.add(req)
        }

        public fun fetchChapter(chapter: Chapter,
                                caller: Any,
                                complete: (thiS: Any, provider: Chapter) -> Unit,
                                progress: (thiS: Any, progress: Float) -> Unit,
                                priority: Int = LowPriority,
                                behavior: FetcherSync.Behavior = FetcherSync.Behavior.LazyFetch)
        {
            val req = FetchRequest(chapter, caller, complete, progress, ::failureNop, priority, behavior, RequestType.FetchChapter)
            Fetcher!!.queue.add(req)
        }

        public fun fetchPage(page: Page,
                             caller: Any,
                             complete: (thiS: Any, provider: Page) -> Unit,
                             progress: (thiS: Any, progress: Float) -> Unit,
                             priority: Int = LowPriority,
                             behavior: FetcherSync.Behavior = FetcherSync.Behavior.LazyFetch)
        {
            val req = FetchRequest(page, caller, complete, progress, ::failureNop, priority, behavior, RequestType.FetchPage)
            Fetcher!!.queue.add(req)
        }

        public fun fetchNextPage(page: Page,
                                 caller: Any,
                                 complete: (thiS: Any, provider: Page) -> Unit,
                                 progress: (thiS: Any, progress: Float) -> Unit,
                                 failure: (thiS: Any) -> Unit,
                                 priority: Int = LowPriority)
        {
            val req = FetchRequest(page, caller, complete, progress, failure, priority, FetcherSync.Behavior.LazyFetch, RequestType.FetchNextPage)
            Fetcher!!.queue.add(req)
        }

        public fun fetchPrevPage(page: Page,
                                 caller: Any,
                                 complete: (thiS: Any, provider: Page) -> Unit,
                                 progress: (thiS: Any, progress: Float) -> Unit,
                                 failure: (thiS: Any) -> Unit,
                                 priority: Int = LowPriority)
        {
            val req = FetchRequest(page, caller, complete, progress, failure, priority, FetcherSync.Behavior.LazyFetch, RequestType.FetchPrevPage)
            Fetcher!!.queue.add(req)
        }

        public fun fetchFirstPageFromChapter(chapter: Chapter,
                                             caller: Any,
                                             complete: (thiS: Any, provider: Page) -> Unit,
                                             progress: (thiS: Any, progress: Float) -> Unit,
                                             priority: Int = LowPriority)
        {
            val req = FetchRequest(chapter, caller, complete, progress, ::failureNop, priority, FetcherSync.Behavior.LazyFetch, RequestType.FetchFirstPageFromChapter)
            Fetcher!!.queue.add(req)
        }

        public fun fetchPageFromSeries(series: Series,
                                       caller: Any,
                                       complete: (thiS: Any, provider: Page) -> Unit,
                                       progress: (thiS: Any, progress: Float) -> Unit,
                                       priority: Int = LowPriority)
        {
            val req = FetchRequest(series, caller, complete, progress, ::failureNop, priority, FetcherSync.Behavior.LazyFetch, RequestType.FetchPageFromSeries)
            Fetcher!!.queue.add(req)
        }

        public fun fetchOffsetFromPage(page: Page,
                                       offset: Int,
                                       direction: Direction,
                                       caller: Any,
                                       complete: ((thiS: Any, provider: Page) -> Unit),
                                       progress:((thiS: Any, progress: Float) -> Unit),
                                       priority: Int = LowPriority)
        {
            val req = FetchRequest(page, caller, complete, progress, ::failureNop, priority, FetcherSync.Behavior.LazyFetch, RequestType.FetchOffsetFromPage, Pair(offset, direction))
            Fetcher!!.queue.add(req)
        }

        public fun fetchNew(provider: Provider,
                            caller: Any,
                            complete: ((thiS: Any, news: List<Uri>) -> Unit),
                            priority: Int = LowPriority)
        {
            val req = FetchRequest(provider, caller, complete, ::statusNop,  ::failureNop, priority, FetcherSync.Behavior.LazyFetch, RequestType.FetchNew)
            Fetcher!!.queue.add(req)
        }
    }

    override fun onBind(intent: Intent?): IBinder?
    {
        throw UnsupportedOperationException()
    }
}
