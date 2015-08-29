package ninja.dudley.yamr.svc.util

import android.os.AsyncTask

/**
 * Created by mdudley on 8/28/15.
 */

public abstract class LambdaAsyncTask<Params, Progress, Result> : AsyncTask<Params, Progress, Result>
{

    private val caller: Any

    private val complete: (thiS: Any, complete: Result) -> Unit
    private val progress: (thiS: Any, progress: Progress) -> Unit
    private var failure: ((thiS: Any) -> Unit)? = null

    private var failed: Boolean = false

    constructor (callee: Any,
                 complete: (thiS: Any, complete: Result) -> Unit,
                 progress: (thiS: Any, progress: Progress) -> Unit,
                 failure: ((thiS: Any) -> Unit)? = null
                 )
    {
        this.caller = callee
        this.progress = progress
        this.complete = complete
        this.failure = failure
    }

    override fun onProgressUpdate(vararg values: Progress)
    {
        super.onProgressUpdate(*values)
        for (value in values)
        {
            progress(caller, value)
        }
    }

    override fun onPostExecute(result: Result)
    {
        super.onPostExecute(result)
        if (!failed)
        {
            complete(caller, result)
        }
        else if (failure != null)
        {
            failure!!(caller)
        }
    }

    protected fun fail()
    {
        failed = true
    }
}
