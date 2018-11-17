package com.brainor.bilihelper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

import java.util.Arrays;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.RecyclerView;

public class Settings extends AppCompatActivity {
    static VideoQuality videoQuality;
    static ClientType clientType=ClientType.release;
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

            ListPreference quality = new ListPreference(context);
            quality.setKey("quality");
            quality.setEntries(VideoQuality.getEntries());
            quality.setEntryValues(VideoQuality.getEntries());
            quality.setTitle("视频清晰度");
            quality.setSummary(PreferenceManager.getDefaultSharedPreferences(quality.getContext()).getString(quality.getKey(), "超清"));
            quality.setOnPreferenceChangeListener((preference, newValue) -> {
                preference.setSummary(newValue.toString());
                videoQuality = VideoQuality.list[Arrays.asList(VideoQuality.getEntries()).indexOf(newValue.toString())];
                return true;
            });
            SwitchPreference clientDownloadSwitch=new SwitchPreference(context);
            clientDownloadSwitch.setSummaryOn("B站");
            clientDownloadSwitch.setSummaryOff("系统");
            clientDownloadSwitch.setKey("clientDown");
            clientDownloadSwitch.setChecked(PreferenceManager.getDefaultSharedPreferences(clientDownloadSwitch.getContext()).getBoolean(clientDownloadSwitch.getKey(), true));
            clientDownloadSwitch.setTitle("下载方式");
            clientDownloadSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                clientDownload = (boolean) newValue;
                return true;
            });

            screen.addPreference(quality);
            screen.addPreference(clientDownloadSwitch);
            setPreferenceScreen(screen);

        }

    }

}
