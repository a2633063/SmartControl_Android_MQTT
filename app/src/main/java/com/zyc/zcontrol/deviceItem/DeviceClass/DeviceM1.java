package com.zyc.zcontrol.deviceItem.DeviceClass;

import android.preference.PreferenceFragment;

import androidx.fragment.app.Fragment;

import com.zyc.zcontrol.deviceItem.m1.M1Fragment;
import com.zyc.zcontrol.deviceItem.m1.M1SettingFragment;

public class DeviceM1 extends Device {

    public DeviceM1(String name, String mac) {
        super(TYPE_M1, name, mac);
    }

    //region 必须重构的函数
    public String[] getRecvMqttTopic() {
        String[] topic = new String[3];
        topic[0] = "device/zm1/" + getMac() + "/state";
        topic[1] = "device/zm1/" + getMac() + "/sensor";
        topic[2] = "device/zm1/" + getMac() + "/availability";
        return topic;
    }

    public String getSendMqttTopic() {
        return "device/zm1/" + getMac() + "/set";
    }


    Fragment fragment;
    PreferenceFragment settingFragment;

    public Fragment getFragment() {
        if (fragment == null) {
            fragment = new M1Fragment(this);
        }
        return fragment;
    }

    public PreferenceFragment getSettingFragment() {
        if (settingFragment == null) {
            settingFragment = new M1SettingFragment(this);
        }
        return settingFragment;
    }
    //endregion


}
