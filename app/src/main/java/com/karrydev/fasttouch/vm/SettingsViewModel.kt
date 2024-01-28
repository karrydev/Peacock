package com.karrydev.fasttouch.vm

import com.karrydev.fasttouch.FastTouchService
import com.karrydev.fasttouch.base.BaseMviViewModel
import com.karrydev.fasttouch.base.IUiIntent
import com.karrydev.fasttouch.base.IUiState
import com.karrydev.fasttouch.model.Settings

class SettingsViewModel : BaseMviViewModel() {

    val settings by lazy { Settings }

    override fun handleUserIntent(intent: IUiIntent) {
        when (intent) {
            is SettingsIntent.OnKeywordsChangeIntent -> onKeywordsChange(intent.newValue)
            is SettingsIntent.ShowCustomizationDialog -> showCustomizationDialog()
        }
    }

    private fun onKeywordsChange(newValue: String) {
        settings.keywordList = newValue.split(" ").toList() as ArrayList<String>

        FastTouchService.dispatchReceiverAction(FastTouchService.ACTION_REFRESH_KEYWORDS)

        sendUiState { SettingsState.UpdateKeywordsState() }
    }

    private fun showCustomizationDialog() {
        val success = FastTouchService.dispatchReceiverAction(FastTouchService.ACTION_SHOW_CUSTOMIZATION_DIALOG)
        sendUiState { SettingsState.ShowCustomizationDialogState(success) }
    }

    fun dispatchServiceAction(action: Int) = FastTouchService.dispatchReceiverAction(action)
}

sealed class SettingsIntent {
    class OnKeywordsChangeIntent(val newValue: String) : IUiIntent // 关键词发生了改变

    class ShowCustomizationDialog : IUiIntent // 打开添加控件坐标弹窗
}

sealed class SettingsState {
    class UpdateKeywordsState : IUiState // 更新关键词列表

    class ShowCustomizationDialogState(success : Boolean) : IUiState // 是否成功打开弹窗
}