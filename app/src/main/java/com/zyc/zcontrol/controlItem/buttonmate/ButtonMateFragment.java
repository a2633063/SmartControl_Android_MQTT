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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.zyc.zcontrol.MQTTService;
import com.zyc.zcontrol.R;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class ButtonMateFragment extends Fragment {
    public final static String Tag = "ButtonMateFragment";


    //region 使用本地广播与service通信
    LocalBroadcastManager localBroadcastManager;
    private MsgReceiver msgReceiver;
    //endregion
    MQTTService mMQTTService;

    Button button;

    public ButtonMateFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view=inflater.inflate(R.layout.fragment_button_mate, container, false);

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

        //region 启动MQTT服务 不启动
        Intent intent = new Intent(getContext(), MQTTService.class);

        getActivity().bindService(intent, mMQTTServiceConnection, BIND_AUTO_CREATE);

        //endregion
        //endregion

        button=view.findViewById(R.id.button);
        return view;
    }
    @Override
    public void onDestroy() {
        //注销广播
        localBroadcastManager.unregisterReceiver(msgReceiver);
        //停止服务
        Intent intent = new Intent(getContext(), MQTTService.class);
        getActivity().unbindService( mMQTTServiceConnection);

        super.onDestroy();
    }

    //region MQTT服务有关

    private final ServiceConnection mMQTTServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mMQTTService = ((MQTTService.LocalBinder) service).getService();
            // Automatically connects to the device upon successful start-up initialization.
            mMQTTService.connect("tcp://47.112.16.98:1883", "mqtt_id_dasdf",
                    "z", "2633063");
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
                if (mMQTTService != null) {
                    if (mMQTTService.isConnected()) {
                        mMQTTService.disconnect();
                    }
                    mMQTTService.connect("tcp://47.112.16.98:1883", "mqtt_id_dasdf",
                            "z", "2633063");
                }
            } else if (MQTTService.ACTION_DATA_AVAILABLE.equals(action)) {  //接收到数据
                String topic = intent.getStringExtra(MQTTService.EXTRA_DATA_TOPIC);
                String str = intent.getStringExtra(MQTTService.EXTRA_DATA_CONTENT);
                Log.d(Tag, "RECV DATA,topic:" + topic + ",content:" + str);
                button.setText(str);
            }
        }
    }
    //endregion


}
