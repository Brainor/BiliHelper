package com.brainor.bilihelper;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Objects;

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
    release,//正式版
    blue,//概念版
    play//Play商店版
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

    HistoryInfo(String title, String url) {
        this.title = title;
        this.url = url;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HistoryInfo)) return false;
        return Objects.equals(this.title, ((HistoryInfo) obj).title) && Objects.equals(this.url, ((HistoryInfo) obj).url);
    }
}