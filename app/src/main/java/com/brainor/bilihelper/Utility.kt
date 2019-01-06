package com.brainor.bilihelper

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException

internal object Utility {
    /**
     * 将ep_id转成season_id
     *
     * @param id id
     * @return value[0]返回数据传输是否成功
     * value[1]返回出错信息或者ss+seasonId
     */
    fun ep2sid(id: String): String {
        val htmlBody = Api.getSeasonIdURL(id)
        val matchResult = Regex("""ss\d{1,9}""").find(htmlBody)
        return matchResult?.value ?: "错误:ep有误"
    }

    /**
     * 将获得的JSON字符串转换成EpInfo
     *
     * @param season_id  剧集ID
     * @param seriesInfo 需要填充数据的VideoInfo
     * @return value[0]返回数据传输是否成功
     * value[1]返回出错信息
     */
    fun sid2EpInfo(season_id: String, seriesInfo: SeriesInfo): String {
        try {
            val json = JSONObject(Api.getSeriesInfoURL(season_id)).getJSONObject("result")
            seriesInfo.title = json.getString("title")
            seriesInfo.cover = json.getString("cover")
            val infoList = json.getJSONArray("episodes")
            seriesInfo.epInfo.clear()
            var videoType: VideoType
            for (i in 0 until infoList.length()) {
                val info = infoList.getJSONObject(i)

                videoType = if (seriesInfo.title.contains("僅") ||
                        info.getInt("episode_status") == 13 ||//会员番
                        info.getInt("episode_status") == 6)//付费番
                    VideoType.AreaAnime//区域受限或者会员权限
                else VideoType.Anime//episode_status=2
                seriesInfo.epInfo.add(EpInfo(info.getLong("aid"),
                        info.getInt("page"),
                        info.getLong("ep_id"),
                        info.getLong("cid"),
                        info.getString("index"),
                        info.getString("index_title"),
                        info.getString("cover"), videoType))
            }
            return "成功:EpInfo完成"
        } catch (e: JSONException) {
            return "错误:" + e.message
        }

    }

    fun aid2PageInfo(aid: String, seriesInfo: SeriesInfo): String {
        try {
            var json = JSONObject(Api.getPageInfoURL(aid, 1))
            if (!json.has("title")) {//如果B站查不到再去BiliPlus查
                json = JSONObject(Api.getPageInfoURL(aid, 0))
                seriesInfo.title = json.getString("title")
                seriesInfo.cover = json.getString("pic")
                seriesInfo.epInfo.clear()
                val tid = json.getLong("tid")
                val lists = json.getJSONArray("list")
                for (i in 0 until lists.length()) {
                    val list = lists.getJSONObject(i)
                    seriesInfo.epInfo.add(EpInfo(0, list.getInt("page"), tid, list.getLong("cid"), (i + 1).toString(), list.getString("part"), "vupload", VideoType.VipVideo))
                }
            } else {
                seriesInfo.title = json.getString("title")
                seriesInfo.cover = json.getString("pic")
                seriesInfo.epInfo.clear()
                var i = 1
                val pages = json.getInt("pages")
                do {
                    seriesInfo.epInfo.add(EpInfo(0, i, json.getLong("tid"), json.getLong("cid"), i.toString(), json.getString("part"), json.getString("face"), VideoType.Video))
                    json = JSONObject(Api.getPageInfoURL(aid, ++i))
                } while (i <= pages)
            }
            return "成功:PageInfo完成"
        } catch (e: JSONException) {
            return "错误:" + e.message
        }

    }

    private fun epInfo2EntryJson(seriesInfo: SeriesInfo): String {//要考虑AreaAnime和Anime
        val epInfo = seriesInfo.epInfo[seriesInfo.position]
        val entryJson = JSONObject()
        try {
            entryJson.put("is_completed", epInfo.videoType !== VideoType.Anime && epInfo.videoType !== VideoType.Video)
                    .put("total_bytes", seriesInfo.totalBytes)
                    .put("downloaded_bytes", if (epInfo.videoType !== VideoType.Anime && epInfo.videoType !== VideoType.Video) seriesInfo.totalBytes else 0)
                    .put("title", seriesInfo.title)
                    .put("type_tag", Settings.videoQuality.type_tag)
                    .put("cover", seriesInfo.cover)
                    .put("prefered_video_quality", Settings.videoQuality.prefered_video_quality)
                    .put("guessed_total_bytes", 0)
                    .put("total_time_milli", seriesInfo.totalTimeMilli)
                    .put("danmaku_count", 3000)
                    .put("time_update_stamp", System.currentTimeMillis())
                    .put("time_create_stamp", System.currentTimeMillis())
            when (epInfo.videoType) {
                VideoType.Anime, VideoType.AreaAnime -> entryJson.put("season_id", seriesInfo.seasonId.toString())
                        .put("source", JSONObject()
                                .put("av_id", epInfo.aid)
                                .put("cid", epInfo.cid)
                                .put("website", "bangumi")
                                .put("webvideo_id", ""))
                        .put("ep", JSONObject()
                                .put("av_id", epInfo.aid)
                                .put("page", epInfo.page)
                                .put("danmaku", epInfo.cid)
                                .put("cover", epInfo.cover)
                                .put("episode_id", epInfo.ep_id)
                                .put("index", epInfo.index)
                                .put("index_title", epInfo.index_title)
                                .put("from", "bangumi")
                                .put("season_type", 1))
                VideoType.Video, VideoType.VipVideo -> entryJson.put("avid", seriesInfo.seasonId)
                        .put("spid", 0)
                        .put("season_id", 0)
                        .put("page_data", JSONObject()
                                .put("cid", epInfo.cid)
                                .put("page", epInfo.page)
                                .put("from", "vupload")
                                .put("part", epInfo.index_title)
                                .put("vid", "")
                                .put("has_alias", false)
                                .put("weblink", "")
                                .put("tid", epInfo.ep_id))
                VideoType.Movie, VideoType.VipAnime, VideoType.VipMovie -> {
                }
            }
            return entryJson.toString()
        } catch (e: JSONException) {
            return "错误:" + e.message
        }

    }

    private fun cid2IndexJson(seriesInfo: SeriesInfo): String {
        val returnValue = cid2DownSegInfo(seriesInfo)
        if (returnValue.substring(0, 2) == "错误") return returnValue
        //写index.json文件
        for (info in seriesInfo.downloadSegmentInfo) {
            seriesInfo.totalTimeMilli = seriesInfo.totalTimeMilli + info.duration
            seriesInfo.totalBytes = seriesInfo.totalBytes + info.bytes
        }
        val indexJson = JSONObject()
        try {
            when (seriesInfo.epInfo[seriesInfo.position].videoType) {
                VideoType.Anime, VideoType.AreaAnime -> indexJson.put("from", "bangumi")
                VideoType.Video, VideoType.VipVideo -> indexJson.put("from", "vupload")
                VideoType.Movie, VideoType.VipMovie, VideoType.VipAnime -> {
                }
            }
            indexJson.put("type_tag", Settings.videoQuality.type_tag)
                    .put("description", Settings.videoQuality.description)
                    .put("is_stub", false)
                    .put("psedo_bitrate", 0)
            val segmentList = JSONArray()
            for (info in seriesInfo.downloadSegmentInfo) {
                segmentList.put(JSONObject().put("url", info.url)
                        .put("duration", info.duration)
                        .put("bytes", info.bytes)
                        .put("meta_url", ""))
            }
            indexJson.put("segment_list", segmentList)
            indexJson.put("parse_timestamp_milli", System.currentTimeMillis())
                    .put("available_period_milli", 0)
                    .put("local_proxy_type", 0)
                    .put("user_agent", "Bilibili Freedoooooom/MarkII")
                    .put("is_downloaded", false)
                    .put("is_resolved", true)
                    .put("player_codec_config_list", JSONArray("""[{"use_list_player":false,"use_ijk_media_codec":false,"player":"IJK_PLAYER"},{"use_list_player":false,"use_ijk_media_codec":false,"player":"ANDROID_PLAYER"}]"""))
                    .put("time_length", seriesInfo.totalTimeMilli)
                    .put("marlin_token", "")
                    .put("video_codec_id", 7)
                    .put("video_project", true)
            return indexJson.toString()
        } catch (e: JSONException) {
            return "错误:" + e.message
        }

    }

    private fun cid2DownSegInfo(seriesInfo: SeriesInfo): String {
        //获取下载路径信息
        val htmlBody = Api.getMediaURL(seriesInfo.epInfo[seriesInfo.position].cid, Settings.videoQuality.prefered_video_quality)
        seriesInfo.downloadSegmentInfo.clear()
        var downSegInfo: DownloadSegmentInfo
        val serverHost = arrayOf("video-us.biliplus.com:17020", "video-bg.biliplus.com:13120", "video-sg.biliplus.com:1520", "us.biliplus-vid.top", "bg.biliplus-vid.top", "sg.biliplus-vid.top")
        try {
            val html = JSONObject(htmlBody)
            if (html.has("result") && html.getString("result") == "error")
                return "错误:需要Cookies或视频已删除"
            val jsonArray = html.getJSONArray("durl")
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                downSegInfo = DownloadSegmentInfo()
                downSegInfo.url = json.getString("url")
                downSegInfo.url = downSegInfo.url.replace(serverHost[2], serverHost[1]).replace(serverHost[5], serverHost[4])//新加坡的更快点
                downSegInfo.duration = json.getLong("length")
                downSegInfo.bytes = json.getLong("size")
                seriesInfo.downloadSegmentInfo.add(downSegInfo)
            }
            return if (seriesInfo.downloadSegmentInfo[0].url.contains("acgvideo"))
                "错误:链接需要跨域"
            else
                "成功"
        } catch (e: JSONException) {
            return "错误:" + e.message
        }

    }

    fun downloadVideo(seriesInfo: SeriesInfo): String {//要考虑AreaAnime和Anime
        var returnValue: String
        val epInfo = seriesInfo.epInfo[seriesInfo.position]
        val biliPath = Settings.rootPath + Settings.clientType.appPath
        val seriesPath: String
        val epPath: String
        when (epInfo.videoType) {
            VideoType.Anime, VideoType.AreaAnime -> {
                //番剧目录
                seriesPath = "${biliPath}s_${seriesInfo.seasonId}/"
                //剧集目录
                epPath = "$seriesPath${epInfo.ep_id}/"
            }
            VideoType.Video, VideoType.VipVideo -> {
                //视频目录
                seriesPath = "$biliPath${seriesInfo.seasonId}/"
                //分页目录
                epPath = "$seriesPath${epInfo.page}/"
            }
            else -> {
                seriesPath = "$biliPath${seriesInfo.seasonId}/"
                epPath = "$seriesPath${epInfo.page}/"
            }
        }
        //剧集视频文件夹
        val videoPath = "$epPath${Settings.videoQuality.type_tag}/"
        createFolder(videoPath)
        //剧集entry.json文件
        val entryJsonPath = "${epPath}entry.json"
        createFile(entryJsonPath)
        //剧集弹幕文件
        val danmuPath = "${epPath}danmaku.xml"
        createFile(danmuPath)
        //剧集视频index.json
        val indexjsonPath = "${videoPath}index.json"
        createFile(indexjsonPath)
        //写文件
        seriesInfo.totalBytes = 0
        seriesInfo.totalTimeMilli = 0
        if (epInfo.videoType !== VideoType.Anime && epInfo.videoType !== VideoType.Video) { //AreaAnime和VipVideo需要手动下载文件
            returnValue = writeFileContent(indexjsonPath, cid2IndexJson(seriesInfo))
            if (returnValue.substring(0, 2) == "错误") return returnValue
            returnValue = writeFileContent(entryJsonPath, epInfo2EntryJson(seriesInfo))
            if (returnValue.substring(0, 2) == "错误") return returnValue
            for (i in 0 until seriesInfo.downloadSegmentInfo.size) {
                //写blv.4m.sum
                val sumPath = "$videoPath$i.blv.4m.sum"
                createFile(sumPath)
//                writeFileContent(sumPath, "{\"length\":" + seriesInfo.downloadSegmentInfo[i].bytes + "}")
                writeFileContent(sumPath, """{"length":${seriesInfo.downloadSegmentInfo[i].bytes}}""")
            }
            //创建视频文件
            return videoPath
        } else {//Episode文件创建成功
            returnValue = writeFileContent(entryJsonPath, epInfo2EntryJson(seriesInfo))
            return if (returnValue.substring(0, 2) == "错误") returnValue else "成功:在客户端创建"
        }
    }

    private fun createFile(path: String) {
        val file = File(path)
        if (file.exists())
            file.delete()
        else {
            try {
                file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun createFolder(path: String) {
        val dirv = File(path)
        if (!dirv.exists()) dirv.mkdirs()
    }

    private fun writeFileContent(path: String, content: String): String {
        if (content.substring(0, 2) == "错误") return content
        val file = File(path)
        try {
            val writer = FileWriter(file)
            writer.append(content)
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            return "错误:" + e.message
        }
        return "成功"
    }
}
