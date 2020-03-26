package com.zyc.zcontrol.deviceItem.DeviceClass;

import android.preference.PreferenceFragment;

import androidx.fragment.app.Fragment;

import com.zyc.zcontrol.deviceItem.rgbw.RGBWFragment;
import com.zyc.zcontrol.deviceItem.rgbw.RGBWSettingFragment;

public class DeviceRGBW extends Device {

    public DeviceRGBW(String name, String mac) {
        super(TYPE_RGBW, name, mac);
    }

    //region 必须重构的函数
    public String[] getRecvMqttTopic() {
        String[] topic = new String[2];
        topic[0] = "device/rgbw/" + getMac() + "/state";
        topic[1] = "device/rgbw/" + getMac() + "/availability";
        return topic;
    }

    public String getSendMqttTopic() {
        return "device/rgbw/" + getMac() + "/set";
    }


    Fragment fragment;
    PreferenceFragment settingFragment;

    public Fragment getFragment() {
        if (fragment == null) {
            fragment = new RGBWFragment(this);
        }
        return fragment;
    }

    public PreferenceFragment getSettingFragment() {
        if (settingFragment == null) {
            settingFragment = new RGBWSettingFragment(this);
        }
        return settingFragment;
    }
    //endregion


}
