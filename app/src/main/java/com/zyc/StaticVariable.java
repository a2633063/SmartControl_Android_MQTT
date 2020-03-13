package com.zyc;

import androidx.annotation.DrawableRes;

import com.zyc.zcontrol.R;

/**
 * Created by Zip on 2017/5/19.
 * 静态变量
 */

public class StaticVariable {
    //静态变量
    public final static int TYPE_UNKNOWN = -1;
    public final static int TYPE_BUTTON_MATE = 0;
    public final static int TYPE_TC1 = 1;
    public final static int TYPE_DC1 = 2;
    public final static int TYPE_A1 = 3;
    public final static int TYPE_M1 = 4;
    //    public final static int TYPE_BUTTON=5;
    public final static int TYPE_RGBW = 5;
    public final static int TYPE_CLOCK = 6;
    public final static int TYPE_COUNT = 7;

    public final static String TYPE_NAME[] = {
            "按键伴侣",//0
            "智能排插zTC1",//1
            "智能排插zDC1",   //2
            "空气净化器zA1", //3
            "空气检测仪zM1", //4
            "zRGBW灯",   //5
            "时钟",    //
            // "智能按键",//1
    };

    public final static @DrawableRes
    int TYPE_ICON[] = {
            R.drawable.device_icon_diy,//0
            R.drawable.device_icon_ztc1,//1
            R.drawable.device_icon_zdc1,//2
            R.drawable.device_icon_za1,//3
            R.drawable.device_icon_zm1,//4
            R.drawable.device_icon_ongoing,//5
            R.drawable.device_icon_ongoing,//4
    };
}
