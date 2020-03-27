package com.zyc.zcontrol.deviceItem.tc1;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.zyc.zcontrol.R;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceFragment;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceTC1;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 */
public class TC1Fragment extends DeviceFragment {
    public final static String Tag = "TC1Fragment";

    DeviceTC1 device;
    //region 控件
    private SwipeRefreshLayout mSwipeLayout;
    Switch tbtn_all;
    TextView tv_power;
    Switch tbtn_plug[] = new Switch[6];
    TextView tv_plug_name[] = new TextView[6];
    TextView tv_total_time;
    //endregion

    public TC1Fragment() {
        // Required empty public constructor
    }

    @SuppressLint("ValidFragment")
    public TC1Fragment(DeviceTC1 device) {
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
                    Send("{\"mac\": \"" + device.getMac() + "\","
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
        //region 控件初始化

        //region 控制按钮/功率/运行时间等
        tv_total_time = view.findViewById(R.id.tv_total_time);
        tbtn_all = view.findViewById(R.id.tbtn_all);
        tv_power = view.findViewById(R.id.tv_power);
        tbtn_plug[0] = view.findViewById(R.id.tbtn_main_button1);
        tbtn_plug[1] = view.findViewById(R.id.tbtn_main_button2);
        tbtn_plug[2] = view.findViewById(R.id.tbtn_main_button3);
        tbtn_plug[3] = view.findViewById(R.id.tbtn_main_button4);
        tbtn_plug[4] = view.findViewById(R.id.tbtn_main_button5);
        tbtn_plug[5] = view.findViewById(R.id.tbtn_main_button6);
        tv_plug_name[0] = view.findViewById(R.id.tv_main_button1);
        tv_plug_name[1] = view.findViewById(R.id.tv_main_button2);
        tv_plug_name[2] = view.findViewById(R.id.tv_main_button3);
        tv_plug_name[3] = view.findViewById(R.id.tv_main_button4);
        tv_plug_name[4] = view.findViewById(R.id.tv_main_button5);
        tv_plug_name[5] = view.findViewById(R.id.tv_main_button6);

        tbtn_all.setOnClickListener(MainButtonListener);
        for (int i = 0; i < 6; i++) {
            tbtn_plug[i].setId(i);
            tv_plug_name[i].setId(i + 6);
            tbtn_plug[i].setOnClickListener(MainButtonListener);
            tbtn_plug[i].setOnCheckedChangeListener(MainButtonChangeListener);
            tv_plug_name[i].setOnClickListener(MainTextListener);
        }
        //endregion

        //region 下拉请求更新当前状态
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
            if (id >= 0 && id <= 5)
                Send("{\"mac\":\"" + device.getMac() + "\",\"plug_" + id + "\":{\"on\":" + String.valueOf(((Switch) arg0).isChecked() ? 1 : 0) + "}" + "}");
            else if (id == tbtn_all.getId()) {
                int s = ((Switch) arg0).isChecked() ? 1 : 0;
                Send("{\"mac\":\"" + device.getMac() + "\","
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
            for (int i = 0; i < tbtn_plug.length; i++) {
                if (tbtn_plug[i].isChecked()) {
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
            intent.putExtra("plug_name", ((TextView) v).getText());
            intent.putExtra("mac", device.getMac());
            intent.putExtra("plug_id", (v.getId()) % 6);
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
            if (oldProtocol) topic = "device/ztc1/set";
            else topic = device.getSendMqttTopic();
        }
        super.Send(udp, topic, message);
    }

    //数据接收处理更新函数
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
            if (jsonObject.has("name")) device.setName(jsonObject.getString("name"));

            //region sensor部分
            //{"mac":"d0bae4638baa","power":"24.7","total_time":9139790}
            if (jsonObject.has("power") && jsonObject.get("power") instanceof String) {
                try {
//                    device.setPower(jsonObject.getDouble("power"));
//                    tv_power.setText(String.format("%.1fW", device.getPower()));
                    tv_power.setText(jsonObject.getString("power")+"W");
                } catch (JSONException e) {
                    Log("功率数据出错");
                    e.printStackTrace();
                }
            }
            if (jsonObject.has("total_time") && jsonObject.get("total_time") instanceof Integer) {
                try {
//                    device.setTotal_time(jsonObject.getInt("total_time"));
//                    int total_time = device.getTotal_time();
                    int total_time = jsonObject.getInt("total_time");

                    String timeStr = "";
                    int days = total_time / 86400; //天
                    int hours = ((total_time % 86400) / 3600); //小时
                    int minutes = ((total_time % 3600) / 60); //分
                    int second = ((total_time % 60)); //秒
                    if (days > 0)   //天
                    {
                        timeStr += days + "天";
                    }
                    if (hours > 0)   //小时
                    {
                        timeStr += hours + "小时";
                    }
                    if (minutes > 0)   //分
                    {
                        timeStr += minutes + "分";
                    }
                    if (second > 0)   //秒
                    {
                        timeStr += second + "秒";
                    }
                    tv_total_time.setText("运行时间: " + timeStr);
                } catch (JSONException e) {
                    Log("运行时间数据出错");
                    e.printStackTrace();
                }
            }
            //endregion

            //region 开关状态部分
            for (int plug_id = 0; plug_id < 6; plug_id++) {
                if (!jsonObject.has("plug_" + plug_id)) continue;
                JSONObject jsonPlug = jsonObject.getJSONObject("plug_" + plug_id);
                if (jsonPlug.has("on")) {
                    int on = jsonPlug.getInt("on");
//                    device.setPlug(plug_id, on != 0);
//                    tbtn_plug[plug_id].setChecked(device.isPlug(plug_id));
                    tbtn_plug[plug_id].setChecked(on != 0);
                }
                if (!jsonPlug.has("setting")) continue;
                JSONObject jsonPlugSetting = jsonPlug.getJSONObject("setting");
                if (jsonPlugSetting.has("name")) {
//                    device.setPlug_name(plug_id, jsonPlugSetting.getString("name"));
//                    tv_plug_name[plug_id].setText(device.getPlug_name(plug_id));
                    tv_plug_name[plug_id].setText(jsonPlugSetting.getString("name"));
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
