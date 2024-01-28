package com.karrydev.fasttouch.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import com.karrydev.fasttouch.FastTouchService
import com.karrydev.fasttouch.R
import com.karrydev.fasttouch.adapter.PackageListAdapter
import com.karrydev.fasttouch.base.BaseMviPreferenceFragment
import com.karrydev.fasttouch.model.AppInformation
import com.karrydev.fasttouch.model.PackagePositionDescription
import com.karrydev.fasttouch.model.PackageWidgetDescription
import com.karrydev.fasttouch.vm.*
import java.util.TreeMap

class SettingsFragment : BaseMviPreferenceFragment<SettingsViewModel>() {

    private val TAG = "SettingsFragment"
    override val viewModel: SettingsViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    private lateinit var windowManager: WindowManager
    private lateinit var packageManager: PackageManager
    private lateinit var inflater: LayoutInflater
    private var widgetsPreference: MultiSelectListPreference? = null
    private var mapPackageWidgets = TreeMap<String, Set<PackageWidgetDescription>>()
    private var positionsPreference: MultiSelectListPreference? = null
    private var mapPackagePositions = TreeMap<String, PackagePositionDescription>()
    private lateinit var pkgListDialog: AlertDialog

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        initObserver()

        initPreferences()

        windowManager = activity?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        packageManager = activity?.packageManager!!
        inflater = activity?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return super.onCreateView(inflater, container, savedInstanceState)
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
                    findPreference<EditTextPreference>("setting_keywords")?.text = viewModel.settings.keywordList.joinToString(" ")
                }
            }
        }
    }

    private fun initPreferences() {
        // 跳过广告时显示通知
        val showSkipAdToast = findPreference<CheckBoxPreference>("show_skip_ad_toast")
        showSkipAdToast?.apply {
            isChecked = viewModel.settings.showSkipAdToastFlag
            setOnPreferenceChangeListener { _, newValue ->
                viewModel.settings.showSkipAdToastFlag = newValue as Boolean
                true
            }
        }

        // 检测时长
        val duration = findPreference<SeekBarPreference>("skip_ad_duration")
        duration?.apply {
            max = 10
            min = 1
            updatesContinuously = true
            value = viewModel.settings.skipAdDuration

            setOnPreferenceChangeListener { _, newValue ->
                viewModel.settings.skipAdDuration = newValue as Int
                true
            }
        }

        // 跳过广告按钮的关键字
        val keywords = findPreference<EditTextPreference>("setting_keywords")
        keywords?.apply {
            text = viewModel.settings.keywordList.joinToString(" ")

            setOnPreferenceChangeListener { _, newValue ->
                viewModel.dispatchIntent(SettingsIntent.OnKeywordsChangeIntent(newValue.toString()))
                true
            }
        }

        // 应用程序白名单
        val whiteList = findPreference<Preference>("setting_white_list")
        whiteList?.apply {
            setOnPreferenceClickListener {
                // 打开应用列表弹窗
                pkgListDialog.show()
                true
            }
        }

        // 添加程序的跳过方法
        val customization = findPreference<Preference>("setting_show_customization_dialog")
        customization?.apply {
            setOnPreferenceClickListener {
                if (!viewModel.dispatchServiceAction(FastTouchService.ACTION_SHOW_CUSTOMIZATION_DIALOG)) {
                    showToast("开屏跳过服务未运行，请先打开无障碍服务!")
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
        mapPackageWidgets = viewModel.settings.mapPackageWidgets
        updateSelectListEntries(widgetsPreference, mapPackageWidgets.keys)
        widgetsPreference?.setOnPreferenceChangeListener { _, newValue ->
            // 将 mapPackageWidgets 和 newValue 做对比，删除掉 mapPackageWidgets 内多余的元素
            // 因为添加入口不在这里，所以只需要处理 删除 这一种情况
            val keys = HashSet(mapPackageWidgets.keys)
            keys.forEach { key ->
                if (!(newValue as HashSet<*>).contains(key)) {
                    mapPackageWidgets.remove(key)
                }
            }
            viewModel.settings.mapPackageWidgets = mapPackageWidgets

            // 更新 Preference 的列表数据
            updateSelectListEntries(widgetsPreference, mapPackageWidgets.keys)

            // 发送消息通知 Service 更新 Widget 数据
            viewModel.dispatchServiceAction(FastTouchService.ACTION_REFRESH_CUSTOMIZED_WIDGETS_POSITIONS)

            true
        }

        // 管理已添加坐标的程序
        positionsPreference = findPreference("setting_activity_positions")
        mapPackagePositions = viewModel.settings.mapPackagePositions
        updateSelectListEntries(positionsPreference, mapPackagePositions.keys)
        positionsPreference?.setOnPreferenceChangeListener { _, newValue ->
            // 将 mapPackagePositions 和 newValue 做对比，删除掉 mapPackageWidgets 内多余的元素
            // 因为添加入口不在这里，所以只需要处理 删除 这一种情况，处理方法和【管理已添加按钮的程序】一样
            val keys = HashSet(mapPackagePositions.keys)
            keys.forEach { key ->
                if (!(newValue as HashSet<*>).contains(key)) {
                    mapPackagePositions.remove(key)
                }
            }
            viewModel.settings.mapPackagePositions = mapPackagePositions

            Log.d(TAG, "size:${viewModel.settings.mapPackagePositions.size}==s:${mapPackagePositions.size}")
            // 更新 Preference 的列表数据
            updateSelectListEntries(positionsPreference, mapPackagePositions.keys)

            // 发送消息通知 Service 更新 Widget 数据
            viewModel.dispatchServiceAction(FastTouchService.ACTION_REFRESH_CUSTOMIZED_WIDGETS_POSITIONS)

            true
        }
    }

    /**
     * 初始化 pkgListDialog
     */
    private fun initPkgListDialog() {
        // 先找到所有的包
        val pkgNameList = ArrayList<String>()
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolverInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        resolverInfoList.forEach { info ->
            if (info.activityInfo.packageName != "com.karrydev.fasttouch") { // 排除这个包
                pkgNameList.add(info.activityInfo.packageName)
            }
        }

        // 包装每一个非白名单 pkg，用作列表数据
        val appInfoList = ArrayList<AppInformation>()
        val whiteList = viewModel.settings.whiteList
        pkgNameList.forEach { pkgName ->
            val info = packageManager.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
            val appInfo = AppInformation(pkgName, packageManager.getApplicationLabel(info).toString(), packageManager.getApplicationIcon(info))
            appInfo.checkFlag = whiteList.contains(pkgName)
            appInfoList.add(appInfo)
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
            appInfo.checkFlag = !appInfo.checkFlag
            checkBox.isChecked = appInfo.checkFlag
        }

        pkgListDialog = AlertDialog.Builder(context)
            .setView(appListDialogLayout)
            .create()

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
            viewModel.settings.whiteList = list

            // 通知 Service 更新白名单
            viewModel.dispatchServiceAction(FastTouchService.ACTION_REFRESH_PACKAGE)

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
        if (!state.accessibility || !state.powerIgnored) {
            // 缺少相应权限，回到PermissionFragment
            mainViewModel.handleUserIntent(MainUiIntent.ToPermissionIntent())
        } else {
            // 进行 onResume 操作
            mapPackageWidgets = viewModel.settings.mapPackageWidgets
            updateSelectListEntries(widgetsPreference, mapPackageWidgets.keys)

            mapPackagePositions = viewModel.settings.mapPackagePositions
            updateSelectListEntries(positionsPreference, mapPackagePositions.keys)

            initPkgListDialog()
        }
    }
}