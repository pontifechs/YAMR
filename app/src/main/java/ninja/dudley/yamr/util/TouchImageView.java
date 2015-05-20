package ninja.dudley.yamr.util;


import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

public class TouchImageView extends ImageView implements OrientationAware.I, ScaleGestureDetector.OnScaleGestureListener, GestureDetector.OnGestureListener
{

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;


    // Image positioning information
    private Matrix matrixHistory;

    public TouchImageView(Context context)
    {
        super(context);
        scaleGestureDetector = new ScaleGestureDetector(context, this);
        gestureDetector = new GestureDetector(context, this);
        init();
    }

    public TouchImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        scaleGestureDetector = new ScaleGestureDetector(context, this);
        gestureDetector = new GestureDetector(context, this);
        init();
    }

    public TouchImageView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        scaleGestureDetector = new ScaleGestureDetector(context, this);
        gestureDetector = new GestureDetector(context, this);
        init();
    }

    private int getScreenWidthPx()
    {
        Configuration config = getResources().getConfiguration();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, config.screenWidthDp, getResources().getDisplayMetrics());
    }

    private int getScreenHeightPx()
    {
        Configuration config = getResources().getConfiguration();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, config.screenHeightDp, getResources().getDisplayMetrics());
    }

    private void init()
    {
        reset();
        setScaleType(ScaleType.MATRIX);
    }

    private void reset()
    {
        matrixHistory = new Matrix();
        if (getDrawable() != null)
        {
            float scale = (float) getScreenWidthPx() / getDrawable().getIntrinsicWidth();
            applyToImageMatrix(new PointF(), scale, new PointF());
        }
    }

    /**
     * Applies an in-flight set of modifications to the historic matrix
     *
     * @param scaleCenter  The center of the in-flight scale. (Focus of scale gesture)
     * @param scale        The actual scale to apply
     * @param scrollOffset The displacement to scroll the image to the correct place.
     */
    private void applyToImageMatrix(PointF scaleCenter, float scale, PointF scrollOffset)
    {
        Matrix m = matrixHistory;
        // Translate to the scaling center
        m.postTranslate(-scaleCenter.x, -scaleCenter.y);
        // Scale to the appropriate size
        m.postScale(scale, scale);
        // Translate back to 0,0 and then  to the correct scroll position.
        m.postTranslate(scaleCenter.x + scrollOffset.x, scaleCenter.y + scrollOffset.y);

        // Take a peek into the matrix to do the clamping.
        //   -- All in all, I think it's easier to let the matrix math do its thing
        //   and then make sure the final translates (with scales already applied because
        //   of the order we multiplied in) don't take the image out of bounds.
        float[] matrixGuts = new float[9];
        m.getValues(matrixGuts);

        float totalScale = matrixGuts[Matrix.MSCALE_X];

        if (matrixGuts[Matrix.MTRANS_X] >= 0)
        {
            matrixGuts[Matrix.MTRANS_X] = 0;
        }
        if (matrixGuts[Matrix.MTRANS_Y] >= 0)
        {
            matrixGuts[Matrix.MTRANS_Y] = 0;
        }

        Drawable image = getDrawable();

        float minXEdge = -(image.getIntrinsicWidth() * totalScale - getScreenWidthPx());
        if (matrixGuts[Matrix.MTRANS_X] <= minXEdge)
        {
            matrixGuts[Matrix.MTRANS_X] = minXEdge;
        }
        float minYEdge = -(image.getIntrinsicHeight() * totalScale - getScreenHeightPx());
        if (matrixGuts[Matrix.MTRANS_Y] <= minYEdge)
        {
            matrixGuts[Matrix.MTRANS_Y] = minYEdge;
        }

        m.setValues(matrixGuts);

        setImageMatrix(m);
        invalidate();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        OrientationAware.handleOrientationAware(this, newConfig);
    }

    @Override
    public void onPortrait(Configuration newConfig)
    {
        reset();
    }

    @Override
    public void onLandscape(Configuration newConfig)
    {
        reset();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e)
    {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e)
    {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e)
    {
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
    {
        applyToImageMatrix(new PointF(), 1.0f, new PointF(-distanceX, -distanceY));
        matrixHistory = getImageMatrix();
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e)
    {
        reset();
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
    {
        // TODO:: Put fancy fling in.
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector)
    {
        PointF center = new PointF(detector.getFocusX(), detector.getFocusY());
        float scale = detector.getCurrentSpan() / detector.getPreviousSpan();
        applyToImageMatrix(center, scale, new PointF());
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector)
    {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector)
    {
        PointF center = new PointF(detector.getFocusX(), detector.getFocusY());
        float scale = detector.getCurrentSpan() / detector.getPreviousSpan();
        applyToImageMatrix(center, scale, new PointF());
        matrixHistory = getImageMatrix();
    }

    @Override
    public void setImageDrawable(Drawable drawable)
    {
        super.setImageDrawable(drawable);
        reset();
    }
}

