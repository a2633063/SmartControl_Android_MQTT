package com.zyc.zcontrol.deviceItem.DeviceClass;

import android.preference.PreferenceFragment;

import androidx.annotation.DrawableRes;
import androidx.fragment.app.Fragment;

import com.espressif.ESPtouchActivity;
import com.zyc.zcontrol.R;
import com.zyc.zcontrol.deviceItem.a1.A1LinkActivity;
import com.zyc.zcontrol.deviceItem.clock.ClockLinkActivity;
import com.zyc.zcontrol.deviceItem.dc1.DC1LinkActivity;
import com.zyc.zcontrol.deviceItem.m1.M1LinkActivity;
import com.zyc.zcontrol.deviceItem.s7.S7LinkActivity;
import com.zyc.zcontrol.deviceItem.tc1.TC1LinkActivity;
import com.zyc.zcontrol.deviceItem.mops.MOPSLinkActivity;


public class Device {


    //region 静态变量
    public final static int TYPE_UNKNOWN = -1;
    public final static int TYPE_BUTTON_MATE = 0;
    public final static int TYPE_TC1 = 1;
    public final static int TYPE_DC1 = 2;
    public final static int TYPE_A1 = 3;
    public final static int TYPE_M1 = 4;
    public final static int TYPE_S7 = 5;
    public final static int TYPE_CLOCK = 6;
    public final static int TYPE_MOPS = 7;
    public final static int TYPE_RGBW = 8;
    public final static int TYPE_CLOCK_MATRIX = 9;
    public final static int TYPE_COUNT = 10;

    //设备名称
    public final static String[] TypeName = new String[]{
            "按键伴侣",//0
            "zTC1智能排插",//1
            "zDC1智能排插",   //2
            "zA1空气净化器", //3
            "zM1空气检测仪", //4
            "zS7/zS7pe体重秤",   //5
            "zClock时钟",    //6
            "zMOPS插座",   //7
            "zRGBW灯",   //8
            "zClock点阵时钟",   //9
    };
    //设备链接地址
    public final static String[] TypeUri = new String[]{
            "https://github.com/a2633063/SmartControl_ButtonMate_ESP8266",//0
            "https://github.com/a2633063/zTC1",//1
            "https://github.com/a2633063/zDC1",   //2
            "https://github.com/a2633063/zA1", //3
            "https://github.com/a2633063/zM1", //4
            "https://github.com/a2633063/zS7",   //5
            "https://github.com/a2633063/zClock",    //6
            "https://github.com/a2633063/zMOPS",   //7
            "https://github.com/a2633063/zRGBW",   //8
            "https://github.com/a2633063/zClock_Matrix",   //9
    };
    //设备图标
    public final static @DrawableRes
    int Type_Icon[] = {
            R.drawable.device_icon_diy,//0
            R.drawable.device_icon_ztc1,//1
            R.drawable.device_icon_zdc1,//2
            R.drawable.device_icon_za1,//3
            R.drawable.device_icon_zm1,//4
            R.drawable.device_icon_zs7,//5
            R.drawable.device_icon_zclock,//6
            R.drawable.device_icon_zmops,//7
            R.drawable.device_icon_zrgbw,//8
            R.drawable.device_icon_ongoing,//9
    };

    //设备对应配对页面
    public final static Class LinkActivity[] =
            {
                    ESPtouchActivity.class,
                    TC1LinkActivity.class,
                    DC1LinkActivity.class,
                    A1LinkActivity.class,
                    M1LinkActivity.class,
                    S7LinkActivity.class,
                    ClockLinkActivity.class,
                    MOPSLinkActivity.class,
                    null,
                    null,
            };
//endregion

    protected int type = TYPE_UNKNOWN;
    private String name;
    private String mac;
    private String ip;
    private String group;
    private boolean online;
//    private String version=null;
//    private String ssid=null;

    //region gitter and setter
    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMac() {
        return mac;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }


    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

//    public String getVersion() {
//        return version;
//    }
//
//    public void setVersion(String version) {
//        this.version = version;
//    }
//
//    public String getSsid() {
//        return ssid;
//    }
//
//    public void setSsid(String ssid) {
//        this.ssid = ssid;
//    }

//endregion


    public Device(int type, String name, String mac) {
        this.name = name;
        this.mac = mac;
        this.type = type;
    }

    public String getTypeName() {
        return TypeName[type];
    }

    public int getIcon() {
        return Type_Icon[type];
    }

    //region 子类必须重构函数
    public String getDocUri() {
        return TypeUri[type];
    }


    public String[] getRecvMqttTopic() {
        return null;
    }

    public String getSendMqttTopic() {
        return null;
    }

    Fragment fragment;

    public Fragment getFragment() {
        return fragment;
    }

    public PreferenceFragment getSettingFragment() {
        return null;
    }
    //endregion

}
