package ninja.dudley.yamr.ui.fragments;

import android.app.DialogFragment;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ninja.dudley.yamr.R;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LoadingDialog#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LoadingDialog extends DialogFragment
{

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LoadingDialog.
     */
    // TODO: Rename and change types and number of parameters
    public static LoadingDialog newInstance()
    {
        LoadingDialog fragment = new LoadingDialog();
        return fragment;
    }

    public LoadingDialog()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_loading_dialog, container, false);
    }
}
