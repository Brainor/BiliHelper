package com.brainor.bilihelper;

import android.support.annotation.NonNull;
import android.util.Xml;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

class Utility {
    /*
    B站API: https://github.com/Vespa314/bilibili-api/blob/master/api.md
    BiliPlus API: https://www.biliplus.com/api/README
    appSecret = "560c52ccd288fed045859ed18bffd973";
appKey = "1d8b6e7d45233436";
appSecret_VIP = "9b288147e5474dd2aa67085f716c560d";
appSecret_PlayUrl = "1c15888dc316e05a15fdd0a02ed6584f";
http://api.bilibili.cn/view?id=28143592&appkey=1d8b6e7d45233436

视频
https://www.bilibili.com/video/av16785474/  aid:16785474
视频信息API "http://app.bilibili.com/x/view?aid={0}&appkey=1d8b6e7d45233436&build=521000&ts={1}&sign=" + GetSign(uri)
http://app.bilibili.com/x/view?aid=16785474&appkey=1d8b6e7d45233436&build=521000&ts=1538141261"&sign=" + GetSign(uri)



番剧
https://www.bilibili.com/bangumi/play/ep8206 ep_id:8206
cid:27425145
https//www.bilibili.com/bangumi/play/ss441/ 从网页中查找ss(\d+)
番剧信息API https://bangumi.bilibili.com/view/web_api/season?season_id=$ss
https://bangumi.bilibili.com/view/web_api/season?season_id=441


     */
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


    /**
     * 将ep_id转成season_id
     *
     * @param id id
     * @return value[0]返回数据传输是否成功
     * value[1]返回出错信息或者ss+season_id
     */
    static String ep2sid(String id) {
        String HTMLBody;
        //需要获得season_id
        try {//从ep编码变成season_id编码
            HTMLBody = Objects.requireNonNull(okHttpClient.newCall(new Request.Builder()
                    .url(Api.getSeasonIdURL(id))
                    .build()).execute().body()).string();
        } catch (IOException | NullPointerException e) {
            return "错误:" + e.getMessage();
        }
        Matcher m = Pattern.compile("ss\\d{1,9}").matcher(HTMLBody);
        if (m.find()) return m.group();
        else return "错误:ep有误";
    }

    private static String sid2EpJson(String season_id) {//字符串形式是因为直接从文本框中获取
        String HTMLBody;
        try {//获得番剧数据
            HTMLBody = Objects.requireNonNull(okHttpClient.newCall(new Request.Builder()
                    .url(Api.getSeriesInfoURL(Long.valueOf(season_id)))
                    .build()).execute().body()).string();
        } catch (IOException | NullPointerException e) {
            return "错误:" + e.getMessage();
        }
        return HTMLBody;
    }

    private static String aid2VideoJson(String aid, int page) {
        String HTMLBody;
        try {//获得番剧数据
            if (page > 0) {//av开头的id, 获取的是视频
                HTMLBody = Objects.requireNonNull(okHttpClient.newCall(new Request.Builder()
                        .url(Api.getPageInfoURL(Long.valueOf(aid), page))
                        .build()).execute().body()).string();
            } else {//page=0表示使用BiliPlus API查pageInfo
                HTMLBody = Objects.requireNonNull(okHttpClient.newCall(new Request.Builder()
                        .url(Api.getPageInfoURL2(Long.valueOf(aid)))
                        .build()).execute().body()).string();
            }
        } catch (IOException | NullPointerException e) {
            return "错误:" + e.getMessage();
        }

        return HTMLBody;
    }

    /**
     * 将获得的JSON字符串转换成EpInfo
     *
     * @param season_id  剧集ID
     * @param seriesInfo 需要填充数据的VideoInfo
     * @return value[0]返回数据传输是否成功
     * value[1]返回出错信息
     */
    static String sid2EpInfo(String season_id, SeriesInfo seriesInfo) {
        try {
            JSONObject json = new JSONObject(sid2EpJson(season_id)).getJSONObject("result");
            seriesInfo.title = json.getString("title");
            seriesInfo.cover = json.getString("cover");
            JSONArray infoList = json.getJSONArray("episodes");
            seriesInfo.epInfo.clear();
            VideoType videoType;
            for (int i = 0; i < infoList.length(); i++) {
                JSONObject info = infoList.getJSONObject(i);

                if (seriesInfo.title.contains("僅") ||
                        info.getInt("episode_status") == 13 ||//会员番
                        info.getInt("episode_status") == 6)//付费番
                    videoType = VideoType.AreaAnime;//区域受限或者会员权限
                else videoType = VideoType.Anime;//episode_status=2
                seriesInfo.epInfo.add(new EpInfo(info.getLong("aid"),
                        info.getInt("page"),
                        info.getLong("ep_id"),
                        info.getLong("cid"),
                        info.getString("index"),
                        info.getString("index_title"),
                        info.getString("cover"), videoType));
            }
            return "成功:EpInfo完成";
        } catch (JSONException e) {
            return "错误:" + e.getMessage();
        }
    }

    static String aid2PageInfo(String aid, SeriesInfo seriesInfo) {
        try {
            JSONObject json = new JSONObject(aid2VideoJson(aid, 1));
            if (!json.has("title")) {//如果B站查不到再去BiliPlus查
                json = new JSONObject(aid2VideoJson(aid, 0));
                seriesInfo.title = json.getString("title");
                seriesInfo.cover = json.getString("pic");
                seriesInfo.epInfo.clear();
                Long tid = json.getLong("tid");
                JSONArray lists = json.getJSONArray("list");
                for (int i = 0; i < lists.length(); i++) {
                    JSONObject list = lists.getJSONObject(i);
                    seriesInfo.epInfo.add(new EpInfo(0, list.getInt("page"), tid, list.getLong("cid"), String.valueOf(i + 1), list.getString("part"), "vupload", VideoType.VipVideo));
                }
            } else {
                seriesInfo.title = json.getString("title");
                seriesInfo.cover = json.getString("pic");
                seriesInfo.epInfo.clear();
                int i = 1;
                int pages = json.getInt("pages");
                do {
                    seriesInfo.epInfo.add(new EpInfo(0, i, json.getLong("tid"), json.getLong("cid"), String.valueOf(i), json.getString("part"), json.getString("face"), VideoType.Video));
                    json = new JSONObject(aid2VideoJson(aid, ++i));
                } while (i <= pages);
            }
            return "成功:PageInfo完成";
        } catch (JSONException e) {
            return "错误:" + e.getMessage();
        }
    }

    private static String epInfo2EntryJson(SeriesInfo seriesInfo) {//要考虑AreaAnime和Anime
        EpInfo epInfo = seriesInfo.epInfo.get(seriesInfo.position);
        JSONObject entryJson = new JSONObject();
        try {
            entryJson.put("is_completed", (epInfo.videoType != VideoType.Anime) && (epInfo.videoType != VideoType.Video))
                    .put("total_bytes", seriesInfo.total_bytes)
                    .put("downloaded_bytes", (epInfo.videoType != VideoType.Anime) && (epInfo.videoType != VideoType.Video) ? seriesInfo.total_bytes : 0)
                    .put("title", seriesInfo.title)
                    .put("type_tag", SettingInfo.type_tag)
                    .put("cover", seriesInfo.cover)
                    .put("prefered_video_quality", SettingInfo.prefered_video_quality)
                    .put("guessed_total_bytes", 0)
                    .put("total_time_milli", seriesInfo.total_time_milli)
                    .put("danmaku_count", 3000)
                    .put("time_update_stamp", System.currentTimeMillis())
                    .put("time_create_stamp", System.currentTimeMillis());
            switch (epInfo.videoType) {
                case Anime:
                case AreaAnime:
                    entryJson.put("season_id", seriesInfo.season_id)
                            .put("source", new JSONObject()
                                    .put("av_id", epInfo.aid)
                                    .put("cid", epInfo.cid)
                                    .put("website", "bangumi")
                                    .put("webvideo_id", ""))
                            .put("ep", new JSONObject()
                                    .put("av_id", epInfo.aid)
                                    .put("page", epInfo.page)
                                    .put("danmaku", epInfo.cid)
                                    .put("cover", epInfo.cover)
                                    .put("episode_id", epInfo.ep_id)
                                    .put("index", epInfo.index)
                                    .put("index_title", epInfo.index_title)
                                    .put("from", "bangumi")
                                    .put("season_type", 1));
                    break;
                case Video:
                case VipVideo:
                    entryJson.put("avid", seriesInfo.season_id)
                            .put("spid", 0)
                            .put("season_id", 0)
                            .put("page_data", new JSONObject()
                                    .put("cid", epInfo.cid)
                                    .put("page", epInfo.page)
                                    .put("from", "vupload")
                                    .put("part", epInfo.index_title)
                                    .put("vid", "")
                                    .put("has_alias", false)
                                    .put("weblink", "")
                                    .put("tid", epInfo.ep_id));
            }
            return entryJson.toString();
        } catch (JSONException e) {
            return "错误:" + e.getMessage();
        }
    }

    private static String cid2IndexJson(SeriesInfo seriesInfo) {
        String returnValue = cid2DownSegInfo(seriesInfo);
        if (Objects.equals(returnValue.substring(0, 2), "错误")) return returnValue;
        //写index.json文件
        for (DownloadSegmentInfo info : seriesInfo.downloadSegmentInfo) {
            seriesInfo.total_time_milli += info.duration;
            seriesInfo.total_bytes += info.bytes;
        }
        JSONObject indexJson = new JSONObject();
        try {
            switch (seriesInfo.epInfo.get(seriesInfo.position).videoType) {
                case Anime:
                case AreaAnime:
                    indexJson.put("from", "bangumi");
                    break;
                case Video:
                case VipVideo:
                    indexJson.put("from", "vupload");
                    break;
            }
            indexJson.put("type_tag", SettingInfo.type_tag)
                    .put("description", SettingInfo.description)
                    .put("is_stub", false)
                    .put("psedo_bitrate", 0);
            JSONArray segment_list = new JSONArray();
            for (DownloadSegmentInfo info : seriesInfo.downloadSegmentInfo) {
                segment_list.put(new JSONObject().put("url", info.url)
                        .put("duration", info.duration)
                        .put("bytes", info.bytes)
                        .put("meta_url", ""));
            }
            indexJson.put("segment_list", segment_list);
            indexJson.put("parse_timestamp_milli", System.currentTimeMillis())
                    .put("available_period_milli", 0)
                    .put("local_proxy_type", 0)
                    .put("user_agent", "Bilibili Freedoooooom/MarkII")
                    .put("is_downloaded", false)
                    .put("is_resolved", true)
                    .put("player_codec_config_list", new JSONArray("[{\"use_list_player\":false,\"use_ijk_media_codec\":false,\"player\":\"IJK_PLAYER\"},{\"use_list_player\":false,\"use_ijk_media_codec\":false,\"player\":\"ANDROID_PLAYER\"}]"))
                    .put("time_length", seriesInfo.total_time_milli)
                    .put("marlin_token", "")
                    .put("video_codec_id", 7)
                    .put("video_project", true);
            return indexJson.toString();
        } catch (JSONException e) {
            return "错误:" + e.getMessage();
        }

    }

    private static String cid2DownSegInfo(SeriesInfo seriesInfo) {
        //获取下载路径信息
        String HTMLBody;
        try {//获得番剧数据
            EpInfo epInfo = seriesInfo.epInfo.get(seriesInfo.position);
            HTMLBody = Objects.requireNonNull(okHttpClient.newCall(new Request.Builder()
                    .url(Api.getMediaURL(epInfo.cid))
                    .build()).execute().body()).string();
        } catch (IOException | NullPointerException e) {
            return "错误:" + e.getMessage();
        }
        seriesInfo.downloadSegmentInfo.clear();
        DownloadSegmentInfo downSegInfo;
        try {
            JSONObject HTML = new JSONObject(HTMLBody);
            if (HTML.has("result") && HTML.getString("result").equals("error"))
                return "错误:需要Cookies或视频已删除";
            JSONArray jsonArray = HTML.getJSONArray("durl");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                downSegInfo = new DownloadSegmentInfo();
                downSegInfo.url = json.getString("url");
                downSegInfo.duration = json.getLong("length");
                downSegInfo.bytes = json.getLong("size");
                seriesInfo.downloadSegmentInfo.add(downSegInfo);
            }
            if(seriesInfo.downloadSegmentInfo.get(0).url.contains("acgvideo")) return "错误:链接需要跨域";
            else return "成功";
        } catch (JSONException e) {
            return "错误:" + e.getMessage();
        }
        /*/XML语言
            try {//not done
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(new StringReader(HTMLBody));
                DownloadSegmentInfo downSegInfo = new DownloadSegmentInfo();
                while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                    switch (parser.getEventType()) {
                        case XmlPullParser.START_TAG:
                            switch (parser.getName()) {
                                case "length":
                                    downSegInfo = new DownloadSegmentInfo();
                                    parser.nextToken();
                                    downSegInfo.duration = Long.parseLong(parser.getText());
                                    parser.nextToken();
                                    break;
                                case "size":
                                    parser.nextToken();
                                    downSegInfo.bytes = Long.parseLong(parser.getText());
                                    parser.nextToken();
                                    break;
                                case "result":
                                    return "错误:需要Cookies";
                                default:
                                    break;
                            }
                            break;
                        case XmlPullParser.CDSECT:
                            downSegInfo.url = parser.getText();
                            seriesInfo.downloadSegmentInfo.add(downSegInfo);
                            break;
                        default:
                            break;
                    }
                    parser.nextToken();
                }

                return "成功";
            } catch (XmlPullParserException | IOException e) {
                return "错误:" + e.getMessage();
            }
        */
    }

    static String downloadVideo(SeriesInfo seriesInfo) {//要考虑AreaAnime和Anime
        String returnValue;
        EpInfo epInfo = seriesInfo.epInfo.get(seriesInfo.position);
        String biliPath = SettingInfo.appPath + SettingInfo.GetLocalPath(ClientType.release);
        String seriesPath, epPath;
        switch (epInfo.videoType) {
            case Anime:
            case AreaAnime:
                //番剧目录
                seriesPath = biliPath + "s_" + seriesInfo.season_id + "/";
                //剧集目录
                epPath = seriesPath + epInfo.ep_id + "/";
                break;
            case Video:
            case VipVideo:
            default:
                //视频目录
                seriesPath = biliPath + seriesInfo.season_id + "/";
                //分页目录
                epPath = seriesPath + epInfo.page + "/";
                break;
        }
        //剧集视频文件夹
        String videoPath = epPath + SettingInfo.type_tag + "/";
        CreateFolder(videoPath);
        //剧集entry.json文件
        String entryJsonPath = epPath + "entry.json";
        CreateFile(entryJsonPath);
        //剧集弹幕文件
        String danmuPath = epPath + "danmaku.xml";
        CreateFile(danmuPath);
        //剧集视频index.json
        String indexjsonPath = videoPath + "index.json";
        CreateFile(indexjsonPath);
        //写文件
        seriesInfo.total_bytes = 0;
        seriesInfo.total_time_milli = 0;
        if ((epInfo.videoType != VideoType.Anime) && (epInfo.videoType != VideoType.Video)) { //AreaAnime和VipVideo需要手动下载文件
            returnValue = WriteFileContent(indexjsonPath, cid2IndexJson(seriesInfo));
            if (Objects.equals(returnValue.substring(0, 2), "错误")) return returnValue;
            returnValue = WriteFileContent(entryJsonPath, epInfo2EntryJson(seriesInfo));
            if (Objects.equals(returnValue.substring(0, 2), "错误")) return returnValue;
            for (int i = 0; i < seriesInfo.downloadSegmentInfo.size(); i++) {
                //写blv.4m.sum
                String sumPath = videoPath + i + ".blv.4m.sum";
                CreateFile(sumPath);
                WriteFileContent(sumPath, "{\"length\":" + seriesInfo.downloadSegmentInfo.get(i).bytes + "}");
            }
            //创建视频文件
            return videoPath;
        } else {//Episode文件创建成功
            returnValue = WriteFileContent(entryJsonPath, epInfo2EntryJson(seriesInfo));
            if (Objects.equals(returnValue.substring(0, 2), "错误")) return returnValue;
            return "成功:在客户端创建";
        }
    }

    private static void CreateFile(String path) {
        File file = new File(path);
        if (file.exists()) file.delete();
        else {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void CreateFolder(String path) {
        File _dirv = new File(path);
        if (!_dirv.exists()) _dirv.mkdirs();
    }

    private static String WriteFileContent(String path, String content) {
        if (Objects.equals(content.substring(0, 2), "错误")) return content;
        File file = new File(path);
        try {
            FileWriter writer = new FileWriter(file);
            writer.append(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            return "错误:" + e.getMessage();
        }
        return "成功";
    }


}
