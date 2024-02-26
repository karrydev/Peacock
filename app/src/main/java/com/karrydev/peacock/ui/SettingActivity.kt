package com.karrydev.peacock.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.karrydev.peacock.R
import com.karrydev.peacock.base.BaseActivity
import com.karrydev.peacock.databinding.ActivitySettingBinding
import com.karrydev.peacock.model.Setting
import com.karrydev.peacock.service.ForegroundService
import com.karrydev.peacock.vm.SettingViewModel

class SettingActivity : BaseActivity<SettingViewModel, ActivitySettingBinding>() {

    override val layoutId = R.layout.activity_setting
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private var foregroundServiceIntent: Intent? = null

    companion object {
        const val TAG = "SettingActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 这里需要在onCreate中注册
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            globalViewModel.activityResultCallback?.invoke()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!viewModel.checkNotificationPermission()) {
            // 若通知权限未获取，关闭与通知相关的设置
            missNotificationPermission()
        }
    }

    override fun initData() {}

    override fun initView() {
        binding.sbDuration.apply {
            // min是在SDK26以上才有的API，这里直接把max设为9，min默认是0，所以获取的时候要+1
            max = 9
            keyProgressIncrement = 1
            progress = 3 // 初始化为3（实际是4）
        }
    }

    override fun initListener() {
        // 跳过广告时显示提示
        binding.cbToast.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!viewModel.checkNotificationPermission()) {
                    // TODO 展示通知权限获取弹窗，再执行以下逻辑
                    // 打开通知设置界面
                    globalViewModel.activityResultCallback = {
                        if (viewModel.checkNotificationPermission()) {
                            // 开启跳过提示
                            Setting.showSkipAdToastFlag = true
                        } else {
                            // TODO 弹窗提示：提示需要通知权限
                            missNotificationPermission()
                        }
                    }
                    val it = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    activityResultLauncher.launch(it)
                } else {
                    Setting.showSkipAdToastFlag = true
                }
            } else {
                Setting.showSkipAdToastFlag = false
            }
        }

        // 展示前台通知
        binding.cbNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!viewModel.checkNotificationPermission()) {
                    // TODO 展示通知权限获取弹窗，再执行以下逻辑
                    // 打开通知设置界面
                    globalViewModel.activityResultCallback = {
                        if (viewModel.checkNotificationPermission()) {
                            // 打开前台通知
                            Setting.showNotification = true
                            startNotification()
                        } else {
                            // TODO 弹窗提示：提示需要通知权限
                            missNotificationPermission()
                        }
                    }
                    val it = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    activityResultLauncher.launch(it)
                } else {
                    Setting.showNotification = true
                    startNotification()
                }
            } else {
                Setting.showNotification = false
                stopNotification()
            }
        }

        // 检测时长
        binding.sbDuration.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvDuration.text = (progress + 1).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Setting.skipAdDuration = binding.tvDuration.text.toString().toInt()
            }
        })

        // 电池优化设置
        binding.tvPower.setOnClickListener {
            //  打开电池优化的界面，让用户设置
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        }
    }

    private fun missNotificationPermission() {
        binding.cbToast.isChecked = false
        binding.cbNotification.isChecked = false
        Setting.showSkipAdToastFlag = false
        Setting.showNotification = false
        stopNotification()
    }

    private fun startNotification() {
        // 开启前台通知
        if (foregroundServiceIntent == null) {
            foregroundServiceIntent = Intent(this, ForegroundService::class.java)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(foregroundServiceIntent)
        } else {
            startService(foregroundServiceIntent)
        }
    }

    private fun stopNotification() {
        // 关闭前台通知
        if (foregroundServiceIntent != null) {
            stopService(foregroundServiceIntent)
        }
    }

    override fun getViewModelClass() = SettingViewModel::class.java
}