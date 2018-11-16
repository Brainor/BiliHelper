package com.brainor.bilihelper;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import java.util.Objects;

public class AboutSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        TextView aboutTextView = findViewById(R.id.aboutText);

        Spanned styledText = Html.fromHtml("<p style=\"text-align: center;\">版本号 " + BuildConfig.VERSION_NAME + "</p>\n" +
                "<h1><a id=\"_0\"></a>开发</h1>\n" +
                "<p><a href=\"https://github.com/brainor\">@Brainor</a> 使用Java开发<br>\n" +
                "<a href=\"mailto:brainor@qq.com\">brainor@qq.com</a><br><br>\n" +
                "本程序主要参考<a href=\"https://github.com/xiaoyaocz/BiliAnimeDownload\">@xiaoyaocz</a>的Xamarin程序, 该Android版APP可在<a href=\"https://www.coolapk.com/apk/com.xiaoyaocz.bilidownload\">酷安</a>下载.<br>\n" +
                "<a href=\"mailto:xiaoyaocz@52uwp.com\">xiaoyaocz@52uwp.com</a></p>\n" +
                "<h1><a id=\"_5\"></a>赞助</h1>\n" +
                "<p>虽然只有我一个人用, 但请赞助原作者xiaoyaocz<br>\n" +
                "支付宝<a href=\"https://qr.alipay.com/FKX06526G3SYZ8MZZE2Q77\">2500655055@qq.com</a><br>\n" +
                "我的支付宝<a href=\"https://qr.alipay.com/tsx01990uusq195jv3zsi49\">brainor@qq.com</a></p>\n" +
                "<h1><a id=\"_9\"></a>使用声明</h1>\n" +
                "<ol>\n" +
                "<li>此程序仅供学习交流编程技术使用</li>\n" +
                "<li>如侵犯你的合法权益, 请联系本人以第一时间删除</li>\n" +
                "</ol>\n" +
                "<h1><a id=\"_12\"></a>引用&amp;开源</h1>\n" +
                "<p>本程序使用了Biliplus的API, 具体可参考Biliplus的<a href=\"https://www.biliplus.com/api/README\">开放接口</a>.<br>\n" +
                "网络通讯 <a href=\"http://square.github.io/okhttp/\">square/okhttp</a></p>"+
                "<h1><a id=\"_15\"></a>已知问题</h1>\n" +
                "<ol>\n" +
                "<li>可能BiliPlus也没有相关资源的下载链接</li>\n" +
                "<li>B站和BiliPlus提供的链接都是acgvideo.com域名下的, 需要特定的headers或者有IP限制, 会导致下载失败. 前者可以解决, 后者无法解决</li>\n" +
                "</ol>\n", Html.FROM_HTML_MODE_LEGACY);
        aboutTextView.setText(styledText);
        aboutTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
