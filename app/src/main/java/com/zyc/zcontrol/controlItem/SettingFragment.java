package com.zyc.zcontrol.controlItem;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.zyc.zcontrol.R;

@SuppressLint("ValidFragment")
public class SettingFragment extends PreferenceFragment {
    final static String Tag = "SettingFragment_Tag";
    SharedPreferences mSharedPreferences;
    SharedPreferences.Editor editor;


    public SettingFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName("Setting");
        Log.d(Tag, "设置文件:" + "Setting");
        addPreferencesFromResource(R.xml.setting);

        EditTextPreference mqtt_uri = (EditTextPreference) findPreference("mqtt_uri");
        EditTextPreference mqtt_user = (EditTextPreference) findPreference("mqtt_user");
        EditTextPreference mqtt_password = (EditTextPreference) findPreference("mqtt_password");

        mqtt_uri.setOnPreferenceChangeListener(PreferenceChangeListener);
        mqtt_user.setOnPreferenceChangeListener(PreferenceChangeListener);
        mqtt_password.setOnPreferenceChangeListener(PreferenceChangeListener);


        mqtt_uri.setSummary(mqtt_uri.getText());
        mqtt_user.setSummary(mqtt_user.getText());
        mqtt_password.setSummary(mqtt_password.getText());

    }

    private static Preference.OnPreferenceChangeListener PreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            preference.setSummary((String) newValue);
            return true;
        }
    };


}
