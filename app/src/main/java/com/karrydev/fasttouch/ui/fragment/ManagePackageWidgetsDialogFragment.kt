package com.karrydev.fasttouch.ui.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.google.gson.Gson
import com.karrydev.fasttouch.R
import com.karrydev.fasttouch.model.Settings
import com.karrydev.fasttouch.util.showToast

class ManagePackageWidgetsDialogFragment : DialogFragment() {

    private lateinit var editRules: EditText
    private lateinit var originalRules: String
    private lateinit var setting: Settings

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        dialog!!.setCanceledOnTouchOutside(false)

        val view = inflater.inflate(R.layout.layout_manage_package_widgets, container, false)

        editRules = view.findViewById(R.id.editText_rules)
        setting = Settings
        originalRules = Gson().toJson(setting.pkgWidgetMap)
        editRules.setText(originalRules)

        // 【重置规则】
        val btReset = view.findViewById<Button>(R.id.button_reset)
        btReset?.setOnClickListener { editRules.setText(originalRules) }

        // 【复制规则】
        val btCopy = view.findViewById<Button>(R.id.button_copy)
        btCopy?.setOnClickListener {
            // 将规则复制到剪贴板
            val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Rules: Widget in Packages", editRules.text.toString())
            clipboard.setPrimaryClip(clip)
            showToast("规则已复制到剪贴板!")
        }

        // 【粘贴规则】
        val btPaste = view.findViewById<Button>(R.id.button_paste)
        btPaste?.setOnClickListener {
            val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val pasteData: String = clipData.getItemAt(0).text.toString()
                editRules.setText(pasteData)
                showToast("已从剪贴板获取规则!")
            } else {
                showToast("未从剪贴板发现规则!")
            }
        }

        // 【取消修改】
        val btCancel = view.findViewById<Button>(R.id.button_widgets_cancel)
        btCancel?.setOnClickListener {
            showToast("修改已取消")
            dialog!!.dismiss()
        }

        // 【在线规则】
//        val btRules = view.findViewById<Button>(R.id.button_widgets_rules)
//        btRules?.setOnClickListener {
//            val url = "http://touchhelper.zfdang.com/rules"
//            val i = Intent(Intent.ACTION_VIEW)
//            i.setData(Uri.parse(url))
//            startActivity(i)
//        }

        // 【确认修改】
        val btConfirm = view.findViewById<Button>(R.id.button_widgets_confirm)
        btConfirm?.setOnClickListener {
            val result: Boolean = setting.setPackageWidgetsInString(editRules.text.toString())
            if (result) {
                showToast("规则已保存!")
                dialog!!.dismiss()
            } else {
                showToast("规则有误，请修改后再次保存!")
            }
        }
        return view
    }
}