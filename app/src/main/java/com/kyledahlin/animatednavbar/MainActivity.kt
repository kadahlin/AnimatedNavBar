package com.kyledahlin.animatednavbar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        index_text.text = "Current Item is one"

        animated_nav_bar.setIndexSelectedListener { id ->
            val logMessage = when(id) {
                R.id.nav_bar_one -> "one"
                R.id.nav_bar_two -> "two"
                R.id.nav_bar_three -> "three"
                R.id.nav_bar_four -> "four"
                R.id.nav_bar_five -> "five"
                else -> "unknown"
            }
            index_text.text = "Current Item is $logMessage"
        }
    }
}
