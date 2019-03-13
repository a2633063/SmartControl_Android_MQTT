package com.zyc.zcontrol.controlItem.tc1;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.zyc.zcontrol.ConnectService;
import com.zyc.zcontrol.R;

import org.json.JSONException;
import org.json.JSONObject;

import static android.content.Context.BIND_AUTO_CREATE;

@SuppressLint("ValidFragment")
public class TC1SettingFragment extends PreferenceFragment {
    final static String Tag = "ButtonMateSetting";
    SharedPreferences mSharedPreferences;
    SharedPreferences.Editor editor;

    //region 使用本地广播与service通信
    LocalBroadcastManager localBroadcastManager;
    private MsgReceiver msgReceiver;
    ConnectService mConnectService;
    //endregion

    EditTextPreference name_preference;
    EditTextPreference domoticz_idx;

    String device_name = null;
    String device_mac = null;

    public TC1SettingFragment(String name, String mac) {
        this.device_name = name;
        this.device_mac = mac;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName("Setting_" + device_mac);

        Log.d(Tag, "设置文件:" + "Setting" + device_mac);
        addPreferencesFromResource(R.xml.button_mate_setting);


        //region MQTT服务有关
        //region 动态注册接收mqtt服务的广播接收器,
        localBroadcastManager = LocalBroadcastManager.getInstance(getActivity().getApplicationContext());
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(ConnectService.ACTION_UDP_DATA_AVAILABLE);//UDP监听
        localBroadcastManager.registerReceiver(msgReceiver, intentFilter);
        //endregion

        //region 启动MQTT 服务以及启动,无需再启动
        Intent intent = new Intent(getActivity().getApplicationContext(), ConnectService.class);
        getActivity().bindService(intent, mMQTTServiceConnection, BIND_AUTO_CREATE);
        //endregion
        //endregion

//
//        CheckBoxPreference mEtPreference = (CheckBoxPreference) findPreference("theme");
        name_preference = (EditTextPreference) findPreference("name");
        domoticz_idx = (EditTextPreference) findPreference("domoticz_idx");
//

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

                mConnectService.Send("domoticz/out",
                        "{\"mac\":\"" + device_mac + "\",\"setting\":{\"idx\":" + (String) newValue + "}}");
                return false;
            }
        });

        name_preference.setSummary(device_name);

        name_preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                mConnectService.Send("domoticz/out",
                        "{\"mac\":\"" + device_mac + "\",\"setting\":{\"name\":\"" + (String) newValue + "\"}}");
                return false;
            }
        });


    }


    @Override
    public void onDestroy() {
        //注销广播
        localBroadcastManager.unregisterReceiver(msgReceiver);
        //停止服务
        getActivity().unbindService(mMQTTServiceConnection);
        super.onDestroy();
    }


    //数据接收处理函数
    void Receive(String ip, int port, String message) {
        //TODO 数据接收处理
        Receive(null, message);
    }

    void Receive(String topic, String message) {
        //TODO 数据接收处理
        Log.d(Tag, "RECV DATA,topic:" + topic + ",content:" + message);

        try {
            JSONObject jsonObject = new JSONObject(message);
            String name = null;
            String mac = null;
            JSONObject jsonSetting = null;
            if (jsonObject.has("name")) name = jsonObject.getString("name");
            if (jsonObject.has("mac")) mac = jsonObject.getString("mac");
            if (jsonObject.has("setting")) jsonSetting = jsonObject.getJSONObject("setting");
            if (mac == null) return;
            else if (mac.equals(device_mac)) {
                if (name != null) {
                    name_preference.setSummary(name);
                    name_preference.setText(name);
                }
                if (jsonSetting != null) {
                    if (jsonSetting.has("idx")) {
                        String idx = jsonSetting.getString("idx");
                        domoticz_idx.setSummary(idx);
                        domoticz_idx.setText(idx);
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //region MQTT服务有关

    private final ServiceConnection mMQTTServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mConnectService = ((ConnectService.LocalBinder) service).getService();
            //{"mac":"mac","setting":{"idx":null}}
            mConnectService.Send("domoticz/out", "{\"mac\":\"" + device_mac + "\",\"setting\":{\"idx\":null}}");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mConnectService = null;
        }
    };

    //广播接收,用于处理接收到的数据
    public class MsgReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ConnectService.ACTION_UDP_DATA_AVAILABLE.equals(action)) {
                String ip = intent.getStringExtra(ConnectService.EXTRA_UDP_DATA_IP);
                String message = intent.getStringExtra(ConnectService.EXTRA_UDP_DATA_MESSAGE);
                int port = intent.getIntExtra(ConnectService.EXTRA_UDP_DATA_PORT, -1);
                Receive(ip, port, message);
            } else if (ConnectService.ACTION_DATA_AVAILABLE.equals(action)) {  //接收到数据
                String topic = intent.getStringExtra(ConnectService.EXTRA_DATA_TOPIC);
                String message = intent.getStringExtra(ConnectService.EXTRA_DATA_MESSAGE);
                Receive(topic, message);
            }
        }
    }
    //endregion

}
