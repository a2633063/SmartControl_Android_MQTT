package com.zyc.zcontrol.deviceItem.s7;


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
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceS7;

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
public class S7Fragment extends DeviceFragment {
    public final static String Tag = "S7Fragment";


    DeviceS7 device;
    //region 控件
    ImageView im_battery;
    TextView tv_weight;
    TextView tv_time;
    Switch sw_heat;
    LineTableView lineTable;
    private SwipeRefreshLayout mSwipeLayout;
    //endregion

    int bat_level = -1;
    boolean heat_flag = false;//接收到关闭且此为true时提示插上usb电才能加热
    final @DrawableRes
    int battery_res[] = {
            R.drawable.ic_battery_0_black_24dp,
            R.drawable.ic_battery_20_black_24dp,
            R.drawable.ic_battery_30_black_24dp,
            R.drawable.ic_battery_50_black_24dp,
            R.drawable.ic_battery_60_black_24dp,
            R.drawable.ic_battery_80_black_24dp,
            R.drawable.ic_battery_90_black_24dp,
            R.drawable.ic_battery_full_black_24dp,

            R.drawable.ic_battery_charging_0_black_24dp,
            R.drawable.ic_battery_charging_20_black_24dp,
            R.drawable.ic_battery_charging_30_black_24dp,
            R.drawable.ic_battery_charging_50_black_24dp,
            R.drawable.ic_battery_charging_60_black_24dp,
            R.drawable.ic_battery_charging_80_black_24dp,
            R.drawable.ic_battery_charging_90_black_24dp,
            R.drawable.ic_battery_charging_full_black_24dp,
    };

    public S7Fragment() {

        // Required empty public constructor
    }

    @SuppressLint("ValidFragment")
    public S7Fragment(DeviceS7 device) {
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
                            + "\"battery\" : null,"
                            + "\"heat\" : null,"
                            + "\"charge\" : null,"
                            + "\"history\" : null}"
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
        View view = inflater.inflate(R.layout.fragment_s7, container, false);

        //region 控件初始化
        lineTable = view.findViewById(R.id.lineTableView);
        im_battery = view.findViewById(R.id.im_battery);
        tv_weight = view.findViewById(R.id.tv_weight);
        tv_time = view.findViewById(R.id.tv_time);
        sw_heat = view.findViewById(R.id.sw_heat);

        sw_heat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((Switch) v).isChecked()) heat_flag = true;
                Send("{\"mac\":\"" + device.getMac() + "\",\"heat\":" + String.valueOf(((Switch) v).isChecked() ? 1 : 0) + "}");
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

//        lineTable.getWeightList().add(new WeightHistoryData(10, 1500000000));
//        lineTable.getWeightList().add(new WeightHistoryData(50, 1500100000));
//        lineTable.getWeightList().add(new WeightHistoryData(70, 1500200000));
//        lineTable.getWeightList().add(new WeightHistoryData(50,1500300000));
//        lineTable.getWeightList().add(new WeightHistoryData(20,1500400000));
//        lineTable.getWeightList().add(new WeightHistoryData(250,1500500000));
//        lineTable.getWeightList().add(new WeightHistoryData(75,1500600000));
//        lineTable.getWeightList().add(new WeightHistoryData(76,1500700000));
//        lineTable.getWeightList().add(new WeightHistoryData(77,1500800000));
//        lineTable.getWeightList().add(new WeightHistoryData(78,1500900000));
        lineTable.invalidate();

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
                    Log(device.isOnline() ? "设备在线" : "设备离线,请站上称点亮屏幕以启动连接");
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

            //region 电池/充电状态
            if (jsonObject.has("battery")) {
                int bat = jsonObject.getInt("battery");
                bat = (bat - 370) * 2;
                if (bat_level == -1) bat_level = 0;
                if (bat > 95) bat_level = 7;
                else if (bat > 85) bat_level = 6;
                else if (bat > 70) bat_level = 5;
                else if (bat > 55) bat_level = 4;
                else if (bat > 40) bat_level = 3;
                else if (bat > 25) bat_level = 2;
                else if (bat > 10) bat_level = 1;
                else bat_level = 0;
            }
            if (jsonObject.has("charge")) {
                int charge = jsonObject.getInt("charge");
                if (charge > 0) {
                    if (bat_level == -1) bat_level = battery_res.length / 2;
                    else if (bat_level < battery_res.length / 2)
                        bat_level += battery_res.length / 2;
                } else if (bat_level >= battery_res.length / 2) {
                    bat_level -= battery_res.length / 2;
                }
            }

            if (jsonObject.has("battery") || jsonObject.has("charge")) {
                im_battery.setImageResource(battery_res[bat_level]);
            }
            //endregion

            //region 加热
            if (jsonObject.has("heat")) {
                int heat = jsonObject.getInt("heat");
                sw_heat.setChecked(heat != 0);

                if (heat == 0 && jsonObject.has("charge") && jsonObject.getInt("charge") == 0
                        && heat_flag) {
                    heat_flag = false;
                    Log("请插USB电源后再打开加热功能");
                }
            }
            //endregion

            //region 历史体重数据
            if (jsonObject.has("history")) {
                JSONObject jsonHistory = jsonObject.getJSONObject("history");
                JSONArray jsonWeight = jsonHistory.getJSONArray("weight");
                JSONArray jsonTime = jsonHistory.getJSONArray("utc");
                int length = jsonWeight.length();
                if (length < 1) {
                    tv_weight.setText("无历史数据");
                } else {

                    int weight = jsonWeight.getInt(length - 1);
                    long utc = jsonTime.getLong(length - 1) - 28800;   //多算了时区
                    if (utc > 1500000000) {
                        Date date = new Date(utc * 1000);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd  HH:mm ");
                        tv_time.setText("上次测量时间: " + sdf.format(date));
                    } else {
                        tv_time.setText("上次测量时间: 未知");
                    }

                    tv_weight.setText(weight / 100 + "." + weight % 100 + "kg");
                    lineTable.getWeightList().clear();
                    for (int i = 0; i < length; i++) {
                        lineTable.getWeightList().add(new WeightHistoryData(jsonWeight.getInt(i), jsonTime.getLong(i) - 28800));
                    }
                    lineTable.invalidate();
                }
            }
            //endregion

            //region 更新体重信息
            if (jsonObject.has("weight") && jsonObject.has("time")) {
                int weight = jsonObject.getInt("weight");
                long utc = jsonObject.getLong("time") - 28800;   //多算了时区
                if (utc > 1500000000) {
                    Date date = new Date(utc * 1000);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd  HH:mm ");
                    tv_time.setText("上次测量时间: " + sdf.format(date));
                } else {
                    tv_time.setText("上次测量时间: 未知");
                }

                tv_weight.setText(weight / 100 + "." + weight % 100 + "kg");
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
