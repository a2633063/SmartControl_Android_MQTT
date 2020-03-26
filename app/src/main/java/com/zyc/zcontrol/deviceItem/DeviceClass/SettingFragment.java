package com.zyc.zcontrol.deviceItem.DeviceClass;


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
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.CallSuper;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.zyc.zcontrol.ConnectService;

import static android.content.Context.BIND_AUTO_CREATE;

public class SettingFragment extends PreferenceFragment implements AdapterView.OnItemLongClickListener {
    public final static String Tag = "DeviceFragment";

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    //region 使用本地广播与service通信
    LocalBroadcastManager localBroadcastManager;
    private MsgReceiver msgReceiver;
    ConnectService mConnectService;
    //endregion


    String device_mac = null;
    String device_name = null;

    public SettingFragment() {
    }

    @SuppressLint("ValidFragment")
    public SettingFragment(String name, String mac) {
        this.device_mac = mac;
        this.device_name = name;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(getActivity());
        setPreferenceScreen(root);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = super.onCreateView(inflater, container, savedInstanceState);
        //region 长按功能
        if (result != null) {
            View lv = result.findViewById (android.R.id.list);
            if (lv instanceof ListView) {
                ((ListView)lv).setOnItemLongClickListener(this);
            }
        }
        //endregion

        //region 动态注册广播接收器,
        localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(ConnectService.ACTION_UDP_DATA_AVAILABLE);
//        intentFilter.addAction(ConnectService.ACTION_MQTT_CONNECTED);
//        intentFilter.addAction(ConnectService.ACTION_MQTT_DISCONNECTED);
//        intentFilter.addAction(ConnectService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(device_mac);
//        intentFilter.addAction(device_mac+"UI");
        localBroadcastManager.registerReceiver(msgReceiver, intentFilter);
        //endregion
        //region 启动MQTT 服务以及启动,无需再启动
        Intent intent = new Intent(getActivity(), ConnectService.class);
        getActivity().bindService(intent, mMQTTServiceConnection, BIND_AUTO_CREATE);
        //endregion


        return result;
    }

    @Override
    public void onDestroy() {
        //注销广播
        localBroadcastManager.unregisterReceiver(msgReceiver);
        //停止服务
        getActivity().unbindService(mMQTTServiceConnection);
        super.onDestroy();
    }

    //region 配置长按功能
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return false;
    }
    //endregion


    //region 数据发送/接收处理函数
    //此函数在子类中被同名函数调用以发送数据
    public void Send(boolean isUDP, String topic, String message) {
        Log.d(Tag, "Send:[" + topic + "]:" + message);
        mConnectService.Send(isUDP ? null : topic, message);
    }

    //接收处理函数,修改device属性,同时修改ui界面    此函数在子类中需要重写
    @CallSuper
    public void Receive(String ip, int port, String topic, String message) {

    }
    //endregion

    //region 事件监听调用函数,主要为在子类中重写此函数实现在service建立成功/mqtt连接成功/失败时执行功能
    //Service建立成功时调用    此函数需要时在子类中重写
    public void ServiceConnected(){

    }
    //mqtt连接成功时调用    此函数需要时在子类中重写
    public void MqttConnected(){

    }

    //mqtt连接断开时调用    此函数需要时在子类中重写
    public void MqttDisconnected(){

    }
    //endregion
    //region mqtt服务及广播接收有关
    private final ServiceConnection mMQTTServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mConnectService = ((ConnectService.LocalBinder) service).getService();
            ServiceConnected();
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
            if (ConnectService.ACTION_MQTT_CONNECTED.equals(action)) {  //连接成功
                Log.d(Tag, "ACTION_MQTT_CONNECTED");
                MqttConnected();
            } else if (ConnectService.ACTION_MQTT_DISCONNECTED.equals(action)) {  //连接失败/断开
                Log.w(Tag, "ACTION_MQTT_DISCONNECTED");
                MqttDisconnected();
            } else if (action.equals(device_mac)) {//接收到设备独立数据
                String ip = intent.getStringExtra(ConnectService.EXTRA_UDP_DATA_IP);
                int port = intent.getIntExtra(ConnectService.EXTRA_UDP_DATA_PORT, -1);
                String topic = intent.getStringExtra(ConnectService.EXTRA_DATA_TOPIC);
                String message = intent.getStringExtra(ConnectService.EXTRA_DATA_MESSAGE);
                Receive(ip, port, topic, message);
            }
        }
    }
    //endregion

}