package com.zyc.zcontrol.deviceItem.rgbw;

import android.content.Context;

import com.zyc.Function;


public class TaskItem {

    public TaskItem(Context context) {
        this.context = context;
    }

    private Context context;

    public int on = 0;    //开关
    public int hour = 0;      //小时
    public int minute = 0;    //分钟
    public int repeat = 0x7f;   //重复
    public int[] rgb = new int[4];   //目标亮度颜色
    public int gradient = 0;    //渐变效果


    //public int action = 4;    //动作


    private void init() {
    }

    public String getTime() {
        return String.format("%02d:%02d", hour, minute);
    }

    public int getColor() {
        if (rgb[3] < 0x10 && (rgb[0] > 0 || rgb[1] > 0 || rgb[2] > 0))
            return 0xff000000 + rgb[0] * 0x10000 + rgb[1] * 0x100 + rgb[2];
        return 0xff000000 + rgb[3] * 0x10000 + rgb[3] * 0x100 + rgb[3];
    }

    public String getRGBW() {
        return "[" + rgb[0] + "," + rgb[1] + "," + rgb[2] + "," + rgb[3] + "]";
    }

    public boolean getOn() {
        return on != 0;
    }

    public String getRepeat() {
        return Function.getWeek(repeat);
    }

}
