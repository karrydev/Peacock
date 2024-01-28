package com.karrydev.fasttouch.base

import com.karrydev.fasttouch.BuildConfig
import com.karrydev.fasttouch.FastTouchApp

/**
 * 顶层对象和函数集合
 */

/**
 * appContext
 */
val appContext = FastTouchApp.context

/**
 * debug环境执行方法
 */
fun isDebug(block: () -> Unit) {
    if (BuildConfig.DEBUG) {
        block()
    }
}