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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.zyc.zcontrol.R;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceFragment;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceKey51;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.Group;
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
        final View view = inflater.inflate(R.layout.key51_fragment, container, false);

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
        for (int i = 0; i < 10; i++) {
            TaskItem t = new TaskItem();
            t.setBase("任务"+i,0,0,0);
            //t.setMqtt("");
            data.add(t);
        }
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

    TaskItem task_backup = null;

    //region 弹窗
    private void popupwindowTask(final int task_id) {

        final View popupView = getLayoutInflater().inflate(R.layout.key51_popupwindow_set_task, null);
        final PopupWindow window = new PopupWindow(popupView, MATCH_PARENT, MATCH_PARENT, true);//wrap_content,wrap_content

        final TaskItem task = adapter.getItem(task_id);

        //region 控件初始化
        //region 控件定义
        TextView tv_id = popupView.findViewById(R.id.tv_id);
        tv_id.setText("任务" + task_id);
        final Button btn_ok = popupView.findViewById(R.id.btn_ok);
        final Button btn_cancel = popupView.findViewById(R.id.btn_cancel);
        final EditText edt_name = popupView.findViewById(R.id.edt_name);
        final EditText edt_key = popupView.findViewById(R.id.edt_key);
        final Spinner spinner_type = popupView.findViewById(R.id.spinner_type);
        final EditText edt_topic = popupView.findViewById(R.id.edt_topic);
        final EditText edt_payload = popupView.findViewById(R.id.edt_payload);
        final Spinner spinner_qos = popupView.findViewById(R.id.spinner_qos);
        final CheckBox chk_retained = popupView.findViewById(R.id.chk_retained);
        final CheckBox chk_udp = popupView.findViewById(R.id.chk_udp);
        final ImageView img_udp_help = popupView.findViewById(R.id.img_udp_help);
        final EditText edt_mac = popupView.findViewById(R.id.edt_mac);
        final EditText edt_ip = popupView.findViewById(R.id.edt_ip);
        final EditText edt_port = popupView.findViewById(R.id.edt_port);
        final EditText edt_secure = popupView.findViewById(R.id.edt_secure);
        final EditText edt_max = popupView.findViewById(R.id.edt_max);
        final EditText edt_min = popupView.findViewById(R.id.edt_min);
        final EditText edt_step = popupView.findViewById(R.id.edt_step);
        final EditText edt_val = popupView.findViewById(R.id.edt_val);

        final Group group_custom = popupView.findViewById(R.id.group_custom);
        final Group group_wol = popupView.findViewById(R.id.group_wol);
        final Group group_encoder = popupView.findViewById(R.id.group_encoder);
        //endregion


        spinner_type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        group_custom.setVisibility(View.VISIBLE);
                        group_encoder.setVisibility(View.GONE);
                        group_wol.setVisibility(View.GONE);
                        break;
                    case 1:
                        group_custom.setVisibility(View.GONE);
                        group_encoder.setVisibility(View.GONE);
                        group_wol.setVisibility(View.VISIBLE);
                        break;
                    case 2:
                        group_custom.setVisibility(View.VISIBLE);
                        group_encoder.setVisibility(View.VISIBLE);
                        group_wol.setVisibility(View.GONE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        //region 确认按钮初始化
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    //region 确认name
                    if (edt_name.length() < 1) edt_name.setText("任务" + task_id);
                    //endregion


                    //region 确认ip
                    if (edt_ip.length() < 1) {
                        edt_ip.setText("255.255.255.255");
                    }
                    String ip_str = edt_ip.getText().toString().replaceAll("[^0123456789.]", ".");
                    String[] s = ip_str.split("\\.");
                    if (s.length != 4) throw new Exception("ip地址格式填写错误!");

                    int[] ip;
                    try {
                        ip = new int[]{
                                Integer.parseInt(s[0]),
                                Integer.parseInt(s[1]),
                                Integer.parseInt(s[2]),
                                Integer.parseInt(s[3])
                        };
                    } catch (NumberFormatException e) {
//                        e.printStackTrace();
                        throw new Exception("ip地址格式填写错误!");
                    }
                    if (ip[0] == 0 || ip[0] > 255 || ip[1] > 255 || ip[2] > 255 || ip[3] > 255)
                        throw new Exception("ip地址格式填写错误!");
                    //endregion

                    //region 端口确认
                    if (edt_port.length() < 1) edt_port.setText("0");
                    int port = 0;
                    try {
                        port = Integer.parseInt(edt_port.getText().toString());
                    } catch (NumberFormatException e) {
//                        e.printStackTrace();
                        throw new Exception("端口填写错误!");
                    }
                    if (port > 65535)
                        throw new Exception("端口填写错误!");
                    //endregion

                    //region 按键组合键值判断
                    if (edt_key.length() < 1) edt_port.setText("0");
                    int key = 0;
                    try {
                        key = Integer.parseInt(edt_key.getText().toString());
                    } catch (NumberFormatException e) {
//                        e.printStackTrace();
                        throw new Exception("按键顺序填写错误!");
                    }
                    for (int key_temp = key; key_temp != 0; key_temp = key_temp / 10) {
                        int c = key_temp % 10;
                        if (c == 0 || c > 6) throw new Exception("按键顺序填写错误!");
                    }
                    //endregion

                    switch (spinner_type.getSelectedItemPosition()) {
                        case 0:
                            task_backup = new TaskItem();
                            task_backup.setBase(edt_name.getText().toString(), 1, key, 0);
                            task_backup.setMqtt(
                                    edt_topic.getText().toString(),
                                    edt_payload.getText().toString(),
                                    spinner_qos.getSelectedItemPosition(),
                                    chk_retained.isChecked() ? 1 : 0,
                                    chk_udp.isChecked() ? 1 : 0,
                                    ip, port);
                            break;
                        case 1:
                            //region 确认mac

                            if (edt_mac.length() != 17) {
                                throw new Exception("mac格式填写错误!");
                            }
                            String mac_str = edt_mac.getText().toString().replaceAll("[^0123456789abcdefABCEDF:]", ":");
                            String[] mac_str_arr = mac_str.split(":");
                            if (mac_str_arr.length != 6) throw new Exception("mac格式填写错误!");

                            int[] mac;
                            try {
                                mac = new int[]{
                                        Integer.parseInt(mac_str_arr[0], 16),
                                        Integer.parseInt(mac_str_arr[1], 16),
                                        Integer.parseInt(mac_str_arr[2], 16),
                                        Integer.parseInt(mac_str_arr[3], 16),
                                        Integer.parseInt(mac_str_arr[4], 16),
                                        Integer.parseInt(mac_str_arr[5], 16)
                                };
                            } catch (NumberFormatException e) {
//                        e.printStackTrace();
                                throw new Exception("mac格式填写错误!");
                            }
                            if (mac[0] > 255 || mac[1] > 255 || mac[2] > 255 || mac[3] > 255 || mac[4] > 255 || mac[5] > 255)
                                throw new Exception("mac格式填写错误!");
                            //endregion
                            //region 确认secure

                            int[] secure;
                            if (edt_secure.length() < 1) {
                                secure = new int[]{0, 0, 0, 0, 0, 0};
                            } else {
                                if (edt_secure.length() != 17) {
                                    throw new Exception("secure格式填写错误!");
                                }
                                String secure_str = edt_secure.getText().toString().replaceAll("[^0123456789abcdefABCEDF:]", ":");
                                String[] secure_str_arr = secure_str.split(":");
                                if (secure_str_arr.length != 6)
                                    throw new Exception("secure格式填写错误!");

                                try {
                                    secure = new int[]{
                                            Integer.parseInt(secure_str_arr[0], 16),
                                            Integer.parseInt(secure_str_arr[1], 16),
                                            Integer.parseInt(secure_str_arr[2], 16),
                                            Integer.parseInt(secure_str_arr[3], 16),
                                            Integer.parseInt(secure_str_arr[4], 16),
                                            Integer.parseInt(secure_str_arr[5], 16)
                                    };
                                } catch (NumberFormatException e) {
//                                    e.printStackTrace();
                                    throw new Exception("secure格式填写错误!");
                                }
                                if (secure[0] > 255 || secure[1] > 255 || secure[2] > 255 || secure[3] > 255 || secure[4] > 255 || secure[5] > 255)
                                    throw new Exception("secure格式填写错误!");
                            }
                            //endregion
                            if (port == 0) {
                                edt_port.setText("9");
                                port = 9;
                            }
                            task_backup = new TaskItem();
                            task_backup.setBase(edt_name.getText().toString(), 1, key, 1);
                            task_backup.setWol(mac, ip, port, secure);
                            break;
                        //region 编码器部分
                        case 2:

                            //region 最大值/最小值/步进值/默认值确认
                            int max = -1, min = -1, step = -1, val = -1;
                            try {
                                if (edt_max.length() < 1) edt_max.setText(edt_max.getHint());
                                if (edt_min.length() < 1) edt_min.setText(edt_min.getHint());
                                if (edt_step.length() < 1) edt_step.setText(edt_step.getHint());
                                if (edt_val.length() < 1) edt_val.setText(edt_val.getHint());

                                max = Integer.parseInt(edt_max.getText().toString());
                                min = Integer.parseInt(edt_min.getText().toString());
                                step = Integer.parseInt(edt_step.getText().toString());
                                val = Integer.parseInt(edt_val.getText().toString());

                            } catch (NumberFormatException e) {
//                                e.printStackTrace();
                                if (max < 0) throw new Exception("最大值填写错误!");
                                if (min < 0) throw new Exception("最小值填写错误!");
                                if (step < 0) throw new Exception("步进值填写错误!");
                                if (val < 0) throw new Exception("默认值填写错误!");
                            }

                            if (max < min || step > max - min || val > max || val < min) {
                                throw new Exception("最大值/最小值填写错误!");
                            }
                            //endregion

                            task_backup = new TaskItem();
                            task_backup.setBase(edt_name.getText().toString(), 1, key, 2);
                            task_backup.setEncoder(
                                    edt_topic.getText().toString(),
                                    edt_payload.getText().toString(),
                                    spinner_qos.getSelectedItemPosition(),
                                    chk_retained.isChecked() ? 1 : 0,
                                    chk_udp.isChecked() ? 1 : 0,
                                    ip, port, max, min, step, val);

                            break;
                        //endregion
                    }

                    Send("{\"mac\": \"" + device.getMac() + "\",\"task_" + task_id + "\":" + task_backup.getJson() + "}");
                    window.dismiss();

                } catch (Exception e) {
//                    e.printStackTrace();
                    AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                            .setTitle("错误!")
                            .setMessage(e.getMessage())
                            .setPositiveButton("确认", null)
                            .create();
                    alertDialog.show();
                }
            }
        });
        //endregion

        //region 取消按钮
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                window.dismiss();
            }
        });
        btn_cancel.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (task_backup != null) {
                    edt_name.setText(task_backup.name);
                    edt_key.setText(String.valueOf(task_backup.key));
                    spinner_type.setSelection(task_backup.type);

                    switch (task_backup.type) {
                        case 0:
                            group_custom.setVisibility(View.VISIBLE);
                            group_encoder.setVisibility(View.GONE);
                            group_wol.setVisibility(View.GONE);
                            edt_topic.setText(task_backup.topic);
                            edt_payload.setText(task_backup.payload.replaceAll("%%d", "%%d").replaceAll("%d", "%%d"));
                            spinner_qos.setSelection(task_backup.qos);
                            chk_retained.setChecked(task_backup.retained == 1);
                            chk_udp.setChecked(task_backup.udp == 1);
                            break;
                        case 1:
                            group_custom.setVisibility(View.GONE);
                            group_encoder.setVisibility(View.GONE);
                            group_wol.setVisibility(View.VISIBLE);
                            edt_mac.setText(task_backup.getMacString());
                            edt_secure.setText(task_backup.getSecureString());
                            break;
                        case 2:
                            group_custom.setVisibility(View.VISIBLE);
                            group_encoder.setVisibility(View.VISIBLE);
                            group_wol.setVisibility(View.GONE);
                            edt_topic.setText(task_backup.topic);
                            edt_payload.setText(task_backup.payload.replaceAll("%%d", "%%d").replaceAll("%d", "%%d"));
                            spinner_qos.setSelection(task_backup.qos);
                            chk_retained.setChecked(task_backup.retained == 1);
                            chk_udp.setChecked(task_backup.udp == 1);
                            edt_max.setText(String.valueOf(task_backup.max));
                            edt_min.setText(String.valueOf(task_backup.min));
                            edt_step.setText(String.valueOf(task_backup.step));
                            edt_val.setText(String.valueOf(task_backup.val));
                            break;
                    }
                    edt_ip.setText(task_backup.getIPString());
                    edt_port.setText(String.valueOf(task_backup.port));
                }
                return true;
            }
        });
        //endregion
        //region window初始化
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.alpha(0xffff0000)));
        //endregion
        //endregion


        //region 数据初始化

        edt_name.setText(task.name);
        edt_key.setText(String.valueOf(task.key));
        spinner_type.setSelection(task.type);

        switch (task.type) {
            case 0:
                group_custom.setVisibility(View.VISIBLE);
                group_encoder.setVisibility(View.GONE);
                group_wol.setVisibility(View.GONE);
                edt_topic.setText(task.topic);
                edt_payload.setText(task.payload.replaceAll("%%d", "%%d").replaceAll("%d", "%%d"));
                spinner_qos.setSelection(task.qos);
                chk_retained.setChecked(task.retained == 1);
                chk_udp.setChecked(task.udp == 1);
                break;
            case 1:
                group_custom.setVisibility(View.GONE);
                group_encoder.setVisibility(View.GONE);
                group_wol.setVisibility(View.VISIBLE);
                edt_mac.setText(task.getMacString());
                edt_secure.setText(task.getSecureString());
                break;
            case 2:
                group_custom.setVisibility(View.VISIBLE);
                group_encoder.setVisibility(View.VISIBLE);
                group_wol.setVisibility(View.GONE);
                edt_topic.setText(task.topic);
                edt_payload.setText(task.payload.replaceAll("%%d", "%%d").replaceAll("%d", "%%d"));
                spinner_qos.setSelection(task.qos);
                chk_retained.setChecked(task.retained == 1);
                chk_udp.setChecked(task.udp == 1);
                edt_max.setText(String.valueOf(task.max));
                edt_min.setText(String.valueOf(task.min));
                edt_step.setText(String.valueOf(task.step));
                edt_val.setText(String.valueOf(task.val));
                break;
        }
        edt_ip.setText(task.getIPString());
        edt_port.setText(String.valueOf(task.port));
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
                int light = jsonObject.getInt("light");
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
