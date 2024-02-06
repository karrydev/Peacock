package com.karrydev.peacock.vm

import com.karrydev.peacock.service.PeacockService
import com.karrydev.peacock.base.BaseMviViewModel
import com.karrydev.peacock.base.IUiIntent
import com.karrydev.peacock.base.IUiState
import com.karrydev.peacock.model.Settings

class SettingsViewModel : BaseMviViewModel() {

    override fun handleUserIntent(intent: IUiIntent) {
        when (intent) {
            is SettingsIntent.OnKeywordsChangeIntent -> onKeywordsChange(intent.newValue)
            is SettingsIntent.ShowCustomizationDialog -> showCustomizationDialog()
        }
    }

    private fun onKeywordsChange(newValue: String) {
        Settings.keywordList = newValue.split(" ").toMutableList()

        PeacockService.dispatchReceiverAction(PeacockService.ACTION_REFRESH_KEYWORDS)

        sendUiState { SettingsState.UpdateKeywordsState() }
    }

    private fun showCustomizationDialog() {
        val success = PeacockService.dispatchReceiverAction(PeacockService.ACTION_SHOW_CUSTOMIZATION_DIALOG)
        sendUiState { SettingsState.ShowCustomizationDialogState(success) }
    }

    fun dispatchServiceAction(action: Int) = PeacockService.dispatchReceiverAction(action)
}

sealed class SettingsIntent {
    class OnKeywordsChangeIntent(val newValue: String) : IUiIntent // 关键词发生了改变

    class ShowCustomizationDialog : IUiIntent // 打开添加控件坐标弹窗
}

sealed class SettingsState {
    class UpdateKeywordsState : IUiState // 更新关键词列表

    class ShowCustomizationDialogState(success : Boolean) : IUiState // 是否成功打开弹窗
}