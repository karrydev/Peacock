package com.karrydev.peacock.ui.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ListView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import com.karrydev.peacock.service.PeacockService
import com.karrydev.peacock.R
import com.karrydev.peacock.adapter.PackageListAdapter
import com.karrydev.peacock.base.BaseMviPreferenceFragment
import com.karrydev.peacock.model.AppInformation
import com.karrydev.peacock.model.PackagePositionDescription
import com.karrydev.peacock.model.PackageWidgetDescription
import com.karrydev.peacock.model.Settings
import com.karrydev.peacock.util.DLog
import com.karrydev.peacock.util.showToast
import com.karrydev.peacock.vm.*
import java.util.TreeMap

class SettingsFragment : BaseMviPreferenceFragment<SettingsViewModel>() {

    override val viewModel: SettingsViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    private val packageManager = activity?.packageManager
    private val inflater by lazy { activity?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater }
    private var widgetsPreference: MultiSelectListPreference? = null
    private var pkgWidgetMap = TreeMap<String, Set<PackageWidgetDescription>>()
    private var positionsPreference: MultiSelectListPreference? = null
    private var pkgPosMap = TreeMap<String, PackagePositionDescription>()
    private var pkgListDialog: AlertDialog? = null

    companion object {
        const val TAG = "SettingsFragment"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        initObserver()

        initPreferences()
    }

    override fun onResume() {
        super.onResume()
        // 检查权限是否满足
        mainViewModel.dispatchIntent(MainUiIntent.CheckPermissionIntent())
    }

    private fun initObserver() {
        stateFlowHandle(mainViewModel.uiStateFlow) {
            when (it) {
                is MainUiState.CheckPermissionState -> checkPermission(it)
            }
        }
        stateFlowHandle(viewModel.uiStateFlow) {
            when (it) {
                is SettingsState.UpdateKeywordsState -> {
                    findPreference<EditTextPreference>("setting_keywords")?.text = Settings.keywordList.joinToString(" ")
                }
            }
        }
    }

    private fun initPreferences() {
        // 跳过广告时显示通知
        val showSkipAdToast = findPreference<CheckBoxPreference>("show_skip_ad_toast")
        showSkipAdToast?.apply {
            isChecked = Settings.showSkipAdToastFlag
            setOnPreferenceChangeListener { _, newValue ->
                Settings.showSkipAdToastFlag = newValue as Boolean
                true
            }
        }

        // 检测时长
        val duration = findPreference<SeekBarPreference>("skip_ad_duration")
        duration?.apply {
            max = 10
            min = 1
            updatesContinuously = true
            value = Settings.skipAdDuration

            setOnPreferenceChangeListener { _, newValue ->
                Settings.skipAdDuration = newValue as Int
                true
            }
        }

        // 跳过广告按钮的关键字
        val keywords = findPreference<EditTextPreference>("setting_keywords")
        keywords?.apply {
            text = Settings.keywordList.joinToString(" ")

            setOnPreferenceChangeListener { _, newValue ->
                val value = (newValue as String).split(" ").filter { it.isNotEmpty() }.joinToString(" ")
                viewModel.dispatchIntent(SettingsIntent.OnKeywordsChangeIntent(value))
                true
            }
        }

        // 应用程序白名单
        val whiteList = findPreference<Preference>("setting_white_list")
        whiteList?.apply {
            setOnPreferenceClickListener {
                // 打开应用列表弹窗
                pkgListDialog?.show()
                true
            }
        }

        // 添加程序的跳过方法
        val customization = findPreference<Preference>("setting_show_customization_dialog")
        customization?.apply {
            setOnPreferenceClickListener {
                if (!viewModel.dispatchServiceAction(PeacockService.ACTION_SHOW_CUSTOMIZATION_DIALOG)) {
                    showToast("Peacock服务未运行，请先打开无障碍服务!")
                    mainViewModel.dispatchIntent(MainUiIntent.CheckPermissionIntent())
                }

                true
            }
        }

        // 导入/导出程序的按钮跳过规则
        val advance = findPreference<Preference>("setting_activity_widgets_advanced")
        advance?.apply {
            setOnPreferenceClickListener {
                val fragmentManager = activity?.supportFragmentManager!!
                val fragmentDialog = ManagePackageWidgetsDialogFragment()
                fragmentDialog.show(fragmentManager, "dialog")

                true
            }
        }

        // 管理已添加按钮的程序
        widgetsPreference = findPreference("setting_activity_widgets")
        pkgWidgetMap = Settings.pkgWidgetMap
        updateSelectListEntries(widgetsPreference, pkgWidgetMap.keys)
        widgetsPreference?.setOnPreferenceChangeListener { _, newValue ->
            // 将 mapPackageWidgets 和 newValue 做对比，删除掉 mapPackageWidgets 内多余的元素
            // 因为添加入口不在这里，所以只需要处理 删除 这一种情况
            val keys = HashSet(pkgWidgetMap.keys)
            keys.forEach { key ->
                if (!(newValue as HashSet<*>).contains(key)) {
                    pkgWidgetMap.remove(key)
                }
            }
            Settings.pkgWidgetMap = pkgWidgetMap

            // 更新 Preference 的列表数据
            updateSelectListEntries(widgetsPreference, pkgWidgetMap.keys)

            // 发送消息通知 Service 更新 Widget 数据
            viewModel.dispatchServiceAction(PeacockService.ACTION_REFRESH_CUSTOMIZED_WIDGETS_POSITION)

            true
        }

        // 管理已添加坐标的程序
        positionsPreference = findPreference("setting_activity_positions")
        pkgPosMap = Settings.pkgPosMap
        updateSelectListEntries(positionsPreference, pkgPosMap.keys)
        positionsPreference?.setOnPreferenceChangeListener { _, newValue ->
            // 将 mapPackagePositions 和 newValue 做对比，删除掉 mapPackageWidgets 内多余的元素
            // 因为添加入口不在这里，所以只需要处理 删除 这一种情况，处理方法和【管理已添加按钮的程序】一样
            val keys = HashSet(pkgPosMap.keys)
            keys.forEach { key ->
                if (!(newValue as HashSet<*>).contains(key)) {
                    pkgPosMap.remove(key)
                }
            }
            Settings.pkgPosMap = pkgPosMap

            DLog.d(TAG, "size:${Settings.pkgPosMap.size}==s:${pkgPosMap.size}")
            // 更新 Preference 的列表数据
            updateSelectListEntries(positionsPreference, pkgPosMap.keys)

            // 发送消息通知 Service 更新 Widget 数据
            viewModel.dispatchServiceAction(PeacockService.ACTION_REFRESH_CUSTOMIZED_WIDGETS_POSITION)

            true
        }
    }

    /**
     * 初始化 pkgListDialog
     */
    private fun initPkgListDialog() {
        val manager = packageManager ?: return
        // 先找到所有的包
        val pkgNameList = ArrayList<String>()
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolverInfoList = manager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        resolverInfoList.forEach { info ->
            if (info.activityInfo.packageName != "com.karrydev.peacock") { // 排除这个包
                pkgNameList.add(info.activityInfo.packageName)
            }
        }

        // 包装每一个非白名单 pkg，用作列表数据
        val appInfoList = ArrayList<AppInformation>()
        val whiteList = Settings.whiteListSet
        pkgNameList.forEach { pkgName ->
            val info = manager.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
            val appInfo = AppInformation(pkgName, manager.getApplicationLabel(info).toString(), manager.getApplicationIcon(info))
            appInfoList.add(
                appInfo.copy(
                    checkFlag = whiteList.contains(pkgName)
                )
            )
        }
        appInfoList.sort()

        // 创建列表 Adapter
        val adapter = PackageListAdapter(appInfoList, inflater)

        // 创建列表弹窗布局
        val appListDialogLayout = inflater.inflate(R.layout.layout_select_packages_dialog, null)
        val listView = appListDialogLayout.findViewById<ListView>(R.id.listView)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, view, position, _ ->
            val checkBox = (view.tag as PackageListAdapter.ViewHolder).checkBox
            val appInfo = appInfoList[position]
            val info = appInfo.copy(
                checkFlag = !appInfo.checkFlag
            )
            appInfoList[position] = info
            checkBox.isChecked = info.checkFlag
        }

        val pkgListDialog = AlertDialog.Builder(context)
            .setView(appListDialogLayout)
            .create()
        this.pkgListDialog = pkgListDialog

        val btnCancel = appListDialogLayout.findViewById<Button>(R.id.button_cancel)
        btnCancel?.setOnClickListener {
            pkgListDialog.dismiss()
        }
        val btnConfirm = appListDialogLayout.findViewById<Button>(R.id.button_confirm)
        btnConfirm?.setOnClickListener {
            // 更新白名单
            val list = HashSet<String>()
            appInfoList.forEach { info ->
                if (info.checkFlag) {
                    list.add(info.packageName)
                }
            }
            Settings.whiteListSet = list

            // 通知 Service 更新白名单
            viewModel.dispatchServiceAction(PeacockService.ACTION_REFRESH_PACKAGE)

            pkgListDialog.dismiss()
        }
    }

    /**
     * 更新 已添加按钮 和 已添加坐标 列表的数据
     */
    private fun updateSelectListEntries(preference: MultiSelectListPreference?, keys: Set<String>) {
        preference?.apply {
            val keyList = keys.toList()
            val entries = Array<CharSequence>(keys.size) {
                keyList[it]
            }
            setEntries(entries)
            entryValues = entries
            values = keys
        }
    }

    /**
     * 检查权限，onResume 中要执行的初始化任务也放在这里
     */
    private fun checkPermission(state: MainUiState.CheckPermissionState) {
        if (!state.notification || !state.accessibility || !state.powerIgnored) {
            // 缺少相应权限，回到PermissionFragment
            mainViewModel.handleUserIntent(MainUiIntent.ToPermissionIntent())
        } else {
            // 进行 onResume 操作
            pkgWidgetMap = Settings.pkgWidgetMap
            updateSelectListEntries(widgetsPreference, pkgWidgetMap.keys)

            pkgPosMap = Settings.pkgPosMap
            updateSelectListEntries(positionsPreference, pkgPosMap.keys)

            initPkgListDialog()
        }
    }
}