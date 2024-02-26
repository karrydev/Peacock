package com.karrydev.peacock.vm

import com.karrydev.peacock.base.BaseViewModel
import com.karrydev.peacock.service.PeacockService

class GlobalViewModel : BaseViewModel() {

    var activityResultCallback: (() -> Unit)? = null

    fun dispatchServiceAction(action: Int) = PeacockService.dispatchReceiverAction(action)
}