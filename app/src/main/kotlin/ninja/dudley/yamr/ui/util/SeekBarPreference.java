package ninja.dudley.yamr.ui.util;


import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import ninja.dudley.yamr.R;


/**
 * Created by Stelian Morariu on 12/9/13.
 * Modified by Matthew Dudley on 08/15/15
 */
public class SeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener
{
    private static final int DEFAULT_VALUE = 0;
    private static final String LOG_TAG = SeekBarPreference.class.getSimpleName();
    private int mMaxValue = 10;
    private int mMinValue = 0;
    private int mInterval = 1;
    private int mCurrentValue;
    private SeekBar mSeekBar;
    private TextView mValueTextView;

    public SeekBarPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initPreference(context, attrs);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initPreference(context, attrs);
    }

    private void initPreference(Context context, AttributeSet attrs)
    {
        setValuesFromXml(attrs);
        mSeekBar = new SeekBar(context, attrs);
        mSeekBar.setMax(mMaxValue);
        mSeekBar.setOnSeekBarChangeListener(this);

        setWidgetLayoutResource(R.layout.seek_bar_preference);
    }

    private void setValuesFromXml(AttributeSet attrs)
    {
        TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.SeekBarPreference, 0, 0);
        mInterval = a.getInteger(R.styleable.SeekBarPreference_interval, 1);
        mMinValue= a.getInteger(R.styleable.SeekBarPreference_min, 0);
        mMaxValue= a.getInteger(R.styleable.SeekBarPreference_max, 255);
        mCurrentValue = a.getInteger(R.styleable.SeekBarPreference_defaultValue, 0);
    }

    @Override
    protected View onCreateView(ViewGroup parent)
    {
        View view = super.onCreateView(parent);

        // The basic preference layout puts the widget frame to the right of the title and summary,
        // so we need to change it a bit - the seekbar should be under them.
        LinearLayout layout = (LinearLayout) view;
        layout.setOrientation(LinearLayout.VERTICAL);

        return view;
    }

    @Override
    public void onBindView(View view)
    {
        super.onBindView(view);
        ViewGroup newContainer = (ViewGroup) view.findViewById(R.id.seekBarPrefBarContainer);
        ViewParent oldContainer = mSeekBar.getParent();

        try
        {
            // move our seekbar to the new view we've been given

            if (oldContainer != newContainer)
            {
                // remove the seekbar from the old view
                if (oldContainer != null)
                {
                    ((ViewGroup) oldContainer).removeView(mSeekBar);
                }
                // remove the existing seekbar (there may not be one) and add ours
                newContainer.removeAllViews();
                newContainer.addView(mSeekBar, ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
        catch (Exception ex)
        {
            Log.e(LOG_TAG, "Error binding view: " + ex.toString());
        }

        //if dependency is false from the beginning, disable the seek bar
        if (!view.isEnabled())
        {
            mSeekBar.setEnabled(false);
        }
        mValueTextView = (TextView) view.findViewById(R.id.seekbarMaxLabel);
        updateView(view);
    }

    /**
     * Update a SeekBarPreference view with our current state
     */
    private void updateView(View view)
    {
        try
        {
            mSeekBar.setProgress(mCurrentValue);

            TextView seekMax = (TextView) view.findViewById(R.id.seekbarMaxLabel);
            seekMax.setText(String.valueOf(mCurrentValue));
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "Error updating seek bar preference", e);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
        int newValue = progress + mMinValue;

        if (newValue > mMaxValue)
            newValue = mMaxValue;
        else if (newValue < mMinValue)
            newValue = mMinValue;
        else if (mInterval != 1 && newValue % mInterval != 0)
            newValue = Math.round(((float) newValue) / mInterval) * mInterval;

        // change rejected, revert to the previous value
        if (!callChangeListener(newValue))
        {
            seekBar.setProgress(mCurrentValue - mMinValue);
            return;
        }

        // change accepted, store it
        mCurrentValue = newValue;
        mValueTextView.setText(String.valueOf(newValue));
        persistInt(newValue);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar)
    {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar)
    {
        notifyChanged();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray ta, int index)
    {
        return ta.getInt(index, DEFAULT_VALUE);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue)
    {
        if (restoreValue)
        {
            mCurrentValue = getPersistedInt(mCurrentValue);
        }
        else
        {
            int temp = 0;
            try
            {
                temp = (Integer) defaultValue;
            }
            catch (Exception ex)
            {
                Log.e(LOG_TAG, "Invalid default value: " + defaultValue.toString());
            }

            persistInt(temp);
            mCurrentValue = temp;
        }
    }

    /**
     * make sure that the seekbar is disabled if the preference is disabled
     */
    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        mSeekBar.setEnabled(enabled);
    }

    @Override
    public void onDependencyChanged(Preference dependency, boolean disableDependent)
    {
        super.onDependencyChanged(dependency, disableDependent);

        //Disable movement of seek bar when dependency is false
        if (mSeekBar != null)
        {
            mSeekBar.setEnabled(!disableDependent);
        }
    }
}

