package com.brainor.bilihelper;

import android.text.TextUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

class Api {
    static HashMap<String, List<Cookie>> cookieStore = new HashMap<>();
    private static OkHttpClient okHttpClient = new OkHttpClient.Builder().cookieJar(new CookieJar() {//这里可以做cookie传递，保存等操作
        @Override
        public void saveFromResponse(@NonNull HttpUrl url, @NonNull List<Cookie> cookies) {//可以做保存cookies操作
//            cookieStore.put(url.host(), cookies);
        }

        @Override
        public List<Cookie> loadForRequest(@NonNull HttpUrl url) {//加载新的cookies
            List<Cookie> cookies = cookieStore.get(url.host());
            return cookies != null ? cookies : new ArrayList<>();
        }
    }).build();

    private static String appSecret = "94aba54af9065f71de72f5508f1cd42e";
    private static String appKey = "84956560bc028eb7";
    static String _appSecret_VIP = "9b288147e5474dd2aa67085f716c560d";
    static String _appSecret_PlayUrl = "1c15888dc316e05a15fdd0a02ed6584f";
    static String BiliplusHost = "https://www.biliplus.com";
    static String getSeasonIdURL(String ep_id) {//从ep_id网页获取season_id
        //需要获得season_id
        try {//从ep编码变成season_id编码
            return Objects.requireNonNull(okHttpClient.newCall(new Request.Builder()
                    .url("https://www.bilibili.com/bangumi/play/ep" + ep_id)
                    .build()).execute().body()).string();
        } catch (IOException | NullPointerException e) {
            return "错误:" + e.getMessage();
        }
    }

    static String getSeriesInfoURL(String season_id) {//利用season_id获得SeriesInfo
        try {//获得番剧数据
            return Objects.requireNonNull(okHttpClient.newCall(new Request.Builder()
                    .url("https://bangumi.bilibili.com/view/web_api/season?season_id=" + season_id)
                    .build()).execute().body()).string();
        } catch (IOException | NullPointerException e) {
            return "错误:" + e.getMessage();
        }
    }

    static String getMediaURL(long cid,int prefered_video_quality) {//获取视频下载地址
        try {
            return Objects.requireNonNull(okHttpClient.newCall(new Request.Builder()
                    .url(BiliplusHost + "/BPplayurl.php?cid=" + cid + "&otype=json&qn="+prefered_video_quality)
                    .build()).execute().body()).string();
        } catch (IOException | NullPointerException e) {
            return "错误:" + e.getMessage();
        }
    }

    static String getMediaURL2(Long aid, int page, VideoType videoType) {//BiliPlus接口
        int bangumi;//番剧类型，0普通 1番剧 2电影
        switch (videoType) {
            case Anime:
            case AreaAnime:
            case VipAnime:
                bangumi = 1;
                break;
            case Movie:
            case VipMovie:
                bangumi = 2;
                break;
            default:
                bangumi = 1;
                break;
        }
        return String.format(Locale.CHINA, BiliplusHost + "/api/geturl?av=%d&page=%d&bangumi=%d&update=1", aid, page, bangumi);
    }

    static String getMediaURL3(Long cid) {//HTML5播放器获得视频地址, checked
        String url = String.format(Locale.CHINA, "https://bangumi.bilibili.com/player/web_api/v2/playurl?appkey=%s&cid=%s&module=bangumi&otype=json&season_type=1&type=", appKey, cid);
        url += "&sign=" + getSign(url, appSecret);
        return url;
    }

    static String getPageInfoURL(String aid, int page) {////利用aid获得PageInfo
        String url;
        try {//获得番剧数据
            if (page > 0) {//av开头的id, 获取的是视频
                url = "https://api.bilibili.com/view?appkey=" + appKey + "&id=" + aid + "&page=" + page;
            } else {//page=0表示使用BiliPlus API查pageInfo
                url = BiliplusHost + "/api/view?id=" + aid;
            }
            return Objects.requireNonNull(okHttpClient.newCall(new Request.Builder()
                    .url(url)
                    .build()).execute().body()).string();
        } catch (IOException | NullPointerException e) {
            return "错误:" + e.getMessage();
        }
    }

    static String getPageInfoURL2(Long aid) {//利用aid在BiliPlus获得PageInfo
        return BiliplusHost + "/api/view?id=" + aid;
    }

    private static String getSign(String url, String secret) {
        String str = url.substring(url.indexOf("?", 4) + 1);
        List<String> list = Arrays.asList(str.split("&"));
        Collections.sort(list);
        url = TextUtils.join("&", list) + secret;//String.join适合后续版本
        return MD5(url);
    }

    static String loginBiliPlus() {
        String loginUrl = BiliplusHost + "/login";
        return loginUrl;
//        return "https://passport.bilibili.com/login?appkey=27eb53fc9058f8c3&api=" + loginUrl + "&sign=" + MD5("api=" + loginUrl + "c2ed53a74eeefe3cf99fbd01d8c9c375");
    }

    private static String MD5(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(str.getBytes());
            return String.format("%0" + (messageDigest.length << 1) + "x", new BigInteger(1, messageDigest));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }
}
