package com.karrydev.peacock.ui

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.karrydev.peacock.R
import com.karrydev.peacock.base.BaseActivity
import com.karrydev.peacock.base.BaseViewModel
import com.karrydev.peacock.databinding.ActivityNewMainBinding
import com.karrydev.peacock.service.PeacockService
import com.karrydev.peacock.vm.NewMainViewModel

class NewMainActivity : BaseActivity<NewMainViewModel, ActivityNewMainBinding>() {

    override val layoutId = R.layout.activity_new_main

    companion object {
        const val TAG = "NewMainActivity"
    }

    override fun initData() {}

    override fun initView() {}

    override fun initListener() {
        binding.btnOnOff.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 检查是否获得无障碍权限
                if (viewModel.checkAccessibilityPermission()) {
                    globalViewModel.dispatchServiceAction(PeacockService.ACTION_START_SERVICE)
                } else {
                    // TODO 展示无障碍权限获取弹窗，再执行以下逻辑
                    val it = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(it)
                    binding.btnOnOff.isChecked = false
                }
            } else {
                globalViewModel.dispatchServiceAction(PeacockService.ACTION_STOP_SERVICE)
            }
        }
        binding.cardFloatingWindow.setOnClickListener {

        }
        binding.cardSetting.setOnClickListener {
            toSettingActivity()
        }
    }

    private fun toSettingActivity() {
        val it = Intent(this, SettingActivity::class.java)
        startActivity(it)
    }

    override fun getViewModelClass() = NewMainViewModel::class.java
}