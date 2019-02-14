//package com.kyledahlin.highlighttabbar
//
//import android.content.res.XmlResourceParser
//import android.util.AttributeSet
//import org.xmlpull.v1.XmlPullParser
//
//private const val XML_MENU = "menu"
//private const val XML_ITEM = "item"
//
//
//fun getDrawableIdsToAndroidIds(parser: XmlResourceParser, attributeSet: AttributeSet): List<Pair<Int, Int>> {
//    var reachedEndOfMenu = false
//    var eventType = parser.eventType
//    var tagName: String
//    var lookingForEndOfUnknownTag = false
//    var unknownTagName: String? = null
//
//    while (!reachedEndOfMenu) {
//        when (eventType) {
//            XmlPullParser.START_TAG -> {
//                if (lookingForEndOfUnknownTag) {
//                    //break
//                }
//                tagName = parser.name
//                when (tagName) {
//                    XML_ITEM -> menuState.readItem(attrs)
//                    XML_MENU -> {
//                        // A menu start tag denotes a submenu for an item
//                        val subMenu = menuState.addSubMenuItem()
//                        registerMenu(subMenu, attrs)
//                        // Parse the submenu into returned SubMenu
//                        parseMenu(parser, attrs, subMenu)
//                    }
//                    else -> {
//                        lookingForEndOfUnknownTag = true
//                        unknownTagName = tagName
//                    }
//                }
//            }
//            XmlPullParser.END_TAG -> {
//                tagName = parser.name
//                if (lookingForEndOfUnknownTag && tagName == unknownTagName) {
//                    lookingForEndOfUnknownTag = false
//                    unknownTagName = null
//                } else if (tagName == XML_ITEM) {
//                    // Add the item if it hasn't been added (if the item was
//                    // a submenu, it would have been added already)
//                    if (!menuState.hasAddedItem()) {
//                        if (menuState.itemActionProvider != null && menuState.itemActionProvider.hasSubMenu()) {
//                            registerMenu(menuState.addSubMenuItem(), attrs)
//                        } else {
//                            registerMenu(menuState.addItem(), attrs)
//                        }
//                    }
//                } else if (tagName == XML_MENU) {
//                    reachedEndOfMenu = true
//                }
//            }
//            XmlPullParser.END_DOCUMENT -> throw RuntimeException("Unexpected end of document")
//        }
//        eventType = parser.next()
//    }
//}