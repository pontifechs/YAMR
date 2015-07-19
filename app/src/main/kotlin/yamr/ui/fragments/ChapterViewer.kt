package ninja.dudley.yamr.ui.fragments

import android.app.Activity
import android.app.Fragment
import android.app.LoaderManager
import android.app.ProgressDialog
import android.content.*
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import ninja.dudley.yamr.R
import ninja.dudley.yamr.model.Chapter
import ninja.dudley.yamr.model.Page
import ninja.dudley.yamr.svc.FetcherAsync

public class ChapterViewer(private val chapterUri: Uri) : Fragment(), LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener
{
    private var chapter: Chapter? = null

    private var fetchStatusReceiver: BroadcastReceiver? = null
    private var fetchCompleteReceiver: BroadcastReceiver? = null

    private var loading: ProgressDialog? = null

    public interface LoadPage
    {
        public fun loadPage(page: Uri)
    }

    private var adapter: PageThumbAdapter? = null
    public inner class PageThumbAdapter : SimpleCursorAdapter(getActivity(), 0, null, arrayOf<String>(), intArrayOf(), 0)
    {
        private val inflater: LayoutInflater

        init
        {
            inflater = LayoutInflater.from(getActivity())
        }

        override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View
        {
            return inflater.inflate(R.layout.thumb_page_item, null)
        }

        override fun bindView(view: View, context: Context?, cursor: Cursor)
        {
            // FYI:: Android will send in some views which have been recycled. This took me
            // quite a while to figure out. This means that you need to clear it out to get rid of
            // whatever used to be there.
            super.bindView(view, context, cursor)
            val page = Page(cursor, false)

            val thumb = view.findViewById(R.id.page_thumb) as ImageView
            val number = view.findViewById(R.id.page_number) as TextView
            if (page.imagePath != null)
            {
                val d = BitmapDrawable(getResources(), ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(page.imagePath), 192, 192))
                thumb.setImageDrawable(d)
            }
            else
            {
                thumb.setImageDrawable(getResources().getDrawable(R.drawable.panic))
            }

            number.setText("${page.number}")
        }
    }

    private var parent: LoadPage? = null
    override fun onAttach(activity: Activity?)
    {
        super<Fragment>.onAttach(activity)
        this.parent = activity as LoadPage?
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super<Fragment>.onCreate(savedInstanceState)
        chapter = Chapter(getActivity().getContentResolver().query(chapterUri, null, null, null, null))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_chapter_viewer, container, false) as LinearLayout

        fetchStatusReceiver = object : BroadcastReceiver()
        {
            override fun onReceive(context: Context, intent: Intent)
            {
                val percent = 100 * intent.getFloatExtra(FetcherAsync.FETCH_CHAPTER_STATUS, 0.0f)
                loading!!.setProgress(percent.toInt())
            }
        }

        fetchCompleteReceiver = object : BroadcastReceiver()
        {
            override fun onReceive(context: Context, intent: Intent)
            {
                getLoaderManager().restartLoader(0, Bundle(), this@ChapterViewer)
                adapter!!.notifyDataSetChanged()
                loading!!.dismiss()
            }
        }

        val chapterName = view.findViewById(R.id.chapter_name) as TextView
        chapterName.setText("Chapter ${chapter!!.number}: ${chapter!!.name}")

        val fetchChapter = Intent(getActivity(), javaClass<FetcherAsync>())
        fetchChapter.setAction(FetcherAsync.FETCH_CHAPTER)
        fetchChapter.setData(chapter!!.uri())
        getActivity().startService(fetchChapter)

        adapter = PageThumbAdapter()
        val grid = view.findViewById(R.id.grid) as GridView
        grid.setAdapter(adapter)
        grid.setOnItemClickListener(this)

        getLoaderManager().initLoader(0, Bundle(), this)
        loading = ProgressDialog(getActivity())
        loading!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        loading!!.setTitle("Loading Chapters for " + chapter!!.name!!)
        loading!!.show()
        return view
    }

    override fun onResume()
    {
        super<Fragment>.onResume()
        val completeFilter = IntentFilter(FetcherAsync.FETCH_CHAPTER_COMPLETE)
        try
        {
            completeFilter.addDataType(getActivity().getContentResolver().getType(Chapter.baseUri()))
        }
        catch (e: IntentFilter.MalformedMimeTypeException)
        {
            throw AssertionError(e)
        }

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(fetchCompleteReceiver, completeFilter)
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(fetchStatusReceiver, IntentFilter(FetcherAsync.FETCH_CHAPTER_STATUS))
    }

    override fun onPause()
    {
        super<Fragment>.onPause()
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(fetchStatusReceiver)
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(fetchCompleteReceiver)
    }

    override fun onItemClick(viewParent: AdapterView<*>, view: View, position: Int, id: Long)
    {
        val pageUri = Page.uri(id.toInt())
        parent?.loadPage(pageUri)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor>?
    {
        return CursorLoader(getActivity(), chapter!!.pages(), null, null, null, null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>?, data: Cursor?)
    {
        Log.d("ChapterViewer", "onLoadFinished")
        adapter!!.changeCursor(data)
    }

    override fun onLoaderReset(loader: Loader<Cursor>?)
    {
        Log.d("ChapterViewer", "onLoaderReset")
        adapter!!.changeCursor(null)
    }
}