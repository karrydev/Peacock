package com.karrydev.peacock.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.karrydev.peacock.R
import com.karrydev.peacock.model.AppInformation

class PackageListAdapter(
    private val appInfoList: List<AppInformation>,
    private val inflater: LayoutInflater
) : BaseAdapter() {

    override fun getCount() = appInfoList.size

    override fun getItem(position: Int) = appInfoList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = inflater.inflate(R.layout.layout_package_item_adapter, null)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = convertView.tag as ViewHolder
        }
        val appInfo = appInfoList[position]
        holder.imageView.setImageDrawable(appInfo.icon)
        holder.textView.text = appInfo.appName
        holder.checkBox.isChecked = appInfo.checkFlag

        return view
    }


    internal class ViewHolder(v: View) {
        var textView: TextView
        var imageView: ImageView
        var checkBox: CheckBox

        init {
            textView = v.findViewById(R.id.name)
            imageView = v.findViewById(R.id.img)
            checkBox = v.findViewById(R.id.check)
        }
    }
}