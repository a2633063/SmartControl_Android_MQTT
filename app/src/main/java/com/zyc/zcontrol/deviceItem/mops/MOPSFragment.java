package com.zyc.zcontrol.deviceItem.mops;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.zyc.linetable.LineTableView;
import com.zyc.linetable.WeightHistoryData;
import com.zyc.zcontrol.R;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceFragment;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceMOPS;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * A simple {@link Fragment} subclass.
 */
public class MOPSFragment extends DeviceFragment {
    public final static String Tag = "MOPSFragment";


    DeviceMOPS device;
    //region 控件
    TextView tv_task;
    Switch sw_on;
    private SwipeRefreshLayout mSwipeLayout;
    //endregion


    public MOPSFragment() {

        // Required empty public constructor
    }

    @SuppressLint("ValidFragment")
    public MOPSFragment(DeviceMOPS device) {
        super(device.getName(), device.getMac());
        this.device = device;
    }

    //region Handler
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {// handler接收到消息后就会执行此方法
            switch (msg.what) {
                case 1:
                    handler.removeMessages(1);
                    Send("{\"mac\": \"" + device.getMac() + "\","
                            + "\"on\" : null}"
                    );
                    break;

                case 101:
                    new AlertDialog.Builder(getActivity()).setTitle("命令超时")
                            .setMessage("接收反馈数据超时,请重试")
                            .setPositiveButton("确定", null).show();
                    break;
            }
        }
    };

    //endregion
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_mops, container, false);

        //region 控件初始化
        sw_on= view.findViewById(R.id.sw_on);
        tv_task  = view.findViewById(R.id.tv_task  );

        sw_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Send("{\"mac\":\"" + device.getMac() + "\",\"on\":" + String.valueOf(((Switch) v).isChecked() ? 1 : 0) + "}");
            }
        });
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
        setLogTextView((TextView) view.findViewById(R.id.tv_log));
        //endregion

        //endregion

        super.onCreateView(inflater, container, savedInstanceState);
        return view;
    }


    void Send(String message) {
        boolean udp = getActivity().getSharedPreferences("Setting_" + device.getMac(), 0).getBoolean("always_UDP", false);

        String topic = device.getSendMqttTopic();

        super.Send(udp, topic, message);
    }

    //数据接收处理函数
    public void Receive(String ip, int port, String topic, String message) {
        super.Receive(ip, port, topic, message);
        //region 接收availability数据,非Json,单独处理
        if (topic != null && topic.endsWith("availability")) {
            String regexp = "device/(.*?)/([0123456789abcdef]{12})/(.*)";
            Pattern pattern = Pattern.compile(regexp);
            Matcher matcher = pattern.matcher(topic);
            if (matcher.find() && matcher.groupCount() == 3) {
                String device_mac = matcher.group(2);
                if (device_mac.equals(device.getMac())) {
                    device.setOnline(message.equals("1"));
                    Log(device.isOnline() ? "设备在线" : "设备离线");
                    if (device.isOnline()) {
                        handler.sendEmptyMessageDelayed(1, 0);
                    }
                }
                return;
            }
        }
        //endregion

        try {
            JSONObject jsonObject = new JSONObject(message);
            if (!jsonObject.has("mac") || !jsonObject.getString("mac").equals(device.getMac())) {
                return;
            }


            //region 开关
            if (jsonObject.has("on")) {
                int heat = jsonObject.getInt("on");
                sw_on.setChecked(heat != 0);
            }
            //endregion




        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //region 事件监听调用函数,主要为在子类中重写此函数实现在service建立成功/mqtt连接成功/失败时执行功能
    //Service建立成功时调用    此函数需要时在子类中重写
    public void ServiceConnected() {
        handler.sendEmptyMessageDelayed(1, 0);
    }

    //mqtt连接成功时调用    此函数需要时在子类中重写
    public void MqttConnected() {
        handler.sendEmptyMessageDelayed(1, 0);
    }

    //mqtt连接断开时调用    此函数需要时在子类中重写
    public void MqttDisconnected() {
        handler.sendEmptyMessageDelayed(1, 0);
    }
    //endregion


}
