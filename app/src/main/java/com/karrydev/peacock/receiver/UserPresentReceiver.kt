package com.karrydev.peacock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.karrydev.peacock.service.PeacockService

class UserPresentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (action != null) {
            if (action == Intent.ACTION_USER_PRESENT) {
                // 接收【设备唤醒】广播，通知 Service 启动 skip-ad 任务
                PeacockService.dispatchReceiverAction(PeacockService.ACTION_START_TASK)
            }
        }
    }
}