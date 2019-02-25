package com.zyc.zcontrol.controlItem.buttonmate;


import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import com.zyc.zcontrol.R;
@SuppressLint("ValidFragment")
public class ButtonMateSettingFragment extends PreferenceFragment {
    final static String Tag = "RGBSettingFragment_Tag";
    SharedPreferences mSharedPreferences;
    SharedPreferences.Editor editor;
    String name=null;
    String mac=null;


    public ButtonMateSettingFragment(String name,String mac) {
        this.name=name;
        this.mac=mac;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName("Setting_" + mac);
        Log.d(Tag,"设置文件:"+"Setting" + mac);
        addPreferencesFromResource(R.xml.button_mate_setting);
//
//        CheckBoxPreference mEtPreference = (CheckBoxPreference) findPreference("theme");
        EditTextPreference WAN_IP = (EditTextPreference) findPreference("WAN_IP");
        EditTextPreference LAN_IP = (EditTextPreference) findPreference("LAN_IP");
//
        WAN_IP.setOnPreferenceChangeListener(PreferenceChangeListener);
        LAN_IP.setOnPreferenceChangeListener(PreferenceChangeListener);

        WAN_IP.setSummary(WAN_IP.getText());
        LAN_IP.setSummary(LAN_IP.getText());

    }

    private static Preference.OnPreferenceChangeListener PreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            preference.setSummary((String) newValue);
            return true;
        }
    };



}
