package com.brainor.bilihelper;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import okhttp3.Cookie;

public class MainActivity extends AppCompatActivity {
    SeriesInfo seriesInfo = new SeriesInfo();
    TextView inputTextView;
    ListView infoListView;
    TextView titleTextView;
    public static ArrayList<HistoryInfo> HistoryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!Objects.isNull(savedInstanceState)) return;
        Toolbar toolbar = findViewById(R.id.toolbar);
        Button searchButton = findViewById(R.id.searchButton);
        inputTextView = findViewById(R.id.inputText);
//        inputTextView.setText("ss25696");//调试用
        infoListView = findViewById(R.id.infoListView);
        infoListView.setAdapter(new ArrayAdapter<EpInfo>(MainActivity.this, R.layout.support_simple_spinner_dropdown_item, seriesInfo.epInfo){
            @Override
            public @NonNull View getView(int position, View convertView,@NonNull ViewGroup  parent){
                View view=super.getView(position,convertView,parent);
                if (position==HistoryList.get(0).position){//永远是最前面的那个
                    ((TextView)view).setTextColor(Color.BLUE);
                }else ((TextView)view).setTextColor(Color.BLACK);
                return view;
            }
        });
        titleTextView = findViewById(R.id.titleTextView);
        LoadCookies();
        if (HistoryList.size() == 0) LoadHistory();
        Settings.videoQuality = VideoQuality.values()[Arrays.asList(VideoQuality.getEntries()).indexOf(Objects.requireNonNull(getSharedPreferences("Settings", Context.MODE_PRIVATE).getString("quality", VideoQuality._2.description)))];
        Settings.clientType=Stream.of(ClientType.values()).filter(item -> Objects.equals(item.packageName, getSharedPreferences("Settings", Context.MODE_PRIVATE).getString("clientType", ClientType.release.packageName))).findFirst().get();
        Settings.clientDownload = getSharedPreferences("Settings", Context.MODE_PRIVATE).getBoolean("clientDown", true);
        searchButton.setOnClickListener(v -> {
//            sendBroadcast(new Intent("com.github.shadowsocks.CLOSE"));
            String URL = inputTextView.getText().toString();
            new getList().execute(URL);
        });
        infoListView.setOnItemClickListener((parent, view, position, id) -> {
            seriesInfo.position = position;
            //客户端创建文本文件, 或者从第三方下载
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... voids) {
                    String returnValue = Utility.downloadVideo(seriesInfo);
                    if (Objects.equals(returnValue.substring(0, 2), "错误")) cancel(true);
                    return returnValue;
                }

                @Override
                protected void onCancelled(String errMsg) {
                    Toast.makeText(MainActivity.this, errMsg, Toast.LENGTH_LONG).show();
                    if (errMsg.contains("需要Cookies")) {
                        PopupWebview();
                    } else if (errMsg.contains("timeout"))
                        Api.BiliplusHost = "https://" + (Api.BiliplusHost.equals("https://www.biliplus.com") ? "backup" : "www") + ".biliplus.com";
                }

                @Override
                protected void onPostExecute(String successMsg) {
                    if (successMsg.contains("成功")) {
                        Toast.makeText(MainActivity.this, successMsg, Toast.LENGTH_LONG).show();
                        //重启哔哩哔哩
                        Intent intent=getPackageManager().getLaunchIntentForPackage(Settings.clientType.packageName);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                        startActivity(intent);
                    }
                    else {//创建下载任务, 返回值是创建文件的路径
                        for (int i = 0; i < seriesInfo.downloadSegmentInfo.size(); i++) {
                            DownloadSegmentInfo downSegInfo = seriesInfo.downloadSegmentInfo.get(i);
                            EpInfo epInfo = seriesInfo.epInfo.get(seriesInfo.position);
                            DownloadTask(downSegInfo.url, successMsg + i + ".blv", seriesInfo.title + epInfo.index + epInfo.index_title + i,MainActivity.this);
                        }
                        Toast.makeText(MainActivity.this, "成功: 正在下载文件\n需要关闭VPN", Toast.LENGTH_LONG).show();
                    }
                    int position = HistoryList.get(0).position;//历史记录中的位置
                    if (position != seriesInfo.position) {
                        HistoryList.get(0).position = seriesInfo.position;
                        ((ArrayAdapter) infoListView.getAdapter()).notifyDataSetChanged();
                        StoreHistory(getApplicationContext());
                    }
                }
            }.execute();

        });
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show());
        CheckPermissions();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                this.startActivity(new Intent(this, Settings.class));
                return true;
            case R.id.history_settings:
                this.startActivity(new Intent(this, HistorySettingsActivity.class));
                return true;
            case R.id.about_settings:
                this.startActivity(new Intent(this, AboutSettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class getList extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... URLs) {
            String id;//判断URL是ss, ep, av
            String returnValue;
            Matcher m = Pattern.compile("\\w{2}\\d{1,9}").matcher(URLs[0]);
            if (m.find()) {
                id = m.group();
                switch (id.substring(0, 2).toLowerCase()) {
                    case "av"://video
                        id = id.substring(2);
                        seriesInfo.season_id = Long.valueOf(id);
                        returnValue = Utility.aid2PageInfo(id, seriesInfo);
                        break;
                    case "ep"://episodes
                        returnValue = Utility.ep2sid(id.substring(2));
                        if (Objects.equals(returnValue.substring(0, 2), "错误")) break;
                        else id = returnValue;
                    case "ss"://ss\d+
                        id = id.substring(2);
                    default://纯数字看做season_id
                        seriesInfo.season_id = Long.valueOf(id);
                        returnValue = Utility.sid2EpInfo(id, seriesInfo);
                }
            } else {
                returnValue = "错误:链接错误";
            }
            if (Objects.equals(returnValue.substring(0, 2), "错误")) cancel(true);
            return returnValue;
        }

        @Override//不能放在doInBackguound中
        protected void onCancelled(String errorMsg) {
            Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPostExecute(String HTMLBody) {
            titleTextView.setText(seriesInfo.title);
            //添加historylist
            String url;
            switch (seriesInfo.epInfo.get(0).videoType) {
                case Video:
                case VipVideo:
                    url = "av";
                    break;
                case Anime:
                case VipAnime:
                default:
                    url = "ss";
                    break;
            }
            HistoryInfo info = new HistoryInfo(seriesInfo.title, url + seriesInfo.season_id, -1);
            int position = HistoryList.indexOf(info);
            if (position > -1) {
                info.position=HistoryList.get(position).position;
                HistoryList.remove(position);
            }
            HistoryList.add(0, info);
            ((ArrayAdapter) infoListView.getAdapter()).notifyDataSetChanged();
            StoreHistory(getApplicationContext());
        }

    }

    /**
     * https://developer.android.com/guide/topics/security/permissions#normal-dangerous
     * 危险权限需要请求权限
     */
    private void CheckPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    private void PopupWebview() {

        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
        WebView webView = new WebView(this) {
            @Override
            public boolean onCheckIsTextEditor() {
                return true;
            }

            @Override
            protected void onSizeChanged(int w, int h, int ow, int oh) {
                super.onSizeChanged(w, (int) (point.y * 0.5), ow, oh);
            }
        };
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        CookieManager.getInstance().removeAllCookies(null);//进入该页面说明之前的cookies无效
        CookieManager.getInstance().flush();//不知道要不要
        webView.loadUrl(Api.loginBiliPlus());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                String cookies = CookieManager.getInstance().getCookie("biliplus.com");
                if (cookies != null && cookies.contains("access_key")) {
                    getPreferences(Context.MODE_PRIVATE).edit()
                            .putString("biliplus.com", cookies)
                            .apply();
                    LoadCookies();
                }
            }

        });
        AlertDialog ad = new AlertDialog.Builder(this)
                .setTitle("获取BiliPlus cookies")
                .setView(webView)
                .setNegativeButton("Close", (dialog, id) -> dialog.dismiss())
                .show();
    }

    void LoadCookies() {
        List<Cookie> cookieList = new ArrayList<>();
        String cookies = getPreferences(Context.MODE_PRIVATE).getString("biliplus.com", "");
        if (Objects.equals(cookies, "")) return;
        for (String cookie : Objects.requireNonNull(cookies).split(";\\s")) {
            String[] cookieSplit = cookie.split("=");
            cookieList.add(new Cookie.Builder().name(cookieSplit[0]).value(cookieSplit[1]).domain("biliplus.com").build());
        }
        Api.cookieStore.put(Api.BiliplusHost.replace("https://", ""), cookieList);
    }

    void LoadHistory() {
        List<String> HistoryStrList = new ArrayList<>(Objects.requireNonNull(getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getStringSet("HistoryList", new HashSet<>())));
        Collections.sort(HistoryStrList);
        for (String history : HistoryStrList) {
            String[] historys = history.split(";");
            HistoryList.add(new HistoryInfo(historys[1], historys[2], Integer.parseInt(historys[3])));
        }
    }

    void ClearHistory() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        sharedPref.edit().remove("HistoryList").apply();
    }

    static void StoreHistory(Context context) {
        HashSet<String> HistoryStrSet = new HashSet<>();
        for (int i = 0; i < HistoryList.size(); i++) {
            HistoryInfo info = HistoryList.get(i);
            HistoryStrSet.add(i + ";" + info.title + ";" + info.url + ";" + info.position);
        }
        SharedPreferences sharedPref = context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE);
        sharedPref.edit().putStringSet("HistoryList", HistoryStrSet).apply();
    }

    static void DownloadTask(String url, String filePath, String title,Context context) {
        //关闭VPN
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setAllowedOverMetered(false);
        request.setAllowedOverRoaming(false);
        request.addRequestHeader("Referer", "https://www.bilibili.com/video/av14543079/")
                .addRequestHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.13; rv:56.0) Gecko/20100101 Firefox/56.0");
        //MIUI必须关闭迅雷下载引擎
        request.setTitle(title);
        request.setDestinationUri(Uri.fromFile(new File(filePath)));
        downloadManager.enqueue(request);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (Objects.equals(intent.getAction(), Intent.ACTION_SEND) && intent.getFlags() - (Intent.FLAG_ACTIVITY_FORWARD_RESULT | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP | Intent.FLAG_GRANT_READ_URI_PERMISSION) == 0) {
            ((TextView) findViewById(R.id.inputText)).setText(intent.getStringExtra(Intent.EXTRA_TEXT).replaceFirst("^.*(?=http)", ""));
            findViewById(R.id.searchButton).callOnClick();
            intent.setAction("").replaceExtras(new Bundle()).setFlags(0);
        } else if (intent.hasExtra("position")) {
            String url = HistoryList.get(intent.getIntExtra("position", 0)).url;
            inputTextView.setText(url);
            new getList().execute(url);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        onNewIntent(intent);
    }
}
