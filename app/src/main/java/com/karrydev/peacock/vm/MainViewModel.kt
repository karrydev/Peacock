package com.karrydev.peacock.vm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.karrydev.peacock.service.PeacockService
import com.karrydev.peacock.base.BaseMviViewModel
import com.karrydev.peacock.base.IUiIntent
import com.karrydev.peacock.base.IUiState
import com.karrydev.peacock.util.appContext

class MainViewModel : BaseMviViewModel() {

    private var checkPermission = false

    override fun handleUserIntent(intent: IUiIntent) {
        when (intent) {
            is MainUiIntent.CheckPermissionIntent -> checkPermission()
            is MainUiIntent.ToSettingsFragmentIntent -> toSettingsFragment()
            is MainUiIntent.ToPermissionIntent -> toPermissionFragment()
        }
    }

    /**
     * 检查权限（无障碍、电池优化）
     */
    private fun checkPermission() {
        // 检查通知权限是否开启
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        // 检查是否开启无障碍服务
        val accessibility = PeacockService.isServiceRunning()

        // 检查电池优化权限是否开启
        val pm = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val hasIgnored = pm.isIgnoringBatteryOptimizations(appContext.packageName)

        // 更新state
        sendUiState { MainUiState.CheckPermissionState(notification, accessibility, hasIgnored) }
        checkPermission = notification && accessibility && hasIgnored
    }

    /**
     * 检查是否完成权限获取，并通知Activity切换Fragment
     */
    private fun toSettingsFragment() {
        sendUiState { MainUiState.ToSettingsFragmentState(checkPermission) }
    }

    private fun toPermissionFragment() {
        sendUiState { MainUiState.ToPermissionFragmentState() }
    }
}

sealed class MainUiState {

    class CheckPermissionState(val notification: Boolean, val accessibility: Boolean, val powerIgnored: Boolean) : IUiState // 检查结果（无障碍、电池优化）

    class ToSettingsFragmentState(val permissionDone: Boolean) : IUiState // 完成权限获取，切换SettingsFragment

    class ToPermissionFragmentState : IUiState // 权限未获取，切换PermissionFragment
}

sealed class MainUiIntent {

    class CheckPermissionIntent : IUiIntent // 检查权限

    class ToSettingsFragmentIntent : IUiIntent // 完成权限获取，切换SettingsFragment

    class ToPermissionIntent : IUiIntent  // 权限未获取，切换PermissionFragment
}