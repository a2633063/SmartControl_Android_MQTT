package com.zyc.zcontrol.controlItem.tc1;

import android.content.Context;


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
        String str = "";
        String Week[] = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        repeat &= 0x7f;
        if (repeat == 0) str = "一次";
        else if ((repeat & 0x7f) == 0x7f) str = "每天";
        else {
            for (int i = 0; i < 7; i++) {
                if ((repeat & (1 << i)) != 0) {
                    str = str + "," + Week[i];
                }
            }
            str = str.replaceFirst(",", "");
        }

        return str;
    }


}
