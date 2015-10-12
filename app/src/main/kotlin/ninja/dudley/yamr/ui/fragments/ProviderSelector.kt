package ninja.dudley.yamr.ui.fragments

import android.app.Activity
import android.app.ListFragment
import android.app.LoaderManager
import android.content.Context
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import ninja.dudley.yamr.R
import ninja.dudley.yamr.model.Provider
import ninja.dudley.yamr.model.Series
import ninja.dudley.yamr.ui.activities.Browse

public class ProviderSelector : ListFragment(), LoaderManager.LoaderCallbacks<Cursor>
{
    private var adapter: SimpleCursorAdapter? = null

    private var parent: Browse? = null
    override fun onAttach(activity: Activity?)
    {
        super<ListFragment>.onAttach(activity)
        this.parent = activity as Browse
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        setHasOptionsMenu(true)

        val layout = inflater.inflate(R.layout.fragment_provider_selector, container, false) as LinearLayout

        adapter = SimpleCursorAdapter(getActivity(),
                R.layout.simple_series_item,
                null,
                arrayOf(Series.nameCol),
                intArrayOf(R.id.series_name), 0)
        setListAdapter(adapter)

        getLoaderManager().initLoader(0, Bundle(), this)
        return layout
    }

    override fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long)
    {
        super<ListFragment>.onListItemClick(l, v, position, id)
        parent!!.loadProvider(Provider.uri(id.toInt()))
        val imm: InputMethodManager = getActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v!!.getWindowToken(), 0)
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor>
    {
        return CursorLoader(getActivity(), Provider.all(), null, null, null, null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor)
    {
        adapter!!.changeCursor(data)
    }

    override fun onLoaderReset(loader: Loader<Cursor>)
    {
        adapter!!.changeCursor(null)
    }
}


