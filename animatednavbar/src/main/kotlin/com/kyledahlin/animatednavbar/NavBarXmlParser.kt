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

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import org.xmlpull.v1.XmlPullParser

private const val NAV_BAR_ITEM = "navbaritem"
private const val ICON = "icon"
private const val ANIMATED_ICON = "animated_icon"
private const val ID = "id"

internal fun loadNavBarItems(context: Context, xmlFileResId: Int): List<NavBarItem> {
    val parser = context.resources.getXml(xmlFileResId)
    val items = mutableListOf<NavBarItem>()

    var token: Int = parser.next()
    while (token != XmlPullParser.END_DOCUMENT) {
        if (token == XmlPullParser.START_TAG) {
            if (parser.name == NAV_BAR_ITEM) {
                items.add(NavBarItem.fromAttributeSet(parser))
            }
        }
        token = parser.next()
    }

    return items
}

internal class NavBarItem(
    val androidId: Int,
    val unselectedDrawableId: Int,
    val selectedDrawableId: Int
) {
    companion object {

        fun fromAttributeSet(attrs: AttributeSet): NavBarItem {
            var id = -1
            var icon = -1
            var animatedIcon = -1

            for (index in 0 until attrs.attributeCount) {
                val attributeName = attrs.getAttributeName(index)
                when (attributeName) {
                    ICON -> {
                        icon = attrs.getAttributeResourceValue(index, -1)
                    }
                    ANIMATED_ICON -> {
                        animatedIcon = attrs.getAttributeResourceValue(index, -1)
                    }
                    ID -> {
                        id = attrs.getAttributeResourceValue(index, -1)
                    }
                    else -> {
                        Log.d("AnimatedNavBar", "invalid attribute name, $attributeName")
                    }
                }
            }
            if (id == -1) {
                throw InvalidXmlException("missing id for nav bar item")
            }
            if (icon == -1) {
                throw InvalidXmlException("missing icon for nav bar item")
            }
            if (animatedIcon == -1) {
                throw InvalidXmlException("missing animatedIcon for nav bar item")
            }
            return NavBarItem(id, icon, animatedIcon)
        }
    }
}

internal class InvalidXmlException(message: String) : Exception(message)