package com.brainor.bilihelper;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import okhttp3.OkHttpClient;
import okhttp3.Request;


public class AboutSettingsActivity extends AppCompatActivity {
    static private String downUrl;
    static private String latestVersion;
    Button updateButton;
    Long extra_download_id = 0L;
    BroadcastReceiver downloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getLongExtra("extra_download_id", 0) == extra_download_id) {
                updateButton.callOnClick();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        String verText = "版本号 " + BuildConfig.VERSION_NAME;
        ((TextView) findViewById(R.id.version)).setText(verText);
        updateButton = findViewById(R.id.updateButton);
        updateButton.setOnClickListener(v -> {
            File appFile = new File(Settings.downloadAPKPath, "Bilihelper-" + latestVersion + ".apk");
            if (appFile.exists())//若存在则安装
                startActivity(new Intent(Intent.ACTION_INSTALL_PACKAGE)
                        .setData(FileProvider.getUriForFile(AboutSettingsActivity.this, getApplicationContext().getPackageName() + ".fileprovider", appFile))
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
            else {//若不存在则下载
                extra_download_id = MainActivity.DownloadTask(downUrl, Settings.downloadAPKPath + "BiliHelper-" + latestVersion + ".apk", "BiliHelper-" + latestVersion + ".apk", AboutSettingsActivity.this);
                Toast.makeText(AboutSettingsActivity.this, "下载Bilihelper.apk", Toast.LENGTH_SHORT).show();
            }
        });

        updateButton.setOnLongClickListener(v -> {
            File appFile = new File(Settings.downloadAPKPath, "Bilihelper-" + latestVersion + ".apk");
            if (appFile.exists())//若存在则删除
                if (appFile.delete())
                    Toast.makeText(AboutSettingsActivity.this, "文件已删除", Toast.LENGTH_SHORT).show();
                else Toast.makeText(AboutSettingsActivity.this, "文件未删除", Toast.LENGTH_SHORT).show();

            return true;
        });
        new getLatestVersion().execute();

        Spanned styledText = Html.fromHtml("<h1><a id=\"_0\"></a>开发</h1>\n" +
                "<p><a href=\"https://github.com/brainor\">@Brainor</a> 使用Java开发<br>\n" +
                "<a href=\"mailto:brainor@qq.com\">brainor@qq.com</a>.<br><br>\n" +
                "本程序主要参考<a href=\"https://github.com/xiaoyaocz/BiliAnimeDownload\">@xiaoyaocz</a>的Xamarin程序, 该Android版APP可在<a href=\"https://www.coolapk.com/apk/com.xiaoyaocz.bilidownload\">酷安</a>下载.<br>\n" +
                "<a href=\"mailto:xiaoyaocz@52uwp.com\">xiaoyaocz@52uwp.com</a>.</p>\n" +
                "<h1><a id=\"_5\"></a>赞助</h1>\n" +
                "<p>虽然只有我一个人用, 但请赞助原作者xiaoyaocz<br>\n" +
                "支付宝<a href=\"https://qr.alipay.com/FKX06526G3SYZ8MZZE2Q77\">2500655055@qq.com</a>.<br>\n" +
                "我的支付宝<a href=\"https://qr.alipay.com/tsx01990uusq195jv3zsi49\">brainor@qq.com</a>.</p>\n" +
                "<h1><a id=\"_9\"></a>使用声明</h1>\n" +
                "<ol>\n" +
                "<li>此程序仅供学习交流编程技术使用</li>\n" +
                "<li>如侵犯你的合法权益, 请联系本人以第一时间删除</li>\n" +
                "</ol>\n" +
                "<h1><a id=\"_12\"></a>引用&amp;开源</h1>\n" +
                "<p>本程序使用了Biliplus的API, 具体可参考Biliplus的<a href=\"https://www.biliplus.com/api/README\">开放接口</a>.<br>\n" +
                "网络通讯 <a href=\"http://square.github.io/okhttp/\">square/okhttp</a>.</p>" +
                "<h1><a id=\"_15\"></a>已知问题</h1>\n" +
                "<ol>\n" +
                "<li>可能BiliPlus也没有相关资源的下载链接</li>\n" +
                "<li>B站和BiliPlus提供的链接都是acgvideo.com域名下的, 需要特定的headers或者有IP限制, 会导致下载失败. 前者可以解决, 后者无法解决.</li>\n" +
                "<li>B站账户必须超过5级才可以看会员视频(Biliplus限制).</li>" +
                "<li><font color=\"red\"><b>目前Biliplus已挂.</b></font></li>" +
                "</ol>\n", Html.FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM);
        TextView aboutTextView = findViewById(R.id.aboutText);
        aboutTextView.setText(styledText);
        aboutTextView.setMovementMethod(LinkMovementMethod.getInstance());


    }

    class getLatestVersion extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {//获得最新版本
                final OkHttpClient okHttpClient = new OkHttpClient();
                return Objects.requireNonNull(okHttpClient.newCall(new Request.Builder()
                        .url("https://api.github.com/repos/Brainor/BiliHelper/releases/latest")
                        .build()).execute().body()).string();
            } catch (IOException | NullPointerException e) {
                cancel(true);
                return e.getMessage();
            }
        }

        @Override
        protected void onCancelled(String ErrMessage) {
            Toast.makeText(AboutSettingsActivity.this, ErrMessage, Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPostExecute(String html) {
            updateButton.setVisibility(View.VISIBLE);
            latestVersion = BuildConfig.VERSION_NAME;
            try {
                JSONObject json = new JSONObject(html);
                latestVersion = json.getString("name").substring(1);//第一个字是"v"
                downUrl = json.getJSONArray("assets").getJSONObject(0).getString("browser_download_url");
            } catch (JSONException e) {
                Toast.makeText(AboutSettingsActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
            if (!Objects.equals(BuildConfig.VERSION_NAME, latestVersion)) {
                String updateText = "更新至" + latestVersion;
                updateButton.setText(updateText);
            } else updateButton.setText("更新");

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(downloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(downloadComplete);
    }
}
