package com.zyc.zcontrol;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Random;

public class MQTTService extends Service {

    //region 广播相关定义
    private MsgReceiver msgReceiver;
    private LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
    //endregion

    private String mqtt_uri = null;
    private String mqtt_id = null;
    private String mqtt_user = null;
    private String mqtt_password = null;

    boolean flag = true;


    private String mqtt_send_topic = null;
    private String mqtt_send_string = null;
    private int qos = 0;

    public MQTTService() {
    }

    //region 广播接收,用于处理接收到的数据
    public class MsgReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //拿到进度，更新UI
//            int progress = intent.getIntExtra("progress", 0);
//            Log.d("MainActivity", "MsgReceiver:" + progress);
            mqtt_send_topic = intent.getStringExtra("topic");
            mqtt_send_string = intent.getStringExtra("string");
            qos = intent.getIntExtra("qos", 0);
        }

    }
    //endregion

    //region 线程函数,mqtt相关功能在此实现
    Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            MqttClient mqttClient = null;
            Log.d("MQTTThread", "start");
            while (flag) {

                //region 等待获取到mqtt相关信息
                while ((mqtt_uri == null || mqtt_id == null
                        || mqtt_user == null || mqtt_password == null) && flag) ;
                if (flag == false) break;
                //endregion
                Log.d("MQTTThread", "url:" + mqtt_uri + ",id:" + mqtt_id
                        + "user,pwd:" + mqtt_user + "," + mqtt_password);


                //消息缓存方式，内存缓存
                MemoryPersistence persistence = new MemoryPersistence();
                try {
                    if (mqttClient != null && mqttClient.isConnected()) {
                        mqttClient.disconnect();
                        mqttClient = null;
                    }

                    //region 建立客户端
                    mqttClient = new MqttClient(mqtt_uri, mqtt_id, persistence);
                    //连接的配置参数
                    MqttConnectOptions connectOptions = new MqttConnectOptions();
                    connectOptions.setCleanSession(true);  //不记忆上次会话
                    connectOptions.setUserName(mqtt_user); //用户名
                    connectOptions.setPassword(mqtt_password.toCharArray()); //密码
                    connectOptions.setConnectionTimeout(30);  //超时时间
                    connectOptions.setKeepAliveInterval(60); //心跳时间,单位秒
                    connectOptions.setAutomaticReconnect(true);//自动重连
                    //setWill方法，如果项目中需要知道客户端是否掉线可以调用该方法。设置最终端口的通知消息
                    //            connectOptions.setWill(topic, "close".getBytes(), 2, true);
                    Log.d("MQTTThread", "connecting to broker");
                    //endregion


                    mqttClient.setCallback(new MqttCallback() {
                        @Override
                        public void connectionLost(Throwable cause) {
                            Log.d("MQTTThread", "connectionLost");

                        }

                        @Override
                        public void messageArrived(String topic, MqttMessage message) throws Exception {
                            Log.d("MQTTThread", "topic:" + topic);
                            Log.d("MQTTThread", "Qos:" + message.getQos());
                            Log.d("MQTTThread", "message content:" + new String(message.getPayload()));

                            //region 广播测试
                            Intent intent = new Intent("com.zyc.zcontrol.MQTTRECEIVER");
                            intent.putExtra("string", new String(message.getPayload()));
                            localBroadcastManager.sendBroadcast(intent);
                            //endregion

                        }

                        @Override
                        public void deliveryComplete(IMqttDeliveryToken token) {
                            Log.d("MQTTThread", "deliveryComplete");
                        }
                    });
                    //连接服务器
                    mqttClient.connect(connectOptions);

                    //订阅消息
                    mqttClient.subscribe("/test/androidGet", 0);

                    while (flag) {
                        while (mqtt_send_topic != null &&
                                mqtt_send_string != null && flag) {
                            MqttMessage message = new MqttMessage(mqtt_send_string.getBytes());
                            //设定消息发送等级
                            message.setQos(qos);
                            //发布消息
                            mqttClient.publish(/*topic*/mqtt_send_topic, message);
                            mqtt_send_topic = null;
                            mqtt_send_string = null;
                        }
                    }


                    // System.exit (0);//关闭UI进程
                } catch (MqttException e) {
                    Log.e("MQTTThread", "reason " + e.getReasonCode());
                    Log.e("MQTTThread", "msg " + e.getMessage());
                    Log.e("MQTTThread", "loc " + e.getLocalizedMessage());
                    Log.e("MQTTThread", "cause " + e.getCause());
                    Log.e("MQTTThread", "excep " + e);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ee) {
                    }
                    e.printStackTrace();
                }

            }
            //断开连接
            if (mqttClient != null) {
                try {
                    mqttClient.disconnect();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
            Log.d("MQTTThread", "end");

        }
    });
    //endregion


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        Log.d("MQTTService", "OnCreate");

        //region 参数初始化
        mqtt_uri = null;
        mqtt_id = null;
        mqtt_user = null;
        mqtt_password = null;

        flag = true;
        //endregion


        //region 动态注册广播接收器
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.zyc.zcontrol.MQTTSEND");
        localBroadcastManager.registerReceiver(msgReceiver, intentFilter);
        //endregion

        //region 广播测试
        Intent intent = new Intent("com.zyc.zcontrol.MQTTRECEIVER");
        intent.putExtra("progress", 50);
        localBroadcastManager.sendBroadcast(intent);
        //endregion
        thread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.hasExtra("mqtt_uri"))
            mqtt_uri = intent.getStringExtra("mqtt_uri");
        if (intent.hasExtra("mqtt_id"))
            mqtt_id = intent.getStringExtra("mqtt_id");
        if (intent.hasExtra("mqtt_user"))
            mqtt_user = intent.getStringExtra("mqtt_user");
        if (intent.hasExtra("mqtt_password"))
            mqtt_password = intent.getStringExtra("mqtt_password");


        Log.d("MQTTService", "onStartCommand");

        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        localBroadcastManager.unregisterReceiver(msgReceiver);
        flag = false;
        super.onDestroy();
        Log.d("MQTTService", "onDestroy");
    }

}
