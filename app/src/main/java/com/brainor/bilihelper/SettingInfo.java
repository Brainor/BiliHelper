package com.brainor.bilihelper;


//从网上获取信息
class SettingInfo {
    static String appPath = "/storage/emulated/0/Android/data/";
    static String type_tag = "lua.flv.bb2api.80";
    static int prefered_video_quality = 80;
    static String description = "超清";

    static String GetLocalPath(ClientType clientType) {//不同版本数据安装的文件夹位置
        switch (clientType) {
            case blue:
                return "com.bilibili.app.blue/download/";
            case play:
                return "com.bilibili.app.in/download/";
            case release:
            default:
                return "tv.danmaku.bili/download/";
        }
    }

    public void GetVideoQuality(int q) {
        switch (q) {
            case 4:
                type_tag = "lua.hdflv2.bb2api.bd";
                prefered_video_quality = 112;
                description = "1080P";
                break;
            case 3:
                type_tag = "lua.flv.bb2api.80";
                prefered_video_quality = 80;
                description = "超清";
                break;
            case 2:
                type_tag = "lua.flv720.bb2api.64";
                prefered_video_quality = 64;
                description = "高清";
                break;
            case 1:
            default:
                type_tag = "lua.mp4.bb2api.16";
                prefered_video_quality = 16;
                description = "清晰";
                break;
        }
    }

}
