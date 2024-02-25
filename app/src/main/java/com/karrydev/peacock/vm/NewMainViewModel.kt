package com.karrydev.peacock.vm

import com.karrydev.peacock.base.BaseViewModel
import com.karrydev.peacock.service.PeacockService

class NewMainViewModel : BaseViewModel() {

    companion object {
        const val TAG = "NewMainViewModel"
    }

    /**
     * 检查是否开启无障碍权限
     */
    fun checkAccessibilityPermission() = PeacockService.isServiceRunning()
}