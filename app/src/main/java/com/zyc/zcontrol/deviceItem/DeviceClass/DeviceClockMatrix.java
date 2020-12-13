package com.zyc.zcontrol.deviceItem.DeviceClass;

import android.preference.PreferenceFragment;

import com.zyc.zcontrol.deviceItem.clockMatrix.ClockMatrixFragment;
import com.zyc.zcontrol.deviceItem.clockMatrix.ClockMatrixSettingFragment;

import androidx.fragment.app.Fragment;

public class DeviceClockMatrix extends Device {

    public DeviceClockMatrix(String name, String mac) {
        super(TYPE_CLOCK_MATRIX, name, mac);
    }

    //region 必须重构的函数
    public String[] getRecvMqttTopic() {
        String[] topic = new String[3];
        topic[0] = "device/zclock_matrix/" + getMac() + "/state";
        topic[1] = "device/zclock_matrix/" + getMac() + "/sensor";
        topic[2] = "device/zclock_matrix/" + getMac() + "/availability";
        return topic;
    }

    public String getSendMqttTopic() {
        return "device/zclock_matrix/" + getMac() + "/set";
    }


    Fragment fragment;
    PreferenceFragment settingFragment;

    public Fragment getFragment() {
        if (fragment == null) {
            fragment = new ClockMatrixFragment(this);
        }
        return fragment;
    }

    public PreferenceFragment getSettingFragment() {
        if (settingFragment == null) {
            settingFragment = new ClockMatrixSettingFragment(this);
        }
        return settingFragment;
    }
    //endregion


}
