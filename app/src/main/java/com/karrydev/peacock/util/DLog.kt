package com.karrydev.peacock.util

import android.util.Log

object DLog {
    fun i(tag: String, msg: String) {
        isDebug {
            Log.i(tag, msg)
        }
    }

    fun v(tag: String, msg: String) {
        isDebug {
            Log.v(tag, msg)
        }
    }

    fun d(tag: String, msg: String) {
        isDebug {
            Log.d(tag, msg)
        }
    }

    fun w(tag: String, msg: String) {
        isDebug {
            Log.w(tag, msg)
        }
    }

    fun e(tag: String, msg: String) {
        isDebug {
            Log.e(tag, msg)
        }
    }
}