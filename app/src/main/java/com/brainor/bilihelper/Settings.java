package com.brainor.bilihelper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.RecyclerView;

public class Settings extends AppCompatActivity {
    static VideoQuality videoQuality;
    static ClientType clientType = ClientType.release;
    static boolean clientDownload;
    static String rootPath = "/storage/emulated/0/Android/data/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new MySettingsFragment())
                .commit();
    }


    public static class MySettingsFragment extends PreferenceFragmentCompat {

        @Override//去除前面的空白
        protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
            return new PreferenceGroupAdapter(preferenceScreen) {
                @SuppressLint("RestrictedApi")
                @Override
                public void onBindViewHolder(PreferenceViewHolder holder, int position) {
                    super.onBindViewHolder(holder, position);
                    Preference preference = getItem(position);
                    View iconFrame = holder.itemView.findViewById(R.id.icon_frame);
                    if (iconFrame != null) {
                        iconFrame.setVisibility(preference.getIcon() == null ? View.GONE : View.VISIBLE);
                    }
                }
            };
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            Context context = getPreferenceManager().getContext();
            PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
            screen.setIconSpaceReserved(false);

            PreferenceCategory downCategory = new PreferenceCategory(context) {
                @Override
                public void onBindViewHolder(PreferenceViewHolder holder) {
                    super.onBindViewHolder(holder);
                    holder.itemView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
                }
            };
            downCategory.setTitle("下载设置");

            ListPreference qualityList = new ListPreference(context);
            qualityList.setNegativeButtonText("");
            qualityList.setKey("quality");
            qualityList.setEntries(VideoQuality.getEntries());
            qualityList.setEntryValues(VideoQuality.getEntries());
            qualityList.setTitle("视频清晰度");
            qualityList.setSummary(PreferenceManager.getDefaultSharedPreferences(context).getString(qualityList.getKey(), VideoQuality._2.description));
            qualityList.setValue((String) qualityList.getSummary());
            qualityList.setOnPreferenceChangeListener((preference, newValue) -> {
                preference.setSummary((String) newValue);
                videoQuality = Stream.of(VideoQuality.values()).filter(item -> Objects.equals(item.description, newValue)).findFirst().get();
//                videoQuality = VideoQuality.values()[Arrays.asList(VideoQuality.getEntries()).indexOf(newValue.toString())];
                return true;
            });

            SwitchPreference clientDownloadSwitch = new SwitchPreference(context);
            clientDownloadSwitch.setSummaryOn("B站");
            clientDownloadSwitch.setSummaryOff("系统");
            clientDownloadSwitch.setKey("clientDown");
            clientDownloadSwitch.setChecked(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(clientDownloadSwitch.getKey(), true));
            clientDownloadSwitch.setTitle("下载方式");
            clientDownloadSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                clientDownload = (boolean) newValue;
                return true;
            });

            PreferenceCategory paraCategory = new PreferenceCategory(context) {
                @Override
                public void onBindViewHolder(PreferenceViewHolder holder) {
                    super.onBindViewHolder(holder);
                    holder.itemView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
                }
            };
            paraCategory.setTitle("参数设置");

            ListPreference clientTypeList = new ListPreference(context);
            clientTypeList.setNegativeButtonText("");
            clientTypeList.setKey("clientType");
            clientTypeList.setEntries(ClientType.getEntries());
            Set<String> packageNames = context.getPackageManager().getInstalledApplications(0).stream().map(item -> item.packageName).collect(Collectors.toSet());
            clientTypeList.setEntries(Stream.of(ClientType.values()).map(cT -> cT.packageName).filter(item -> !packageNames.add(item)).toArray(String[]::new));
            clientTypeList.setEntryValues(clientTypeList.getEntries());
            clientTypeList.setTitle("客户端类型");
            clientTypeList.setSummary(PreferenceManager.getDefaultSharedPreferences(context).getString(clientTypeList.getKey(), ClientType.release.packageName));
            clientTypeList.setValue((String) clientTypeList.getSummary());
            clientTypeList.setOnPreferenceChangeListener((preference, newValue) -> {
                preference.setSummary(newValue.toString());
                clientType = ClientType.values()[Arrays.asList(ClientType.getEntries()).indexOf((String) newValue)];
                return true;
            });

            screen.addPreference(downCategory);
            downCategory.addPreference(qualityList);
            downCategory.addPreference(clientDownloadSwitch);
            screen.addPreference(paraCategory);
            paraCategory.addPreference(clientTypeList);
            setPreferenceScreen(screen);
        }
    }

}
