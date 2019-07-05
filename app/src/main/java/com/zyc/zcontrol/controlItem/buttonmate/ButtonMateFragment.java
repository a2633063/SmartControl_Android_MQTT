package com.zyc.zcontrol.controlItem.buttonmate;


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
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

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
public class ButtonMateFragment extends Fragment {
    public final static String Tag = "ButtonMateFragment";


    //region 使用本地广播与service通信
    LocalBroadcastManager localBroadcastManager;
    private MsgReceiver msgReceiver;
    ConnectService mConnectService;
    //endregion

    //region 控件
    private SwipeRefreshLayout swipeLayout;
    private LinearLayout ll;
    private SeekBar seekBar_angle;
    private TextView tv_seekbarAngleVal;
    private SeekBar seekBar_delay;
    private TextView tv_seekbarDelayVal;

    private Button bt_left;
    private Button bt_right;
    private Button bt_middle;
    //endregion

    TextView log;

    String device_mac = null;
    String device_name = null;

    public ButtonMateFragment() {

        // Required empty public constructor
    }

    @SuppressLint("ValidFragment")
    public ButtonMateFragment(String name, String mac) {
        this.device_mac = mac;
        this.device_name = name;
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_button_mate, container, false);

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

        //region SwipeRefreshLayout 控件
        ll = (LinearLayout) view.findViewById(R.id.ll);
        swipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        swipeLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimaryDark, R.color.colorAccent, R.color.colorPrimary);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (ll.getVisibility() != View.VISIBLE) {
                    ll.setVisibility(View.VISIBLE);

//                        TcpSocketClient.MQTTSend(setting_get_all);
                    Send("{\"mac\":\"" + device_mac + "\",\"setting\":{\"min\":null,\"max\":null,\"middle\":null,\"middle_delay\":null}}");

                } else ll.setVisibility(View.GONE);
                swipeLayout.setRefreshing(false);
            }
        });
        //endregion

        //region seekBar及对应TextView
        seekBar_angle = (SeekBar) view.findViewById(R.id.seekBar_angle);
        tv_seekbarAngleVal = (TextView) view.findViewById(R.id.tv_seekbarAngleVal);
        seekBar_delay = (SeekBar) view.findViewById(R.id.seekBar_delay);
        tv_seekbarDelayVal = (TextView) view.findViewById(R.id.tv_seekbarDelayVal);

        seekBar_angle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tv_seekbarAngleVal.setText("角度值:" + String.format("%03d", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //TODO 发送设置测试角度
                Send("{\"mac\":\"" + device_mac + "\",\"setting\":{\"test\":"+String.format("%d", seekBar.getProgress() + 20)+"}}");

            }
        });
        seekBar_delay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tv_seekbarDelayVal.setText("按下延时时间:" + String.format("%03d", progress + 20) + "ms");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //发送delay时间
                Send("{\"mac\":\"" + device_mac + "\",\"setting\":{\"middle_delay\":"+String.format("%d", seekBar.getProgress() + 20)+"}}");

            }
        });
        //endregion

        //region 按键
        bt_left = (Button) view.findViewById(R.id.btn_left);
        bt_middle = (Button) view.findViewById(R.id.btn_middle);
        bt_right = (Button) view.findViewById(R.id.btn_right);
        bt_right.setOnClickListener(buttonListener);
        bt_left.setOnClickListener(buttonListener);
        bt_middle.setOnClickListener(buttonListener);

        ImageView imageView = (ImageView) view.findViewById(R.id.tbtn_main_button1);
        imageView.setOnClickListener(buttonListener);
        imageView = (ImageView) view.findViewById(R.id.tbtn_main_button2);
        imageView.setOnClickListener(buttonListener);
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
    private View.OnClickListener buttonListener = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            //TODO 测试是否连接
            //TODO 对应按键发送对应数据

            switch (arg0.getId()) {
                case R.id.tbtn_main_button1:
                    Send("{\"name\":\"" + device_name + "\",\"mac\":\"" + device_mac + "\",\"nvalue\" : 0}");
                    break;
                case R.id.tbtn_main_button2:
                    Send("{\"name\":\"" + device_name + "\",\"mac\":\"" + device_mac + "\",\"nvalue\" : 1}");
                    break;
                case R.id.btn_left:
                    Send("{\"mac\":\"" + device_mac + "\",\"setting\":{\"min\":" + seekBar_angle.getProgress() + "}}");
                    break;
                case R.id.btn_middle:
                    Send("{\"mac\":\"" + device_mac + "\",\"setting\":{\"middle\":" + seekBar_angle.getProgress() + "}}");
                    break;
                case R.id.btn_right:
                    Send("{\"mac\":\"" + device_mac + "\",\"setting\":{\"max\":" + seekBar_angle.getProgress() + "}}");

                    break;
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
            if (mac == null) return;
            else if (mac.equals(device_mac)) {
                if (jsonSetting != null) {
                    if (jsonSetting.has("min")) {
                        int min = jsonSetting.getInt("min");
                        bt_left.setText("设为左侧按键(" + min + ")");
                    }
                    if (jsonSetting.has("max")) {
                        int max = jsonSetting.getInt("max");
                        bt_right.setText("设为右侧按键(" + max + ")");
                    }
                    if (jsonSetting.has("middle")) {
                        int middle = jsonSetting.getInt("middle");
                        bt_middle.setText("设为平均值(" + middle + ")");
                    }
                    if (jsonSetting.has("middle_delay")) {
                        int middle_delay = jsonSetting.getInt("middle_delay");
                        tv_seekbarDelayVal.setText("按下延时时间:" + String.format("%03d", middle_delay) + "ms");
                        seekBar_delay.setProgress(middle_delay - 20);
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
