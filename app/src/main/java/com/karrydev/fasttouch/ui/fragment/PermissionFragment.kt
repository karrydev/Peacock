package com.karrydev.fasttouch.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.karrydev.fasttouch.R
import com.karrydev.fasttouch.base.BaseMviFragment
import com.karrydev.fasttouch.databinding.FragmentPermissionBinding
import com.karrydev.fasttouch.vm.MainUiIntent
import com.karrydev.fasttouch.vm.MainUiState
import com.karrydev.fasttouch.vm.MainViewModel

class PermissionFragment : BaseMviFragment<MainViewModel, FragmentPermissionBinding>() {

    override val viewModel: MainViewModel by activityViewModels()

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
        binding.buttonAccessibilityPermission.setOnClickListener {
            // 打开无障碍设置界面
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        binding.buttonPowerPermission.setOnClickListener {
            //  打开电池优化的界面，让用户设置
            val intent = Intent()
            intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
//            val packageName = activity?.packageName
//            val pm = activity?.getSystemService(Context.POWER_SERVICE) as PowerManager
//            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent)
        }

        binding.btnDone.setOnClickListener {
            viewModel.dispatchIntent(MainUiIntent.ToSettingsFragmentIntent())
        }
    }

    override fun initBinding(view: View): FragmentPermissionBinding =
        FragmentPermissionBinding.bind(view)
}