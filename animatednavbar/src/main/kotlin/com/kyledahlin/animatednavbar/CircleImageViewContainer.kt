package com.kyledahlin.animatednavbar

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.widget.ImageView

internal class CircleImageViewContainer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    constructor(
        colorInt: Int,
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : this(
        context,
        attrs,
        defStyleAttr
    ) {
        background = resources.getDrawable(R.drawable.nav_bar_circle_background)
        val gradientDrawable = background.mutate() as GradientDrawable
        gradientDrawable.setColor(colorInt)
    }
}