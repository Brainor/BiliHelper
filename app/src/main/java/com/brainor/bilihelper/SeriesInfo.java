package com.brainor.bilihelper;

import java.util.ArrayList;
import java.util.Objects;

import androidx.annotation.NonNull;

/**
 * 存储数据的结构体
 * 对于番剧来说是SeriesInfo
 * 对于视频来说是VideoInfo
 */
public class SeriesInfo {
    long season_id;//video, avid
    public String title;//video
    String cover;//video, pic
    /**
     * 对于番剧是epInfo
     * 对于视频是pageInfo, 共pages个元素
     */
    ArrayList<EpInfo> epInfo = new ArrayList<>();

    int position;//选择剧集的序号
    long total_time_milli;//总长度
    long total_bytes;//总大小
    ArrayList<DownloadSegmentInfo> downloadSegmentInfo = new ArrayList<>();//下载链接信息, 用于无法客户端下载的番剧
}

class EpInfo {
    long aid;
    Integer page;//video
    String cover;//video, from
    long cid;//video
    long ep_id;//video, tid
    String index;
    String index_title;//video, part

    VideoType videoType;

    @NonNull
    @Override
    public String toString() {
        return index + "\t" + index_title;
    }

    EpInfo(long aid, Integer page, long ep_id, long cid, String index, String index_title, String cover, VideoType videoType) {
        this.aid = aid;
        this.page = page;
        this.ep_id = ep_id;
        this.index = index;
        this.index_title = index_title;
        this.cid = cid;
        this.cover = cover;
        this.videoType = videoType;
    }
}

enum ClientType {
    release("tv.danmaku.bili/download/"),//正式版
    blue("com.bilibili.app.blue/download/"),//概念版
    play("com.bilibili.app.in/download/");//Play商店版
    String appPath;

    ClientType(String appPath) {
        this.appPath = appPath;
    }
}

enum VideoType {
    Anime,//普通动漫
    AreaAnime,//地区受限动漫
    Video, //普通视频
    VipVideo,//B站不存在的视频, 在BiliPlus下载
    Movie, //电影
    VipAnime,//大会员动漫
    VipMovie, //大会员电影
}

class DownloadSegmentInfo {
    String url;
    long duration = 0;
    long bytes = 0;
}

class HistoryInfo {
    String title;
    String url;
    int position;

    HistoryInfo(String title, String url, int position) {
        this.title = title;
        this.url = url;
        this.position = position;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HistoryInfo)) return false;
        return Objects.equals(this.title, ((HistoryInfo) obj).title) && Objects.equals(this.url, ((HistoryInfo) obj).url);
    }
}

enum VideoQuality {
    _1("lua.flv360.bili2api.15", 15, "流畅 360P"), _2("lua.flv480.bili2api.32", 32, "清晰 480P"), _3("lua.flv720.bili2api.64", 64, "高清 720P"), _4("lua.flv.bili2api.80", 80, "高清 1808P"), _5("lua.hdflv2.bb2api.bd", 112, "高清 1080P+");
    String type_tag, description;
    int prefered_video_quality;

    VideoQuality(String type_tag, int prefered_video_quality, String description) {
        this.type_tag = type_tag;
        this.prefered_video_quality = prefered_video_quality;
        this.description = description;
    }

    static VideoQuality[] list = VideoQuality.values();

    public static String[] getEntries() {
        String[] entries = new String[list.length];
        for (int i = 0; i < list.length; i++) entries[i] = list[i].description;
        return entries;
    }
}