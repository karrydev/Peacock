package com.karrydev.fasttouch.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.karrydev.fasttouch.FastTouchService

class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (action != null) {
            if (action == Intent.ACTION_PACKAGE_ADDED || action == Intent.ACTION_PACKAGE_REMOVED) {
                // 接收【新应用安装】和【应用卸载】的系统广播，并通知 Service 更新 package 列表
                FastTouchService.dispatchReceiverAction(FastTouchService.ACTION_REFRESH_PACKAGE)
            }
        }
    }
}