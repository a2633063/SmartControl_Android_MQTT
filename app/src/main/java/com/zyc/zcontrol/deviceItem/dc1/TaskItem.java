package com.zyc.zcontrol.deviceItem.dc1;

import android.content.Context;

import com.zyc.Function;


public class TaskItem {

    public TaskItem(Context context) {
        this.context = context;
    }

    private Context context;

    public int hour = 0;      //小时
    public int minute = 0;    //分钟
    public int repeat = 0; //全部为0:一次   bit6-0:周日-周一
    public int action = 1;    //动作
    public int on = 0;    //开关

    private void init() {
    }

    public String getTime() {
        return String.format("%02d:%02d", hour, minute);
    }

    public String getAction() {
        return action != 0 ? "打开" : "关闭";
    }

    public boolean getOn() {
        return on != 0;
    }

    public String getRepeat() {
        return Function.getWeek(repeat);
    }


}
