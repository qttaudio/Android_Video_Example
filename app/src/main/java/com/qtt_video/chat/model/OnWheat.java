package com.qtt_video.chat.model;

import android.graphics.drawable.Drawable;

/**
 * 上麦信息
 */
public class OnWheat {
    /*用户id*/
    public long uid;
    /*静音*/
    public boolean isMute;
    /*头像*/
    public Drawable headerImage;
    public int volumeValue;
    public boolean muteAudio;




    @Override
    public String toString() {
        return "OnWheatBean{" +
                "uid=" + uid +
                ", isMute=" + isMute +
                ", headerImage=" + headerImage +
                '}';
    }
}
