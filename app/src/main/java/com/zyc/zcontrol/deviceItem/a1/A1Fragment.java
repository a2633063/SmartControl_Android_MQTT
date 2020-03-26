package com.zyc.zcontrol.deviceItem.a1;


import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.zyc.zcontrol.R;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceA1;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 */
public class A1Fragment extends DeviceFragment {
    public final static String Tag = "A1Fragment";

    DeviceA1 device;
    //region 控件
    private SwipeRefreshLayout mSwipeLayout;
    Switch tbtn_switch;
    TextView tv_task;
    TextView tv_speed;
    SeekBar seekBar;
    //region imageview及动画效果
    ImageView iv_fan;
    private ObjectAnimator objectAnimator;
    //endregion
    //endregion


    public A1Fragment() {

        // Required empty public constructor
    }

    @SuppressLint("ValidFragment")
    public A1Fragment(DeviceA1 device) {
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
                    Send("{\"mac\":\"" + device.getMac() + "\",\"on\":null,\"speed\":null}");
                    break;
                case 2:
                    Log.d(Tag, "send seekbar:" + msg.arg1);

                    Send("{\"mac\":\"" + device.getMac() + "\",\"speed\":" + msg.arg1 + "}");
                    break;
            }
        }
    };

    //endregion
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_a1, container, false);


        //region 控件初始化

        //region 图片及动画
        iv_fan = view.findViewById(R.id.iv_fan);
        objectAnimator = ObjectAnimator.ofFloat(iv_fan, "rotation", 0f, 360f);//添加旋转动画，旋转中心默认为控件中点
        objectAnimator.setDuration(3600);//设置动画时间
        objectAnimator.setInterpolator(new LinearInterpolator());//动画时间线性渐变
        objectAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        objectAnimator.setRepeatMode(ObjectAnimator.RESTART);

        //endregion


        //region 控制按钮/跳转定时任务界面等
        tbtn_switch = view.findViewById(R.id.tbtn_button);
        tbtn_switch.setOnClickListener(MainButtonListener);

        tv_speed = view.findViewById(R.id.tv_speed);
        tv_task = view.findViewById(R.id.tv_task);
        tv_task.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), A1PlugActivity.class);
                intent.putExtra("mac", device.getMac());
                startActivity(intent);
            }
        });
        //endregion

        //region 拖动条 处理viewpage/SwipeRefreshLayout滑动冲突事件
        seekBar = view.findViewById(R.id.seekBarR);
        //region 处理viewpage/SwipeRefreshLayout滑动冲突事件
        seekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                view.getParent().requestDisallowInterceptTouchEvent(true);
                if (event.getAction() == MotionEvent.ACTION_UP) mSwipeLayout.setEnabled(true);
                else mSwipeLayout.setEnabled(false);
                return false;
            }
        });
        //endregion
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Message msg = new Message();
                msg.arg1 = seekBar.getProgress();
                msg.what = 2;
                handler.sendMessageDelayed(msg, 1);
            }
        });

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
            Send("{\"mac\":\"" + device.getMac() + "\",\"on\":" + String.valueOf(((Switch) arg0).isChecked() ? 1 : 0) + "}");
        }
    };

    //endregion
    //endregion


    void Send(String message) {
        boolean b = getActivity().getSharedPreferences("Setting_" + device.getMac(), 0).getBoolean("always_UDP", false);
        super.Send(b, device.getSendMqttTopic(), message);
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
            if (jsonObject.has("name")) device.setName(jsonObject.getString("name"));

            //region 解析on
            if (jsonObject.has("on")) {
                int on = jsonObject.getInt("on");
                tbtn_switch.setChecked(on != 0);
                if (tbtn_switch.isChecked()) objectAnimator.start();
                else objectAnimator.pause();
            }
            //endregion
            //region 解析speed
            if (jsonObject.has("speed")) {
                int speed = jsonObject.getInt("speed");
                seekBar.setProgress(speed);
                objectAnimator.setDuration(7000 - speed * 68);
                tv_speed.setText("风速:" + String.format("%03d", speed) + "%");
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
