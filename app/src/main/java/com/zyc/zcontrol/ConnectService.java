package com.zyc.zcontrol;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ConnectService extends Service {
    public final static String Tag = "ConnectService";

    //region 广播静态变量


    public final static String ACTION_MAINACTIVITY_DEVICELISTUPDATE = "com.zyc.zcontrol.mainactivity.DEVICELISTUPDATE";
    //region MQTT相关
    public final static String ACTION_MQTT_CONNECTED = "com.zyc.zcontrol.mqtt.ACTION_MQTT_CONNECTED";
    public final static String ACTION_MQTT_DISCONNECTED = "com.zyc.zcontrol.mqtt.ACTION_MQTT_DISCONNECTED";
    public final static String ACTION_DATA_AVAILABLE = "com.zyc.zcontrol.mqtt.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA_TOPIC = "com.zyc.zcontrol.mqtt.EXTRA_DATA_TOPIC";
    public final static String EXTRA_DATA_MESSAGE = "com.zyc.zcontrol.mqtt.EXTRA_DATA_MESSAGE";
    //endregion

    //region UDP相关
    public final static String ACTION_UDP_DATA_AVAILABLE = "com.zyc.zcontrol.mqtt.ACTION_UDP_DATA_AVAILABLE";
    public final static String EXTRA_UDP_DATA_IP = "com.zyc.zcontrol.mqtt.EXTRA_UDP_DATA_IP";
    public final static String EXTRA_UDP_DATA_PORT = "com.zyc.zcontrol.mqtt.EXTRA_UDP_DATA_PORT";
    public final static String EXTRA_UDP_DATA_MESSAGE = "com.zyc.zcontrol.mqtt.EXTRA_UDP_DATA_MESSAGE";
    //endregion

    //endregion

    public final static int PHONE_UDP_PORT = 10181;
    public final static int DEVICE_UDP_PORT = 10182;

    //region 广播相关定义
    private LocalBroadcastManager localBroadcastManager;
    //endregion

    public String mqtt_uri = "";
    public String mqtt_id = null;
    public String mqtt_user = "";
    public String mqtt_password = "";


    MqttAsyncClient mqttClient = null;
    DatagramSocket datagramSocket = null;


    public ConnectService() {
    }


    //region 线程函数,udp监听
    boolean flag = true;
    Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            Log.d("UDPThread", "start");
            try {
                //1、创建udp socket ，建立端点，并指定固定端口
                if (datagramSocket == null)
                    datagramSocket = new DatagramSocket(PHONE_UDP_PORT);
                //2、定义数据包，用于存储数据
                byte[] buf = new byte[1024];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                while (flag) {

                    //3、通过服务的receive方法，接收数据并存入数据包中
                    try {
                        datagramSocket.setSoTimeout(500);
                        datagramSocket.receive(dp);
                        //4、通过数据包中的方法，获取其中的数据。
                        String ip = dp.getAddress().getHostAddress();
                        int port = dp.getPort();
                        String data = new String(dp.getData(), 0, dp.getLength());
                        broadcastUpdate(ACTION_UDP_DATA_AVAILABLE, ip, port, data);
                    } catch (IOException e) {
//                        e.printStackTrace();
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }

            //断开连接

            //关闭资源
            if (datagramSocket != null)
                datagramSocket.close();
            Log.d("UDPThread", "end");

        }
    });
    //endregion

    //region Service相关配置
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public ConnectService getService() {
            return ConnectService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    //endregion
    @Override
    public void onCreate() {
        Log.d(Tag, "OnCreate");

        //region 广播相关初始化
        try {
            localBroadcastManager = LocalBroadcastManager.getInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
            this.stopSelf();
        }
        //endregion

        //region UDP监听初始化
        try {
            datagramSocket = new DatagramSocket(PHONE_UDP_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
            datagramSocket = null;
        }
        //endregion

        thread.start(); //启动UDP监听进程
    }

    @Override
    public void onDestroy() {
        disconnect();
        flag = false;
        super.onDestroy();
        Log.d(Tag, "onDestroy");
    }

    //region 广播子函数
    void broadcastUpdate(String action) {
        final Intent intent = new Intent(action);
        localBroadcastManager.sendBroadcast(intent);
    }

    void broadcastUpdate(String action, String ip, int port, String message) {
        final Intent intent = new Intent(action);

        intent.putExtra(EXTRA_UDP_DATA_IP, ip);
        intent.putExtra(EXTRA_UDP_DATA_PORT, port);
        intent.putExtra(EXTRA_UDP_DATA_MESSAGE, message);
        localBroadcastManager.sendBroadcast(intent);
    }

    void broadcastUpdate(String action, String topic, MqttMessage message) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA_TOPIC, topic);
        intent.putExtra(EXTRA_DATA_MESSAGE, new String(message.getPayload()));
        localBroadcastManager.sendBroadcast(intent);
    }
    //endregion

    //region MQTT连接/状态函数/订阅topic/取消订阅topic
    public void connect(String mqtt_uri, String mqtt_id, String mqtt_user, String mqtt_password) {
        if (mqtt_uri == null || mqtt_uri.length() < 3) return;
        if (mqtt_user == null) mqtt_user = "";
        if (mqtt_password == null) mqtt_password = "";


        this.mqtt_uri = mqtt_uri;
        this.mqtt_id = mqtt_id;
        this.mqtt_user = mqtt_user;
        this.mqtt_password = mqtt_password;


        //消息缓存方式，内存缓存
        MemoryPersistence persistence = new MemoryPersistence();
        try {

            //region 建立客户端连接的配置参数
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);  //不记忆上次会话
            if (mqtt_user != null && mqtt_user.length() > 0)
                connectOptions.setUserName(mqtt_user); //用户名
            if (mqtt_password != null && mqtt_password.length() > 0)
                connectOptions.setPassword(mqtt_password.toCharArray()); //密码
            connectOptions.setConnectionTimeout(30);  //超时时间
            connectOptions.setKeepAliveInterval(60); //心跳时间,单位秒
            connectOptions.setAutomaticReconnect(true);//自动重连
            //setWill方法，如果项目中需要知道客户端是否掉线可以调用该方法。设置最终端口的通知消息
            //connectOptions.setWill(topic, "close".getBytes(), 2, true);
            Log.d("ConnectService", "read to connecting to MQTT server");
            //endregion
            if (mqttClient == null)
                mqttClient = new MqttAsyncClient("tcp://" + mqtt_uri, mqtt_id, persistence);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.d("MQTTThread", "connectionLost");
                    broadcastUpdate(ACTION_MQTT_DISCONNECTED);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
//        Log.d("MQTTThread", "topic:" + topic);
//        Log.d("MQTTThread", "Qos:" + message.getQos());
//        Log.d("MQTTThread", "message content:" + new String(message.getPayload()));
                    broadcastUpdate(ACTION_DATA_AVAILABLE, topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
//                    Log.d("MQTTThread", "deliveryComplete");
                }
            });
            //连接服务器

            mqttClient.connect(connectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("ConnectService", "connect onSuccess");
//                    try {
//                        mqttClient.subscribe("domoticz/out", 0);
//                        mqttClient.subscribe("device/+/+/state", 0);
//                        mqttClient.subscribe("device/+/+/sensor", 0);
                    //mqttClient.subscribe("homeassistant/+/+/#", 0);
                    broadcastUpdate(ACTION_MQTT_CONNECTED); //连接成功
//                    } catch (MqttException e) {
//                        e.printStackTrace();
//                        Log.d("ConnectService", "connect fail");
//                        broadcastUpdate(ACTION_MQTT_DISCONNECTED); //连接失败
//                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e("ConnectService", "onFailure:" + exception.getMessage());
                    broadcastUpdate(ACTION_MQTT_DISCONNECTED); //连接失败
                }
            });


            //mqttClient.connect(connectOptions);

            //订阅消息
//            mqttClient.subscribe("/test/android", 0);

        } catch (MqttException e) {
            Log.e("ConnectService", "reason " + e.getReasonCode());
            Log.e("ConnectService", "msg " + e.getMessage());
            Log.e("ConnectService", "loc " + e.getLocalizedMessage());
            Log.e("ConnectService", "cause " + e.getCause());
            Log.e("ConnectService", "excep " + e);
            e.printStackTrace();
            broadcastUpdate(ACTION_MQTT_DISCONNECTED); //连接失败
        }
    }

    public boolean isConnected() {
        if (mqttClient == null) return false;
        return mqttClient.isConnected();
    }

    public void disconnect() {
        try {
            if (mqttClient != null)
                mqttClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(String topic, int qos) {
        try {
            mqttClient.subscribe(topic, qos);
        } catch (MqttException e) {
            e.printStackTrace();
            disconnect();
        }
    }

    public void subscribe(String[] topic, int[] qos) {
        try {
            mqttClient.subscribe(topic, qos);
        } catch (MqttException e) {
            e.printStackTrace();
            disconnect();
        }
    }

    public void unsubscribe(String topic) {
        try {
            mqttClient.unsubscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
            disconnect();
        }
    }

    public void unsubscribe(String[] topic) {
        try {
            mqttClient.unsubscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
            disconnect();
        }
    }

    //endregion

    //region 发送

    //topic为null时始终使用udp发送,否则根据mqtt连接状态现在udp或mqtt发送
    public void Send(String topic, String str) {

        if (!isConnected() || topic == null) {
            UDPsend(str);
        } else {
            MQTTSend(topic, str);
        }
    }

    //region UDP发送
    public void UDPsend(String message) {
        UDPsend("255.255.255.255", DEVICE_UDP_PORT, message);
    }

    public void UDPsend(String ip, String message) {
        UDPsend(ip, DEVICE_UDP_PORT, message);
    }

    public void UDPsend(String ip, int port, String message) {

        if (message == null || message.length() < 1) return;
        if (port < 1) port = DEVICE_UDP_PORT;
        if (ip == null) ip = "255.255.255.255";
        try {
            if (datagramSocket == null)
                datagramSocket = new DatagramSocket(PHONE_UDP_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        InetAddress local = null;
        try {
            // 换成服务器端IP
            local = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        final DatagramPacket p = new DatagramPacket(message.getBytes(), message.getBytes().length,
                local, port);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    datagramSocket.send(p);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    //endregion

    //region MQTT发送函数
    public void MQTTSend(String topic, String str) {
        MQTTSend(topic, str, 1);
    }

    public void MQTTSend(String topic, String str, int qos) {
        //region 发送

        try {
            MqttMessage message = new MqttMessage(str.getBytes());
            message.setQos(qos);
            mqttClient.publish(topic, message);//发布消息
        } catch (MqttException e) {
            e.printStackTrace();
        }
        //endregion

    }
    //endregion
    //endregion
}
