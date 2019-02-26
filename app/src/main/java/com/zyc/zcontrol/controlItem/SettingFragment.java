package com.zyc.zcontrol.controlItem;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

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

        mqtt_user.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String) newValue);
                return true;
            }
        });
        mqtt_password.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int len = ((String) newValue).length();

                if (len == 0) preference.setSummary("");
                else
                    preference.setSummary("***********");
                return true;
            }
        });
        mqtt_uri.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                String str = (String) newValue;
                String[] strArry = str.split(":");
                if (strArry.length == 2 && strArry[0].length() > 0 && strArry.length > 0) {
                    try {
                        int port = Integer.parseInt(strArry[1]);
                        preference.setSummary(str);
                        return true;
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                Toast.makeText(getActivity(), "保存失败!格式错误.\n格式:地址:端口\n如192.168.1.1:1883", Toast.LENGTH_SHORT).show();

                return false;
            }
        });


        mqtt_uri.setSummary(mqtt_uri.getText());
        mqtt_user.setSummary(mqtt_user.getText());

        if (mqtt_password.getText()!=null && mqtt_password.getText().length() > 0)
            mqtt_password.setSummary("***********");
        else
            mqtt_password.setSummary("");
    }

}
