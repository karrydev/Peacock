package com.karrydev.fasttouch.base

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class Utils {
    companion object {
        fun showToast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
            Toast.makeText(appContext, msg, duration).show()
        }

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
    }
}