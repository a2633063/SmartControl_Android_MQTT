package com.zyc.zcontrol.deviceItem.DeviceClass;

import androidx.fragment.app.Fragment;

import com.zyc.zcontrol.deviceItem.tc1.TC1Fragment;

public class DeviceTC1 extends Device {

    public DeviceTC1(String name, String mac) {
        super(TYPE_TC1, name, mac);
    }

    //region 必须重构的函数
    public String[] getRecvMqttTopic() {
        String[] topic = new String[3];
        topic[0] = "device/ztc1/" + getMac() + "/state";
        topic[1] = "device/ztc1/" + getMac() + "/sensor";
        topic[2] = "device/ztc1/" + getMac() + "/availability";
        return topic;
    }

    public String getSendMqttTopic() {
        return "device/ztc1/" + getMac() + "/set";
    }


    Fragment fragment;

    public Fragment getFragment() {
        if (fragment == null) {
            fragment = new TC1Fragment(this);
        }
        return fragment;
    }
    //endregion

    //region 参数
    double power;
    int total_time;
    boolean[] plug = new boolean[6];
    String[] plug_name = new String[6];


    public double getPower() {
        return power;
    }

    public void setPower(double power) {
        this.power = power;
    }

    public int getTotal_time() {
        return total_time;
    }

    public void setTotal_time(int total_time) {
        this.total_time = total_time;
    }

    public boolean isPlug(int index) {
        return plug[index];
    }

    public void setPlug(int index, boolean plug) {
        this.plug[index] = plug;
    }

    public String getPlug_name(int index) {
        return plug_name[index];
    }

    public void setPlug_name(int index, String plug_name) {
        this.plug_name[index] = plug_name;
    }
    //endregion

}
