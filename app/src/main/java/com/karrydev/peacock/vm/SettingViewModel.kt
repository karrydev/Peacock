package com.karrydev.peacock.vm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.karrydev.peacock.base.BaseViewModel
import com.karrydev.peacock.service.PeacockService
import com.karrydev.peacock.util.appContext

class SettingViewModel : BaseViewModel() {

    companion object {
        const val TAG = "SettingViewModel"
    }

    /**
     * 检查是否开启通知权限，Android 13及以上系统需动态获取通知权限
     */
    fun checkNotificationPermission() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES .TIRAMISU) {
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}