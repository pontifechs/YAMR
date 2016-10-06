package ninja.dudley.yamr.ui.fragments

import android.app.*
import android.content.Context
import android.content.CursorLoader
import android.content.DialogInterface
import android.content.Loader
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import ninja.dudley.yamr.R
import ninja.dudley.yamr.model.Chapter
import ninja.dudley.yamr.model.Page
import ninja.dudley.yamr.model.Series
import ninja.dudley.yamr.svc.FetcherAsync
import ninja.dudley.yamr.ui.activities.Browse
import ninja.dudley.yamr.ui.notifications.FetchAllProgress
import org.acra.ACRA
import java.io.File

fun chapterViewerStatus(thiS: Any, status: Float)
{
    (thiS as ChapterViewer).status(status)
}

fun chapterViewerComplete(thiS: Any, chapter: Chapter)
{
    (thiS as ChapterViewer).complete(chapter)
}

class ChapterViewer :
        Fragment(), LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener
{
    private var chapterUri: Uri? = null
    private var chapter: Chapter? = null

    private var loading: ProgressDialog? = null

    private var adapter: PageThumbAdapter? = null
    inner class PageThumbAdapter :
            SimpleCursorAdapter(activity, 0, null, arrayOf<String>(), intArrayOf(), 0)
    {
        private val inflater: LayoutInflater

        init
        {
            inflater = LayoutInflater.from(activity)
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
                val bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(page.imagePath), 192, 192)
                val d = BitmapDrawable(resources, bitmap)
                thumb.setImageDrawable(d)
            }
            else
            {
                thumb.setImageDrawable(resources.getDrawable(R.drawable.panic))
            }

            number.text = "${page.number}"
        }
    }

    private var parent: Browse? = null
    override fun onAttach(activity: Activity?)
    {
        super.onAttach(activity)
        this.parent = activity as Browse?
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        chapterUri = Uri.parse(arguments.getString(uriArgKey))
        chapter = Chapter(activity.contentResolver.query(chapterUri, null, null, null, null))
    }

    fun status(status: Float)
    {
        if (!isAdded)
        {
            return
        }
        val percent = 100 * status
        loading!!.progress = percent.toInt()
    }

    fun complete(chapter: Chapter)
    {
        if (!isAdded)
        {
            return
        }
        loaderManager.restartLoader(0, Bundle(), this@ChapterViewer)
        adapter!!.notifyDataSetChanged()
        loading!!.dismiss()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        setHasOptionsMenu(true)

        val view = inflater.inflate(R.layout.fragment_chapter_viewer, container, false) as LinearLayout

        val chapterName = view.findViewById(R.id.chapter_name) as TextView
        chapterName.text = "Chapter ${chapter!!.number}: ${chapter!!.name}"

        FetcherAsync.fetchChapter(chapter!!, this, FetcherAsync.Comms(::chapterViewerComplete, ::chapterViewerStatus))

        adapter = PageThumbAdapter()
        val grid = view.findViewById(R.id.grid) as GridView
        grid.adapter = adapter
        grid.onItemClickListener = this

        loaderManager.initLoader(0, Bundle(), this)
        loading = ProgressDialog(activity)
        loading!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        val chapterNameText = chapter!!.name?: chapter!!.number
        loading!!.setTitle("Loading Chapters for " + chapterNameText)
        loading!!.show()
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?)
    {
        inflater!!.inflate(R.menu.menu_chapter_viewer, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
    {
        when (item!!.itemId)
        {
            R.id.fetch_all ->
            {
                val series = Series(activity.contentResolver.query(Series.uri(chapter!!.seriesId), null, null, null, null))
                FetcherAsync.fetchEntireChapter(chapter!!, this,
                        FetcherAsync.Comms({ thiS, series -> FetchAllProgress.notify(activity, "Fetching ${series.name} ch. ${chapter!!.number} Complete!", 1.0f) },
                                           { thiS, status -> FetchAllProgress.notify(activity, "Fetching ${series.name} ch. ${chapter!!.number}", status) }))
                return true
            }
            R.id.delete ->
            {
                AlertDialog.Builder(activity)
                        .setTitle("Really Delete?")
                        .setMessage("This will delete all the images in this chapter. They can be re-downloaded")
                        .setPositiveButton("OK!", { dialogInterface: DialogInterface?, i: Int ->
                            val pages = activity.contentResolver.query(chapter!!.pages(), null, null, null, null)
                            while (pages.moveToNext())
                            {
                                val page = Page(pages, false)
                                if (page.imagePath.isNullOrEmpty())
                                {
                                    continue
                                }
                                val image = File(page.imagePath)
                                image.delete()
                                page.imagePath = null
                                page.fullyParsed = false
                                activity.contentResolver.update(page.uri(), page.getContentValues(), null, null)
                            }
                            pages.close()
                            parent!!.redraw(this)
                        })
                        .setNegativeButton("JK.", null).create().show()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onItemClick(viewParent: AdapterView<*>, view: View, position: Int, id: Long)
    {
        val pageUri = Page.uri(id.toInt())
        parent?.loadPage(pageUri)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor>?
    {
        return CursorLoader(activity, chapter!!.pages(), null, null, null, null)
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

    companion object
    {
        fun newInstance(uri: Uri): ChapterViewer
        {
            val chapterViewer: ChapterViewer = ChapterViewer()
            val bundle: Bundle = Bundle()
            bundle.putString(uriArgKey, uri.toString())
            chapterViewer.arguments = bundle
            return chapterViewer
        }

        private val uriArgKey = "uri"
    }
}
