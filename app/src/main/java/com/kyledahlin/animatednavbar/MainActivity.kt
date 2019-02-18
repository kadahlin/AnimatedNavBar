package com.kyledahlin.animatednavbar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        index_text.text = "Current Item is 0"

        animated_nav_bar.setIndexSelectedListener { index ->
            index_text.text = "Current Item is $index"
        }
    }
}
