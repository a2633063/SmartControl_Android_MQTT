package com.zyc.zcontrol.controlItem.buttonmate;


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
import android.widget.Toast;

import com.zyc.zcontrol.MQTTService;
import com.zyc.zcontrol.MyFunction;
import com.zyc.zcontrol.R;

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
    MQTTService mMQTTService;
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


    public ButtonMateFragment() {
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
        intentFilter.addAction(MQTTService.ACTION_MQTT_CONNECTED);
        intentFilter.addAction(MQTTService.ACTION_MQTT_DISCONNECTED);
        intentFilter.addAction(MQTTService.ACTION_DATA_AVAILABLE);
        localBroadcastManager.registerReceiver(msgReceiver, intentFilter);
        //endregion

        //region 启动MQTT 服务以及启动,无需再启动
        Intent intent = new Intent(getContext(), MQTTService.class);
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

//                        TcpSocketClient.Send(setting_get_all);

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
                //TODO 发送delay时间

            }
        });
        //endregion

        //region 按键
        bt_left = (Button) view.findViewById(R.id.btn_1);
        bt_middle = (Button) view.findViewById(R.id.btn_2);
        bt_right = (Button) view.findViewById(R.id.btn_3);
        bt_right.setOnClickListener(buttonListener);
        bt_left.setOnClickListener(buttonListener);
        bt_middle.setOnClickListener(buttonListener);

        ImageView imageView = (ImageView) view.findViewById(R.id.iv_main_button1);
        imageView.setOnClickListener(buttonListener);
        imageView = (ImageView) view.findViewById(R.id.iv_main_button2);
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
    private View.OnClickListener buttonListener = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            //TODO 测试是否连接
            //TODO 对应按键发送对应数据

            switch (arg0.getId()) {
                case R.id.iv_main_button1:
                    break;
                case R.id.iv_main_button2:
                    break;
                case R.id.btn_1:
                    break;
                case R.id.btn_2:
                    break;
                case R.id.btn_3:
                    break;
            }

        }

    };
//endregion


    //发送
    void send(String topic, String message, boolean choice) {
        if (choice) {
            MyFunction.UDPsend(message);
        } else mMQTTService.Send(topic, message);
    }

    void send(String topic, String message) {
        send(topic, message, false);
    }

    //数据接收处理函数
    void Receive(String topic, String message) {
        //TODO 数据接收处理
        Log.d(Tag, "RECV DATA,topic:" + topic + ",content:" + message);

    }

    //region MQTT服务有关

    private final ServiceConnection mMQTTServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mMQTTService = ((MQTTService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mMQTTService = null;
        }
    };

    //广播接收,用于处理接收到的数据
    public class MsgReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (MQTTService.ACTION_MQTT_CONNECTED.equals(action)) {  //连接成功
                Log.d(Tag, "ACTION_MQTT_CONNECTED");
            } else if (MQTTService.ACTION_MQTT_DISCONNECTED.equals(action)) {  //连接失败/断开
                Log.w(Tag, "ACTION_MQTT_DISCONNECTED");
            } else if (MQTTService.ACTION_DATA_AVAILABLE.equals(action)) {  //接收到数据
                String topic = intent.getStringExtra(MQTTService.EXTRA_DATA_TOPIC);
                String message = intent.getStringExtra(MQTTService.EXTRA_DATA_CONTENT);
                Receive(topic, message);
            }
        }
    }
    //endregion


}
