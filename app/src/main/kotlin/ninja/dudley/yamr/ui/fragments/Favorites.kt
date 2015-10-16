package ninja.dudley.yamr.ui.fragments

import android.app.Activity
import android.app.ListFragment
import android.app.LoaderManager
import android.content.Context
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import android.widget.*
import ninja.dudley.yamr.R
import ninja.dudley.yamr.model.Heritage
import ninja.dudley.yamr.model.Page
import ninja.dudley.yamr.model.Series
import ninja.dudley.yamr.ui.activities.Browse

public class Favorites : ListFragment(), LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemLongClickListener
{
    private var adapter: ThumbSeriesAdapter? = null
    public inner class ThumbSeriesAdapter :
            SimpleCursorAdapter(activity, 0, null, arrayOf<String>(), intArrayOf(), 0)
    {
        private val inflater: LayoutInflater

        init
        {
            inflater = LayoutInflater.from(activity)
        }

        override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View
        {
            return inflater.inflate(R.layout.thumb_series_item, null)
        }

        override fun bindView(view: View, context: Context?, cursor: Cursor)
        {
            super.bindView(view, context, cursor)
            val series = Series(cursor, false)
            val thumb = view.findViewById(R.id.seriesThumbnail) as ImageView
            val name = view.findViewById(R.id.seriesName) as TextView
            val progress = view.findViewById(R.id.seriesProgress) as TextView

            if (series.updated)
            {
                view.setBackgroundColor(Color.argb(127, 102, 153, 255))
            }
            else
            {
                view.setBackgroundColor(Color.TRANSPARENT)
            }

            if (series.thumbnailPath != null)
            {
                val d = Drawable.createFromPath(series.thumbnailPath)
                thumb.setImageDrawable(d)
            }
            else
            {
                thumb.setImageDrawable(resources.getDrawable(R.drawable.panic, null))
            }

            name.text = series.name

            if (series.progressPageId != -1)
            {
                val heritage = Heritage(context!!.contentResolver
                        .query(Page.heritage(series.progressPageId!!), null, null, null, null))
                progress.text = "Chapter: " + heritage.chapterNumber + ", Page: " + heritage.pageNumber
            }
            else
            {
                progress.text = "Not Started"
            }
        }
    }

    private var parent: Browse? = null
    override fun onAttach(activity: Activity?)
    {
        super.onAttach(activity)
        this.parent = activity as Browse
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        setHasOptionsMenu(true)
        val layout = inflater.inflate(R.layout.fragment_favorites, container, false) as RelativeLayout
        val listView = layout.findViewById(android.R.id.list) as ListView
        listView.onItemLongClickListener = this

        adapter = ThumbSeriesAdapter()
        listAdapter = adapter

        loaderManager.initLoader(0, Bundle(), this)

        return layout;
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater!!.inflate(R.menu.menu_favorites, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
    {
        when (item!!.itemId)
        {
            R.id.action_settings -> return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long)
    {
        parent?.loadBookmarkFromSeries(Series.uri(id.toInt()))
    }

    override fun onItemLongClick(parentView: AdapterView<*>?, view: View?, position: Int, id: Long): Boolean
    {
        parent?.loadSeries(Series.uri(id.toInt()))
        return true
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor>
    {
        return CursorLoader(activity, Series.favorites(), null, null, null, null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor)
    {
        adapter!!.changeCursor(data)
    }

    override fun onLoaderReset(loader: Loader<Cursor>)
    {
        adapter!!.changeCursor(null)
    }

    override fun onResume()
    {
        super.onResume()
        loaderManager.restartLoader(0, Bundle(), this)
    }
}
