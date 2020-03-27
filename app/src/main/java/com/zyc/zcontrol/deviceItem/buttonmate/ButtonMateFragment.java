package com.zyc.zcontrol.deviceItem.buttonmate;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.zyc.zcontrol.R;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceButtonMate;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 */
public class ButtonMateFragment extends DeviceFragment {
    public final static String Tag = "ButtonMateFragment";


    DeviceButtonMate device;
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

    public ButtonMateFragment() {

        // Required empty public constructor
    }

    @SuppressLint("ValidFragment")
    public ButtonMateFragment(DeviceButtonMate device) {
        super(device.getName(), device.getMac());
        this.device = device;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_button_mate, container, false);

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
                    Send("{\"mac\":\"" + device.getMac() + "\",\"setting\":{\"min\":null,\"max\":null,\"middle\":null,\"middle_delay\":null}}");

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
                //发送设置测试角度
                Send("{\"mac\":\"" + device.getMac() + "\",\"setting\":{\"test\":" + String.format("%d", seekBar.getProgress() + 20) + "}}");

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
                Send("{\"mac\":\"" + device.getMac() + "\",\"setting\":{\"middle_delay\":" + String.format("%d", seekBar.getProgress() + 20) + "}}");

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
        setLogTextView((TextView) view.findViewById(R.id.tv_log));
        //endregion

        //endregion
        super.onCreateView(inflater, container, savedInstanceState);

        return view;
    }


    //region 按钮事件
    private View.OnClickListener buttonListener = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            switch (arg0.getId()) {
                case R.id.tbtn_main_button1:
                    Send("{\"mac\":\"" + device.getMac() + "\",\"nvalue\" : 0}");
                    break;
                case R.id.tbtn_main_button2:
                    Send("{\"mac\":\"" + device.getMac() + "\",\"nvalue\" : 1}");
                    break;
                case R.id.btn_left:
                    Send("{\"mac\":\"" + device.getMac() + "\",\"setting\":{\"min\":" + seekBar_angle.getProgress() + "}}");
                    break;
                case R.id.btn_middle:
                    Send("{\"mac\":\"" + device.getMac() + "\",\"setting\":{\"middle\":" + seekBar_angle.getProgress() + "}}");
                    break;
                case R.id.btn_right:
                    Send("{\"mac\":\"" + device.getMac() + "\",\"setting\":{\"max\":" + seekBar_angle.getProgress() + "}}");

                    break;
            }

        }

    };
//endregion


    void Send(String message) {
        boolean b = getActivity().getSharedPreferences("Setting_" + device.getMac(), 0).getBoolean("always_UDP", false);
        super.Send(b, device.getSendMqttTopic(), message);
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

            JSONObject jsonSetting = null;
            if (jsonObject.has("setting")) jsonSetting = jsonObject.getJSONObject("setting");
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
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
