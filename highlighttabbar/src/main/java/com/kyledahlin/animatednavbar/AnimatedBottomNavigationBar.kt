package com.kyledahlin.animatednavbar

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.AnimatedVectorDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet as CS

typealias OnIndexSelectedListener = (Int) -> Unit

class AnimatedBottomNavigationBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val ANIMATION_DURATION = 600L
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

    private var mItemCenter = 0f //this is x coordinate at the center of the currently selected index
    private var mSelectedIndex = 0

    private var mAnimator: ValueAnimator? = null
    private val mImageViews = mutableListOf<ImageView>()
    private val mImageAnimators = mutableListOf<AnimatorContainer?>()
    private val mAlphaDuration: Long

    private var mCircleAnimator: AnimatorContainer? = null
    private val mCircleContainer: CircleImageViewContainer   //the container that will hold the selected item image view
    private var mNavBarHeightGap = 0f   //the distance between the 0 y coordinate and the top of the nav bar background

    private var mIndexSelectedListener: OnIndexSelectedListener? = null

    private val mBackground: NavBarBackground

    init {
        mPaint.style = Paint.Style.FILL_AND_STROKE
        setBackgroundColor(Color.TRANSPARENT)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AnimatedBottomNavigationBar,
            0, 0
        ).apply {

            try {
                mPaint.color = getColor(
                    R.styleable.AnimatedBottomNavigationBar_navBarColor,
                    resources.getColor(android.R.color.white)
                )
                //TODO: find the default nav bar color
            } finally {
                recycle()
            }
        }

        mCircleContainer = CircleImageViewContainer(mPaint.color, curveRadius, context).apply {
            id = View.generateViewId()
            setPadding(
                curveRadius - 24.toPx() / 2,
                curveRadius - 24.toPx() / 2,
                curveRadius - 24.toPx() / 2,
                curveRadius - 24.toPx() / 2
            )
        }
        addView(mCircleContainer)

        mBackground = NavBarBackground(context, attrs, defStyleAttr).apply {
            id = View.generateViewId()
            elevation = 6f
        }
        addView(mBackground)

        //TODO: pull this from xml
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
                elevation = 6f
            }
            addView(imageView)
            mImageViews.add(imageView)
        }
        mAlphaDuration = ANIMATION_DURATION / mImageViews.size / 20
        repeat(mImageViews.size) { mImageAnimators.add(null) }
    }

    fun setIndexSelectedListener(onIndexSelectedListener: OnIndexSelectedListener) {
        mIndexSelectedListener = onIndexSelectedListener
    }

    private fun applyConstraints() {
        CS().apply {
            clone(this@AnimatedBottomNavigationBar)
            mImageViews.forEachIndexed { index, img ->
                val startMargin =
                    (index * (mWidth / mImageViews.size) + (mWidth / mImageViews.size / 2)).toInt() - img.width / 2
                connect(img.id, CS.START, CS.PARENT_ID, CS.START, startMargin)
                connect(img.id, CS.TOP, CS.PARENT_ID, CS.TOP, mNavBarHeightGap.toInt())
                connect(img.id, CS.BOTTOM, CS.PARENT_ID, CS.BOTTOM)
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

        }.applyTo(this@AnimatedBottomNavigationBar)
    }

    private fun onNavigate(index: Int): Boolean {
        val oldSelectedIndex = mSelectedIndex
        mSelectedIndex = index
        if (oldSelectedIndex != mSelectedIndex) {
            startSlideAnimation(getCenterForIndex(mSelectedIndex))
        }
        mIndexSelectedListener?.invoke(index)
        return true
    }

    //Return the X coordinate that matches this item index
    private fun getCenterForIndex(index: Int): Float {
        return (mWidth / mImageViews.size * index) + (mWidth / mImageViews.size / 2)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w.toFloat()
        mHeight = h.toFloat()
        mNavBarHeightGap = mHeight - 56.toPx()
        mItemCenter = getCenterForIndex(mSelectedIndex)
        for (imageChild in mImageViews) {
            val params = imageChild.layoutParams as ConstraintLayout.LayoutParams
            params.width = 48.toPx()
            params.height = 40.toPx()
            imageChild.layoutParams = params
            imageChild.setPadding(12.toPx(), 8.toPx(), 12.toPx(), 8.toPx())
        }
        mBackground.calculatePath()
    }

    //Animate only the values that change in the background path (i.e. the cutout from the top of the background)
    private fun startSlideAnimation(targetCenter: Float) {
        mAnimator?.cancel()
        mAnimator = ValueAnimator.ofFloat(mItemCenter, targetCenter)
        mAnimator?.duration = ANIMATION_DURATION
        mAnimator?.interpolator = DecelerateInterpolator()
        mAnimator?.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            mItemCenter = value
            val params = mCircleContainer.layoutParams as ConstraintLayout.LayoutParams
            params.marginStart = (mItemCenter - mCircleContainer.width / 2).toInt()
            mCircleContainer.layoutParams = params
            if (itemCenterCollidesWith(mImageViews[mSelectedIndex])) {
                animateCircleBack()
            }
            mBackground.calculatePath()
            applyAlphaAnimators()
            invalidate()
        }

        mCircleAnimator?.getAnimator()?.cancel()
        val newCircleAnimator = ValueAnimator.ofFloat(mCircleContainer.y, mHeight)
        newCircleAnimator.duration = ANIMATION_DURATION / 8
        newCircleAnimator.interpolator = AccelerateInterpolator()
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
            newCircleAnimator.interpolator = DecelerateInterpolator()
            newCircleAnimator?.addUpdateListener {
                val params = mCircleContainer.layoutParams as ConstraintLayout.LayoutParams
                params.topMargin = (it.animatedValue as Float).toInt()
                mCircleContainer.layoutParams = params
            }
            mCircleAnimator = CircleInAnimator(newCircleAnimator)
            mCircleAnimator?.getAnimator()?.start()
            mCircleContainer.setImageResource(R.drawable.avd_anim)
            val drawable = mCircleContainer.drawable
            if (drawable is AnimatedVectorDrawable) {
                drawable.start()
            }
        }
    }

    private fun itemCenterCollidesWith(view: View): Boolean {
        return (mItemCenter < (view.x + view.width + 12.toPx() * 2)) && (mItemCenter > view.x - 12.toPx() * 2)
    }

    private fun applyAlphaAnimators() {
        for ((index, imageView) in mImageViews.withIndex()) {
            if (itemCenterCollidesWith(imageView)) {
                if (mImageAnimators[index] == null) {
                    val alphaOutAnimation = ObjectAnimator.ofFloat(imageView, "alpha", imageView.alpha, 0f)
                    alphaOutAnimation.interpolator = AccelerateInterpolator()
                    alphaOutAnimation.duration = mAlphaDuration
                    alphaOutAnimation.start()
                    mImageAnimators[index] = AlphaOutAnimator(alphaOutAnimation)
                }
            } else if (mImageAnimators[index] != null && mImageAnimators[index] is AlphaOutAnimator) {
                mImageAnimators[index]?.getAnimator()?.cancel()
                val alphaInAnimation = ObjectAnimator.ofFloat(imageView, "alpha", imageView.alpha, MAX_ALPHA)
                alphaInAnimation.duration = mAlphaDuration
                alphaInAnimation.interpolator = DecelerateInterpolator()
                alphaInAnimation.addOnFinishListener {
                    mImageAnimators[index] = null
                }
                alphaInAnimation.start()
                mImageAnimators[index] = AlphaInAnimator(alphaInAnimation)
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas?) {
        applyConstraints()  //set the placement of the children before drawing them
        super.dispatchDraw(canvas)
    }

    //Convert the given int DP amount to its equivalent pixels
    private fun Int.toPx(): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), resources.displayMetrics).toInt()
    }

    //Class that implements the "background" of the navbar. Having this as a separate view was the easiest way to have the circle image view
    //animate behind the rest of the navbar
    private inner class NavBarBackground @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr) {

        private var mFirstCurveStartPoint = FloatPoint(0f, 0f)
        private var mFirstCurveEndPoint = FloatPoint(0f, 0f)
        private var mSecondCurveStartPoint = FloatPoint(0f, 0f)
        private var mSecondCurveEndPoint = FloatPoint(0f, 0f)

        private var mFirstCurveControlPoint1 = FloatPoint(0f, 0f)
        private var mFirstCurveControlPoint2 = FloatPoint(0f, 0f)
        private var mSecondCurveControlPoint1 = FloatPoint(0f, 0f)
        private var mSecondCurveControlPoint2 = FloatPoint(0f, 0f)

        fun calculatePath() {
            mFirstCurveStartPoint.set(mItemCenter - (curveRadius * 2), mNavBarHeightGap)
            mFirstCurveEndPoint.set(mItemCenter, mNavBarHeightGap + curveRadius + (curveRadius / 2f))
            mSecondCurveStartPoint = mFirstCurveEndPoint
            mSecondCurveEndPoint.set(mItemCenter + (curveRadius * 2), mNavBarHeightGap)

            mFirstCurveControlPoint1.set(
                mFirstCurveStartPoint.x + curveRadius,
                mFirstCurveStartPoint.y
            )

            mFirstCurveControlPoint2.set(
                mFirstCurveEndPoint.x - (curveRadius * 2) + curveRadius / 2,
                mFirstCurveEndPoint.y
            )

            mSecondCurveControlPoint1.set(
                mSecondCurveStartPoint.x + (curveRadius * 2) - curveRadius / 2,
                mSecondCurveStartPoint.y
            )
            mSecondCurveControlPoint2.set(
                mSecondCurveEndPoint.x - curveRadius,
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

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawPath(mPath, mPaint)
        }
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