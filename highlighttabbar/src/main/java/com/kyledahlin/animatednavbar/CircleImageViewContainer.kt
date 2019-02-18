package com.kyledahlin.animatednavbar

import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.widget.ImageView

internal class CircleImageViewContainer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    constructor(
        colorRes: Int,
        radius: Int,
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : this(
        context,
        attrs,
        defStyleAttr
    ) {
        mPaint.color = 0xffffff
        mRadius = radius
    }

    private val mPaint: Paint = Paint()
    private var mRadius = 1
    private var mRect = RectF(0f, 0f, 0f, 0f)

    init {
        mPaint.style = Paint.Style.FILL_AND_STROKE
        background = resources.getDrawable(R.drawable.nav_bar_circle_background)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            elevation = 6f
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
    }
}