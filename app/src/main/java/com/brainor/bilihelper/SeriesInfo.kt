package com.brainor.bilihelper

import java.util.ArrayList

/**
 * 存储数据的结构体
 * 对于番剧来说是SeriesInfo
 * 对于视频来说是VideoInfo
 */
class SeriesInfo {
    var seasonId: Long = 0//video, avid
    var title: String = ""//video
    var cover: String = ""//video, pic
    /**
     * 对于番剧是epInfo
     * 对于视频是pageInfo, 共pages个元素
     */
    var epInfo = ArrayList<EpInfo>()

    var position: Int = 0//选择剧集的序号
    var totalTimeMilli: Long = 0//总长度
    var totalBytes: Long = 0//总大小
    var downloadSegmentInfo = ArrayList<DownloadSegmentInfo>()//下载链接信息, 用于无法客户端下载的番剧

}

class EpInfo(var aid: Long, val page: Int//video
             , val ep_id: Long//video, tid
             , val cid: Long//video
             , val index: String, val index_title: String//video, part
             , val cover: String//video, from
             , val videoType: VideoType) {

    override fun toString(): String {
        return index + "\t" + index_title
    }
}

enum class ClientType(var packageName: String) {
    Release("tv.danmaku.bili"), //正式版
    Blue("com.bilibili.app.blue"), //概念版
    Play("com.bilibili.app.in");

    //Play商店版
    var appPath: String = "$packageName/download/"

    companion object {
        val entries: Array<String>
            //get() = Stream.of(*ClientType.values()).map { cT -> cT.packageName }.toArray(String[]::new  /* Currently unsupported in Kotlin */)
            get() = ClientType.values().map { cT -> cT.packageName }.toTypedArray()
    }
}

enum class VideoType {
    Anime, //普通动漫
    AreaAnime, //地区受限动漫
    Video, //普通视频
    VipVideo, //B站不存在的视频, 在BiliPlus下载
    Movie, //电影
    VipAnime, //大会员动漫
    VipMovie
    //大会员电影
}

class DownloadSegmentInfo {
    var url: String = ""
    var duration: Long = 0
    var bytes: Long = 0
}

class HistoryInfo(var title: String, var url: String, var position: Int) {

    override fun equals(other: Any?): Boolean {
        return if (other !is HistoryInfo) false else this.title == other.title && this.url == other.url
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + url.hashCode()
        return result
    }
}

enum class VideoQuality constructor(var type_tag: String, var prefered_video_quality: Int, var description: String) {
    T1("lua.flv360.bili2api.15", 15, "流畅 360P"),
    T2("lua.flv480.bili2api.32", 32, "清晰 480P"),
    T3("lua.flv720.bili2api.64", 64, "高清 720P"),
    T4("lua.flv.bili2api.80", 80, "高清 1808P"),
    T5("lua.hdflv2.bb2api.bd", 112, "高清 1080P+");


    companion object {
        val entries: Array<String>
            //get() = Stream.of(*VideoQuality.values()).map { vQ -> vQ.description }.toArray(String[]::new  /* Currently unsupported in Kotlin */)
            get() = VideoQuality.values().map { vQ -> vQ.description }.toTypedArray()
    }
}