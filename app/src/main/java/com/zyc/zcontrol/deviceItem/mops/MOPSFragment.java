package com.zyc.zcontrol.deviceItem.mops;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.zyc.Function;
import com.zyc.linetable.LineTableView;
import com.zyc.linetable.WeightHistoryData;
import com.zyc.zcontrol.MainActivity;
import com.zyc.zcontrol.R;
import com.zyc.zcontrol.deviceAdd.DeviceAddChoiceActivity;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceFragment;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceMOPS;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

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


    ListView lv_task;
    ArrayList<TaskItem> data = new ArrayList<>();
    MOPSTaskListAdapter adapter;
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
                            + "\"on\" : null,"
                            + "\"task_0\" : null,"
                            + "\"task_1\" : null,"
                            + "\"task_2\" : null,"
                            + "\"task_3\" : null,"
                            + "\"task_4\" : null"
                            + "}"
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
        sw_on = view.findViewById(R.id.sw_on);
        tv_task = view.findViewById(R.id.tv_task);
        tv_task.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lv_task.getVisibility() != View.VISIBLE)
                    lv_task.setVisibility(View.VISIBLE);
                else lv_task.setVisibility(View.GONE);
            }
        });
        sw_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Send("{\"mac\":\"" + device.getMac() + "\",\"on\":" + String.valueOf(((Switch) v).isChecked() ? 1 : 0) + "}");
            }
        });

        //region listview及adapter
        data.add(new TaskItem(getActivity()));
        data.add(new TaskItem(getActivity()));
        data.add(new TaskItem(getActivity()));
        data.add(new TaskItem(getActivity()));
        data.add(new TaskItem(getActivity()));
        lv_task = view.findViewById(R.id.lv);
        lv_task.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                popupwindowTask(position);
            }
        });
        adapter = new MOPSTaskListAdapter(getActivity(), data, new MOPSTaskListAdapter.Callback() {
            @Override
            public void click(View v, int position) {
                TaskItem task = adapter.getItem(position);
                int hour = task.hour;
                int minute = task.minute;
                int action = task.action;
                int on = ((Switch) v).isChecked() ? 1 : 0;
                int repeat = task.repeat;

                Send("{\"mac\": \"" + device.getMac() + "\",\"task_" + position + "\":{\"hour\":" + hour + ",\"minute\":" + minute + ",\"repeat\":" + repeat + ",\"action\":" + action + ",\"on\":" + on + "}}");
            }
        });
        lv_task.setAdapter(adapter);


        Button b = new Button(getActivity());
//        b.setBackground(null);
        b.setTextColor(0xffffffff);
//        b.setBackgroundResource(R.drawable.background_gray_borders);
        b.setText("设置倒计时");
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupwindowCountDown();
            }
        });
        lv_task.addFooterView(b);
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

    //region 弹窗
    private void popupwindowTask(final int task_id) {

        final View popupView = getLayoutInflater().inflate(R.layout.popupwindow_mops_set_time, null);
        final PopupWindow window = new PopupWindow(popupView, MATCH_PARENT, MATCH_PARENT, true);//wrap_content,wrap_content

        final TaskItem task = adapter.getItem(task_id);

        //region 控件初始化
        //region 控件定义
        final NumberPicker hour_picker = popupView.findViewById(R.id.hour_picker);
        final NumberPicker minute_picker = popupView.findViewById(R.id.minute_picker);
        final NumberPicker action_picker = popupView.findViewById(R.id.on_picker);
        final TextView tv_repeat = popupView.findViewById(R.id.tv_repeat);
        final Button btn_ok = popupView.findViewById(R.id.btn_ok);
        final ToggleButton tbtn_week[] = {popupView.findViewById(R.id.tbtn_week_1),
                popupView.findViewById(R.id.tbtn_week_2), popupView.findViewById(R.id.tbtn_week_3),
                popupView.findViewById(R.id.tbtn_week_4), popupView.findViewById(R.id.tbtn_week_5),
                popupView.findViewById(R.id.tbtn_week_6), popupView.findViewById(R.id.tbtn_week_7),
        };
        //endregion

        //region ToggleButton week 初始化
        ToggleButton.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int repeat = 0;
                for (int i = tbtn_week.length; i > 0; i--) {
                    repeat = repeat << 1;
                    if (tbtn_week[i - 1].isChecked()) repeat |= 1;
                }
                tv_repeat.setText("重复:" + Function.getWeek(repeat));

            }
        };
        int temp = task.repeat;
        for (int i = 0; i < tbtn_week.length; i++) {
            tbtn_week[i].setOnCheckedChangeListener(checkedChangeListener);
            if ((temp & 0x01) != 0) tbtn_week[i].setChecked(true);
            temp = temp >> 1;
        }
        //endregion
        //region NumberPicker初始化
        //region 小时
        hour_picker.setMaxValue(23);
        hour_picker.setMinValue(0);
        hour_picker.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format("%02d", value);
            }
        });
        hour_picker.setValue(task.hour);
        //endregion
        //region 分钟
        minute_picker.setMaxValue(59);
        minute_picker.setMinValue(0);
        minute_picker.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format("%02d", value);
            }
        });
        minute_picker.setValue(task.minute);
        //endregion
        //region 开关
        String[] action = {"关闭", "开启"};
        action_picker.setDisplayedValues(action);
        action_picker.setMinValue(0);
        action_picker.setMaxValue(action.length - 1);
        action_picker.setValue(task.action);
        //endregion
        //endregion
        //region 快捷按钮
        final Button btn_time_now = popupView.findViewById(R.id.btn_time_now);
        final Button btn_repeat_everyday = popupView.findViewById(R.id.btn_repeat_everyday);
        btn_time_now.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                hour_picker.setValue(calendar.get(Calendar.HOUR_OF_DAY));
                minute_picker.setValue(calendar.get(Calendar.MINUTE));
            }
        });

        btn_repeat_everyday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(tbtn_week[0].isChecked()&&tbtn_week[1].isChecked()&&
                        tbtn_week[2].isChecked()&&tbtn_week[3].isChecked()&&
                        tbtn_week[4].isChecked()&&tbtn_week[5].isChecked()&&
                        tbtn_week[6].isChecked()){
                    for (int i = 0; i < tbtn_week.length; i++)
                        tbtn_week[i].setChecked(false);
                }else{

                    for (int i = 0; i < tbtn_week.length; i++)
                        tbtn_week[i].setChecked(true);
                }

            }
        });
        //endregion

        //region 确认按钮初始化
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hour = hour_picker.getValue();
                int minute = minute_picker.getValue();
                int action = action_picker.getValue();
                int on = 1;
                int repeat = 0;
                for (int i = tbtn_week.length; i > 0; i--) {
                    repeat = repeat << 1;
                    if (tbtn_week[i - 1].isChecked()) repeat |= 1;
                }

                Send("{\"mac\": \"" + device.getMac() + "\",\"task_" + task_id + "\":{\"hour\":" + hour + ",\"minute\":" + minute + ",\"repeat\":" + repeat + ",\"action\":" + action + ",\"on\":" + on + "}}");
                window.dismiss();
            }
        });
        //endregion
        //region window初始化
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.alpha(0xffff0000)));
        //endregion
        //endregion
        window.update();
        window.showAtLocation(popupView, Gravity.CENTER, 0, 0);

    }


    private void popupwindowCountDown() {

        final View popupView = getLayoutInflater().inflate(R.layout.popupwindow_mops_set_time_count_down, null);
        final PopupWindow window = new PopupWindow(popupView, MATCH_PARENT, MATCH_PARENT, true);//wrap_content,wrap_content

        final int task_id = 4;
        //region 控件初始化
        //region 控件定义
        final NumberPicker hour_picker = popupView.findViewById(R.id.hour_picker);
        final NumberPicker minute_picker = popupView.findViewById(R.id.minute_picker);
        final NumberPicker action_picker = popupView.findViewById(R.id.on_picker);
        final Button btn_ok = popupView.findViewById(R.id.btn_ok);

        //endregion

        //region NumberPicker初始化
        //region 小时
        hour_picker.setMaxValue(23);
        hour_picker.setMinValue(0);
        hour_picker.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format("%02d", value);
            }
        });
        hour_picker.setValue(1);
        //endregion
        //region 分钟
        minute_picker.setMaxValue(59);
        minute_picker.setMinValue(0);
        minute_picker.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format("%02d", value);
            }
        });
        minute_picker.setValue(0);
        //endregion
        //region 开关
        String[] action = {"关闭", "开启"};
        action_picker.setDisplayedValues(action);
        action_picker.setMinValue(0);
        action_picker.setMaxValue(action.length - 1);
        action_picker.setValue(1);
        //endregion
        //endregion
        //region 快捷时间按钮初始化
        final Button btn_1_hour = popupView.findViewById(R.id.btn_time_now);
        final Button btn_2_hour = popupView.findViewById(R.id.btn_repeat_everyday);
        final Button btn_4_hour = popupView.findViewById(R.id.btn_4_hour);
        final Button btn_8_hour = popupView.findViewById(R.id.btn_8_hour);
        btn_1_hour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hour_picker.setValue(1);
                minute_picker.setValue(0);
            }
        });
        btn_2_hour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hour_picker.setValue(2);
                minute_picker.setValue(0);
            }
        });
        btn_4_hour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hour_picker.setValue(4);
                minute_picker.setValue(0);
            }
        });
        btn_8_hour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hour_picker.setValue(8);
                minute_picker.setValue(0);
            }
        });
        //endregion

        //region 确认按钮初始化
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hour = hour_picker.getValue();
                int minute = minute_picker.getValue();
                int action = action_picker.getValue();
                int on = 1;
                int repeat = 0;

                Calendar c = Calendar.getInstance();
                c.add(Calendar.HOUR_OF_DAY, hour);
                c.add(Calendar.MINUTE, minute);
                hour = c.get(Calendar.HOUR_OF_DAY);
                minute = c.get(Calendar.MINUTE);

                Send("{\"mac\": \"" + device.getMac() + "\",\"task_" + task_id + "\":{\"hour\":" + hour + ",\"minute\":" + minute + ",\"repeat\":" + repeat + ",\"action\":" + action + ",\"on\":" + on + "}}");
                window.dismiss();
            }
        });
        //endregion
        //region window初始化
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.alpha(0xffff0000)));
        //endregion
        //endregion
        window.update();
        window.showAtLocation(popupView, Gravity.CENTER, 0, 0);

    }

    //endregion

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
            //region 识别5组定时任务
            for (int i = 0; i < 5; i++) {
                if (!jsonObject.has("task_" + i)) continue;
                JSONObject jsonPlugTask = jsonObject.getJSONObject("task_" + i);
                if (!jsonPlugTask.has("hour") || !jsonPlugTask.has("minute") ||
                        !jsonPlugTask.has("repeat") || !jsonPlugTask.has("action") ||
                        !jsonPlugTask.has("on")) continue;

                adapter.setTask(i, jsonPlugTask.getInt("hour"),
                        jsonPlugTask.getInt("minute"),
                        jsonPlugTask.getInt("repeat"),
                        jsonPlugTask.getInt("action"),
                        jsonPlugTask.getInt("on"));
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
