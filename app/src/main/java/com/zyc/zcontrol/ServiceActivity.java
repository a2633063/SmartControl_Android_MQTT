package com.zyc.zcontrol;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class ServiceActivity extends AppCompatActivity {
    public final static String Tag = "ServiceActivity";


    //region 使用本地广播与service通信
    LocalBroadcastManager localBroadcastManager;
    private MsgReceiver msgReceiver;
    //endregion

    ConnectService mConnectService;
    String device_mac;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);


        //region MQTT服务有关
        //region 动态注册接收mqtt服务的广播接收器,
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        Intent getIntent = this.getIntent();
        if (getIntent.hasExtra("mac"))//判断是否有值传入,并判断是否有特定key
        {
            try {
                device_mac = getIntent.getStringExtra("mac");
                intentFilter.addAction(device_mac);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
//        intentFilter.addAction(ConnectService.ACTION_UDP_DATA_AVAILABLE);
//        intentFilter.addAction(ConnectService.ACTION_MQTT_CONNECTED);
//        intentFilter.addAction(ConnectService.ACTION_MQTT_DISCONNECTED);
//        intentFilter.addAction(ConnectService.ACTION_DATA_AVAILABLE);
        }
        localBroadcastManager.registerReceiver(msgReceiver, intentFilter);
        //endregion

        //region 启动MQTT 服务以及启动,无需再启动
        Intent intent = new Intent(this, ConnectService.class);
        bindService(intent, mMQTTServiceConnection, BIND_AUTO_CREATE);
        //endregion
        //endregion


    }

    @Override
    protected void onDestroy() {
        //注销广播
        localBroadcastManager.unregisterReceiver(msgReceiver);
        //停止服务
        Intent intent = new Intent(ServiceActivity.this, ConnectService.class);
        stopService(intent);
        unbindService(mMQTTServiceConnection);
        super.onDestroy();
    }

    //region 事件监听调用函数,主要为在子类中重写此函数实现在service建立成功/mqtt连接成功/失败时执行功能
    //Service建立成功时调用    此函数需要时在子类中重写
    public void ServiceConnected() {

    }

    //mqtt连接成功时调用    此函数需要时在子类中重写
    public void MqttConnected() {

    }

    //mqtt连接断开时调用    此函数需要时在子类中重写
    public void MqttDisconnected() {

    }
    //endregion


    //region 数据发送/接收处理函数
    //此函数在子类中被同名函数调用以发送数据
    public void Send(boolean isUDP, String topic, String message) {
        Log.d(Tag, "Send:[" + topic + "]:" + message);
        mConnectService.Send(isUDP ? null : topic, message);
    }

    //接收处理函数,修改device属性,同时修改ui界面    此函数在子类中需要重写
    public void Receive(String ip, int port, String topic, String message) {

    }
    //endregion

    //region MQTT服务有关


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

            if (ConnectService.ACTION_UDP_DATA_AVAILABLE.equals(action)) {
                String ip = intent.getStringExtra(ConnectService.EXTRA_UDP_DATA_IP);
                String message = intent.getStringExtra(ConnectService.EXTRA_UDP_DATA_MESSAGE);
                int port = intent.getIntExtra(ConnectService.EXTRA_UDP_DATA_PORT, -1);
                Receive(ip, port, null, message);
            } else if (ConnectService.ACTION_MQTT_CONNECTED.equals(action)) {  //连接成功
                Log.d(Tag, "ACTION_MQTT_CONNECTED");
                MqttConnected();
            } else if (ConnectService.ACTION_MQTT_DISCONNECTED.equals(action)) {  //连接失败/断开,尝试重新连接
                Log.w(Tag, "ACTION_MQTT_DISCONNECTED");
                MqttDisconnected();
            } else if (ConnectService.ACTION_DATA_AVAILABLE.equals(action)) {  //接收到数据
                String topic = intent.getStringExtra(ConnectService.EXTRA_DATA_TOPIC);
                String message = intent.getStringExtra(ConnectService.EXTRA_DATA_MESSAGE);
                Receive(null, -1, topic, message);
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
//    void broadcastUpdate(String action) {
//        localBroadcastManager.sendBroadcast(new Intent(ConnectService.ACTION_MAINACTIVITY_DEVICELISTUPDATE));
//    }

}
