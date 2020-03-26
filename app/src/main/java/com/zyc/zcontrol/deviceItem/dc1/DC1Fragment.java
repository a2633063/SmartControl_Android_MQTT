package com.zyc.zcontrol.deviceItem.dc1;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.zyc.zcontrol.R;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceDC1;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 */
public class DC1Fragment extends DeviceFragment {
    public final static String Tag = "DC1Fragment";



    DeviceDC1 device;
    //region 控件
    private SwipeRefreshLayout mSwipeLayout;
    Switch tbtn_all;
    TextView tv_power;
    TextView tv_voltage;
    TextView tv_current;
    Switch tbtn_main_button[] = new Switch[4];
    TextView tv_main_button[] = new TextView[4];
    //endregion


    public DC1Fragment() {

        // Required empty public constructor
    }

    @SuppressLint("ValidFragment")
    public DC1Fragment(DeviceDC1 device) {
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
                            + "\"plug_0\" : {\"on\" : null,\"setting\":{\"name\":null}},"
                            + "\"plug_1\" : {\"on\" : null,\"setting\":{\"name\":null}},"
                            + "\"plug_2\" : {\"on\" : null,\"setting\":{\"name\":null}},"
                            + "\"plug_3\" : {\"on\" : null,\"setting\":{\"name\":null}}}");
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
        View view = inflater.inflate(R.layout.fragment_dc1, container, false);

        //region 控件初始化

        //region 开关按钮
        tbtn_all = view.findViewById(R.id.tbtn_all);
        tv_power = view.findViewById(R.id.tv_power);
        tv_voltage = view.findViewById(R.id.tv_voltage);
        tv_current = view.findViewById(R.id.tv_current);
        tbtn_main_button[0] = view.findViewById(R.id.tbtn_main_button1);
        tbtn_main_button[1] = view.findViewById(R.id.tbtn_main_button2);
        tbtn_main_button[2] = view.findViewById(R.id.tbtn_main_button3);
        tbtn_main_button[3] = view.findViewById(R.id.tbtn_main_button4);
        tv_main_button[0] = view.findViewById(R.id.tv_main_button1);
        tv_main_button[1] = view.findViewById(R.id.tv_main_button2);
        tv_main_button[2] = view.findViewById(R.id.tv_main_button3);
        tv_main_button[3] = view.findViewById(R.id.tv_main_button4);


        tbtn_all.setOnClickListener(MainButtonListener);
        for (int i = 0; i < 4; i++) {
            tbtn_main_button[i].setId(i);
            tv_main_button[i].setId(i + 4);
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
        setLogTextView((TextView) view.findViewById(R.id.tv_log));
        //endregion

        //endregion
        super.onCreateView(inflater, container, savedInstanceState);

        return view;
    }


    //region 按钮事件
    // region 开关
    private View.OnClickListener MainButtonListener = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            int id = arg0.getId();
            if (id >= 0 && id < 4)
                Send("{\"mac\":\"" + device.getMac() + "\",\"plug_" + id + "\":{\"on\":" + String.valueOf(((Switch) arg0).isChecked() ? 1 : 0) + "}" + "}");
            else if (id == tbtn_all.getId()) {
                int s = ((Switch) arg0).isChecked() ? 1 : 0;
                Send("{\"mac\":\"" + device.getMac() + "\","
                        + "\"plug_0\":{\"on\":" + s + "},"
                        + "\"plug_1\":{\"on\":" + s + "},"
                        + "\"plug_2\":{\"on\":" + s + "},"
                        + "\"plug_3\":{\"on\":" + s + "}"
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
            Intent intent = new Intent(getContext(), DC1PlugActivity.class);
            intent.putExtra("plug_name", ((TextView) v).getText());
            intent.putExtra("mac", device.getMac());
            intent.putExtra("plug_id", (v.getId()) % 4);
            startActivity(intent);
        }
    };
    //endregion
    //endregion

    void Send(String message) {
        boolean udp = getActivity().getSharedPreferences("Setting_" + device.getMac(), 0).getBoolean("always_UDP", false);
        boolean oldProtocol = getActivity().getSharedPreferences("Setting_" + device.getMac(), 0).getBoolean("old_protocol", false);

        String topic = null;
        if (!udp) {
            if (oldProtocol) topic = "device/zdc1/set";
            else topic = device.getSendMqttTopic();
        }
        super.Send(udp,topic, message);
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
                    Log(device.isOnline() ? "设备在线" : "设备离线" + "(功能调试中)");
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


            //region sensor部分
            if (jsonObject.has("power")) {
                String power = jsonObject.getString("power");
                Log.d(Tag, "power:" + power);
                tv_power.setText(power + "W");
            }
            if (jsonObject.has("voltage")) {
                String voltage = jsonObject.getString("voltage");
                Log.d(Tag, "voltage:" + voltage);
                tv_voltage.setText(voltage + "V");
            }
            if (jsonObject.has("current")) {
                String current = jsonObject.getString("current");
                Log.d(Tag, "current:" + current);
                tv_current.setText(current + "A");
            }
            //endregion

            //region 解析plug
            for (int plug_id = 0; plug_id < 4; plug_id++) {
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
