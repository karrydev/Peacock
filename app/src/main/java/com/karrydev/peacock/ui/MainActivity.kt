package com.karrydev.peacock.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.karrydev.peacock.R
import com.karrydev.peacock.base.BaseMviActivity
import com.karrydev.peacock.databinding.MainActivityBinding
import com.karrydev.peacock.ui.fragment.PermissionFragment
import com.karrydev.peacock.ui.fragment.SettingsFragment
import com.karrydev.peacock.util.showToast
import com.karrydev.peacock.vm.MainUiIntent
import com.karrydev.peacock.vm.MainUiState
import com.karrydev.peacock.vm.MainViewModel

class MainActivity : BaseMviActivity<MainViewModel, MainActivityBinding>() {

    override val viewModel: MainViewModel by viewModels()
    private var initFragmentFlag = false

    companion object {
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        initObserver()
        initBiz()
    }

    private fun initObserver() {
        stateFlowHandle(viewModel.uiStateFlow) {
            when (it) {
                is MainUiState.CheckPermissionState -> {
                    if (!initFragmentFlag) {
                        initFragment(it)
                    }
                }
                is MainUiState.ToSettingsFragmentState -> toSettingsFragment(it)
                is MainUiState.ToPermissionFragmentState -> toPermissionFragment()
            }
        }
    }

    private fun initBiz() {
        viewModel.dispatchIntent(MainUiIntent.CheckPermissionIntent())
    }

    /**
     * 监听权限获取情况
     */
    private fun initFragment(state: MainUiState.CheckPermissionState) {
        if (state.accessibility && state.powerIgnored) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment, SettingsFragment())
                .commit()
        } else {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment, PermissionFragment())
                .commit()
        }
        initFragmentFlag = true
    }

    /**
     * 判断是否完成权限获取
     * 是，切换SettingFragment
     * 否，Toast提示
     */
    private fun toSettingsFragment(state: MainUiState.ToSettingsFragmentState) {
        if (state.permissionDone) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment, SettingsFragment())
                .commit()
        } else {
            showToast(getString(R.string.toast_no_access_permission))
        }
    }

    private fun toPermissionFragment() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment, PermissionFragment())
            .commit()
    }

    override fun initBinding(): MainActivityBinding =
        DataBindingUtil.setContentView(this, R.layout.main_activity)
}