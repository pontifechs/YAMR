package ninja.dudley.yamr.ui.fragments

import android.app.ListFragment
import android.app.LoaderManager
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SimpleCursorAdapter
import ninja.dudley.yamr.R
import ninja.dudley.yamr.model.Genre

class GenreSelector : ListFragment(), LoaderManager.LoaderCallbacks<Cursor>
{

    private var adapter: SimpleCursorAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        super.onCreateView(inflater, container, savedInstanceState)

        val layout = inflater.inflate(R.layout.fragment_genre_selector, container, false) as LinearLayout

        adapter = SimpleCursorAdapter(activity,
                                      R.layout.genre_item,
                                      null,
                                      arrayOf(Genre.nameCol),
                                      intArrayOf(R.id.genre_name), 0)
        listAdapter = adapter

        loaderManager.initLoader(0, Bundle(), this)
        return layout
    }

    override fun onLoaderReset(loader: Loader<Cursor>?)
    {
        adapter!!.changeCursor(null)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor>?
    {
        return CursorLoader(activity, Genre.all(), null, null, null, null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>?, data: Cursor?)
    {
        adapter!!.changeCursor(data)
    }
}
