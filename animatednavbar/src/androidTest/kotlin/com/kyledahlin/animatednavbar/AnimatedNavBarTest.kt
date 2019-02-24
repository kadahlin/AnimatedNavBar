package com.kyledahlin.animatednavbar

import android.app.Activity
import android.os.Bundle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.kyledahlin.animatednavbar.test.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnimatedNavBarTest {

    @get:Rule
    var activityRule: ActivityTestRule<MockActivity> = ActivityTestRule(MockActivity::class.java)

    private var mSelectedId = -1

    //not exactly the cleanest test strategy but we assume that the children of the nav bar are
    //1. circle view, 2. background, and 3. the list of children nav bar items
    @Test
    fun test_item_selection() {
        val navBar = activityRule.activity.getNavBar()
        val idList =
            listOf(R.id.nav_bar_one, R.id.nav_bar_two, R.id.nav_bar_three, R.id.nav_bar_four, R.id.nav_bar_five)
        navBar.setIdSelectedListener { id ->
            mSelectedId = id
        }
        for (index in 2 until navBar.childCount) {
            val childImageView = navBar.getChildAt(index)
            onView(withId(childImageView.id)).perform(click())
            assertEquals(mSelectedId, idList[index - 2])
        }
        for (index in navBar.childCount - 1 downTo 2) {
            val childImageView = navBar.getChildAt(index)
            onView(withId(childImageView.id)).perform(click())
            assertEquals(mSelectedId, idList[index - 2])
        }

    }
}

class MockActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test_layout)
    }

    internal fun getNavBar() = findViewById<AnimatedBottomNavigationBar>(R.id.test_nav_bar)
}