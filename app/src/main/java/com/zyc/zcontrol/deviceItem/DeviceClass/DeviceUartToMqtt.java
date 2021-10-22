package com.zyc.zcontrol.deviceItem.DeviceClass;

import android.preference.PreferenceFragment;

import androidx.fragment.app.Fragment;

import com.zyc.zcontrol.deviceItem.UartToMqtt.UartToMqttFragment;
import com.zyc.zcontrol.deviceItem.UartToMqtt.UartToMqttSettingFragment;
import com.zyc.zcontrol.deviceItem.key51.Key51Fragment;
import com.zyc.zcontrol.deviceItem.key51.Key51SettingFragment;

public class DeviceUartToMqtt extends Device {

    public DeviceUartToMqtt(String name, String mac) {
        super(TYPE_UARTTOMQTT, name, mac);
    }

    //region 必须重构的函数
    public String[] getRecvMqttTopic() {
        String[] topic = new String[3];
        topic[0] = "device/z485tomqtt/" + getMac() + "/state";
        topic[1] = "device/z485tomqtt/" + getMac() + "/sensor";
        topic[2] = "device/z485tomqtt/" + getMac() + "/availability";
        return topic;
    }

    public String getSendMqttTopic() {
        return "device/z485tomqtt/" + getMac() + "/set";
    }


    Fragment fragment;
    PreferenceFragment settingFragment;
    public Fragment getFragment() {
        if (fragment == null) {
            fragment = new UartToMqttFragment(this);
        }
        return fragment;
    }
    public PreferenceFragment getSettingFragment(){
        if (settingFragment == null) {
            settingFragment = new UartToMqttSettingFragment(this);
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
