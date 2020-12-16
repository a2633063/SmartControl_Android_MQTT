package com.zyc.zcontrol.deviceItem.clockMatrix;


import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.method.KeyListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.zyc.zcontrol.R;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceClockMatrix;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.Key;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * A simple {@link Fragment} subclass.
 */
public class ClockMatrixFragment extends DeviceFragment {
    public final static String Tag = "ClockMatrixFragment";


    DeviceClockMatrix device;
    //region 控件
    private SwipeRefreshLayout mSwipeLayout;
    private SeekBar seek_brightness;
    private CheckBox chk_direction;
    private TextView txt_brightness;
    private Button btn_string;
    private EditText edt_string;
    //endregion

    private boolean brightness_auto = false;

    public ClockMatrixFragment() {

        // Required empty public constructor
    }

    @SuppressLint("ValidFragment")
    public ClockMatrixFragment(DeviceClockMatrix device) {
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
                            + "\"direction\" : null,"
                            + "\"auto_brightness\" : null,"
                            + "\"brightness\" : null,"
                            + "\"on\" : null}");
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
        final View view = inflater.inflate(R.layout.fragment_clock_matrix, container, false);

        //region 控件初始化
        //region 拖动条 处理viewpage/SwipeRefreshLayout滑动冲突事件
        seek_brightness = view.findViewById(R.id.seek_brightness);
        //region 处理viewpage/SwipeRefreshLayout滑动冲突事件
        seek_brightness.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                view.getParent().requestDisallowInterceptTouchEvent(true);
                if (event.getAction() == MotionEvent.ACTION_UP) mSwipeLayout.setEnabled(true);
                else mSwipeLayout.setEnabled(false);
                return false;
            }
        });
        //endregion
        seek_brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txt_brightness.setText("亮度:" + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                Message msg = new Message();
//                msg.arg1 = seekBar.getProgress();
//                msg.what = 2;
//                handler.sendMessageDelayed(msg, 1);
                Send("{\"mac\": \"" + device.getMac() + "\",\"brightness\":"
                        + seek_brightness.getProgress() + "}");
            }
        });

        //endregion
        //region 显示方向切换开关
        chk_direction = view.findViewById(R.id.chk_direction);
        chk_direction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Send("{\"mac\": \"" + device.getMac() + "\",\"direction\":"
                        + (chk_direction.isChecked() ? "1" : "0") + "}");
            }
        });
        //endregion

        //region 亮度文本
        txt_brightness = view.findViewById(R.id.txt_brightness);
        txt_brightness.setText("亮度:" + seek_brightness.getProgress());
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

        edt_string = view.findViewById(R.id.edt_string);
        btn_string = view.findViewById(R.id.btn_string);
        btn_string.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                dialogString();
                String str = edt_string.getText().toString();

                str = str.replace("\r\n", "\n").replace("\n", " ");
                if (str.length() > 0 && str.length() < 500) {
                    //json中 需要替换符号:  " => \"  \ => \\
                    str = str.replace("\\", "\\\\").replace("\"", "\\\"");
                    Send("{\"mac\":\"" + device.getMac() + "\",\"string\":\"" + str + "\"}");
                    Log("发送滚动字符串");
                } else if (str.length() < 1) {
                    Log("未输入有效的滚动字符串");
                } else {
                    Log("滚动字符串长度过长");
                }
            }
        });

        //region log 相关
        setLogTextView((TextView) view.findViewById(R.id.tv_log));
        //endregion

        //endregion
        super.onCreateView(inflater, container, savedInstanceState);

        return view;
    }

    void Send(String message) {
        boolean udp = getActivity().getSharedPreferences("Setting_" + device.getMac(), 0).getBoolean("always_UDP", false);
        boolean oldProtocol = getActivity().getSharedPreferences("Setting_" + device.getMac(), 0).getBoolean("old_protocol", false);

        super.Send(udp, device.getSendMqttTopic(), message);
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
                    Log(device.isOnline() ? "设备在线" : "设备离线" + "(请确认设备是否有连接mqtt服务器)");
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
            //region 解析亮度
            if (jsonObject.has("brightness")) {
                int brightness = jsonObject.getInt("brightness");
                seek_brightness.setProgress(brightness);
            }
            //endregion

            //region 解析显示方向
            if (jsonObject.has("direction")) {
                chk_direction.setChecked(jsonObject.getInt("direction") == 1);
            }
            //endregion

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //region 事件监听调用函数,主要为在子类中重写此函数实现在service建立成功/mqtt连接成功/失败时执行功能
    //Service建立成功时调用    此函数需要时在子类中重写
    public void ServiceConnected() {
        handler.sendEmptyMessageDelayed(1, 300);
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
