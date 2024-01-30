package com.karrydev.fasttouch.util

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.karrydev.fasttouch.BuildConfig
import com.karrydev.fasttouch.FastTouchApp

/**
 * 顶层对象和函数集合
 */

/**
 * appContext
 */
val appContext = FastTouchApp.context

fun showToast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(appContext, msg, duration).show()
}

/**
 * debug环境执行方法
 */
fun isDebug(block: () -> Unit) {
    if (BuildConfig.DEBUG) {
        block()
    }
}

/**
 * 解析节点信息
 */
fun describeAccessibilityNode(node: AccessibilityNodeInfo?): String {
    if (node == null) {
        return "null"
    }
    val result = StringBuilder()
    result.apply {
        append(" class =${node.className}")
        val rect = Rect()
        node.getBoundsInScreen(rect)
        append(String.format(
            " Position=[%d, %d, %d, %d]",
            rect.left,
            rect.right,
            rect.top,
            rect.bottom
        ))
        val id: CharSequence? = node.viewIdResourceName
        if (id != null) {
            append(" ResourceId=$id")
        }
        val description = node.contentDescription
        if (description != null) {
            append(" Description=$description")
        }
        val text = node.text
        if (text != null) {
            append(" Text=$text")
        }
    }
    return result.toString()
}