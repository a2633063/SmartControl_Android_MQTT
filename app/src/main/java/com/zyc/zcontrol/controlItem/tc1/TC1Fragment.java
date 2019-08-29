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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.zyc.zcontrol.ConnectService;
import com.zyc.zcontrol.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class TC1Fragment extends Fragment {
    public final static String Tag = "TC1Fragment";

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    //region 使用本地广播与service通信
    LocalBroadcastManager localBroadcastManager;
    private MsgReceiver msgReceiver;
    ConnectService mConnectService;
    //endregion

    //region 控件
    final private int PLUG_COUNT = 6;

    private SwipeRefreshLayout mSwipeLayout;
    Switch tbtn_all;
    TextView tv_power;
    Switch tbtn_main_button[] = new Switch[PLUG_COUNT];
    TextView tv_main_button[] = new TextView[PLUG_COUNT];
    TextView tv_total_time;
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

    //region Handler
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {// handler接收到消息后就会执行此方法
            switch (msg.what) {
                case 1:
                    Send("{\"mac\": \"" + device_mac + "\","
                            + "\"plug_0\" : {\"on\" : null,\"setting\":{\"name\":null}},"
                            + "\"plug_1\" : {\"on\" : null,\"setting\":{\"name\":null}},"
                            + "\"plug_2\" : {\"on\" : null,\"setting\":{\"name\":null}},"
                            + "\"plug_3\" : {\"on\" : null,\"setting\":{\"name\":null}},"
                            + "\"plug_4\" : {\"on\" : null,\"setting\":{\"name\":null}},"
                            + "\"plug_5\" : {\"on\" : null,\"setting\":{\"name\":null}}}");
                    break;
            }
        }
    };

    //endregion
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

        //region 控制按钮/功率/运行时间等
        tv_total_time = view.findViewById(R.id.tv_total_time);
        tbtn_all = view.findViewById(R.id.tbtn_all);
        tv_power = view.findViewById(R.id.tv_power);
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
            tv_main_button[i].setId(i + PLUG_COUNT);
            tbtn_main_button[i].setOnClickListener(MainButtonListener);
            tbtn_main_button[i].setOnCheckedChangeListener(MainButtonChangeListener);
            tv_main_button[i].setOnClickListener(MainTextListener);
        }
        //endregion

        //region 更新当前状态
        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        mSwipeLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimaryDark, R.color.colorAccent, R.color.colorPrimary);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                handler.sendEmptyMessageDelayed(1, 0);
                mSwipeLayout.setRefreshing(false);
            }
        });
        //endregion


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
        DateFormat df = new SimpleDateFormat("---- yyyy/MM/dd HH:mm:ss ----");
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
    // region 开关
    private View.OnClickListener MainButtonListener = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            int id = arg0.getId();
            if (id >= 0 && id <= 5)
                Send("{\"name\":\"" + device_name + "\",\"mac\":\"" + device_mac + "\",\"plug_" + id + "\":{\"on\":" + String.valueOf(((Switch) arg0).isChecked() ? 1 : 0) + "}" + "}");
            else if (id == tbtn_all.getId()) {
                int s = ((Switch) arg0).isChecked() ? 1 : 0;
                Send("{\"name\":\"" + device_name + "\",\"mac\":\"" + device_mac + "\","
                        + "\"plug_0\":{\"on\":" + s + "},"
                        + "\"plug_1\":{\"on\":" + s + "},"
                        + "\"plug_2\":{\"on\":" + s + "},"
                        + "\"plug_3\":{\"on\":" + s + "},"
                        + "\"plug_4\":{\"on\":" + s + "},"
                        + "\"plug_5\":{\"on\":" + s + "}"
                        + "}");
            }

        }

    };

    private Switch.OnCheckedChangeListener MainButtonChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            for (int i = 0; i < tbtn_main_button.length; i++) {
                if (tbtn_main_button[i].isChecked()) {
                    tbtn_all.setChecked(true);
                    return;
                }
            }
            tbtn_all.setChecked(false);
        }
    };
    //endregion
    // region 文本

    private View.OnClickListener MainTextListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getContext(), TC1PlugActivity.class);
            intent.putExtra("name", device_name);
            intent.putExtra("plug_name", ((TextView) v).getText());
            intent.putExtra("mac", device_mac);
            intent.putExtra("plug_id", v.getId() % PLUG_COUNT);
            startActivity(intent);
        }
    };
    //endregion
    //endregion


    void Send(String message) {
        if (mConnectService == null) return;
        boolean b = getActivity().getSharedPreferences("Setting_" + device_mac, 0).getBoolean("always_UDP", false);
        mConnectService.Send(b ? null : "device/ztc1/" + device_mac + "/set", message);
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

            if (jsonObject.has("power")) {
                String power = jsonObject.getString("power");
                Log.d(Tag, "power:" + power);
                tv_power.setText(power + "W");
            }
            if (jsonObject.has("total_time")) {
                int total_time = jsonObject.getInt("total_time");
                Log.d(Tag, "total_time:" + total_time);


                Calendar calendar = Calendar.getInstance();
//                now.setTime(d);
                calendar.add(Calendar.SECOND, 0 - total_time);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

                tv_total_time.setText("运行时间:" + total_time + "秒\n上次开机时间:" + sdf.format(calendar.getTime()));

            }
            //region 解析plug
            for (int plug_id = 0; plug_id < 6; plug_id++) {
                if (!jsonObject.has("plug_" + plug_id)) continue;
                JSONObject jsonPlug = jsonObject.getJSONObject("plug_" + plug_id);
                if (jsonPlug.has("on")) {
                    int on = jsonPlug.getInt("on");
                    tbtn_main_button[plug_id].setChecked(on != 0);
                }
                if (!jsonPlug.has("setting")) continue;
                JSONObject jsonPlugSetting = jsonPlug.getJSONObject("setting");
                if (jsonPlugSetting.has("name")) {
                    tv_main_button[plug_id].setText(jsonPlugSetting.getString("name"));
                }
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
            handler.sendEmptyMessageDelayed(1, 300);
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
                Log("app已连接mqtt服务器");
                handler.sendEmptyMessageDelayed(1, 300);
            } else if (ConnectService.ACTION_MQTT_DISCONNECTED.equals(action)) {  //连接失败/断开
                Log.w(Tag, "ACTION_MQTT_DISCONNECTED");
                Log("已与mqtt服务器已断开");
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
