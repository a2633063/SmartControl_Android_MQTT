package com.zyc.zcontrol.deviceItem.DeviceClass;

import android.preference.PreferenceFragment;

import androidx.fragment.app.Fragment;

import com.zyc.zcontrol.deviceItem.buttonmate.ButtonMateFragment;
import com.zyc.zcontrol.deviceItem.buttonmate.ButtonMateSettingFragment;

public class DeviceButtonMate extends Device {

    public DeviceButtonMate(String name, String mac) {
        super(TYPE_BUTTON_MATE, name, mac);
    }

    //region 必须重构的函数
    public String[] getRecvMqttTopic() {
        String[] topic = new String[1];
        topic[0] = "domoticz/in";
        return topic;
    }

    public String getSendMqttTopic() {
        return "domoticz/out";
    }


    Fragment fragment;
    PreferenceFragment settingFragment;
    public Fragment getFragment() {
        if (fragment == null) {
            fragment = new ButtonMateFragment(this);
        }
        return fragment;
    }
    public PreferenceFragment getSettingFragment(){
        if (settingFragment == null) {
            settingFragment = new ButtonMateSettingFragment(this);
        }
        return settingFragment;
    }
    //endregion


}
