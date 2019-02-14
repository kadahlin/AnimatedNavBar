package com.kyledahlin.highlighttabbar

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet as CS

class CurvedBottomNavigationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val ANIMATION_DURATION = 700L
        private const val MAX_ALPHA = .2f
    }

    private class FloatPoint(var x: Float, var y: Float) {
        fun set(x: Float, y: Float) {
            this.x = x
            this.y = y
        }
    }

    private val curveRadius = 30.toPx()

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
    private val mImageViews = mutableListOf<ImageView>()
    private val mImageAnimators = mutableListOf<AnimatorContainer?>()
    private val ALPHA_DURATION: Long

    private val mCircleY: Float = 0f
    private var mCircleAnimator: AnimatorContainer? = null
    private val mCircleContainer: CircleContainer
    private val mInsetRect = Rect()
    private var mNavBarHeightGap = 0f

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

        val drawableToIds = listOf(
            Pair(R.drawable.android_robot, 1),
            Pair(R.drawable.android_robot, 2),
            Pair(R.drawable.android_robot, 3),
            Pair(R.drawable.android_robot, 4),
            Pair(R.drawable.android_robot, 5)
        )

        drawableToIds.forEachIndexed { index, pair ->
            val imageView = ImageView(context).apply {
                setImageResource(pair.first)
                setOnClickListener {
                    onNavigate(index)
                }
                id = View.generateViewId()
                alpha = if (index == mSelectedIndex) 0f else MAX_ALPHA
            }
            addView(imageView)
            mImageViews.add(imageView)
        }
        ALPHA_DURATION = ANIMATION_DURATION / mImageViews.size / 4
        mImageViews.forEach { mImageAnimators.add(null) }

        mCircleContainer = CircleContainer(mPaint.color, curveRadius, context).apply {
            id = View.generateViewId()
        }
        addView(mCircleContainer)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        applyConstraints()
    }

    private fun applyConstraints() {
        CS().apply {
            clone(this@CurvedBottomNavigationView)
            mImageViews.forEachIndexed { index, img ->
                val startMargin =
                    (index * (mWidth / mImageViews.size) + (mWidth / mImageViews.size / 2)).toInt() - img.width / 2
                constrainHeight(img.id, 46.toPx())
                constrainWidth(img.id, 54.toPx())
                connect(img.id, CS.START, CS.PARENT_ID, CS.START, startMargin)
                connect(img.id, CS.TOP, CS.PARENT_ID, CS.TOP, mNavBarHeightGap.toInt())
                connect(img.id, CS.BOTTOM, CS.PARENT_ID, CS.BOTTOM, 0)
            }
            constrainWidth(mCircleContainer.id, curveRadius * 2)
            constrainHeight(mCircleContainer.id, curveRadius * 2)
            connect(mCircleContainer.id, CS.TOP, CS.PARENT_ID, CS.TOP)
            connect(
                mCircleContainer.id,
                CS.START,
                CS.PARENT_ID,
                CS.START,
                (mItemCenter - mCircleContainer.width / 2).toInt()
            )

        }.applyTo(this@CurvedBottomNavigationView)
    }

    private fun onNavigate(index: Int): Boolean {
        val oldSelectedIndex = mSelectedIndex
        mSelectedIndex = index
        if (oldSelectedIndex != mSelectedIndex) {
            startSlideAnimation(getCenterForIndex(mSelectedIndex))
        }
//        return mSelectedListener?.onNavigationItemSelected(item) ?: true
        return true
    }

    //Return the X coordinate that matches this item index
    private fun getCenterForIndex(index: Int): Float {
        return (mWidth / mImageViews.size * index) + (mWidth / mImageViews.size / 2)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d("Kyle", "onSizeChanged")
        mWidth = w.toFloat()
        mHeight = h.toFloat()
        mNavBarHeightGap = curveRadius.toFloat()
        mItemCenter = getCenterForIndex(mSelectedIndex) //TODO: change to get itemCenter from currentIndex
        for (imageChild in mImageViews) {
            val params = imageChild.layoutParams as ConstraintLayout.LayoutParams
            params.width = mWidth.toInt() / 3 / 2
            params.height = mHeight.toInt() * (2 / 3)
            imageChild.layoutParams = params
            imageChild.setPadding(12.toPx(), 8.toPx(), 12.toPx(), 8.toPx())
        }
        calculatePath()
    }

    private fun calculatePath() {
        Log.d("Kyle", "calculatePath")
        mFirstCurveStartPoint.set(mItemCenter - (curveRadius * 2), mNavBarHeightGap)
        // the coordinates (x,y) of the end point after curve
        mFirstCurveEndPoint.set(mItemCenter, mNavBarHeightGap + curveRadius + (curveRadius / 2f))
        // same thing for the second curve
        mSecondCurveStartPoint = mFirstCurveEndPoint
        mSecondCurveEndPoint.set(mItemCenter + (curveRadius * 2), mNavBarHeightGap)

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
        mPath.moveTo(0f, mNavBarHeightGap)
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

        mPath.lineTo(mWidth, mNavBarHeightGap)
        mPath.lineTo(mWidth, mHeight)
        mPath.lineTo(0f, mHeight)
        mPath.close()
    }

    //Animate only the values that change in the background path (i.e. the cutout from the top of the background)
    private fun startSlideAnimation(targetCenter: Float) {
        mAnimator?.cancel()
        mAnimator = ValueAnimator.ofFloat(mItemCenter, targetCenter)
        mAnimator?.duration = ANIMATION_DURATION
        mAnimator?.interpolator = AccelerateDecelerateInterpolator()
        mAnimator?.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            mItemCenter = value
            val params = mCircleContainer.layoutParams as ConstraintLayout.LayoutParams
            params.marginStart = (mItemCenter - mCircleContainer.width / 2).toInt()
            mCircleContainer.layoutParams = params
            if (itemCenterCollidesWith(mImageViews[mSelectedIndex])) {
                animateCircleBack()
            }
            calculatePath()
            applyAlphaAnimators()
            invalidate()
        }

        mCircleAnimator?.getAnimator()?.cancel()
        val newCircleAnimator = ValueAnimator.ofFloat(mCircleContainer.y, mHeight)
        newCircleAnimator.duration = ANIMATION_DURATION / 4
        newCircleAnimator.addUpdateListener {
            val params = mCircleContainer.layoutParams as ConstraintLayout.LayoutParams
            params.topMargin = (it.animatedValue as Float).toInt()
            mCircleContainer.layoutParams = params
        }
        mAnimator?.start()
        mCircleAnimator = CircleOutAnimator(newCircleAnimator)
        mCircleAnimator?.getAnimator()?.start()

    }

    //animate the circle that shows the currently selected item from the nav bar
    private fun animateCircleBack() {
        if (mCircleAnimator == null || mCircleAnimator is CircleOutAnimator) {
            mCircleAnimator?.getAnimator()?.cancel()
            val newCircleAnimator = ValueAnimator.ofFloat(mCircleContainer.y, 0f)
            newCircleAnimator?.duration = ANIMATION_DURATION / 4
            newCircleAnimator?.addUpdateListener {
                val params = mCircleContainer.layoutParams as ConstraintLayout.LayoutParams
                params.topMargin = (it.animatedValue as Float).toInt()
                mCircleContainer.layoutParams = params
            }
            mCircleAnimator = CircleInAnimator(newCircleAnimator)
            mCircleAnimator?.getAnimator()?.start()
        }
    }

    private fun itemCenterCollidesWith(view: View): Boolean {
        return (mItemCenter < (view.x + view.width + view.width / 2)) && (mItemCenter > view.x - view.width / 2)
    }

    private fun applyAlphaAnimators() {
        for ((index, imageView) in mImageViews.withIndex()) {
            if (itemCenterCollidesWith(imageView)) {
                if (mImageAnimators[index] == null) {
                    val alphaOutAnimation = ObjectAnimator.ofFloat(imageView, "alpha", imageView.alpha, 0f)
                    alphaOutAnimation.duration = ALPHA_DURATION
                    alphaOutAnimation.start()
                    mImageAnimators[index] = AlphaOutAnimator(alphaOutAnimation)
                }
            } else if (mImageAnimators[index] != null && mImageAnimators[index] is AlphaOutAnimator) {
                mImageAnimators[index]?.getAnimator()?.cancel()
                val alphaInAnimation = ObjectAnimator.ofFloat(imageView, "alpha", imageView.alpha, MAX_ALPHA)
                alphaInAnimation.duration = ALPHA_DURATION
                alphaInAnimation.addOnFinishListener {
                    mImageAnimators[index] = null
                }
                alphaInAnimation.start()
                mImageAnimators[index] = AlphaInAnimator(alphaInAnimation)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(mPath, mPaint)
    }

    private fun Int.toPx(): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), resources.displayMetrics).toInt()
    }

    private fun Animator.addOnFinishListener(onFinish: () -> Unit) {
        this.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                onFinish()
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
            }
        })
    }

    private interface AnimatorContainer {
        fun getAnimator(): ValueAnimator
    }

    private class AlphaInAnimator(val objectAnimator: ValueAnimator) : AnimatorContainer {
        override fun getAnimator() = objectAnimator
    }

    private class AlphaOutAnimator(val objectAnimator: ValueAnimator) : AnimatorContainer {
        override fun getAnimator() = objectAnimator
    }

    private class CircleOutAnimator(val objectAnimator: ValueAnimator) : AnimatorContainer {
        override fun getAnimator(): ValueAnimator {
            return objectAnimator
        }
    }

    private class CircleInAnimator(val objectAnimator: ValueAnimator) : AnimatorContainer {
        override fun getAnimator(): ValueAnimator {
            return objectAnimator
        }
    }

}