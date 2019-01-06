package com.brainor.bilihelper

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
import androidx.recyclerview.widget.RecyclerView

class Settings : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, MySettingsFragment())
                .commit()
    }


    class MySettingsFragment : PreferenceFragmentCompat() {

        override//去除前面的空白
        fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> {
            return object : PreferenceGroupAdapter(preferenceScreen) {
                @SuppressLint("RestrictedApi")
                override fun onBindViewHolder(holder: PreferenceViewHolder, position: Int) {
                    super.onBindViewHolder(holder, position)
                    val preference = getItem(position)
                    val iconFrame = holder.itemView.findViewById<View>(R.id.icon_frame)
                    if (iconFrame != null) {
                        iconFrame.visibility = if (preference.icon == null) View.GONE else View.VISIBLE
                    }
                }
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            val screen = preferenceManager.createPreferenceScreen(context)
            screen.isIconSpaceReserved = false

            val downCategory = object : PreferenceCategory(context) {
                override fun onBindViewHolder(holder: PreferenceViewHolder) {
                    super.onBindViewHolder(holder)
                    holder.itemView.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                }
            }
            downCategory.title = "下载设置"

            val qualityList = ListPreference(context)
            qualityList.negativeButtonText = ""
            qualityList.key = "quality"
            qualityList.entries = VideoQuality.entries
            qualityList.entryValues = VideoQuality.entries
            qualityList.title = "视频清晰度"
            qualityList.summary = PreferenceManager.getDefaultSharedPreferences(context).getString(qualityList.key, VideoQuality.T2.description)
            qualityList.value = qualityList.summary as String
            qualityList.setOnPreferenceChangeListener { preference, newValue ->
                preference.summary = newValue as String
                videoQuality = VideoQuality.values().filter { item -> item.description == newValue }[0]
                //                videoQuality = Stream.of(*VideoQuality.values()).filter { item -> item.description == newValue }.findFirst().get()
                //                videoQuality = VideoQuality.values()[Arrays.asList(VideoQuality.getEntries()).indexOf(newValue.toString())];
                true
            }

            val clientDownloadSwitch = SwitchPreference(context)
            clientDownloadSwitch.summaryOn = "B站"
            clientDownloadSwitch.summaryOff = "系统"
            clientDownloadSwitch.key = "clientDown"
            clientDownloadSwitch.isChecked = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(clientDownloadSwitch.key, true)
            clientDownloadSwitch.title = "下载方式"
            clientDownloadSwitch.setOnPreferenceChangeListener { _, newValue ->
                clientDownload = newValue as Boolean
                true
            }

            val paraCategory = object : PreferenceCategory(context) {
                override fun onBindViewHolder(holder: PreferenceViewHolder) {
                    super.onBindViewHolder(holder)
                    holder.itemView.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                }
            }
            paraCategory.title = "参数设置"

            val clientTypeList = ListPreference(context)
            clientTypeList.negativeButtonText = ""
            clientTypeList.key = "clientType"
            clientTypeList.entries = ClientType.entries
            //            val packageNames = context.packageManager.getInstalledApplications(0).stream().map { item -> item.packageName }.collect<Set<String>, Any>(Collectors.toSet())
            //            clientTypeList.entries = Stream.of(*ClientType.values()).map<String>(Function<ClientType, String> { it.getPackageName() }).filter { item -> !packageNames.add(item) }.toArray<String>(String[]::new  /* Currently unsupported in Kotlin */)
            val packageNames = context.packageManager.getInstalledApplications(0).map { item -> item.packageName }.toMutableSet()

            clientTypeList.entries = ClientType.values().map { item -> item.packageName }.filter { item -> !packageNames.add(item) }.toTypedArray()
            clientTypeList.entryValues = clientTypeList.entries
            clientTypeList.title = "客户端类型"
            clientTypeList.summary = PreferenceManager.getDefaultSharedPreferences(context).getString(clientTypeList.key, ClientType.Release.packageName)
            clientTypeList.value = clientTypeList.summary as String
            clientTypeList.setOnPreferenceChangeListener { preference, newValue ->
                preference.summary = newValue.toString()
                clientType = ClientType.values()[ClientType.entries.indexOf(newValue as String)]
                true
            }

            screen.addPreference(downCategory)
            downCategory.addPreference(qualityList)
            downCategory.addPreference(clientDownloadSwitch)
            screen.addPreference(paraCategory)
            paraCategory.addPreference(clientTypeList)
            preferenceScreen = screen
        }
    }

    companion object {
        var videoQuality = VideoQuality.T2
        var clientType = ClientType.Release
        var clientDownload: Boolean = false
        var rootPath = "${Environment.getExternalStorageDirectory()}/Android/data/"
        var downloadAPKPath = "${Environment.getExternalStorageDirectory()}/Download/"
    }

}
