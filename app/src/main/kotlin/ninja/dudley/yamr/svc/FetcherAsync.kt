package ninja.dudley.yamr.svc

import android.content.ContentResolver
import android.util.Log
import ninja.dudley.yamr.model.Chapter
import ninja.dudley.yamr.model.Page
import ninja.dudley.yamr.model.Provider
import ninja.dudley.yamr.model.Series
import ninja.dudley.yamr.svc.util.LambdaAsyncTask
import ninja.dudley.yamr.util.Direction
import java.util.*

/**
 * Created by mdudley on 6/11/15.
 */

public class FetcherAsync
{
    companion object
    {

        public fun fetchProvider(resolver: ContentResolver,
                                 caller: Any,
                                 complete: (thiS: Any, provider: Provider) -> Unit,
                                 progress: (thiS: Any, progress: Float) -> Unit,
                                 behavior: FetcherSync.Behavior = FetcherSync.Behavior.LazyFetch)
                : LambdaAsyncTask<Provider, Float, Provider>
        {
            return object : LambdaAsyncTask<Provider, Float, Provider>(caller, complete, progress), FetcherSync.NotifyStatus
            {
                override fun doInBackground(vararg params: Provider): Provider
                {
                    val fetcher = FetcherSync(resolver);
                    fetcher.register(this)
                    fetcher.fetchProvider(params[0], behavior)
                    return params[0];
                }

                override fun notify(status: Float)
                {
                    publishProgress(status)
                }
            }
        }

        public fun fetchSeries(resolver: ContentResolver,
                               caller: Any,
                               complete: (thiS: Any, provider: Series) -> Unit,
                               progress: (thiS: Any, progress: Float) -> Unit,
                               behavior: FetcherSync.Behavior = FetcherSync.Behavior.LazyFetch)
                : LambdaAsyncTask<Series, Float, Series>
        {
            return object : LambdaAsyncTask<Series, Float, Series>(caller, complete, progress), FetcherSync.NotifyStatus
            {
                override fun doInBackground(vararg params: Series): Series
                {
                    val fetcher = FetcherSync(resolver);
                    fetcher.register(this)
                    fetcher.fetchSeries(params[0], behavior)
                    return params[0];
                }

                override fun notify(status: Float)
                {
                    publishProgress(status)
                }
            }
        }

        public fun fetchChapter(resolver: ContentResolver,
                                caller: Any,
                                complete: (thiS: Any, provider: Chapter) -> Unit,
                                progress: (thiS: Any, progress: Float) -> Unit,
                                behavior: FetcherSync.Behavior = FetcherSync.Behavior.LazyFetch)
                : LambdaAsyncTask<Chapter, Float, Chapter>
        {
            return object : LambdaAsyncTask<Chapter, Float, Chapter>(caller, complete, progress), FetcherSync.NotifyStatus
            {
                override fun doInBackground(vararg params: Chapter): Chapter
                {
                    val fetcher = FetcherSync(resolver);
                    fetcher.register(this)
                    fetcher.fetchChapter(params[0], behavior)
                    return params[0];
                }

                override fun notify(status: Float)
                {
                    publishProgress(status)
                }
            }
        }

        public fun fetchPage(resolver: ContentResolver,
                             caller: Any,
                             complete: (thiS: Any, provider: Page) -> Unit,
                             progress: (thiS: Any, progress: Float) -> Unit,
                             behavior: FetcherSync.Behavior = FetcherSync.Behavior.LazyFetch)
                : LambdaAsyncTask<Page, Float, Page>
        {
            return object : LambdaAsyncTask<Page, Float, Page>(caller, complete, progress), FetcherSync.NotifyStatus
            {
                override fun doInBackground(vararg params: Page): Page
                {
                    val fetcher = FetcherSync(resolver);
                    fetcher.register(this)
                    fetcher.fetchPage(params[0], behavior)
                    return params[0];
                }

                override fun notify(status: Float)
                {
                    publishProgress(status)
                }
            }
        }

        public fun fetchNextPage(resolver: ContentResolver,
                                 caller: Any,
                                 complete: (thiS: Any, provider: Page) -> Unit,
                                 progress: (thiS: Any, progress: Float) -> Unit,
                                 failure: (thiS: Any) -> Unit)
                : LambdaAsyncTask<Page, Float, Page>
        {
            return object: LambdaAsyncTask<Page, Float, Page>(caller, complete, progress, failure), FetcherSync.NotifyStatus
            {
                override fun doInBackground(vararg params: Page): Page?
                {
                    val fetcher = Navigation(resolver)
                    fetcher.register(this)
                    val next = fetcher.nextPage(params[0])
                    if (next == null)
                    {
                        fail()
                    }
                    return next
                }

                override fun notify(status: Float)
                {
                    publishProgress(status)
                }
            }
        }

        public fun fetchPrevPage(resolver: ContentResolver,
                                 caller: Any,
                                 complete: (thiS: Any, provider: Page) -> Unit,
                                 progress: (thiS: Any, progress: Float) -> Unit,
                                 failure: (thiS: Any) -> Unit)
                : LambdaAsyncTask<Page, Float, Page>
        {
            return object: LambdaAsyncTask<Page, Float, Page>(caller, complete, progress, failure), FetcherSync.NotifyStatus
            {
                override fun doInBackground(vararg params: Page): Page?
                {
                    val fetcher = Navigation(resolver)
                    fetcher.register(this)
                    val prev= fetcher.prevPage(params[0])
                    if (prev == null)
                    {
                        fail()
                    }
                    return prev
                }

                override fun notify(status: Float)
                {
                    publishProgress(status)
                }
            }
        }

        public fun fetchFirstPageFromChapter(resolver: ContentResolver,
                                 caller: Any,
                                 complete: (thiS: Any, provider: Page) -> Unit,
                                 progress: (thiS: Any, progress: Float) -> Unit)
                : LambdaAsyncTask<Chapter, Float, Page>
        {
            return object: LambdaAsyncTask<Chapter, Float, Page>(caller, complete, progress), FetcherSync.NotifyStatus
            {
                override fun doInBackground(vararg params: Chapter): Page
                {
                    val fetcher = Navigation(resolver)
                    fetcher.register(this)
                    return fetcher.firstPageFromChapter(params[0])
                }

                override fun notify(status: Float)
                {
                    publishProgress(status)
                }
            }
        }

        public fun fetchPageFromSeries(resolver: ContentResolver,
                                 caller: Any,
                                 complete: (thiS: Any, provider: Page) -> Unit,
                                 progress: (thiS: Any, progress: Float) -> Unit)
                : LambdaAsyncTask<Series, Float, Page>
        {
            return object : LambdaAsyncTask<Series, Float, Page>(caller, complete, progress), FetcherSync.NotifyStatus
            {
                override fun doInBackground(vararg params: Series): Page?
                {
                    val fetcher = Navigation(resolver)
                    fetcher.register(this)
                    var arg = params[0]
                    if (arg.progressPageId == -1)
                    {
                        arg = fetcher.bookmarkFirstPage(arg)
                    }
                    return fetcher.pageFromBookmark(arg)
                }

                override fun notify(status: Float)
                {
                    publishProgress(status)
                }
            }
        }

        public fun fetchOffsetFromPage(offset: Int,
                                       direction: Direction,
                                       resolver: ContentResolver,
                                       caller: Any,
                                       complete: ((thiS: Any, provider: Page) -> Unit)? = null,
                                       progress:((thiS: Any, progress: Float) -> Unit)? = null)
            : LambdaAsyncTask<Page, Float, Page>
        {
            return object : LambdaAsyncTask<Page, Float, Page>(caller, complete, progress), FetcherSync.NotifyStatus
            {
                override fun doInBackground(vararg params: Page): Page?
                {
                    try
                    {

                        Log.d("PreFetch", "Executing Prefetch #${params[0].number} ${direction} ${offset}")
                        val fetcher = Navigation(resolver)
                        fetcher.register(this)
                        val page = fetcher.fetchPageOffset(params[0], offset, direction)
                        if (page == null)
                        {
                            fail()
                        }
                        Log.d("PreFetch", "Finished Prefetch #${params[0].number} ${direction} ${offset}")
                        return page

                    }
                    catch (e: NoSuchElementException)
                    {
                        fail()
                        return null
                    }
                }

                override fun notify(status: Float)
                {
                    Log.d("PreFetch", "PreFetch Status: ${status}")
                    publishProgress(status)
                }
            }
        }
    }
}
