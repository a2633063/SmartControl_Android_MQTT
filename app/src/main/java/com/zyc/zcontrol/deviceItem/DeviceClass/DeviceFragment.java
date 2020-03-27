package com.zyc.zcontrol.deviceItem.DeviceClass;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.zyc.zcontrol.ConnectService;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class DeviceFragment extends Fragment implements View.OnLongClickListener {
    public final static String Tag = "DeviceFragment";

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    //region 使用本地广播与service通信
    LocalBroadcastManager localBroadcastManager;
    private MsgReceiver msgReceiver;
    ConnectService mConnectService;
    //endregion

    TextView log = null;
    String device_mac = null;
    String device_name = null;

    public DeviceFragment() {
    }

    @SuppressLint("ValidFragment")
    public DeviceFragment(String name, String mac) {
        this.device_mac = mac;
        this.device_name = name;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //region 动态注册广播接收器,
        localBroadcastManager = LocalBroadcastManager.getInstance(getContext());
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(ConnectService.ACTION_UDP_DATA_AVAILABLE);
        intentFilter.addAction(ConnectService.ACTION_MQTT_CONNECTED);
        intentFilter.addAction(ConnectService.ACTION_MQTT_DISCONNECTED);
//        intentFilter.addAction(ConnectService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(device_mac);
//        intentFilter.addAction(device_mac+"UI");
        localBroadcastManager.registerReceiver(msgReceiver, intentFilter);
        //endregion
        //region 启动MQTT 服务以及启动,无需再启动
        Intent intent = new Intent(getContext(), ConnectService.class);
        getActivity().bindService(intent, mMQTTServiceConnection, BIND_AUTO_CREATE);
        //endregion

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroy() {
        //注销广播
        localBroadcastManager.unregisterReceiver(msgReceiver);
        //停止服务
        getActivity().unbindService(mMQTTServiceConnection);
        super.onDestroy();
    }


    //region 数据发送/接收处理函数,并在log中显示网络状态(仅改变时显示) 在子类中重写此函数
    //当前网络状态标志位,1udp   2mqtt
    int send_net_flag = 0;
    int rece_net_flag = 0;

    public final void Send(boolean isUDP, String topic, String message) {
        Log.v(Tag, "Send:[" + topic + "]:" + message);
        mConnectService.Send(isUDP ? null : topic, message);
        if (!isUDP && topic != null && mConnectService.isConnected() && send_net_flag != 2) {   //当前通过mqtt发送
            send_net_flag = 2;
            Log("当前发送消息为:mqtt");
        } else if ((isUDP || topic == null || !mConnectService.isConnected()) && send_net_flag != 1) { //当前通过udp发送
            send_net_flag = 1;
            Log("当前发送消息为:udp" + (mConnectService.isConnected() ? "(app已连接mqtt服务器)" : ""));
        }
    }

    //接收处理函数,修改device属性,同时修改ui界面
    //此处要先处理判断网络状态显示log信息,因此需要执行super
    @CallSuper
    public void Receive(String ip, int port, String topic, String message) {
        if (topic != null && rece_net_flag != 2) {  //当前消息为mqtt消息
            rece_net_flag = 2;
            Log("当前接收消息为:mqtt");
        } else if (topic == null && rece_net_flag != 1) {  //当前消息为udp消息
            rece_net_flag = 1;
            Log("当前接收消息为:udp" );
            if (mConnectService.isConnected()) {
                Log("必须同步过mqtt数据,设备才能连接上mqtt服务器");
            }
        }

        //region 反馈mqtt设置回应
        try {
            JSONObject jsonObject = new JSONObject(message);
            JSONObject jsonSetting = jsonObject.getJSONObject("setting");
            if (jsonObject.getString("mac").equals(device_mac)) {
                String toastStr = "已设置\"" + device_name + "\"mqtt服务器:\r\n"
                        + jsonSetting.getString("mqtt_uri") + ":" + jsonSetting.getInt("mqtt_port")
                        + "\n" + jsonSetting.getString("mqtt_user");
                Toast.makeText(getActivity(), toastStr, Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            // e.printStackTrace();
        }
        //endregion
    }
    //endregion

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
                Log("app已连接mqtt服务器");
                MqttConnected();
            } else if (ConnectService.ACTION_MQTT_DISCONNECTED.equals(action)) {  //连接失败/断开
                Log.w(Tag, "ACTION_MQTT_DISCONNECTED");
                Log("app已断开mqtt服务器");
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

    //region log TextView处理
    protected void setLogTextView(TextView v) {
        log = v;
        DateFormat df = new SimpleDateFormat("---- yyyy/MM/dd HH:mm:ss ----");
        log.setText(df.format(new Date()));
        log.setOnLongClickListener(this);

        final View parent = (View) log.getParent().getParent();
        if (parent != null && parent instanceof ScrollView) {
            log.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    ((ScrollView) parent).post(new Runnable() {
                        public void run() {
                            ((ScrollView) parent).fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            });
        }
//        log.post(new Runnable() {
//            public void run() {
//                Log.e("test","post"+device_mac);
//                rece_net_flag=0;
//                send_net_flag=0;
//            }
//        });
    }

    protected void Log(String str) {
        if (log == null) return;

        DateFormat df = new SimpleDateFormat("[HH:mm:ss.sss]");
        log.setText(log.getText() + "\n" + df.format(new Date()) + "" + str);
//        log.setText(log.getText() + "\n" + str);
        Log.w("log", "log:" + log.getText());
//        log.append();
    }

    //用于Log textview长按清除功能 在继承中实使用setOnLongClickListener(this);绑定
    @Override
    public boolean onLongClick(View v) {
        final TextView l = (TextView) v;
        if (((TextView) v).length() > 0) {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                    .setTitle("清除log?")
                    .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            DateFormat df = new SimpleDateFormat("---- yyyy/MM/dd HH:mm:ss ----");
                            l.setText(df.format(new Date()) + "\nlog已经清空");

                        }
                    })
                    .setNegativeButton("取消", null)
                    .create();
            alertDialog.show();
        }
        return false;
    }
    //endregion

}
