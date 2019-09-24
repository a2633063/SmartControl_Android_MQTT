package com.zyc;

import android.support.annotation.DrawableRes;

import com.zyc.zcontrol.R;

/**
 * Created by Zip on 2017/5/19.
 * 静态变量
 */

public class StaticVariable {
    //静态变量
    public final static int TYPE_UNKNOWN =-1;
    public final static int TYPE_BUTTON_MATE=0;
    public final static int TYPE_TC1=1;
    public final static int TYPE_DC1=2;
    public final static int TYPE_A1=3;
    public final static int TYPE_M1=4;
//    public final static int TYPE_BUTTON=5;
    public final static int TYPE_RGB=5;
    public final static int TYPE_CLOCK=6;
    public final static int TYPE_COUNT=7;

    public final static String TYPE_NAME[]={
            "按键伴侣",//0
            "智能排插zTC1",//1
            "智能排插zDC1",   //2
            "空气净化器zA1", //3
            "空气检测仪zM1", //4
            "RGB灯",   //
            "时钟",    //
            // "智能按键",//1
    };

    public final static @DrawableRes
    int  TYPE_ICON[]={
            R.drawable.ic_filter_1_black_24dp,//0
            R.drawable.ic_filter_2_black_24dp,//1
            R.drawable.ic_filter_3_black_24dp,//2
            R.drawable.ic_filter_4_black_24dp,//3
            R.drawable.ic_filter_5_black_24dp,//4
            R.drawable.ic_filter_9_plus_black_24dp,//4
            R.drawable.ic_filter_9_plus_black_24dp,//4
    };


}
