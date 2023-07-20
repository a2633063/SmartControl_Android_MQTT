package com.zyc.zcontrol;

import static com.zyc.Function.returnDeviceClass;
import static com.zyc.zcontrol.ConnectService.ACTION_MAINACTIVITY_DEVICELISTUPDATE;
import static com.zyc.zcontrol.deviceItem.DeviceClass.Device.TYPE_COUNT;
import static com.zyc.zcontrol.deviceItem.DeviceClass.Device.TYPE_UNKNOWN;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.zyc.zcontrol.deviceItem.DeviceClass.Device;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

@SuppressLint("ValidFragment")
public class SettingFragment extends PreferenceFragment {
    final static String Tag = "SettingFragment_Tag";

    List<Device> mData;

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
        EditTextPreference mqtt_clientid = (EditTextPreference) findPreference("mqtt_clientid");

        //region 设置mqtt服务器部分
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
                if (str.length() == 0) {
                    preference.setSummary(str);
                    return true;
                } else {
                    if (strArry.length == 1) {
                        str = str.replace(":", "") + ":1883";
                        strArry = str.split(":");
                    }
                    if (strArry.length == 2 && strArry[0].length() > 0) {
                        try {
                            int port = Integer.parseInt(strArry[1]);
                            preference.setSummary(str);

                            //可能修改了需要保存的数据,因此此处不用listener的返回true自动保存,而是手动保存修改后的值
                            SharedPreferences.Editor mEditor;
                            mEditor = getActivity().getSharedPreferences(getPreferenceManager().getSharedPreferencesName(), 0).edit();
                            mEditor.putString(preference.getKey(), str);
                            mEditor.commit();
                            return false;
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                }
                Toast.makeText(getActivity(), "保存失败!格式错误.\n格式:地址:端口\n如192.168.1.1:1883", Toast.LENGTH_SHORT).show();

                return false;
            }
        });

        mqtt_clientid.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String) newValue);
                Toast.makeText(getActivity(), "注意:同个MQTT服务器内,ClientID必须唯一,否则将导致设备掉线\n修改ClientID APP重启才生效", Toast.LENGTH_SHORT).show();

                return true;
            }
        });
        mqtt_uri.setSummary(mqtt_uri.getText());
        mqtt_user.setSummary(mqtt_user.getText());
        mqtt_clientid.setSummary(mqtt_clientid.getText());
        if (mqtt_password.getText() != null && mqtt_password.getText().length() > 0)
            mqtt_password.setSummary("***********");
        else
            mqtt_password.setSummary("");
        //endregion


        mData = ((MainApplication) getActivity().getApplication()).getDeviceList();
        //region 设备导入导出
        Preference device_import = findPreference("device_import");
        Preference device_export = findPreference("device_export");
        //设备导入
        device_import.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String str = null;
                ClipboardManager manager = (ClipboardManager) getView().getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (manager != null && manager.hasPrimaryClip() && manager.getPrimaryClip().getItemCount() > 0) {
                    ClipData clip=manager.getPrimaryClip();
                    ClipData.Item item=clip.getItemAt(0);
                    str = item.getText().toString();
                    deviceImport(str);
                    //Toast.makeText(getActivity(), "剪贴板:" + str, Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

        //设备导出
        device_export.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    String device_str = deviceExport();
                    ClipboardManager manager = (ClipboardManager) getView().getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    if (manager != null) {
                        manager.setPrimaryClip(ClipData.newPlainText("test", device_str));
                        Toast.makeText(getActivity(), "已经导出到剪贴板中!", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    else Toast.makeText(getActivity(), "设置剪贴板错误,请确认剪贴板权限!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getActivity(), "设置剪贴板错误,请确认剪贴板权限!", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });


        //endregion
    }

    //region 设备导入子函数
    void deviceImport(String str) {
        int device_count_all = -1;
        int device_count_add = 0;
        String title;
        String message;
        try {

            JSONObject obj = new JSONObject(str);
            if (!obj.has("device")) return;
            JSONArray jsonArray = obj.getJSONArray("device");
            device_count_all = 0;
            device_count_add = 0;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                if (!json.has("name") || !json.has("mac") || !json.has("type") || !json.has("type_name")) {
                    continue;
                }

                int type = json.getInt("type");
                String name = json.getString("name");
                String mac = json.getString("mac");

                if (name == null || mac == null || type <= TYPE_UNKNOWN || type >= TYPE_COUNT)
                    continue;
                if (mac.length() != 12) continue;

                device_count_all++;
                if (((MainApplication) getActivity().getApplication()).getDevice(mac) != null)
                    continue;

                Device d = returnDeviceClass(name, mac, type);
                mData.add(d);
                device_count_add++;

            }

        } catch (JSONException e) {
            e.printStackTrace();

        }

        if (device_count_all < 0) {
            title = "导入失败";
            message = "json格数错误!请确认导入内容格式正确!";
        } else if (device_count_all == 0) {
            title = "导入失败";
            message = "未检测到有效设备!";
        } else if (device_count_all > 0 && device_count_add == 0) {
            title = "导入设备重复!";
            message = "导入设备" + device_count_all + "个,"+"无新设备!";
        } else {
            title = "导入设备成功!";
            message = "导入设备" + device_count_all + "个,重复设备" + (device_count_all - device_count_add) + "个\r\n"
                    + "实际导入设备" + device_count_add + "个";

            //通知mainactivity页面更新设备列表
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(ACTION_MAINACTIVITY_DEVICELISTUPDATE));
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", null)
                .create()
                .show();
    }

    //endregion
    //region 设备导出子函数
    String deviceExport() {
        String str = "";

        JSONArray jsonArray = new JSONArray();


        try {
            for (Device d : mData) {
                JSONObject j = new JSONObject();
                j.put("name", d.getName());
                j.put("mac", d.getMac());
                j.put("type", d.getType());
                j.put("type_name", d.getTypeName());
                jsonArray.put(j);
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("device", jsonArray);
            return jsonObject.toString(0);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }
    //endregion
}
