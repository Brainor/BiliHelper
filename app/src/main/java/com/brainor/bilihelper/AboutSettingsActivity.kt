package com.brainor.bilihelper

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.about_settings.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.*


class AboutSettingsActivity : AppCompatActivity() {
    internal var extraDownloadId= 0L
    private var downloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getLongExtra("extraDownloadId", 0) == extraDownloadId) {
                updateButton.callOnClick()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val verText = "版本号 " + BuildConfig.VERSION_NAME
        versionText.text = verText
        updateButton.setOnClickListener {
            val appFile = File(Settings.downloadAPKPath, "Bilihelper-$latestVersion.apk")
            if (appFile.exists())
            //若存在则安装
                startActivity(Intent(Intent.ACTION_INSTALL_PACKAGE)
                        .setData(FileProvider.getUriForFile(this@AboutSettingsActivity, applicationContext.packageName + ".fileprovider", appFile))
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            else {//若不存在则下载
                extraDownloadId = MainActivity.downloadTask(downUrl, Settings.downloadAPKPath + "BiliHelper-" + latestVersion + ".apk", "BiliHelper-$latestVersion.apk", this@AboutSettingsActivity)
                Toast.makeText(this@AboutSettingsActivity, "下载Bilihelper.apk", Toast.LENGTH_SHORT).show()
            }
        }

        updateButton.setOnLongClickListener {
            val appFile = File(Settings.downloadAPKPath, "Bilihelper-$latestVersion.apk")
            if (appFile.exists())
            //若存在则删除
                if (appFile.delete())
                    Toast.makeText(this@AboutSettingsActivity, "文件已删除", Toast.LENGTH_SHORT).show()
                else
                    Toast.makeText(this@AboutSettingsActivity, "文件未删除", Toast.LENGTH_SHORT).show()

            true
        }
        GetLatestVersion().execute()

        val styledText = Html.fromHtml("""<h1><a id="_0"></a>开发</h1>
                <p><a href="https://github.com/brainor">@Brainor</a> 使用Java开发<br>
                <a href="mailto:brainor@qq.com">brainor@qq.com</a>.<br><br>
                本程序主要参考<a href="https://github.com/xiaoyaocz/BiliAnimeDownload">@xiaoyaocz</a>的Xamarin程序, 该Android版APP可在<a href="https://www.coolapk.com/apk/com.xiaoyaocz.bilidownload">酷安</a>下载.<br>
                <a href="mailto:xiaoyaocz@52uwp.com">xiaoyaocz@52uwp.com</a>.</p>
                <h1><a id="_5"></a>赞助</h1>
                <p>虽然只有我一个人用, 但请赞助原作者xiaoyaocz<br>
                支付宝<a href="https://qr.alipay.com/FKX06526G3SYZ8MZZE2Q77">2500655055@qq.com</a>.<br>
                我的支付宝<a href="https://qr.alipay.com/tsx01990uusq195jv3zsi49">brainor@qq.com</a>.</p>
                <h1><a id="_9"></a>使用声明</h1>
                <ol>
                <li>此程序仅供学习交流编程技术使用</li>
                <li>如侵犯你的合法权益, 请联系本人以第一时间删除</li>
                </ol>
                <h1><a id="_12"></a>引用&amp;开源</h1>
                <p>本程序使用了Biliplus的API, 具体可参考Biliplus的<a href="https://www.biliplus.com/api/README">开放接口</a>.<br>
                网络通讯 <a href="http://square.github.io/okhttp/">square/okhttp</a>.</p>
                <h1><a id="_15"></a>已知问题</h1>
                <ol>
                <li>可能BiliPlus也没有相关资源的下载链接</li>
                <li>B站和BiliPlus提供的链接都是acgvideo.com域名下的, 需要特定的headers或者有IP限制, 会导致下载失败. 前者可以解决, 后者无法解决.</li>
                <li>B站账户必须超过5级才可以看会员视频(Biliplus限制).</li>
                </ol>""", Html.FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM)
        aboutTextView.text = styledText
        aboutTextView.movementMethod = LinkMovementMethod.getInstance()
    }

    @SuppressLint("StaticFieldLeak")
    internal inner class GetLatestVersion : AsyncTask<Void, Void, String>() {
        override fun doInBackground(vararg voids: Void): String {
            return try {//获得最新版本
                val okHttpClient = OkHttpClient()
                Objects.requireNonNull<ResponseBody>(okHttpClient.newCall(Request.Builder()
                        .url("https://api.github.com/repos/Brainor/BiliHelper/releases/latest")
                        .build()).execute().body()).string()
            } catch (e: IOException) {
                cancel(true)
                "$e.message"
            } catch (e: NullPointerException) {
                cancel(true)
                "$e.message"
            }
        }

        override fun onCancelled(ErrMessage: String) {
            Toast.makeText(this@AboutSettingsActivity, ErrMessage, Toast.LENGTH_LONG).show()
        }

        override fun onPostExecute(html: String) {
            updateButton.visibility = View.VISIBLE
            latestVersion = BuildConfig.VERSION_NAME
            try {
                val json = JSONObject(html)
                latestVersion = json.getString("name").substring(1)//第一个字是"v"
                downUrl = json.getJSONArray("assets").getJSONObject(0).getString("browser_download_url")
            } catch (e: JSONException) {
                Toast.makeText(this@AboutSettingsActivity, e.message, Toast.LENGTH_LONG).show()
            }

            if (BuildConfig.VERSION_NAME != latestVersion) {
                val updateText = "更新至$latestVersion"
                updateButton.text = updateText
            } else
                updateButton.text = "更新"

        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(downloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(downloadComplete)
    }

    companion object {
        private var downUrl: String = ""
        private var latestVersion: String = ""
    }
}
