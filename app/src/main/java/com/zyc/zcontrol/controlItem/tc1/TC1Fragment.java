package com.zyc.zcontrol.controlItem.tc1;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.zyc.zcontrol.ConnectService;
import com.zyc.zcontrol.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class TC1Fragment extends Fragment {
    public final static String Tag = "TC1Fragment";


    //region 使用本地广播与service通信
    LocalBroadcastManager localBroadcastManager;
    private MsgReceiver msgReceiver;
    ConnectService mConnectService;
    //endregion

    //region 控件
    ToggleButton tbtn_all;
    ToggleButton tbtn_main_button[] = new ToggleButton[6];
    TextView tv_main_button[] = new TextView[6];
    //endregion

    TextView log;

    String device_mac = null;
    String device_name = null;

    public TC1Fragment() {

        // Required empty public constructor
    }

    @SuppressLint("ValidFragment")
    public TC1Fragment(String name, String mac) {
        this.device_mac = mac;
        this.device_name = name;
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_tc1, container, false);

        //region MQTT服务有关
        //region 动态注册接收mqtt服务的广播接收器,
        localBroadcastManager = LocalBroadcastManager.getInstance(getContext());
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectService.ACTION_UDP_DATA_AVAILABLE);
        intentFilter.addAction(ConnectService.ACTION_MQTT_CONNECTED);
        intentFilter.addAction(ConnectService.ACTION_MQTT_DISCONNECTED);
        intentFilter.addAction(ConnectService.ACTION_DATA_AVAILABLE);
        localBroadcastManager.registerReceiver(msgReceiver, intentFilter);
        //endregion

        //region 启动MQTT 服务以及启动,无需再启动
        Intent intent = new Intent(getContext(), ConnectService.class);
        getActivity().bindService(intent, mMQTTServiceConnection, BIND_AUTO_CREATE);
        //endregion
        //endregion

        //region 控件初始化

        tbtn_all = view.findViewById(R.id.tbtn_all);
        tbtn_main_button[0] = view.findViewById(R.id.tbtn_main_button1);
        tbtn_main_button[1] = view.findViewById(R.id.tbtn_main_button2);
        tbtn_main_button[2] = view.findViewById(R.id.tbtn_main_button3);
        tbtn_main_button[3] = view.findViewById(R.id.tbtn_main_button4);
        tbtn_main_button[4] = view.findViewById(R.id.tbtn_main_button5);
        tbtn_main_button[5] = view.findViewById(R.id.tbtn_main_button6);
        tv_main_button[0] = view.findViewById(R.id.tv_main_button1);
        tv_main_button[1] = view.findViewById(R.id.tv_main_button2);
        tv_main_button[2] = view.findViewById(R.id.tv_main_button3);
        tv_main_button[3] = view.findViewById(R.id.tv_main_button4);
        tv_main_button[4] = view.findViewById(R.id.tv_main_button5);
        tv_main_button[5] = view.findViewById(R.id.tv_main_button6);


        tbtn_all.setOnClickListener(MainButtonListener);
        for (int i = 0; i < 6; i++) {
            tbtn_main_button[i].setId(i);
            tv_main_button[i].setId(i);
            tbtn_main_button[i].setOnClickListener(MainButtonListener);
        }
        //region log 相关
        final ScrollView scrollView = (ScrollView) view.findViewById(R.id.scrollView);
        log = (TextView) view.findViewById(R.id.tv_log);
        log.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                scrollView.post(new Runnable() {
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
        DateFormat df = new SimpleDateFormat("---- yyyy/hh/dd HH:mm:ss ----");
        log.setText(df.format(new Date()));
        //endregion

        //endregion

        return view;
    }

    @Override
    public void onDestroy() {
        //注销广播
        localBroadcastManager.unregisterReceiver(msgReceiver);
        //停止服务
        getActivity().unbindService(mMQTTServiceConnection);
        super.onDestroy();
    }

    //region 按钮事件
    private View.OnClickListener MainButtonListener = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            int id = arg0.getId();
            if (id >= 0 && id <= 5)
                Send("{\"name\":\"" + device_name + "\",\"mac\":\"" + device_mac + "\",\"plug_" + id + "\":{\"on\":" + String.valueOf(((ToggleButton) arg0).isChecked() ? 1 : 0) + "}" + "}");
            else if (id == tbtn_all.getId()) {
                Send("{\"name\":\"" + device_name + "\",\"mac\":\"" + device_mac + "\",\"nvalue\":" + String.valueOf(((ToggleButton) arg0).isChecked() ? 1 : 0) + "}");
            }

        }

    };
//endregion


    void Send(String message) {
        boolean b = getActivity().getSharedPreferences("Setting_" + device_mac, 0).getBoolean("always_UDP", false);
        mConnectService.Send(b ? null : "domoticz/out", message);
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
            if (mac == null || !mac.equals(device_mac)) return;


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //region MQTT服务有关

    private final ServiceConnection mMQTTServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mConnectService = ((ConnectService.LocalBinder) service).getService();
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
            } else if (ConnectService.ACTION_MQTT_CONNECTED.equals(action)) {  //连接成功
                Log.d(Tag, "ACTION_MQTT_CONNECTED");
                Log("服务器已连接");
            } else if (ConnectService.ACTION_MQTT_DISCONNECTED.equals(action)) {  //连接失败/断开
                Log.w(Tag, "ACTION_MQTT_DISCONNECTED");
                Log("服务器已断开");
            } else if (ConnectService.ACTION_DATA_AVAILABLE.equals(action)) {  //接收到数据
                String topic = intent.getStringExtra(ConnectService.EXTRA_DATA_TOPIC);
                String message = intent.getStringExtra(ConnectService.EXTRA_DATA_MESSAGE);
                Receive(topic, message);
            }
        }
    }
    //endregion


    void Log(String str) {
        log.setText(log.getText() + "\n" + str);
    }
}
