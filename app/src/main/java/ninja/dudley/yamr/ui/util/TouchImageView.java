package ninja.dudley.yamr.ui.util;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import android.widget.OverScroller;

public class TouchImageView extends ImageView
{
    public TouchImageView(Context context)
    {
        super(context);
        init(context);
    }

    public TouchImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public TouchImageView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public interface SwipeListener
    {
        void onSwipeLeft();

        void onSwipeRight();
    }
    private SwipeListener parent;
    public void register(SwipeListener parent)
    {
        this.parent = parent;
    }

    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private OverScroller overScroller;

    // Read access to effective position and scale.
    // These do not drive the positioning, rather they are updated to reflect the current positions
    // in lieu of calling new float[9]; matrix.getValues everywhere.
    private float scale = 1.0f;
    private PointF translate = new PointF();

    // Actual constructor
    private void init(Context context)
    {
        overScroller = new OverScroller(context);
        gestureDetector = new GestureDetector(context, gestureListener);
        scaleGestureDetector = new ScaleGestureDetector(context, scaleGestureListener);
        setScaleType(ScaleType.MATRIX);
        Matrix m = new Matrix();
        m.setScale(minScale(), minScale());
        setImageMatrix(m);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        gestureDetector.onTouchEvent(event);
        scaleGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public void computeScroll()
    {
        if (overScroller.computeScrollOffset())
        {
            Matrix m = new Matrix(getImageMatrix());
            PointF newPos = new PointF(overScroller.getCurrX(), overScroller.getCurrY());
            m.postTranslate(newPos.x - translate.x, newPos.y - translate.y);

            setImageMatrix(m);
            postInvalidateOnAnimation();
        }
    }

//
//    +------------+------------+
//    |            ^            |
//    |            |            |
//    |            |   Image    |
//    |    Slop    |            |
//    |            |            |
//    |            v            |
//    +<---------->+------------+
//    |            |            |
//    |            |            |
//    |            |            |
//    |            |   Screen   |
//    |            |            |
//    |            |            |
//    |            |            |
//    |            |            |
//    |            |            |
//    +------------+------------+
//
//
//    Neither slop will ever be negative. If the image is larger than the screen, then
//    both will be 0.
//    Scrolling should not translate any further than the slop dimensions. This is enforced in setImageMatrix();
//

    private int screenWidth()
    {
        return getResources().getDisplayMetrics().widthPixels;
    }

    private int screenHeight()
    {
        return getResources().getDisplayMetrics().heightPixels;
    }

    private int horizontalSlop()
    {
        return (int) Math.max(0.0f, (getDrawable().getIntrinsicWidth() * scale) - screenWidth());
    }

    private int verticalSlop()
    {
        return (int) Math.max(0.0f, (getDrawable().getIntrinsicHeight() * scale) - screenHeight());
    }

    private float minScale()
    {
        return screenWidth() / (float) getDrawable().getIntrinsicWidth();
    }

    private GestureDetector.OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener()
    {
        @Override
        public boolean onDown(MotionEvent e)
        {
            overScroller.forceFinished(true);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            Matrix m = new Matrix(getImageMatrix());
            m.postTranslate(-distanceX, -distanceY);
            setImageMatrix(m);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
            // Detect swipe left
            if (parent != null && translate.x == 0 && velocityX > 0 && Math.abs(velocityX) > Math.abs(velocityY))
            {
                parent.onSwipeLeft();
                return true;
            }

            // Detect swipe right
            if (parent != null && translate.x == -horizontalSlop() && velocityX < 0  && Math.abs(velocityX) > Math.abs(velocityY))
            {
                parent.onSwipeRight();
                return true;
            }

            overScroller.fling((int) translate.x, (int) translate.y, (int) velocityX, (int) velocityY, -horizontalSlop(), 0, -verticalSlop(), 0);
            postInvalidateOnAnimation();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e)
        {
            Matrix matrix = new Matrix();
            matrix.setScale(minScale(), minScale());
            setImageMatrix(matrix);
        }
    };

    private ScaleGestureDetector.OnScaleGestureListener scaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener()
    {
        @Override
        public boolean onScale(ScaleGestureDetector detector)
        {
            Matrix m = new Matrix(getImageMatrix());
            m.postTranslate(-detector.getFocusX(), -detector.getFocusY());
            float newScale = detector.getCurrentSpan() / detector.getPreviousSpan();
            m.postScale(newScale, newScale);
            m.postTranslate(detector.getFocusX(), detector.getFocusY());
            setImageMatrix(m);
            return true;
        }
    };

    @Override
    public void setImageMatrix(Matrix matrix)
    {
        // Clamp into place
        float[] guts = new float[9];
        matrix.getValues(guts);
        if (guts[Matrix.MTRANS_X] > 0)
        {
            guts[Matrix.MTRANS_X] = 0;
        }
        if (guts[Matrix.MTRANS_Y] > 0)
        {
            guts[Matrix.MTRANS_Y] = 0;
        }
        if (guts[Matrix.MTRANS_X] < -horizontalSlop())
        {
            guts[Matrix.MTRANS_X] = -horizontalSlop();
        }
        if (guts[Matrix.MTRANS_Y] < -verticalSlop())
        {
            guts[Matrix.MTRANS_Y] = -verticalSlop();
        }

        if (guts[Matrix.MSCALE_X] < minScale())
        {
            guts[Matrix.MSCALE_X] = minScale();
        }
        if (guts[Matrix.MSCALE_Y] < minScale())
        {
            guts[Matrix.MSCALE_Y] = minScale();
        }

        matrix.setValues(guts);
        scale = guts[Matrix.MSCALE_X];
        if (translate != null)
        {
            translate.x = guts[Matrix.MTRANS_X];
            translate.y = guts[Matrix.MTRANS_Y];
        }
        super.setImageMatrix(matrix);
    }

    @Override
    public void setImageDrawable(Drawable drawable)
    {
        super.setImageDrawable(drawable);
        Matrix m = new Matrix();
        m.setScale(minScale(), minScale());
        setImageMatrix(m);
    }
}

