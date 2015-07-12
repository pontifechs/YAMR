package ninja.dudley.yamr.ui.activities

import android.app.FragmentTransaction
import android.app.ListActivity
import android.app.LoaderManager
import android.content.Context
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import android.widget.TextView

import ninja.dudley.yamr.R
import ninja.dudley.yamr.model.Chapter
import ninja.dudley.yamr.model.Page
import ninja.dudley.yamr.model.Series
import ninja.dudley.yamr.ui.fragments.PageViewer

public class Favorites : ListActivity(), LoaderManager.LoaderCallbacks<Cursor> {

    private var adapter: ThumbSeriesAdapter? = null

    public inner class ThumbSeriesAdapter : SimpleCursorAdapter(this@Favorites, 0, null, arrayOf<String>(), intArrayOf(), 0) {
        private val inflater: LayoutInflater

        init {
            inflater = LayoutInflater.from(this@Favorites)
        }

        override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
            return inflater.inflate(R.layout.thumb_series_item, null)
        }

        override fun bindView(view: View, context: Context?, cursor: Cursor) {
            super.bindView(view, context, cursor)
            val series = Series(cursor, false)
            val thumb = view.findViewById(R.id.seriesThumbnail) as ImageView
            val name = view.findViewById(R.id.seriesName) as TextView
            val progress = view.findViewById(R.id.seriesProgress) as TextView

            if (series.thumbnailPath != null) {
                val d = Drawable.createFromPath(series.thumbnailPath)
                thumb.setImageDrawable(d)
            }

            name.setText(series.name)

            if (series.progressPageId != -1) {
                val p = Page(context!!.getContentResolver().query(Page.uri(series.progressPageId), null, null, null, null))
                val c = Chapter(context.getContentResolver().query(Chapter.uri(p.chapterId), null, null, null, null))

                progress.setText("Chapter: " + c.number + ", Page: " + p.number)
            } else {
                progress.setText("Not Started")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super<ListActivity>.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        adapter = ThumbSeriesAdapter()
        setListAdapter(adapter)

        getLoaderManager().initLoader(0, Bundle(), this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_favorites, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.getItemId()) {
            R.id.action_settings -> return true
        }

        return super<ListActivity>.onOptionsItemSelected(item)
    }

    override fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long) {
        val bookmarkUri = Series.uri(id.toInt())
        val transaction = getFragmentManager().beginTransaction()
        val pageViewer = PageViewer()
        val args = Bundle()
        args.putParcelable(PageViewer.SeriesArgumentKey, bookmarkUri)
        pageViewer.setArguments(args)
        transaction.replace(R.id.favorites, pageViewer)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor> {
        return CursorLoader(this, Series.favorites(), null, null, null, null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        adapter!!.changeCursor(data)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        adapter!!.changeCursor(null)
    }
}
