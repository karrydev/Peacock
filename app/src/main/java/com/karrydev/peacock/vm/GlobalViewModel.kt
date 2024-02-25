package com.karrydev.peacock.vm

import com.karrydev.peacock.base.BaseViewModel
import com.karrydev.peacock.service.PeacockService

class GlobalViewModel : BaseViewModel() {

    fun dispatchServiceAction(action: Int) = PeacockService.dispatchReceiverAction(action)
}