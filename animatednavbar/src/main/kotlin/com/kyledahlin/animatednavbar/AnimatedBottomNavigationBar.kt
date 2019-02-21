/*
Copyright 2019 Kyle Dahlin

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
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
import java.lang.IllegalStateException
import androidx.constraintlayout.widget.ConstraintSet as CS

typealias OnIdSelectedListener = (Int) -> Unit

/**
 * An animated bottom navigational bar that follows material design guidelines.
 */
class AnimatedBottomNavigationBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val ANIMATION_DURATION = 600L
        private const val TOTAL_HEIGHT_DP = 78
        private const val MATERIAL_NAV_BAR_HEIGHT_DP = 56
        private const val ITEM_HORIZONTAL_PADDING_DP = 12
        private const val ITEM_VERTICAL_PADDING_DP = 8
        private const val MATERIAL_ICON_SIZE_DP = 24
        private const val CURVE_RADIUS_DP = 30
        private const val ELEVATION = 4f
    }

    private val itemHorizontalPaddingPixels = ITEM_HORIZONTAL_PADDING_DP.toPx()
    private val itemVerticalPaddingPixels = ITEM_VERTICAL_PADDING_DP.toPx()
    private val materialIconSizePixels = MATERIAL_ICON_SIZE_DP.toPx()
    private val curveRadius = CURVE_RADIUS_DP.toPx()

    private val mPath: Path = Path()
    private val mPaint: Paint = Paint()

    private var mHeight = 0f
    private var mWidth = 0f

    private var mItemCenter = 0f //this is x coordinate at the center of the currently selected index
    private var mSelectedIndex = 0

    private var mCircleHorizontalAnimator: ValueAnimator? = null
    private var mCircleVerticalAnimator: AnimatorContainer? = null

    private val mImageViews = mutableListOf<ImageView>()
    private val mImageAnimators = mutableListOf<AnimatorContainer?>()
    private val mNavBarItems: List<NavBarItem>
    private val mAlphaDuration: Long

    private val mCircleContainer: CircleImageViewContainer   //the container that will hold the selected item image view
    private var mNavBarHeightGap = 0f   //the distance between the 0 y coordinate and the top of the nav bar background

    private var mIdSelectedListener: OnIdSelectedListener? = null
    private val mBackground: NavBarBackground

    init {
        mPaint.style = Paint.Style.FILL_AND_STROKE
        setBackgroundColor(Color.TRANSPARENT)

        var colorInt = 0
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AnimatedBottomNavigationBar,
            0, 0
        ).apply {

            try {
                colorInt = getColor(
                    R.styleable.AnimatedBottomNavigationBar_navBarColor,
                    resources.getColor(android.R.color.white)
                )
                mPaint.color = colorInt
                val menuResource = getResourceId(
                    R.styleable.AnimatedBottomNavigationBar_navBarMenu, -1
                )
                if (menuResource == -1) {
                    throw InvalidXmlException("No menu resource given to AnimatedNavBar")
                }
                mNavBarItems = loadNavBarItems(context, menuResource)
                if(mNavBarItems.size < 2 || mNavBarItems.size > 6) {
                    throw IllegalStateException("too many nav bar items for bottom navigation")
                }
                colorInt = getColor(
                    R.styleable.AnimatedBottomNavigationBar_navBarSelectedColor,
                    colorInt
                )
            } finally {
                recycle()
            }
        }

        minimumHeight = TOTAL_HEIGHT_DP.toPx()
        maxHeight = TOTAL_HEIGHT_DP.toPx()

        mCircleContainer = CircleImageViewContainer(colorInt, context).apply {
            id = View.generateViewId()
            setPadding(
                curveRadius - materialIconSizePixels / 2,
                curveRadius - materialIconSizePixels / 2,
                curveRadius - materialIconSizePixels / 2,
                curveRadius - materialIconSizePixels / 2
            )
            setImageResource(mNavBarItems[mSelectedIndex].selectedDrawableId)
            elevation = ELEVATION
        }
        addView(mCircleContainer)

        mBackground = NavBarBackground(context, attrs, defStyleAttr).apply {
            id = View.generateViewId()
            elevation = ELEVATION
        }
        addView(mBackground)

        mNavBarItems.forEachIndexed { index, navBarItem ->
            val imageView = ImageView(context).apply {
                setImageResource(navBarItem.unselectedDrawableId)
                setOnClickListener {
                    onNavigate(index)
                }
                id = View.generateViewId()
                alpha = if (index == mSelectedIndex) 0f else 1f
                elevation = ELEVATION
            }
            addView(imageView)
            mImageViews.add(imageView)
        }
        mAlphaDuration =
            ANIMATION_DURATION / mImageViews.size / 20 //seems to be the ideal animation divisor for a nice visual speed
        repeat(mImageViews.size) { mImageAnimators.add(null) }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w.toFloat()
        mHeight = h.toFloat()
        mNavBarHeightGap = mHeight - MATERIAL_NAV_BAR_HEIGHT_DP.toPx()
        mItemCenter = getCenterForIndex(mSelectedIndex)
        for (imageChild in mImageViews) {
            val params = imageChild.layoutParams as ConstraintLayout.LayoutParams
            params.width = materialIconSizePixels + itemHorizontalPaddingPixels * 2
            params.height = materialIconSizePixels + itemVerticalPaddingPixels * 2
            imageChild.layoutParams = params
            imageChild.setPadding(
                itemHorizontalPaddingPixels,
                itemVerticalPaddingPixels,
                itemHorizontalPaddingPixels,
                itemVerticalPaddingPixels
            )
        }
        mBackground.calculatePath()
        val drawable = mCircleContainer.drawable
        if (drawable is AnimatedVectorDrawable) {
            drawable.start()
        }
    }

    override fun dispatchDraw(canvas: Canvas?) {
        applyConstraints()  //set the placement of the children before drawing them
        super.dispatchDraw(canvas)
    }

    fun setIdSelectedListener(onIdSelectedListener: OnIdSelectedListener) {
        mIdSelectedListener = onIdSelectedListener
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
            startSlideAnimation(index, getCenterForIndex(mSelectedIndex))
        }
        mIdSelectedListener?.invoke(mNavBarItems[index].androidId)
        return true
    }

    //Return the X coordinate that matches this item index
    private fun getCenterForIndex(index: Int): Float {
        return (mWidth / mImageViews.size * index) + (mWidth / mImageViews.size / 2)
    }

    //Animate only the values that change in the background path (i.e. the cutout from the top of the background)
    private fun startSlideAnimation(index: Int, targetCenter: Float) {
        mCircleHorizontalAnimator?.cancel()
        mCircleHorizontalAnimator = ValueAnimator.ofFloat(mItemCenter, targetCenter)
        mCircleHorizontalAnimator?.duration =
            ANIMATION_DURATION
        mCircleHorizontalAnimator?.interpolator = DecelerateInterpolator()
        mCircleHorizontalAnimator?.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            mItemCenter = value
            val params = mCircleContainer.layoutParams as ConstraintLayout.LayoutParams
            params.marginStart = (mItemCenter - mCircleContainer.width / 2).toInt()
            mCircleContainer.layoutParams = params
            if (itemCenterCollidesWith(mImageViews[mSelectedIndex])) {
                animateCircleBack(index)
            }
            mBackground.calculatePath()
            applyAlphaAnimators()
            invalidate()
        }

        mCircleVerticalAnimator?.getAnimator()?.cancel()
        val newCircleAnimator = ValueAnimator.ofFloat(mCircleContainer.y, mHeight)
        newCircleAnimator.duration = ANIMATION_DURATION / 8
        newCircleAnimator.interpolator = AccelerateInterpolator()
        newCircleAnimator.addUpdateListener {
            val params = mCircleContainer.layoutParams as ConstraintLayout.LayoutParams
            params.topMargin = (it.animatedValue as Float).toInt()
            mCircleContainer.layoutParams = params
        }
        mCircleHorizontalAnimator?.start()
        mCircleVerticalAnimator =
            CircleOutAnimator(newCircleAnimator)
        mCircleContainer.setImageResource(android.R.color.transparent)
        mCircleVerticalAnimator?.getAnimator()?.start()

    }

    //animate the circle that shows the currently selected item from the nav bar
    private fun animateCircleBack(index: Int) {
        if (mCircleVerticalAnimator == null || mCircleVerticalAnimator is CircleOutAnimator) {
            mCircleVerticalAnimator?.getAnimator()?.cancel()
            val newCircleAnimator = ValueAnimator.ofFloat(mCircleContainer.y, 0f)
            newCircleAnimator?.duration = ANIMATION_DURATION / 4
            newCircleAnimator.interpolator = DecelerateInterpolator()
            newCircleAnimator?.addUpdateListener {
                val params = mCircleContainer.layoutParams as ConstraintLayout.LayoutParams
                params.topMargin = (it.animatedValue as Float).toInt()
                mCircleContainer.layoutParams = params
            }
            mCircleVerticalAnimator =
                CircleInAnimator(newCircleAnimator)
            mCircleVerticalAnimator?.getAnimator()?.start()
            mCircleContainer.setImageResource(mNavBarItems[index].selectedDrawableId)
            val drawable = mCircleContainer.drawable
            if (drawable is AnimatedVectorDrawable) {
                drawable.start()
            }
        }
    }

    //is the current center (X coordinate at the center of the "dip") colliding with the given view
    private fun itemCenterCollidesWith(view: View): Boolean {
        return (mItemCenter < (view.x + view.width + itemHorizontalPaddingPixels * 3)) && (mItemCenter > view.x - itemHorizontalPaddingPixels * 3)
    }

    //apply any necessary alpha animators on the selectable icons
    private fun applyAlphaAnimators() {
        for ((index, imageView) in mImageViews.withIndex()) {
            if (itemCenterCollidesWith(imageView)) {
                if (mImageAnimators[index] == null) {
                    val alphaOutAnimation = ObjectAnimator.ofFloat(imageView, "alpha", imageView.alpha, 0f)
                    alphaOutAnimation.interpolator = AccelerateInterpolator()
                    alphaOutAnimation.duration = mAlphaDuration
                    alphaOutAnimation.start()
                    mImageAnimators[index] =
                        AlphaOutAnimator(alphaOutAnimation)
                }
            } else if (mImageAnimators[index] != null && mImageAnimators[index] is AlphaOutAnimator) {
                mImageAnimators[index]?.getAnimator()?.cancel()
                val alphaInAnimation = ObjectAnimator.ofFloat(imageView, "alpha", imageView.alpha, 1f)
                alphaInAnimation.duration = mAlphaDuration
                alphaInAnimation.interpolator = DecelerateInterpolator()
                alphaInAnimation.addOnFinishListener {
                    mImageAnimators[index] = null
                }
                alphaInAnimation.start()
                mImageAnimators[index] =
                    AlphaInAnimator(alphaInAnimation)
            }
        }
    }

    //Convert the given int DP amount to its equivalent pixels
    private fun Int.toPx(): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), resources.displayMetrics).toInt()
    }

    //Class that implements the "background" of the navbar. Having this as a separate view was the easiest way to have the selected item image view
    //drawn behind the rest of the navbar
    private inner class NavBarBackground @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr) {

        private var mFirstCurveStartPoint = FloatPoint(0f, 0f)
        private var mFirstCurveEndPoint = FloatPoint(0f, 0f)
        private var mSecondCurveStartPoint =
            FloatPoint(0f, 0f)
        private var mSecondCurveEndPoint = FloatPoint(0f, 0f)

        private var mFirstCurveControlPoint1 =
            FloatPoint(0f, 0f)
        private var mFirstCurveControlPoint2 =
            FloatPoint(0f, 0f)
        private var mSecondCurveControlPoint1 =
            FloatPoint(0f, 0f)
        private var mSecondCurveControlPoint2 =
            FloatPoint(0f, 0f)

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

    private class AlphaInAnimator(val objectAnimator: ValueAnimator) :
        AnimatorContainer {
        override fun getAnimator() = objectAnimator
    }

    private class AlphaOutAnimator(val objectAnimator: ValueAnimator) :
        AnimatorContainer {
        override fun getAnimator() = objectAnimator
    }

    private class CircleOutAnimator(val objectAnimator: ValueAnimator) :
        AnimatorContainer {
        override fun getAnimator(): ValueAnimator {
            return objectAnimator
        }
    }

    private class CircleInAnimator(val objectAnimator: ValueAnimator) :
        AnimatorContainer {
        override fun getAnimator(): ValueAnimator {
            return objectAnimator
        }
    }

    private class FloatPoint(var x: Float, var y: Float) {
        fun set(x: Float, y: Float) {
            this.x = x
            this.y = y
        }
    }
}