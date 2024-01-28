package com.karrydev.fasttouch

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

class FastTouchService : AccessibilityService() {

    companion object {

        const val ACTION_REFRESH_KEYWORDS = 1
        const val ACTION_REFRESH_PACKAGE = 2
        const val ACTION_REFRESH_CUSTOMIZED_WIDGETS_POSITIONS = 3
        const val ACTION_SHOW_CUSTOMIZATION_DIALOG = 4
        const val ACTION_STOP_SERVICE = 5
        const val ACTION_START_TASK = 6
        const val ACTION_STOP_TASK = 7

        private var serviceInstance: WeakReference<FastTouchService>? = null

        fun isServiceRunning(): Boolean {
            return serviceInstance?.get() != null && serviceInstance?.get()!!.serviceImpl != null
        }

        /**
         * 提供给 BroadcastReceiver 调用的事件分发方法
         */
        fun dispatchReceiverAction(action: Int): Boolean {
            val service = serviceInstance?.get()
            if (service?.serviceImpl == null) {
                return false
            }
            service.serviceImpl!!.mainHandler.sendEmptyMessage(action)
            return true
        }
    }

    private var serviceImpl: FastTouchServiceImpl? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInstance = WeakReference(this)
        if (serviceImpl == null) {
            serviceImpl = FastTouchServiceImpl(this)
        }
        serviceImpl!!.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        serviceImpl?.onAccessibilityEvent(event)
    }

    override fun onInterrupt() {
        serviceImpl?.onInterrupt()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        serviceImpl?.onUnbind(intent)
        serviceImpl = null
        serviceInstance = null
        return super.onUnbind(intent)
    }

}