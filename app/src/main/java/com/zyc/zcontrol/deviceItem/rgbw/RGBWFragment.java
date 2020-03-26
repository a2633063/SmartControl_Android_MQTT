package com.zyc.zcontrol.deviceItem.rgbw;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.zyc.zcontrol.R;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceFragment;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceRGBW;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 */
public class RGBWFragment extends DeviceFragment {
    public final static String Tag = "RGBWFragment";


    DeviceRGBW device;
    //region 控件
    private SwipeRefreshLayout mSwipeLayout;

    TextView textViewR;
    TextView textViewG;
    TextView textViewB;
    TextView textViewW;
    SeekBar seekBarR;
    SeekBar seekBarG;
    SeekBar seekBarB;
    SeekBar seekBarW;

    //endregion


    public RGBWFragment() {

        // Required empty public constructor
    }

    @SuppressLint("ValidFragment")
    public RGBWFragment(DeviceRGBW device) {
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
                    Send("{\"mac\":\"" + device.getMac() + "\",\"rgb\":null}");
                    break;
                case 2:
                    Log.d(Tag, "send color:" + msg.obj);

                    Send("{\"mac\":\"" + device.getMac() + "\",\"rgb\":" + msg.obj + "}");
                    break;
            }
        }
    };

    //endregion
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_rgbw, container, false);


        //region 控件初始化

        textViewR = view.findViewById(R.id.textViewR);
        textViewG = view.findViewById(R.id.textViewG);
        textViewB = view.findViewById(R.id.textViewB);
        textViewW = view.findViewById(R.id.textViewW);

        //region 拖动条 处理viewpage/SwipeRefreshLayout滑动冲突事件
        seekBarR = view.findViewById(R.id.seekBarR);
        seekBarG = view.findViewById(R.id.seekBarG);
        seekBarB = view.findViewById(R.id.seekBarB);
        seekBarW = view.findViewById(R.id.seekBarW);
        //region 处理viewpage/SwipeRefreshLayout滑动冲突事件
        View.OnTouchListener seekBarTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                view.getParent().requestDisallowInterceptTouchEvent(true);
                if (event.getAction() == MotionEvent.ACTION_UP) mSwipeLayout.setEnabled(true);
                else mSwipeLayout.setEnabled(false);
                return false;
            }
        };

        seekBarR.setOnTouchListener(seekBarTouchListener);
        seekBarG.setOnTouchListener(seekBarTouchListener);
        seekBarB.setOnTouchListener(seekBarTouchListener);
        seekBarW.setOnTouchListener(seekBarTouchListener);
        //endregion

        //region SeekBar拖动事件函数
        SeekBar.OnSeekBarChangeListener seekBarSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textViewR.setText(String.format(Locale.getDefault(), "R:%03d", seekBarR.getProgress()));
                textViewG.setText(String.format(Locale.getDefault(), "G:%03d", seekBarG.getProgress()));
                textViewB.setText(String.format(Locale.getDefault(), "B:%03d", seekBarB.getProgress()));
                textViewW.setText(String.format(Locale.getDefault(), "W:%03d", seekBarW.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Message msg = new Message();
                msg.obj = "[" + seekBarR.getProgress() + "," + seekBarG.getProgress() + "," +
                        seekBarB.getProgress() + "," + seekBarW.getProgress() + "]";
                msg.what = 2;
                handler.sendMessageDelayed(msg, 1);
            }
        };
        seekBarR.setOnSeekBarChangeListener(seekBarSeekBarChangeListener);
        seekBarG.setOnSeekBarChangeListener(seekBarSeekBarChangeListener);
        seekBarB.setOnSeekBarChangeListener(seekBarSeekBarChangeListener);
        seekBarW.setOnSeekBarChangeListener(seekBarSeekBarChangeListener);
        //endregion
        //endregion

        //region SwipeLayout更新当前状态
        mSwipeLayout = view.findViewById(R.id.swipeRefreshLayout);
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
            if (jsonObject.has("name")) device.setName(jsonObject.getString("name"));


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
