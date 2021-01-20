package com.zyc.zcontrol.deviceItem.key51;


import android.annotation.SuppressLint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.zyc.Function;
import com.zyc.zcontrol.R;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceFragment;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceKey51;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * A simple {@link Fragment} subclass.
 */
public class Key51Fragment extends DeviceFragment {
    public final static String Tag = "Key51Fragment";


    DeviceKey51 device;
    //region 控件
    TextView tv_task;
    SeekBar seekBar;
    private SwipeRefreshLayout mSwipeLayout;


    ListView lv_task;
    ArrayList<TaskItem> data = new ArrayList<>();
    Key51TaskListAdapter adapter;
    //endregion


    public Key51Fragment() {

        // Required empty public constructor
    }

    @SuppressLint("ValidFragment")
    public Key51Fragment(DeviceKey51 device) {
        super(device.getName(), device.getMac());
        this.device = device;
    }

    //region Handler
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {// handler接收到消息后就会执行此方法
            switch (msg.what) {
                //region 获取数据
                case 1:
                    handler.removeMessages(1);
                    handler.removeMessages(2);
                    handler.removeMessages(3);
                    handler.removeMessages(4);
                    handler.removeMessages(5);
                    //同时发送时导致超出udp最大传输长度,分开发送
                    Send("{\"mac\": \"" + device.getMac() + "\","
                            + "\"light\" : null,"
                            + "\"task_0\" : null,"
                            + "\"task_1\" : null"
                            + "}"
                    );
                    handler.sendEmptyMessageDelayed(2, 100);
                    break;
                case 2:
                    Send("{\"mac\": \"" + device.getMac() + "\","
                            + "\"task_2\" : null,"
                            + "\"task_3\" : null"
                            + "}"
                    );
                    handler.sendEmptyMessageDelayed(3, 100);
                    break;
                case 3:
                    Send("{\"mac\": \"" + device.getMac() + "\","
                            + "\"task_4\" : null,"
                            + "\"task_5\" : null"
                            + "}"
                    );
                    handler.sendEmptyMessageDelayed(4, 100);
                    break;
                case 4:
                    Send("{\"mac\": \"" + device.getMac() + "\","
                            + "\"task_6\" : null,"
                            + "\"task_7\" : null"
                            + "}"
                    );
                    handler.sendEmptyMessageDelayed(5, 100);
                    break;
                case 5:
                    Send("{\"mac\": \"" + device.getMac() + "\","
                            + "\"task_8\" : null,"
                            + "\"task_9\" : null"
                            + "}"
                    );

                    break;
                //endregion
                //region 发送设置亮度
                case 10:
                    Log.d(Tag, "send seekbar:" + msg.arg1);

                    Send("{\"mac\":\"" + device.getMac() + "\",\"light\":" + msg.arg1 + "}");
                    break;
                //endregion
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
        final View view = inflater.inflate(R.layout.fragment_key51, container, false);

        //region 控件初始化
        //region 拖动条 处理viewpage/SwipeRefreshLayout滑动冲突事件
        seekBar = view.findViewById(R.id.seekBar);
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
                msg.what = 10;
                handler.sendMessageDelayed(msg, 1);
            }
        });

        //endregion

        tv_task = view.findViewById(R.id.tv_task);
        tv_task.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lv_task.getVisibility() != View.VISIBLE)
                    lv_task.setVisibility(View.VISIBLE);
                else lv_task.setVisibility(View.GONE);
            }
        });


        //region listview及adapter
        data.add(new TaskItem(getActivity()));
        data.add(new TaskItem(getActivity()));
        data.add(new TaskItem(getActivity()));
        data.add(new TaskItem(getActivity()));
        data.add(new TaskItem(getActivity()));
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
        adapter = new Key51TaskListAdapter(getActivity(), data, new Key51TaskListAdapter.Callback() {
            @Override
            public void click(View v, int position) {
                TaskItem task = adapter.getItem(position);
                //TODO
                Send("{\"mac\": \"" + device.getMac() + "\",\"task_" + position + "\":{\"on\":" + (task.on == 0 ? 1 : 0) + "}}");
            }
        });
        lv_task.setAdapter(adapter);

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

        final View popupView = getLayoutInflater().inflate(R.layout.popupwindow_key51_set_task, null);
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


        //region 确认按钮初始化
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


//                Send("{\"mac\": \"" + device.getMac() + "\",\"task_" + task_id + "\":{\"hour\":" + hour + ",\"minute\":" + minute + ",\"repeat\":" + repeat + ",\"action\":" + action + ",\"on\":" + on + "}}");
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
                    Log(device.isOnline() ? "设备在线" : "设备离线" + "(请确认设备是否有连接mqtt服务器)");
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

            if (jsonObject.has("light")) {
                int light=jsonObject.getInt("light");
                seekBar.setProgress(light);
            }
            //region 识别5组定时任务
            for (int i = 0; i < 10; i++) {
                if (!jsonObject.has("task_" + i)) continue;
                JSONObject jsonTask = jsonObject.getJSONObject("task_" + i);


//                if (!jsonTask.has("hour") || !jsonTask.has("minute") ||
//                        !jsonTask.has("repeat") || !jsonTask.has("action") ||
//                        !jsonTask.has("on")) continue;
                adapter.getItem(i).name = jsonTask.getString("name");
                adapter.getItem(i).type = jsonTask.getInt("type");
                adapter.getItem(i).on = jsonTask.getInt("on");
                adapter.getItem(i).key = jsonTask.getInt("key");

                switch (adapter.getItem(i).type) {
                    case 0: //自定义数据
                        adapter.getItem(i).setMqtt(jsonTask.getString("topic"),
                                jsonTask.getString("payload"),
                                jsonTask.getInt("qos"),
                                jsonTask.getInt("retained"),
                                jsonTask.getInt("udp"),
                                new int[]{
                                        jsonTask.getJSONArray("ip").getInt(0),
                                        jsonTask.getJSONArray("ip").getInt(1),
                                        jsonTask.getJSONArray("ip").getInt(2),
                                        jsonTask.getJSONArray("ip").getInt(3)
                                },
                                jsonTask.getInt("port"));
                        break;
                    case 1: //wol
                        adapter.getItem(i).setWol(
                                new int[]{
                                        jsonTask.getJSONArray("mac").getInt(0),
                                        jsonTask.getJSONArray("mac").getInt(1),
                                        jsonTask.getJSONArray("mac").getInt(2),
                                        jsonTask.getJSONArray("mac").getInt(3),
                                        jsonTask.getJSONArray("mac").getInt(4),
                                        jsonTask.getJSONArray("mac").getInt(5)
                                },
                                new int[]{
                                        jsonTask.getJSONArray("ip").getInt(0),
                                        jsonTask.getJSONArray("ip").getInt(1),
                                        jsonTask.getJSONArray("ip").getInt(2),
                                        jsonTask.getJSONArray("ip").getInt(3)
                                },
                                jsonTask.getInt("port"),
                                new int[]{
                                        jsonTask.getJSONArray("secure").getInt(0),
                                        jsonTask.getJSONArray("secure").getInt(1),
                                        jsonTask.getJSONArray("secure").getInt(2),
                                        jsonTask.getJSONArray("secure").getInt(3),
                                        jsonTask.getJSONArray("secure").getInt(4),
                                        jsonTask.getJSONArray("secure").getInt(5)
                                });
                        break;
                    case 2: //编码器
                        adapter.getItem(i).setEncoder(jsonTask.getString("topic"),
                                jsonTask.getString("payload"),
                                jsonTask.getInt("qos"),
                                jsonTask.getInt("retained"),
                                jsonTask.getInt("udp"),
                                new int[]{
                                        jsonTask.getJSONArray("ip").getInt(0),
                                        jsonTask.getJSONArray("ip").getInt(1),
                                        jsonTask.getJSONArray("ip").getInt(2),
                                        jsonTask.getJSONArray("ip").getInt(3)
                                },
                                jsonTask.getInt("port"),
                                jsonTask.getInt("max"),
                                jsonTask.getInt("min"),
                                jsonTask.getInt("step"),
                                jsonTask.getInt("val")
                        );
                        break;
                }

                adapter.notifyDataSetChanged();
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
