package com.zyc.zcontrol.deviceItem.DeviceClass;

import android.preference.PreferenceFragment;

import androidx.annotation.DrawableRes;
import androidx.fragment.app.Fragment;

import com.espressif.ESPtouchActivity;
import com.zyc.zcontrol.R;
import com.zyc.zcontrol.deviceItem.a1.A1LinkActivity;
import com.zyc.zcontrol.deviceItem.dc1.DC1LinkActivity;
import com.zyc.zcontrol.deviceItem.m1.M1LinkActivity;
import com.zyc.zcontrol.deviceItem.tc1.TC1LinkActivity;


public class Device {

    //region 静态变量
    public final static int TYPE_UNKNOWN = -1;
    public final static int TYPE_BUTTON_MATE = 0;
    public final static int TYPE_TC1 = 1;
    public final static int TYPE_DC1 = 2;
    public final static int TYPE_A1 = 3;
    public final static int TYPE_M1 = 4;
    public final static int TYPE_RGBW = 5;
    public final static int TYPE_CLOCK = 6;
    public final static int TYPE_COUNT = 7;


    public final static String[] TypeName = new String[]{
            "按键伴侣",//0
            "智能排插zTC1",//1
            "智能排插zDC1",   //2
            "空气净化器zA1", //3
            "空气检测仪zM1", //4
            "zRGBW灯",   //5
            "时钟",    //
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


    public final static Class LinkActivity[] =
            {
                    ESPtouchActivity.class,
                    TC1LinkActivity.class,
                    DC1LinkActivity.class,
                    A1LinkActivity.class,
                    M1LinkActivity.class,
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
        return TYPE_ICON[type];
    }

    //region 子类必须重构函数
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
