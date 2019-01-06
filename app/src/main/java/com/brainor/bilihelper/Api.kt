package com.brainor.bilihelper

import okhttp3.*
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

internal object Api {
    var cookieStore = HashMap<String, List<Cookie>>()
    private val okHttpClient = OkHttpClient.Builder().cookieJar(object : CookieJar {
        //这里可以做cookie传递，保存等操作
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {//可以做保存cookies操作
            //            cookieStore.put(url.host(), cookies);
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {//加载新的cookies
            val cookies = cookieStore[url.host()]
            return cookies ?: ArrayList()
        }
    }).build()

    private const val appSecret = "94aba54af9065f71de72f5508f1cd42e"
    private const val appKey = "84956560bc028eb7"
    var appSecret_VIP = "9b288147e5474dd2aa67085f716c560d"
    var appSecret_PlayUrl = "1c15888dc316e05a15fdd0a02ed6584f"
    var BiliplusHost = "https://www.biliplus.com"

    fun getSeasonIdURL(ep_id: String): String {//从ep_id网页获取season_id
        //需要获得season_id
        //        try {//从ep编码变成season_id编码
        //            return Objects.requireNonNull(okHttpClient.newCall(new Request.Builder()
        //                    .url("https://www.bilibili.com/bangumi/play/ep" + ep_id)
        //                    .build()).execute().body()).string();
        //        } catch (IOException | NullPointerException e) {
        //            return "错误:" + e.getMessage();
        //        }
        return retrieveFromWeb("https://www.bilibili.com/bangumi/play/ep$ep_id")
    }

    fun getSeriesInfoURL(seasonId: String): String {//利用season_id获得SeriesInfo
        return retrieveFromWeb("https://bangumi.bilibili.com/view/web_api/season?season_id=$seasonId")
    }

    fun getMediaURL(cid: Long, preferedVideoQuality: Int): String {//获取视频下载地址
        return retrieveFromWeb("$BiliplusHost/BPplayurl.php?cid=$cid&otype=json&qn=$preferedVideoQuality")
    }

    fun getMediaURL2(aid: Long, page: Int, videoType: VideoType): String {//BiliPlus接口
        val bangumi: Int = when (videoType) {
            VideoType.Anime, VideoType.AreaAnime, VideoType.VipAnime -> 1
            VideoType.Movie, VideoType.VipMovie -> 2
            else -> 1
        }//番剧类型，0普通 1番剧 2电影
        return "$BiliplusHost/api/geturl?av=$aid&page=%d&bangumi=$bangumi&update=1"
    }

    fun getMediaURL3(cid: Long): String {//HTML5播放器获得视频地址, checked
        var url="https://bangumi.bilibili.com/player/web_api/v2/playurl?appkey=$appKey&cid=$cid&module=bangumi&otype=json&season_type=1&type="
        url += "&sign=" + getSign(url, appSecret)
        return url
    }

    fun getMediaURL4(seriesInfo: SeriesInfo): String {//Android客户端官方API

        return ""
    }

    fun getPageInfoURL(aid: String, page: Int): String {////利用aid获得PageInfo
        val url: String = if (page > 0) {//av开头的id, 获取的是视频
            "https://api.bilibili.com/view?appkey=$appKey&id=$aid&page=$page"
        } else {//page=0表示使用BiliPlus API查pageInfo
            "$BiliplusHost/api/view?id=$aid"
        }
        return retrieveFromWeb(url)
    }

    fun getPageInfoURL2(aid: Long?): String {//利用aid在BiliPlus获得PageInfo
        return "$BiliplusHost/api/view?id=$aid"
    }

    private fun getSign(url: String, secret: String): String {
        val str = HttpUrl.parse(url)!!.encodedQuery()!!//encoded query
        val sortedUrl = str.split("&").sorted().joinToString("&") + secret
        return md5(sortedUrl)
    }

    fun loginBiliPlus(): String {
        return "$BiliplusHost/login"
        //        return "https://passport.bilibili.com/login?appkey=27eb53fc9058f8c3&api=" + loginUrl + "&sign=" + md5("api=" + loginUrl + "c2ed53a74eeefe3cf99fbd01d8c9c375");
    }

    private fun md5(str: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val messageDigest = md.digest(str.toByteArray())
            String.format("%0" + (messageDigest.size shl 1) + "x", BigInteger(1, messageDigest))
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            ""
        }

    }

    private fun retrieveFromWeb(url: String): String {//联网获得信息
        return try {//从ep编码变成season_id编码
            okHttpClient.newCall(Request.Builder()
                    .url(url)
                    .build()).execute().body()!!.string()
        } catch (e: IOException) {
            """错误:${e.message}"""
        } catch (e: NullPointerException) {
            """错误:${e.message}"""
        }

    }
}
