package com.zyc.zcontrol.deviceItem.DeviceClass;

import android.preference.PreferenceFragment;


import com.zyc.zcontrol.deviceItem.z863key.z863keyFragment;
import com.zyc.zcontrol.deviceItem.z863key.z863keySettingFragment;

import androidx.fragment.app.Fragment;

public class Devicez863key extends Device {

    public Devicez863key(String name, String mac) {
        super(TYPE_Z863KEY, name, mac);
    }

    //region 必须重构的函数
    public String[] getRecvMqttTopic() {
        String[] topic = new String[]{
                "device/z86_3key/" + getMac() + "/state",
                "device/z86_3key/" + getMac() + "/sensor"
        };
        //topic[0] = "device/z86_3key/" + getMac() + "/state";
        //topic[1] = "device/z86_3key/" + getMac() + "/sensor";
        //topic[2] = "device/z86_3key/" + getMac() + "/availability";
        return topic;
    }

    public String getSendMqttTopic() {
        return "device/z86_3key/" + getMac() + "/set";
    }


    Fragment fragment;
    PreferenceFragment settingFragment;
    public Fragment getFragment() {
        if (fragment == null) {
            fragment = new z863keyFragment(this);
        }
        return fragment;
    }
    public PreferenceFragment getSettingFragment(){
        if (settingFragment == null) {
            settingFragment = new z863keySettingFragment(this);
        }
        return settingFragment;
    }
    //endregion

//    //region 参数
//    boolean lock;
//    double power;
//    int total_time;
//    boolean[] plug = {false,false,false,false};
//    String[] plug_name = {"总开关","插口1","插口2","插口3"};
//
//
//    public boolean isLock() {
//        return lock;
//    }
//
//    public void setLock(boolean lock) {
//        this.lock = lock;
//    }
//
//    public double getPower() {
//        return power;
//    }
//
//    public void setPower(double power) {
//        this.power = power;
//    }
//
//    public int getTotal_time() {
//        return total_time;
//    }
//
//    public void setTotal_time(int total_time) {
//        this.total_time = total_time;
//    }
//
//    public boolean isPlug(int index) {
//        return plug[index];
//    }
//
//    public void setPlug(int index, boolean plug) {
//        this.plug[index] = plug;
//    }
//
//    public String getPlug_name(int index) {
//        return plug_name[index];
//    }
//
//    public void setPlug_name(int index, String plug_name) {
//        this.plug_name[index] = plug_name;
//    }
//    //endregion

}
