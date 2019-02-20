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

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        index_text.text = "Current Item is one"

        animated_nav_bar.setIdSelectedListener { id ->
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
