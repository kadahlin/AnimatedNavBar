package com.kyledahlin.highlighttabbar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MenuItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator


class CurvedBottomNavigationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr) {

    private class FloatPoint(var x: Float, var y: Float) {
        fun set(x: Float, y: Float) {
            this.x = x
            this.y = y
        }
    }

    private var mSelectedListener: BottomNavigationView.OnNavigationItemSelectedListener? = null

    private val curveRadius = 20.toPx()

    private val mPath: Path = Path()
    private val mPaint: Paint = Paint()

    private var mHeight = 0f
    private var mWidth = 0f

    private var mItemCenter = 0f //this is the point at x = the center of the selected menu item and y = 0
    private var mSelectedIndex = 0

    private var mFirstCurveStartPoint = FloatPoint(0f, 0f)
    private var mFirstCurveEndPoint = FloatPoint(0f, 0f)
    private var mSecondCurveStartPoint = FloatPoint(0f, 0f)
    private var mSecondCurveEndPoint = FloatPoint(0f, 0f)

    private var mFirstCurveControlPoint1 = FloatPoint(0f, 0f)
    private var mFirstCurveControlPoint2 = FloatPoint(0f, 0f)
    private var mSecondCurveControlPoint1 = FloatPoint(0f, 0f)
    private var mSecondCurveControlPoint2 = FloatPoint(0f, 0f)

    private var mAnimator: ValueAnimator? = null

    init {
        mPaint.style = Paint.Style.FILL_AND_STROKE
        setBackgroundColor(Color.TRANSPARENT)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CurvedBottomNavigationView,
            0, 0
        ).apply {

            try {
                mPaint.color = getColor(
                    R.styleable.CurvedBottomNavigationView_navBarColor,
                    resources.getColor(android.R.color.white)
                )
                //TODO: find the default nav bar color
            } finally {
                recycle()
            }
        }

        super.setOnNavigationItemSelectedListener(::onNavigate)
    }

    override fun setOnNavigationItemSelectedListener(listener: OnNavigationItemSelectedListener?) {
        mSelectedListener = listener
    }

    private fun onNavigate(item: MenuItem) : Boolean {
        var itemIndex: Int? = null
        for(index in 0 until menu.size()) {
            if(item == menu.getItem(index)) {
                itemIndex = index
            }
        }
        if(itemIndex != null) {
            mSelectedIndex = itemIndex
            startSlideAnimation(getCenterForIndex(mSelectedIndex))
        }
        return mSelectedListener?.onNavigationItemSelected(item) ?: true
    }

    //Return the X coordinate that matches this item index
    private fun getCenterForIndex(index: Int): Float {
        return (mWidth / menu.size() * index) + (mWidth / menu.size() / 2)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w.toFloat()
        mHeight = h.toFloat()
        mItemCenter =  getCenterForIndex(mSelectedIndex) //TODO: change to get itemCenter from currentIndex
        calculatePath()
    }

    private fun calculatePath() {
        mFirstCurveStartPoint.set(mItemCenter - (curveRadius * 2) - (curveRadius / 2), 0f)
        // the coordinates (x,y) of the end point after curve
        mFirstCurveEndPoint.set(mItemCenter, curveRadius + (curveRadius / 2f))
        // same thing for the second curve
        mSecondCurveStartPoint = mFirstCurveEndPoint
        mSecondCurveEndPoint.set(mItemCenter  + (curveRadius * 2) + (curveRadius / 2), 0f)

        // the coordinates (x,y)  of the 1st control point on a cubic curve
        mFirstCurveControlPoint1.set(
            mFirstCurveStartPoint.x + curveRadius + (curveRadius / 4),
            mFirstCurveStartPoint.y
        )
        // the coordinates (x,y)  of the 2nd control point on a cubic curve
        mFirstCurveControlPoint2.set(
            mFirstCurveEndPoint.x - (curveRadius * 2) + curveRadius,
            mFirstCurveEndPoint.y
        )

        mSecondCurveControlPoint1.set(
            mSecondCurveStartPoint.x + (curveRadius * 2) - curveRadius,
            mSecondCurveStartPoint.y
        )
        mSecondCurveControlPoint2.set(
            mSecondCurveEndPoint.x - (curveRadius + (curveRadius / 4)),
            mSecondCurveEndPoint.y
        )

        mPath.reset()
        mPath.moveTo(0f, 0f)
        mPath.lineTo(mFirstCurveStartPoint.x, mFirstCurveStartPoint.y)

        mPath.cubicTo(
            mFirstCurveControlPoint1.x, mFirstCurveControlPoint1.y,
            mFirstCurveControlPoint2.x, mFirstCurveControlPoint2.y,
            mFirstCurveEndPoint.x, mFirstCurveEndPoint.y
        )

        mPath.cubicTo(
            mSecondCurveControlPoint1.x, mSecondCurveControlPoint1.y,
            mSecondCurveControlPoint2.x, mSecondCurveControlPoint2.y,
            mSecondCurveEndPoint.x, mSecondCurveEndPoint.y
        )

        mPath.lineTo(mWidth, 0f)
        mPath.lineTo(mWidth, mHeight)
        mPath.lineTo(0f, mHeight)
        mPath.close()
    }

    private fun startSlideAnimation(newCenter: Float) {
        mAnimator?.cancel()
        mAnimator = ValueAnimator.ofFloat(mItemCenter, newCenter)
        mAnimator?.duration = 250
        mAnimator?.interpolator = AccelerateDecelerateInterpolator()
        mAnimator?.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            mItemCenter = value
            calculatePath()
            invalidate()
        }
        mAnimator?.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(mPath, mPaint)
    }

    private fun Int.toPx(): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), resources.displayMetrics).toInt()
    }

}