package com.zyc.zcontrol.deviceItem.DeviceClass;

import android.preference.PreferenceFragment;

import androidx.fragment.app.Fragment;

import com.zyc.zcontrol.deviceItem.a1.A1Fragment;
import com.zyc.zcontrol.deviceItem.a1.A1SettingFragment;

public class DeviceA1 extends Device {

    public DeviceA1(String name, String mac) {
        super(TYPE_A1, name, mac);
    }

    //region 必须重构的函数
    public String[] getRecvMqttTopic() {
        String[] topic = new String[2];
        topic[0] = "device/za1/" + getMac() + "/state";
        topic[1] = "device/za1/" + getMac() + "/availability";
        return topic;
    }

    public String getSendMqttTopic() {
        return "device/za1/" + getMac() + "/set";
    }


    Fragment fragment;
    PreferenceFragment settingFragment;
    public Fragment getFragment() {
        if (fragment == null) {
            fragment = new A1Fragment(this);
        }
        return fragment;
    }
    public PreferenceFragment getSettingFragment(){
        if (settingFragment == null) {
            settingFragment = new A1SettingFragment(this);
        }
        return settingFragment;
    }
    //endregion


}
