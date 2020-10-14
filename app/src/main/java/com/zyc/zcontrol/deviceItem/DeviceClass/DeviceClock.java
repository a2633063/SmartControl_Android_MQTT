package com.zyc.zcontrol.deviceItem.DeviceClass;

import android.preference.PreferenceFragment;

import com.zyc.zcontrol.deviceItem.clock.ClockFragment;
import com.zyc.zcontrol.deviceItem.clock.ClockSettingFragment;

import androidx.fragment.app.Fragment;

public class DeviceClock extends Device {

    public DeviceClock(String name, String mac) {
        super(TYPE_CLOCK, name, mac);
    }

    //region 必须重构的函数
    public String[] getRecvMqttTopic() {
        String[] topic = new String[3];
        topic[0] = "device/zclock/" + getMac() + "/state";
        topic[1] = "device/zclock/" + getMac() + "/sensor";
        topic[2] = "device/zclock/" + getMac() + "/availability";
        return topic;
    }

    public String getSendMqttTopic() {
        return "device/zclock/" + getMac() + "/set";
    }


    Fragment fragment;
    PreferenceFragment settingFragment;

    public Fragment getFragment() {
        if (fragment == null) {
            fragment = new ClockFragment(this);
        }
        return fragment;
    }

    public PreferenceFragment getSettingFragment() {
        if (settingFragment == null) {
            settingFragment = new ClockSettingFragment(this);
        }
        return settingFragment;
    }
    //endregion


}
