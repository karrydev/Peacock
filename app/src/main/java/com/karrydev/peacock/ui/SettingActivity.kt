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
import com.karrydev.peacock.vm.SettingViewModel

class SettingActivity : BaseActivity<SettingViewModel, ActivitySettingBinding>() {

    override val layoutId = R.layout.activity_setting
    private lateinit var notificationLauncher: ActivityResultLauncher<Intent>

    companion object {
        const val TAG = "SettingActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 这里需要在onCreate中注册
        // 打开通知设置界面
        // TODO 考虑一下如何传入这个扩展函数
        notificationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (viewModel.checkNotificationPermission()) {
                // 打开前台通知
                Setting.showForegroundNotification = true
            } else {
                // TODO 弹窗提示：提示需要通知权限
                binding.cbNotification.isChecked = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!viewModel.checkNotificationPermission()) {
            // 若通知权限未获取，关闭通知相关设置
            binding.cbNotification.isChecked = false
            binding.cbForeground.isChecked = false
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
        // 跳过广告时显示通知
        binding.cbNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!viewModel.checkNotificationPermission()) {
                    // TODO 展示通知权限获取弹窗，再执行以下逻辑
                    val it = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    notificationLauncher.launch(it)
                } else {
                    Setting.showForegroundNotification = true
                }
            } else {
                Setting.showForegroundNotification = false
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
    }

//    private fun getNotificationPermission(onComplete: (res: Boolean) -> Unit) {
//        // 打开通知设置界面
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            .launch(it)
//        }
//    }

    override fun getViewModelClass() = SettingViewModel::class.java
}