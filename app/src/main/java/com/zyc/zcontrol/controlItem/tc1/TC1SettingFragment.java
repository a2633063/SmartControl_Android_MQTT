package com.zyc.zcontrol.controlItem.tc1;


import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.zyc.zcontrol.ConnectService;
import com.zyc.zcontrol.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import static android.content.Context.BIND_AUTO_CREATE;

@SuppressLint("ValidFragment")
public class TC1SettingFragment extends PreferenceFragment {
    final static String Tag = "TC1SettingFragment";
    SharedPreferences mSharedPreferences;
    SharedPreferences.Editor editor;

    //region 使用本地广播与service通信
    LocalBroadcastManager localBroadcastManager;
    private MsgReceiver msgReceiver;
    ConnectService mConnectService;
    //endregion

    Preference fw_version;
    EditTextPreference name_preference;


    String device_name = null;
    String device_mac = null;

    boolean ota_flag = false;
    private ProgressDialog pd;


    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {// handler接收到消息后就会执行此方法
            JSONObject obj = null;
            switch (msg.what) {
                case 0:
                    String body = null;
                    String name = null;
                    String tag_name = null;
                    String ota_uri = null;
                    String created_at = null;
                    if (pd != null && pd.isShowing()) pd.dismiss();
                    String JsonStr = (String) msg.obj;
                    Log.d(Tag, "result:" + JsonStr);
                    try {
                        obj = new JSONObject(JsonStr);
                        if (obj.has("id") && obj.has("tag_name") && obj.has("target_commitish")
                                && obj.has("name") && obj.has("body") && obj.has("created_at")
                                && obj.has("assets")) {
                            name = obj.getString("name");
                            body = obj.getString("body");
                            tag_name = obj.getString("tag_name");
                            created_at = obj.getString("created_at");

                            JSONArray assets = obj.getJSONArray("assets");

                            for (int i = 0; i < assets.length(); i++) {
                                JSONObject a = assets.getJSONObject(i);
                                if (a.has("browser_download_url")
                                        && a.getString("browser_download_url").endsWith("ota.bin")) {
                                    ota_uri = a.getString("browser_download_url");
                                    String str = new String(ota_uri.getBytes(), "UTF-8");
                                    ota_uri = URLDecoder.decode(str, "UTF-8");
                                    ota_uri = ota_uri.replaceFirst("http.*http", "http");
                                    Log.d(Tag, "ota_uri:" + ota_uri);
                                    break;
                                }
                            }

                            final String ota_uri_final = ota_uri;
                            String version = fw_version.getSummary().toString();
                            if (!version.equals(tag_name)) {
                                AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                                        .setTitle("获取到最新版本:" + tag_name)
                                        .setMessage(name + "\n" + body)
                                        .setPositiveButton("更新", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Send("{\"mac\":\"" + device_mac + "\",\"setting\":{\"ota\":\"" + ota_uri_final + "\"}}");
                                            }
                                        })
                                        .setNegativeButton("取消", null)
                                        .create();
                                alertDialog.show();
                            } else {
                                Toast.makeText(getActivity(), "已是最新版本", Toast.LENGTH_SHORT).show();
                            }

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    break;
            }
        }
    };

    public TC1SettingFragment(String name, String mac) {
        this.device_name = name;
        this.device_mac = mac;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName("Setting_" + device_mac);

        Log.d(Tag, "设置文件:" + "Setting" + device_mac);
        addPreferencesFromResource(R.xml.tc1_setting);


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
        fw_version = findPreference("fw_version");
        name_preference = (EditTextPreference) findPreference("name");


        name_preference.setSummary(device_name);

        findPreference("mac").setSummary(device_mac);

        //region 设置名称
        name_preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Send("{\"mac\":\"" + device_mac + "\",\"setting\":{\"name\":\"" + (String) newValue + "\"}}");
                return false;
            }
        });
        //endregion


        //region 版本
        fw_version.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                //region 手动输入固件下载地址注释

                final EditText et = new EditText(getActivity());
                new AlertDialog.Builder(getActivity()).setTitle("请输入固件下载地址")
                        .setView(et)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String uri = et.getText().toString();
                                if (uri.length() < 1) return;
                                if (uri.startsWith("http") && uri.endsWith("/TC1_MK3031_moc.ota.bin")) {
                                    Send("{\"mac\":\"" + device_mac + "\",\"setting\":{\"ota\":\"" + uri + "\"}}");
                                }
                            }
                        }).setNegativeButton("取消", null).show();

                //endregion
/*
                //region 未获取到当前版本信息
                if (fw_version.getSummary() == null) {
                    AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                            .setTitle("未获取到设备版本")
                            .setMessage("请获取到设备版本后重试.")
                            .setNegativeButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
//                                    getActivity().finish();
                                }
                            })
                            .create();
                    alertDialog.show();
                    return false;
                }
                //endregion

                String version = fw_version.getSummary().toString();
                //region 获取最新版本
                pd = new ProgressDialog(getActivity());
                pd.setMessage("正在获取最新固件版本,请稍后....");
                pd.setCanceledOnTouchOutside(false);
                pd.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Message msg = new Message();
                        msg.what = 0;
                        msg.obj = WebService.WebConnect("https://gitee.com/api/v5/repos/zhangyichen/zTC1/releases/latest");
                        handler.sendMessageDelayed(msg, 0);// 执行耗时的方法之后发送消给handler
                    }
                }).start();

*/
                //endregion
                return false;
            }
        });
        //endregion

    }


    @Override
    public void onDestroy() {
        //注销广播
        localBroadcastManager.unregisterReceiver(msgReceiver);
        //停止服务
        getActivity().unbindService(mMQTTServiceConnection);
        super.onDestroy();
    }

    void Send(String message) {
        boolean b = getActivity().getSharedPreferences("Setting_" + device_mac, 0).getBoolean("always_UDP", false);
        mConnectService.Send(b ? null : "device/ztc1/set", message);
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
            if (jsonObject.has("mac")) mac = jsonObject.getString("mac");
            if (mac == null || !mac.equals(device_mac)) return;

            //region 获取名称
            if (jsonObject.has("name")) {
                name = jsonObject.getString("name");
                name_preference.setSummary(name);
                name_preference.setText(name);
            }
            //endregion

            //region 获取版本号
            if (jsonObject.has("version")) {
                String version = jsonObject.getString("version");
                fw_version.setSummary(version);
            }
            //endregion
            //region ota结果/进度
            if (jsonObject.has("ota_progress")) {
                int ota_progress = jsonObject.getInt("ota_progress");

                if (!(ota_progress >= 0 && ota_progress < 100) && pd != null && pd.isShowing()) {
                    pd.dismiss();

                    String m = "固件更新成功!";
                    if (ota_progress == -1) {
                        m = "固件更新失败!请重试";
                    }
                    if (ota_flag) {
                        ota_flag = false;
                        new android.app.AlertDialog.Builder(getActivity())
                                .setTitle("")
                                .setMessage(m)
                                .setPositiveButton("确定", null)
                                .show();
                    }
                } else {
                    if (ota_flag) {
                        //todo 显示更新进度

                        if(pd!=null && pd.isShowing())
                            pd.setMessage("正在获取最新固件版本,请稍后....\n"+"进度:"+ota_progress+"%");
//                        Toast.makeText(getActivity(), "ota进度:"+ota_progress+"%", Toast.LENGTH_SHORT).show();
                    }
                }

            }
            //endregion
            //region 接收主机setting
            if (jsonObject.has("setting")) jsonSetting = jsonObject.getJSONObject("setting");
            if (jsonSetting != null) {

                //region ota
                if (jsonSetting.has("ota")) {
                    String ota_uri = jsonSetting.getString("ota");
                    if (ota_uri.endsWith("ota.bin")) {
                        ota_flag = true;
                        pd = new ProgressDialog(getActivity());
                        pd.setButton(DialogInterface.BUTTON_POSITIVE, "取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                pd.dismiss();// 关闭ProgressDialog
                                ota_flag = false;
                            }
                        });
                        pd.setCanceledOnTouchOutside(false);
                        pd.setMessage("正在更新固件,请勿断开设备电源!\n大约1分钟左右,请稍后....");
                        pd.show();
//                        handler.sendEmptyMessageDelayed(0,5000);

                    }
                }
                //endregion
            }
            //endregion


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //region MQTT服务有关

    private final ServiceConnection mMQTTServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mConnectService = ((ConnectService.LocalBinder) service).getService();
            Send("{\"mac\":\"" + device_mac + "\",\"version\":null}");
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
