package ninja.dudley.yamr.ui.util

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import android.widget.OverScroller

public class TouchImageView : ImageView
{
    public constructor(context: Context) : super(context)
    {
        init(context)
    }

    public constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    {
        init(context)
    }

    public constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    {
        init(context)
    }

    public interface SwipeListener
    {
        public fun onSwipeLeft()

        public fun onSwipeRight()
    }

    private var parent: SwipeListener? = null
    public fun register(parent: SwipeListener)
    {
        this.parent = parent
    }

    private var gestureDetector: GestureDetector? = null
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var overScroller: OverScroller? = null

    // Read access to effective position and scale.
    // These do not drive the positioning, rather they are updated to reflect the current positions
    // in lieu of calling new float[9]; matrix.getValues everywhere.
    private var scale = 1.0f
    private val translate: PointF? = PointF()

    // Actual constructor
    private fun init(context: Context)
    {
        overScroller = OverScroller(context)
        gestureDetector = GestureDetector(context, gestureListener)
        scaleGestureDetector = ScaleGestureDetector(context, scaleGestureListener)
        setScaleType(ImageView.ScaleType.MATRIX)
        val m = Matrix()
        m.setScale(minScale(), minScale())
        setImageMatrix(m)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean
    {
        gestureDetector!!.onTouchEvent(event)
        scaleGestureDetector!!.onTouchEvent(event)
        return true
    }

    override fun computeScroll()
    {
        if (overScroller!!.computeScrollOffset())
        {
            translate!!
            val m = Matrix(getImageMatrix())
            val newPos = PointF(overScroller!!.getCurrX().toFloat(), overScroller!!.getCurrY().toFloat())
            m.postTranslate(newPos.x - translate.x, newPos.y - translate.y)

            setImageMatrix(m)
            postInvalidateOnAnimation()
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

    private fun screenWidth(): Int
    {
        return getResources().getDisplayMetrics().widthPixels
    }

    private fun screenHeight(): Int
    {
        return getResources().getDisplayMetrics().heightPixels
    }

    private fun horizontalSlop(): Int
    {
        if (getDrawable() == null)
        {
            return 0
        }
        return Math.max(0.0f, (getDrawable().getIntrinsicWidth() * scale) - screenWidth()).toInt()
    }

    private fun verticalSlop(): Int
    {
        if (getDrawable() == null)
        {
            return 0
        }
        return Math.max(0.0f, (getDrawable().getIntrinsicHeight() * scale) - screenHeight()).toInt()
    }

    private fun unsafeVerticalSlop(): Float
    {
        return (getDrawable().getIntrinsicHeight() * scale) - screenHeight()
    }

    private fun minScale(): Float
    {
        if (getDrawable() == null)
        {
            return 1.0f
        }
        return screenWidth() / getDrawable().getIntrinsicWidth().toFloat()
    }

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener()
    {
        override fun onDown(e: MotionEvent): Boolean
        {
            overScroller!!.forceFinished(true)
            return true
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean
        {
            val m = Matrix(getImageMatrix())
            m.postTranslate(-distanceX, -distanceY)
            setImageMatrix(m)
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean
        {
            translate!!
            // Detect swipe left
            if (parent != null && translate.x.toInt() == 0 && velocityX > 0 && Math.abs(velocityX) > Math.abs(velocityY))
            {
                parent!!.onSwipeLeft()
                return true
            }

            // Detect swipe right
            if (parent != null && translate.x.toInt() == -horizontalSlop() && velocityX < 0 && Math.abs(velocityX) > Math.abs(velocityY))
            {
                parent!!.onSwipeRight()
                return true
            }

            overScroller!!.fling(translate.x.toInt(), translate.y.toInt(), velocityX.toInt(), velocityY.toInt(), -horizontalSlop(), 0, -verticalSlop(), 0)
            postInvalidateOnAnimation()
            return true
        }

        override fun onLongPress(e: MotionEvent?)
        {
            val matrix = Matrix()
            matrix.setScale(minScale(), minScale())
            setImageMatrix(matrix)
        }
    }

    private val scaleGestureListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener()
    {
        override fun onScale(detector: ScaleGestureDetector): Boolean
        {
            val m = Matrix(getImageMatrix())
            m.postTranslate(-detector.getFocusX(), -detector.getFocusY())
            val newScale = detector.getCurrentSpan() / detector.getPreviousSpan()
            m.postScale(newScale, newScale)
            m.postTranslate(detector.getFocusX(), detector.getFocusY())
            setImageMatrix(m)
            return true
        }
    }

    override fun setImageMatrix(matrix: Matrix?)
    {
        // Clamp into place
        val guts = FloatArray(9)
        matrix!!.getValues(guts)

        if (guts[Matrix.MSCALE_X] < minScale())
        {
            guts[Matrix.MSCALE_X] = minScale()
        }
        if (guts[Matrix.MSCALE_Y] < minScale())
        {
            guts[Matrix.MSCALE_Y] = minScale()
        }
        scale = guts[Matrix.MSCALE_X]


        if (guts[Matrix.MTRANS_X] > 0)
        {
            guts[Matrix.MTRANS_X] = 0f
        }
        if (guts[Matrix.MTRANS_Y] > 0)
        {
            guts[Matrix.MTRANS_Y] = 0f
        }
        if (guts[Matrix.MTRANS_X] < -horizontalSlop())
        {
            guts[Matrix.MTRANS_X] = (-horizontalSlop()).toFloat()
        }
        if (guts[Matrix.MTRANS_Y] < -verticalSlop())
        {
            guts[Matrix.MTRANS_Y] = (-verticalSlop()).toFloat()
        }


        translate?.x = guts[Matrix.MTRANS_X]
        translate?.y = guts[Matrix.MTRANS_Y]

        // Vertical is a little special. Center it vertically if the image is too small
        if (verticalSlop() == 0)
        {
            guts[Matrix.MTRANS_Y] = -unsafeVerticalSlop() / 2;
        }
        matrix.setValues(guts)

        super.setImageMatrix(matrix)
    }

    override fun setImageDrawable(drawable: Drawable?)
    {
        super.setImageDrawable(drawable)
        val m = Matrix()
        m.setScale(minScale(), minScale())
        setImageMatrix(m)
    }
}

