package com.brainor.bilihelper

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Point
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import okhttp3.Cookie
import java.io.File

class MainActivity : AppCompatActivity() {
    internal var seriesInfo = SeriesInfo()
    @SuppressLint("StaticFieldLeak")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState != null) return
        //        inputTextView.setText("ss25696");//调试用
        infoListView.adapter = object : ArrayAdapter<EpInfo>(this@MainActivity, R.layout.support_simple_spinner_dropdown_item, seriesInfo.epInfo) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                if (position == HistoryList[0].position) {//永远是最前面的那个
                    (view as TextView).setTextColor(Color.BLUE)
                } else
                    (view as TextView).setTextColor(Color.BLACK)
                return view
            }
        }
        loadCookies()
        if (HistoryList.size == 0) loadHistory()
        Settings.videoQuality = VideoQuality.values()[VideoQuality.entries.indexOf(getSharedPreferences("Settings", Context.MODE_PRIVATE).getString("quality", VideoQuality.T2.description))]
        Settings.clientType = ClientType.values().filter { item -> item.packageName == getSharedPreferences("Settings", Context.MODE_PRIVATE).getString("clientType", ClientType.Release.packageName) }[0]
        Settings.clientDownload = getSharedPreferences("Settings", Context.MODE_PRIVATE).getBoolean("clientDown", true)
        searchButton.setOnClickListener {
            val url = inputTextView.text.toString()
            GetList().execute(url)
        }
        infoListView.setOnItemClickListener { _, _, position, _ ->
            seriesInfo.position = position
            //客户端创建文本文件, 或者从第三方下载
            DownloadVideo().execute()
        }
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
        checkPermissions()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                this.startActivity(Intent(this, Settings::class.java))
                true
            }
            R.id.history_settings -> {
                this.startActivity(Intent(this, HistorySettingsActivity::class.java))
                true
            }
            R.id.about_settings -> {
                this.startActivity(Intent(this, AboutSettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("StaticFieldLeak")
    internal inner class GetList : AsyncTask<String, Unit, String>() {
        override fun doInBackground(vararg URLs: String): String {
            var id: String//判断URL是ss, ep, av
            var returnValue: String
            val matchResult = Regex("""\w{2}\d{1,9}""").find(URLs[0])
            if (matchResult != null) {
                id = matchResult.value
                when (id.substring(0, 2).toLowerCase()) {
                    "av" -> {//video
                        id = id.substring(2)
                        seriesInfo.seasonId = id.toLong()
                        returnValue = Utility.aid2PageInfo(id, seriesInfo)
                    }
                    "ep" -> {//episodes
                        returnValue = Utility.ep2sid(id.substring(2))
                        if (returnValue.substring(0, 2) != "错误") {
                            id = returnValue.substring(2)
                            seriesInfo.seasonId = id.toLong()
                            returnValue = Utility.sid2EpInfo(id, seriesInfo)
                        }
                    }
                    "ss" -> {//ss\d+
                        id = id.substring(2)
                        seriesInfo.seasonId = id.toLong()
                        returnValue = Utility.sid2EpInfo(id, seriesInfo)
                    }
                    else -> {//纯数字看做season_id
                        seriesInfo.seasonId = java.lang.Long.valueOf(id)
                        returnValue = Utility.sid2EpInfo(id, seriesInfo)
                    }
                }
            } else returnValue = "错误:链接错误"
            if (returnValue.substring(0, 2) == "错误") cancel(true)
            return returnValue
        }

        override fun onCancelled(errorMsg: String) {//不能放在doInBackguound中
            Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_LONG).show()
        }

        override fun onPostExecute(HTMLBody: String) {
            titleTextView.text = seriesInfo.title
            //添加historylist
            val url = when (seriesInfo.epInfo[0].videoType) {
                VideoType.Video, VideoType.VipVideo -> "av"
                VideoType.Anime, VideoType.VipAnime -> "ss"
                else -> "ss"
            }
            val info = HistoryInfo(seriesInfo.title, url + seriesInfo.seasonId, -1)
            val position = HistoryList.indexOf(info)
            if (position > -1) {
                info.position = HistoryList[position].position
                HistoryList.removeAt(position)
            }
            HistoryList.add(0, info)
            (infoListView.adapter as ArrayAdapter<*>).notifyDataSetChanged()
            storeHistory(applicationContext)
        }

    }
    @SuppressLint("StaticFieldLeak")
    internal inner class DownloadVideo:AsyncTask<Unit, Unit, String>() {
        override fun doInBackground(vararg params: Unit): String {
            val returnValue = Utility.downloadVideo(seriesInfo)
            if (returnValue.substring(0, 2) == "错误") cancel(true)
            return returnValue
        }

        override fun onCancelled(errMsg: String) {
            Toast.makeText(this@MainActivity, errMsg, Toast.LENGTH_LONG).show()
            if (errMsg.contains("需要Cookies")) {
                popupWebview()
            } else if (errMsg.contains("timeout"))
                Api.BiliplusHost = "https://" + (if (Api.BiliplusHost == "https://www.biliplus.com") "backup" else "www") + ".biliplus.com"
        }

        override fun onPostExecute(successMsg: String) {
            if (successMsg.contains("成功")) {
                Toast.makeText(this@MainActivity, successMsg, Toast.LENGTH_LONG).show()
                //重启哔哩哔哩
                val intent = packageManager.getLaunchIntentForPackage(Settings.clientType.packageName)
                intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            } else {//创建下载任务, 返回值是创建文件的路径
                Toast.makeText(this@MainActivity, "成功: 正在下载文件\n关闭VPN", Toast.LENGTH_LONG).show()
                sendBroadcast(Intent("in.zhaoj.shadowsocksr.CLOSE"))//关闭SSR
                for (i in 0 until seriesInfo.downloadSegmentInfo.size) {
                    val downSegInfo = seriesInfo.downloadSegmentInfo[i]
                    val epInfo = seriesInfo.epInfo[seriesInfo.position]
                    downloadTask(downSegInfo.url, "$successMsg$i.blv", seriesInfo.title + epInfo.index + epInfo.index_title + i, this@MainActivity)
                }
            }
            val oldPosition = HistoryList[0].position//历史记录中的位置
            if (oldPosition != seriesInfo.position) {
                HistoryList[0].position = seriesInfo.position
                (infoListView.adapter as ArrayAdapter<*>).notifyDataSetChanged()
                storeHistory(applicationContext)
            }
        }
    }

    /**
     * https://developer.android.com/guide/topics/security/permissions#normal-dangerous
     * 危险权限需要请求权限
     */
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
        }
    }

    private fun popupWebview() {

        val point = Point()
        windowManager.defaultDisplay.getSize(point)
        val webView = object : WebView(this) {
            override fun onCheckIsTextEditor(): Boolean {
                return true
            }

            override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
                super.onSizeChanged(w, (point.y * 0.5).toInt(), ow, oh)
            }
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        CookieManager.getInstance().removeAllCookies(null)//进入该页面说明之前的cookies无效
        CookieManager.getInstance().flush()//不知道要不要
        webView.loadUrl(Api.loginBiliPlus())
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val cookies = CookieManager.getInstance().getCookie("biliplus.com")
                if (cookies != null && cookies.contains("access_key")) {
                    getPreferences(Context.MODE_PRIVATE).edit()
                            .putString("biliplus.com", cookies)
                            .apply()
                    loadCookies()
                }
            }

        }
        AlertDialog.Builder(this)
                .setTitle("获取BiliPlus cookies")
                .setView(webView)
                .setNegativeButton("Close") { dialog, _ -> dialog.dismiss() }
                .show()
    }

    internal fun loadCookies() {
        val cookieList = ArrayList<Cookie>()
        val cookies = getPreferences(Context.MODE_PRIVATE).getString("biliplus.com", "")!!
        if (cookies == "") return
        for (cookie in cookies.split(Regex(""";\s"""))) {
            val cookieSplit = cookie.split("=")
            cookieList.add(Cookie.Builder().name(cookieSplit[0]).value(cookieSplit[1]).domain("biliplus.com").build())
        }
        Api.cookieStore[Api.BiliplusHost.replace("https://", "")] = cookieList
    }

    private fun loadHistory() {
        val historyStrList = getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getStringSet("HistoryList", HashSet())?.sorted()!!
        for (history in historyStrList) {
            val historys = history.split(";")
            HistoryList.add(HistoryInfo(historys[1], historys[2], Integer.parseInt(historys[3])))
        }
    }

    internal fun clearHistory() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        sharedPref.edit().remove("HistoryList").apply()
    }

    override fun onNewIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.flags - (Intent.FLAG_ACTIVITY_FORWARD_RESULT or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP or Intent.FLAG_GRANT_READ_URI_PERMISSION) == 0) {
            inputTextView.setText(intent.getStringExtra(Intent.EXTRA_TEXT).replaceFirst(Regex("^.*(?=http)"), ""))
            searchButton.callOnClick()
            intent.setAction("").replaceExtras(Bundle()).flags = 0
        } else if (intent.hasExtra("position")) {
            val url = HistoryList[intent.getIntExtra("position", 0)].url
            inputTextView.setText(url)
            GetList().execute(url)
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = intent
        onNewIntent(intent)
    }

    companion object {
        var HistoryList = ArrayList<HistoryInfo>()

        internal fun storeHistory(context: Context) {
            val historyStrSet = HashSet<String>()
            for (i in HistoryList.indices) {
                val info = HistoryList[i]
                historyStrSet.add("$i;${info.title};${info.url};${info.position}")
            }
            val sharedPref = context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE)
            sharedPref.edit().putStringSet("HistoryList", historyStrSet).apply()
        }

        internal fun downloadTask(url: String, filePath: String, title: String, context: Context): Long {
            //关闭VPN
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(url))
            request.setAllowedOverMetered(false)
            request.setAllowedOverRoaming(false)
            request.addRequestHeader("Referer", "https://www.bilibili.com/video/av14543079/")
                    .addRequestHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.13; rv:56.0) Gecko/20100101 Firefox/56.0")
            //MIUI必须关闭迅雷下载引擎
            request.setTitle(title)
            request.setDestinationUri(Uri.fromFile(File(filePath)))
            return downloadManager.enqueue(request)
        }
    }
}
