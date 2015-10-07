package ninja.dudley.yamr.svc.util

import android.os.AsyncTask

/**
 * Created by mdudley on 8/28/15.
 */

public abstract class LambdaAsyncTask<Params, Progress, Result> : AsyncTask<Params, Progress, Result>
{

    private val caller: Any

    public var complete: ((thiS: Any, complete: Result) -> Unit)?
    public var progress: ((thiS: Any, progress: Progress)-> Unit)?
    public var failure: ((thiS: Any) -> Unit)?

    private var failed: Boolean = false
    public var finished: Boolean = false

    constructor (callee: Any,
                 complete: ((thiS: Any, complete: Result) -> Unit)? = null,
                 progress: ((thiS: Any, progress: Progress) -> Unit)? = null,
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
        if (progress == null)
        {
            return
        }
        for (value in values)
        {
            progress!!(caller, value)
        }
    }

    override fun onPostExecute(result: Result)
    {
        super.onPostExecute(result)
        finished = true
        if (!failed)
        {
            if (complete != null)
            {
                complete!!(caller, result)
            }
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
