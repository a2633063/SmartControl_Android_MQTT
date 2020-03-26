package com.zyc.zcontrol.deviceItem.m1;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
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
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceM1;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 */
public class M1Fragment extends DeviceFragment {
    public final static String Tag = "M1Fragment";


    DeviceM1 device;
    //region 控件
    private SwipeRefreshLayout mSwipeLayout;
    TextView tvPm25;
    TextView tvFormaldehyde;
    TextView tvTemperature;
    TextView tvHumidity;
    SeekBar seekBar;
    TextView tvBrightness;

    //endregion

    public M1Fragment() {

        // Required empty public constructor
    }

    @SuppressLint("ValidFragment")
    public M1Fragment(DeviceM1 device) {
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
                    Send("{\"mac\":\"" + device.getMac() + "\",\"brightness\":null}");
                    break;
                case 2:
                    Log.d(Tag, "send seekbar:" + msg.arg1);

                    Send("{\"mac\":\"" + device.getMac() + "\",\"brightness\":" + msg.arg1 + "}");
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
        final View view = inflater.inflate(R.layout.fragment_m1, container, false);


        //region 控件初始化

        tvPm25 = view.findViewById(R.id.tv_pm25_value);
        tvFormaldehyde = view.findViewById(R.id.tv_formaldehyde_value);
        tvTemperature = view.findViewById(R.id.tv_temperature_value);
        tvHumidity = view.findViewById(R.id.tv_humidity_value);

        tvPm25.setText("---");
        tvFormaldehyde.setText("-.--");

        String exchange = getActivity().getResources().getString(R.string.m1_num_string, "--", "-");
        tvTemperature.setText(Html.fromHtml(exchange));
        exchange = getActivity().getResources().getString(R.string.m1_num_string, "--", "-").replace("℃", "%");
        tvHumidity.setText(Html.fromHtml(exchange));

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

        tvBrightness = view.findViewById(R.id.textViewR);
        tvBrightness.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), M1PlugActivity.class);
                intent.putExtra("mac", device.getMac());
                startActivity(intent);
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
        boolean b = getActivity().getSharedPreferences("Setting_" + device.getMac(), 0).getBoolean("always_UDP", false);
        super.Send(b, device.getSendMqttTopic(), message);
    }

    //数据接收处理更新函数
    public void Receive(String ip, int port, String topic, String message) {
        super.Receive(ip, port, topic, message);

        try {
            JSONObject jsonObject = new JSONObject(message);
            if (!jsonObject.has("mac") || !jsonObject.getString("mac").equals(device.getMac())) {
                return;
            }

            if (jsonObject.has("name")) device.setName(jsonObject.getString("name"));

            //region 解析传感器参数

            if (jsonObject.has("PM25")) {
                int PM25 = jsonObject.getInt("PM25");
                tvPm25.setText(String.valueOf(PM25));
            }

            if (jsonObject.has("formaldehyde")) {
                double formaldehyde = jsonObject.getDouble("formaldehyde");
                tvFormaldehyde.setText(String.valueOf(formaldehyde));
            }
            String exchange;
            if (jsonObject.has("temperature")) {
                double temperature = jsonObject.getDouble("temperature");
                int t = (int) (temperature * 10);
                exchange = getActivity().getResources().getString(R.string.m1_num_string, String.valueOf(t / 10), String.valueOf(t % 10));
                tvTemperature.setText(Html.fromHtml(exchange));
            }
            if (jsonObject.has("humidity")) {
                double humidity = jsonObject.getDouble("humidity");
                int h = (int) (humidity * 10);
                exchange = getActivity().getResources().getString(R.string.m1_num_string, String.valueOf(h / 10), String.valueOf(h % 10)).replace("℃", "%");
                tvHumidity.setText(Html.fromHtml(exchange));
            }
            //endregion
            //region 解析on
            if (jsonObject.has("brightness")) {
                int brightness = jsonObject.getInt("brightness");
                seekBar.setProgress(brightness);
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
