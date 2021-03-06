package ninja.dudley.yamr.ui.util

import android.content.Context
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import android.widget.OverScroller

class TouchImageView : ImageView
{
    constructor(context: Context) : super(context)
    {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    {
        init(context)
    }

    interface SwipeListener
    {
        fun onSwipeLeft()

        fun onSwipeRight()
    }

    private var parent: SwipeListener? = null
    fun register(parent: SwipeListener)
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
        scaleType = ImageView.ScaleType.MATRIX
        val m = Matrix()
        m.setScale(minScale(), minScale())
        imageMatrix = m
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
            val m = Matrix(imageMatrix)
            val newPos = PointF(overScroller!!.currX.toFloat(), overScroller!!.currY.toFloat())
            m.postTranslate(newPos.x - translate.x, newPos.y - translate.y)

            imageMatrix = m
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
        val pt = Point()
        try {
            display.getRealSize(pt)
        }
        catch (e: NullPointerException)
        {
            return resources.displayMetrics.heightPixels
        }
        return pt.x
    }

    private fun screenHeight(): Int
    {
        val pt = Point()
        try {
            display.getRealSize(pt)
        }
        catch (e: NullPointerException)
        {
            // This happens when we try to get the height without a the display being done initializing.
            // I need to go back through this mess. This isn't the only place I have to deal with something like this
            // as I recall.
            return resources.displayMetrics.heightPixels
        }
        return pt.y
    }

    private fun horizontalSlop(): Int
    {
        if (drawable == null)
        {
            return 0
        }
        return Math.max(0.0f, (drawable.intrinsicWidth * scale) - screenWidth()).toInt()
    }

    private fun verticalSlop(): Int
    {
        if (drawable == null)
        {
            return 0
        }
        return Math.max(0.0f, (drawable.intrinsicHeight * scale) - screenHeight()).toInt()
    }

    private fun unsafeVerticalSlop(): Float
    {
        return (drawable.intrinsicHeight * scale) - screenHeight()
    }

    private fun minScale(): Float
    {
        if (drawable == null)
        {
            return 1.0f
        }
        return screenWidth() / drawable.intrinsicWidth.toFloat()
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
            val m = Matrix(imageMatrix)
            m.postTranslate(-distanceX, -distanceY)
            imageMatrix = m
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean
        {
            Log.d("TouchImageView", "x: $velocityX y: $velocityY")
            translate!!
            // Detect swipe left
            val onRightEdge = translate.x.toInt() == -horizontalSlop()
            val onLeftEdge = translate.x.toInt() == 0
            val movingRight = velocityX > 0 && Math.abs(velocityX) > 3 * Math.abs(velocityY)
            val movingLeft= velocityX < 0 && Math.abs(velocityX) > 3 * Math.abs(velocityY)
            val fastEnough = Math.abs(velocityX) > 1000
            if (parent != null && onLeftEdge && movingRight && fastEnough)
            {
                parent!!.onSwipeRight()
                return true
            }

            // Detect swipe right
            if (parent != null &&  onRightEdge && movingLeft && fastEnough)
            {
                parent!!.onSwipeLeft()
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
            imageMatrix = matrix
        }
    }

    private val scaleGestureListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener()
    {
        override fun onScale(detector: ScaleGestureDetector): Boolean
        {
            val m = Matrix(imageMatrix)
            m.postTranslate(-detector.focusX, -detector.focusY)
            val newScale = detector.currentSpan / detector.previousSpan
            m.postScale(newScale, newScale)
            m.postTranslate(detector.focusX, detector.focusY)
            imageMatrix = m
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
        if (verticalSlop() == 0 && drawable != null)
        {
            guts[Matrix.MTRANS_Y] = -unsafeVerticalSlop() / 2
        }
        matrix.setValues(guts)

        super.setImageMatrix(matrix)
    }

    override fun setImageDrawable(drawable: Drawable?)
    {
        super.setImageDrawable(drawable)
        val m = Matrix()
        m.setScale(minScale(), minScale())
        imageMatrix = m
    }
}

