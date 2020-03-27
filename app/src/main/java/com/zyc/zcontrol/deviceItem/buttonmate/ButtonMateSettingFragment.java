package com.zyc.zcontrol.deviceItem.buttonmate;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.Log;

import com.zyc.zcontrol.R;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceButtonMate;
import com.zyc.zcontrol.deviceItem.DeviceClass.SettingFragment;

import org.json.JSONException;
import org.json.JSONObject;

@SuppressLint("ValidFragment")
public class ButtonMateSettingFragment extends SettingFragment {
    final static String Tag = "ButtonMateSetting";


    EditTextPreference name_preference;
    Preference regetdata;
    EditTextPreference domoticz_idx;

    DeviceButtonMate device;

    public ButtonMateSettingFragment(DeviceButtonMate device) {
        super(device.getName(), device.getMac());
        this.device = device;
    }
    //region Handler
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {// handler接收到消息后就会执行此方法
            JSONObject obj = null;
            String JsonStr = null;
            switch (msg.what) {
//                //region 获取最新版本信息
//                case 0:
//                    if (pd != null && pd.isShowing()) pd.dismiss();
//                    JsonStr = (String) msg.obj;
//                    Log.d(Tag, "result:" + JsonStr);
//                    try {
//                        if (JsonStr == null || JsonStr.length() < 3)
//                            throw new JSONException("获取最新版本信息失败");
//                        obj = new JSONObject(JsonStr);
//                        if (obj.has("id") && obj.has("tag_name") && obj.has("target_commitish")
//                                && obj.has("name") && obj.has("body") && obj.has("created_at")
//                                && obj.has("assets")) {
//                            otaInfo.title = obj.getString("name");   //
//                            otaInfo.message = obj.getString("body");
//                            otaInfo.tag_name = obj.getString("tag_name");
//                            otaInfo.created_at = obj.getString("created_at");
//
//                            String version = fw_version.getSummary().toString();
//                            if (!version.equals(otaInfo.tag_name)) {
//                                handler.sendEmptyMessage(1);
//                            } else {
//                                Toast.makeText(getActivity(), "已是最新版本", Toast.LENGTH_SHORT).show();
//                            }
//                        } else {
//                            throw new JSONException("获取最新版本信息失败");
//                        }
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                        Toast.makeText(getActivity(), "获取最新版本信息失败", Toast.LENGTH_SHORT).show();
//                    }
//
//                    break;
//                //endregion
//                //region 开始获取固件下载地址
//                case 1:
//                    pd.setMessage("正在获取固件地址,请稍后....");
//                    pd.setCanceledOnTouchOutside(false);
//                    pd.show();
//
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Message msg = new Message();
//                            msg.what = 2;
//                            String res = WebService.WebConnect("https://gitee.com/api/v5/repos/a2633063/Release/releases/tags/zTC1");
//                            msg.obj = res;
//                            handler.sendMessageDelayed(msg, 0);// 执行耗时的方法之后发送消给handler
//                        }
//                    }).start();
//                    break;
//                //endregion
//                //region 已获取固件下载地址
//                case 2:
//                    if (pd != null && pd.isShowing()) pd.dismiss();
//                    JsonStr = (String) msg.obj;
//                    Log.d(Tag, "result:" + JsonStr);
//                    try {
//                        if (JsonStr == null || JsonStr.length() < 3)
//                            throw new JSONException("获取固件下载地址失败");
//
//                        obj = new JSONObject(JsonStr);
//
//                        if (obj.getString("name").equals("zTC1发布地址_" + otaInfo.tag_name)) {
//                            String otauriAll = obj.getString("body");
//                            otaInfo.ota = otauriAll.trim();
//
//                            AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
//                                    .setTitle("获取到最新版本:" + otaInfo.tag_name)
//                                    .setMessage(otaInfo.title + "\n" + otaInfo.message)
//                                    .setPositiveButton("更新", new DialogInterface.OnClickListener() {
//                                        @Override
//                                        public void onClick(DialogInterface dialog, int which) {
//                                            Send("{\"mac\":\"" + device.getMac() + "\",\"setting\":{\"ota\":\"" + otaInfo.ota + "\"}}");
//                                        }
//                                    })
//                                    .setNegativeButton("取消", null)
//                                    .create();
//                            alertDialog.show();
//                        } else
//                            throw new JSONException("获取固件下载地址获取失败");
//
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                        Toast.makeText(getActivity(), "固件下载地址获取失败", Toast.LENGTH_SHORT).show();
//
//                    }
//
//                    break;
//                //endregion
                //region 发送请求数据
                case 3:
                    handler.removeMessages(3);
                    Send("{\"mac\":\"" + device.getMac() + "\",\"setting\":{\"idx\":null}}");
                    break;
                //endregion
            }
        }
    };
    //endregion

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName("Setting_" + device.getMac());

        Log.d(Tag, "设置文件:" + "Setting" + device.getMac());
        addPreferencesFromResource(R.xml.button_mate_setting);

        name_preference = (EditTextPreference) findPreference("name");
        domoticz_idx = (EditTextPreference) findPreference("domoticz_idx");
        regetdata = findPreference("regetdata");


        try {
            int idx_temp = Integer.parseInt(domoticz_idx.getText());
            if (idx_temp >= 0)
                domoticz_idx.setSummary(domoticz_idx.getText());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        domoticz_idx.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                Send("{\"mac\":\"" + device.getMac() + "\",\"setting\":{\"idx\":" + (String) newValue + "}}");
                return false;
            }
        });

        name_preference.setSummary(device.getName());

        //region 设置名称
        name_preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Send("{\"mac\":\"" + device.getMac() + "\",\"setting\":{\"name\":\"" + (String) newValue + "\"}}");
                return false;
            }
        });
        //endregion
        //region 重新获取数据
        regetdata.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                handler.sendEmptyMessage(3);
                return false;
            }
        });
        //endregion

    }


    public void Send(String message) {
        boolean udp = getActivity().getSharedPreferences("Setting_" + device.getMac(), 0).getBoolean("always_UDP", false);
        super.Send(udp, device.getSendMqttTopic(), message);
    }

    //数据接收处理函数
    @Override
    public void Receive(String ip, int port, String topic, String message) {
        super.Receive(ip, port, topic, message);
        Log.d(Tag, "RECV DATA,topic:" + topic + ",content:" + message);

        try {
            JSONObject jsonObject = new JSONObject(message);
            if (!jsonObject.has("mac") || !jsonObject.getString("mac").equals(device.getMac())) {
                return;
            }
            //region 获取名称
            if (jsonObject.has("name")) {
                device.setName(jsonObject.getString("name"));
                name_preference.setSummary(device.getName());
                name_preference.setText(device.getName());
            }
            //endregion
            //region 接收主机setting
            JSONObject jsonSetting = null;
            if (jsonObject.has("setting")) jsonSetting = jsonObject.getJSONObject("setting");
            if (jsonSetting != null) {
                if (jsonSetting.has("idx")) {
                    String idx = jsonSetting.getString("idx");
                    domoticz_idx.setSummary(idx);
                    domoticz_idx.setText(idx);
                }
            }
            //endregion

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    //region 事件监听调用函数,主要为在子类中重写此函数实现在service建立成功/mqtt连接成功/失败时执行功能
    //Service建立成功时调用    此函数需要时在子类中重写
    public void ServiceConnected() {
        handler.sendEmptyMessageDelayed(3, 0);
    }

    //mqtt连接成功时调用    此函数需要时在子类中重写
    public void MqttConnected() {
        handler.sendEmptyMessageDelayed(3, 0);
    }

    //mqtt连接断开时调用    此函数需要时在子类中重写
    public void MqttDisconnected() {
        handler.sendEmptyMessageDelayed(3, 0);
    }
    //endregion

}
