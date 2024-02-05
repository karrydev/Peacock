package com.karrydev.peacock.ui.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.karrydev.peacock.R
import com.karrydev.peacock.base.BaseMviFragment
import com.karrydev.peacock.databinding.FragmentPermissionBinding
import com.karrydev.peacock.util.DLog
import com.karrydev.peacock.util.showToast
import com.karrydev.peacock.vm.MainUiIntent
import com.karrydev.peacock.vm.MainUiState
import com.karrydev.peacock.vm.MainViewModel

class PermissionFragment : BaseMviFragment<MainViewModel, FragmentPermissionBinding>() {

    override val viewModel: MainViewModel by activityViewModels()

    companion object {
        const val TAG = "PermissionFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        initObserver()
        return inflater.inflate(R.layout.fragment_permission, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListener()

        // Android 13及以上系统需动态获取通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkNotificationPermission()
        } else {
            binding.cardNotification.visibility = View.GONE
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkNotificationPermission() {
        val activity = activity ?: return
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                DLog.d(TAG, "get permission res=$isGranted")
                if (!isGranted) {
                    showToast("需要您提供通知权限才可使用完整功能")
                }
            }.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        // 检查权限是否满足
        viewModel.dispatchIntent(MainUiIntent.CheckPermissionIntent())
    }

    private fun initObserver() {
        stateFlowHandle(viewModel.uiStateFlow) {
            when (it) {
                is MainUiState.CheckPermissionState -> checkPermissionState(it)
            }
        }
    }

    private fun checkPermissionState(state: MainUiState.CheckPermissionState) {
        if (state.notification) {
            binding.llNotificationPermission.setBackgroundColor(
                resources.getColor(
                    R.color.access_permission,
                    resources.newTheme()
                )
            )
        } else {
            binding.llNotificationPermission.setBackgroundColor(
                resources.getColor(
                    R.color.no_access_permission,
                    resources.newTheme()
                )
            )
        }
        if (state.accessibility) {
            binding.llAccesscibilityPermission.setBackgroundColor(
                resources.getColor(
                    R.color.access_permission,
                    resources.newTheme()
                )
            )
        } else {
            binding.llAccesscibilityPermission.setBackgroundColor(
                resources.getColor(
                    R.color.no_access_permission,
                    resources.newTheme()
                )
            )
        }
        if (state.powerIgnored) {
            binding.llPowerPermission.setBackgroundColor(
                resources.getColor(
                    R.color.access_permission,
                    resources.newTheme()
                )
            )
        } else {
            binding.llPowerPermission.setBackgroundColor(
                resources.getColor(
                    R.color.no_access_permission,
                    resources.newTheme()
                )
            )
        }
    }

    private fun initListener() {
        binding.buttonNotificationPermission.setOnClickListener {
            // 打开通知设置界面
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, activity?.packageName)
                startActivity(intent)
            }
        }

        binding.buttonAccessibilityPermission.setOnClickListener {
            // 打开无障碍设置界面
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        binding.buttonPowerPermission.setOnClickListener {
            //  打开电池优化的界面，让用户设置
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        }

        binding.btnDone.setOnClickListener {
            viewModel.dispatchIntent(MainUiIntent.ToSettingsFragmentIntent())
        }
    }

    override fun initBinding(view: View): FragmentPermissionBinding =
        FragmentPermissionBinding.bind(view)
}