package com.zyc.zcontrol.deviceItem.rgbw;

import android.content.Context;


public class TaskItem {

    public TaskItem(Context context) {
        this.context = context;
    }

    private Context context;

    public int hour = 0;      //小时
    public int minute = 0;    //分钟
    public int action = 4;    //动作
    public int on = 0;    //开关

    private void init() {
    }

    public String getTime() {
        return String.format("%02d:%02d", hour, minute);
    }

    public String getAction() {
        return action != 0 ? "亮度:" + action : "关屏";
    }

    public boolean getOn() {
        return on != 0;
    }


}
