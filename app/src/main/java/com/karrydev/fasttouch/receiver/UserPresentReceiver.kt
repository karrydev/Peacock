package com.karrydev.fasttouch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.karrydev.fasttouch.service.FastTouchService

class UserPresentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (action != null) {
            if (action == Intent.ACTION_USER_PRESENT) {
                // 接收【设备唤醒】广播，通知 Service 启动 skip-ad 任务
                FastTouchService.dispatchReceiverAction(FastTouchService.ACTION_START_TASK)
            }
        }
    }
}