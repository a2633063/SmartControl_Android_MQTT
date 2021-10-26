package com.zyc.zcontrol.deviceItem.UartToMqtt;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.zyc.Function;
import com.zyc.zcontrol.MainApplication;
import com.zyc.zcontrol.R;
import com.zyc.zcontrol.ServiceActivity;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceUartToMqtt;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class UartToMqttTaskActivity extends ServiceActivity {
    public final static String Tag = "UartToMqttTaskActivity";

    private SwipeRefreshLayout mSwipeLayout;
    ListView lv_task;
    ArrayList<TaskItem> data = new ArrayList<>();
    UartToMqttTaskListAdapter adapter;

    DeviceUartToMqtt device;


    //region Handler
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {// handler接收到消息后就会执行此方法
            switch (msg.what) {
                case 1: {
                    if (msg.arg1 < 20) {
                        Send("{\"mac\": \"" + device.getMac() + "\",\"task_" + msg.arg1 + "\":null}");
                        // Send("{\"mac\": \"" + device.getMac() + "\",\"task_" + msg.arg1 + "\":null,\"task_" + (msg.arg1 + 1) + "\":null}");

                        if (msg.arg1 < 19) {
                            Message message = new Message();
                            message.what = 1;
                            message.arg1 = msg.arg1 + 1;
                            handler.sendMessageDelayed(message, message.arg1 % 4 == 0 ? 80 : 300);
                        } else {
                            if (mSwipeLayout.isRefreshing())
                                mSwipeLayout.setRefreshing(false);
                        }
                    }
                    break;
                }
                case 2: {
                    Send("{\"mac\": \"" + device.getMac() + "\",\"uart_last\":null}");
                    break;
                }
            }
        }
    };
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uarttomqtt_activity_task);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//左侧添加一个默认的返回图标
        getSupportActionBar().setHomeButtonEnabled(true); //设置返回键可用

        Intent intent = this.getIntent();

        try {
            device = (DeviceUartToMqtt) ((MainApplication) getApplication()).getDevice(intent.getStringExtra("mac"));
            if (device == null) {
                throw new Exception("获取数据出错:" + intent.getStringExtra("mac")); // 异常信息
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(UartToMqttTaskActivity.this, "数据错误!请联系开发者", Toast.LENGTH_SHORT).show();
            finish();
        }

        for (int i = 0; i < 20; i++) {
            TaskItem t = new TaskItem();
            t.setBase("未获取" + (i + 1), 0, TaskItem.TASK_TYPE_MQTT);
            t.setTriggerUart("");
            t.setMqtt("", "", 0, 0, 0, 0, new int[]{255, 255, 255, 255}, 10182);
            data.add(t);
        }


        //region 控件初始化
        //region listview及adapter
        lv_task = findViewById(R.id.lv);
        lv_task.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                popupwindowTask(position);
            }
        });
        adapter = new UartToMqttTaskListAdapter(UartToMqttTaskActivity.this, data, new UartToMqttTaskListAdapter.Callback() {
            @Override
            public void click(View v, int position) {
                TaskItem task = adapter.getItem(position);
                Send("{\"mac\": \"" + device.getMac() + "\",\"task_" + position + "\":{\"on\":" + (task.on == 0 ? 1 : 0) + "}}");
            }
        });
        lv_task.setAdapter(adapter);

        //endregion

        //endregion


        //region 初始化下滑刷新功能
        mSwipeLayout = findViewById(R.id.swipeRefreshLayout);
        mSwipeLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimaryDark, R.color.colorAccent, R.color.colorPrimary);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                handler.sendEmptyMessageDelayed(1, 0);
            }
        });
        //endregion
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //获取上次串口数据时记录需要显示在哪个EditText中
    EditText editLast = null;

    //region 弹窗
    private void popupwindowTask(final int task_id) {

        final View popupView = getLayoutInflater().inflate(R.layout.uarttomqtt_popupwindow_set_task, null);
        final PopupWindow window = new PopupWindow(popupView, MATCH_PARENT, MATCH_PARENT, true);//wrap_content,wrap_content

        final TaskItem task = adapter.getItem(task_id);

        //region 控件初始化
        //region 获取各控件
        final TextView tv_id = popupView.findViewById(R.id.tv_id);
        final EditText edt_name = popupView.findViewById(R.id.edt_name);
        final Spinner spinner_type = popupView.findViewById(R.id.spinner_type);
        final ImageView img_help = popupView.findViewById(R.id.img_help);
        final Button btn_ok = popupView.findViewById(R.id.btn_ok);
        final Button btn_cancel = popupView.findViewById(R.id.btn_cancel);

        //region 触发页面
        final Group group_trigger_time = popupView.findViewById(R.id.group_trigger_time);
        final Group group_trigger_uart = popupView.findViewById(R.id.group_trigger_uart);
        final Group group_trigger_uart_advanced = popupView.findViewById(R.id.group_trigger_uart_advanced);

        final Button btn_trigger_uart_get_last = popupView.findViewById(R.id.btn_trigger_uart_get_last);
        final EditText edt_trigger_uart_payload = popupView.findViewById(R.id.edt_trigger_uart_payload);
        final EditText edit_trigger_uart_reserved_rec = popupView.findViewById(R.id.edit_trigger_uart_reserved_rec);
        final CheckBox chk_trigger_uart_mqtt_send = popupView.findViewById(R.id.chk_trigger_uart_mqtt_send);
        final CheckBox chk_trigger_uart_advanced = popupView.findViewById(R.id.chk_trigger_uart_advanced);

        final ConstraintLayout layout_mqtt = popupView.findViewById(R.id.layout_mqtt);
        final ConstraintLayout layout_wol = popupView.findViewById(R.id.layout_wol);
        final ConstraintLayout layout_uart = popupView.findViewById(R.id.layout_uart);
        final Group group_mqtt_advanced = popupView.findViewById(R.id.group_mqtt_advanced);
        final CheckBox chk_mqtt_advanced = popupView.findViewById(R.id.chk_mqtt_advanced);
        final Group group_uart_advanced = popupView.findViewById(R.id.group_uart_advanced);
        final CheckBox chk_uart_advanced = popupView.findViewById(R.id.chk_uart_advanced);


        final TextView tv_trigger_time = popupView.findViewById(R.id.tv_trigger_time);
        final TextView tv_trigger_time_repeat = popupView.findViewById(R.id.tv_trigger_time_repeat);
        final ToggleButton tbtn_trigger_time_week[] = {popupView.findViewById(R.id.tbtn_trigger_time_week_1),
                popupView.findViewById(R.id.tbtn_trigger_time_week_2), popupView.findViewById(R.id.tbtn_trigger_time_week_3),
                popupView.findViewById(R.id.tbtn_trigger_time_week_4), popupView.findViewById(R.id.tbtn_trigger_time_week_5),
                popupView.findViewById(R.id.tbtn_trigger_time_week_6), popupView.findViewById(R.id.tbtn_trigger_time_week_7),
        };
        final Button btn_trigger_time_now = popupView.findViewById(R.id.btn_trigger_time_now);
        final Button btn_trigger_time_repeat_everyday = popupView.findViewById(R.id.btn_trigger_time_repeat_everyday);
        //endregion

        //region 自动化mqtt部分
        final EditText edt_mqtt_topic = popupView.findViewById(R.id.edt_mqtt_topic);
        final EditText edt_mqtt_payload = popupView.findViewById(R.id.edt_mqtt_payload);
        final Spinner spinner_mqtt_qos = popupView.findViewById(R.id.spinner_mqtt_qos);
        final CheckBox chk_mqtt_retained = popupView.findViewById(R.id.chk_mqtt_retained);
        final EditText edt_mqtt_udp_ip = popupView.findViewById(R.id.edt_mqtt_udp_ip);
        final EditText edt_mqtt_udp_port = popupView.findViewById(R.id.edt_mqtt_udp_port);
        final EditText edit_mqtt_reserved_rec = popupView.findViewById(R.id.edit_mqtt_reserved_rec);
        final CheckBox chk_mqtt_udp = popupView.findViewById(R.id.chk_mqtt_udp);
        //endregion
        //region 自动化Wol部分
        final EditText edt_wol_mac = popupView.findViewById(R.id.edt_wol_mac);
        final EditText edt_wol_ip = popupView.findViewById(R.id.edt_wol_ip);
        final EditText edt_wol_port = popupView.findViewById(R.id.edt_wol_port);
        final EditText edt_wol_secure = popupView.findViewById(R.id.edt_wol_secure);
        //endregion
        //region 自动化uart部分
        final EditText edt_uart_payload = popupView.findViewById(R.id.edt_uart_payload);
        final EditText edit_uart_reserved_rec = popupView.findViewById(R.id.edit_uart_reserved_rec);
        final EditText edit_mqtt_reserved_send = popupView.findViewById(R.id.edit_mqtt_reserved_send);
        final Button btn_uart_get_last = popupView.findViewById(R.id.btn_uart_get_last);
        //endregion
        //endregion

        //region 根据任务类型切换显示页面
        spinner_type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                group_trigger_uart.setVisibility(View.GONE);
                group_trigger_time.setVisibility(View.GONE);
                layout_mqtt.setVisibility(View.GONE);
                layout_wol.setVisibility(View.GONE);
                layout_uart.setVisibility(View.GONE);
                switch (position) {
                    case 0: //TASK_TYPE_MQTT
                        group_trigger_uart.setVisibility(View.VISIBLE);
                        layout_mqtt.setVisibility(View.VISIBLE);
                        break;
                    case 1: //TASK_TYPE_WOL
                        group_trigger_uart.setVisibility(View.VISIBLE);
                        layout_wol.setVisibility(View.VISIBLE);
                        break;
                    case 2: //TASK_TYPE_UART
                        group_trigger_uart.setVisibility(View.VISIBLE);
                        layout_uart.setVisibility(View.VISIBLE);
                        break;
                    case 3: //TASK_TYPE_TIME_MQTT
                        group_trigger_time.setVisibility(View.VISIBLE);
                        layout_mqtt.setVisibility(View.VISIBLE);
                        break;
                    case 4: //TASK_TYPE_TIME_UART
                        group_trigger_time.setVisibility(View.VISIBLE);
                        layout_uart.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                group_trigger_uart.setVisibility(View.GONE);
                group_trigger_time.setVisibility(View.GONE);
                layout_mqtt.setVisibility(View.GONE);
                layout_wol.setVisibility(View.GONE);
                layout_uart.setVisibility(View.GONE);
            }
        });
        //endregion

        //region 串口触发部分 高级设置开关
        group_trigger_uart_advanced.setVisibility(View.GONE);
        chk_trigger_uart_advanced.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                group_trigger_uart_advanced.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        //endregion
        //region 定时触发部分 周一-周日重复设置
        CompoundButton.OnCheckedChangeListener weekCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int repeat = 0;
                for (int i = tbtn_trigger_time_week.length; i > 0; i--) {
                    repeat = repeat << 1;
                    if (tbtn_trigger_time_week[i - 1].isChecked()) repeat |= 1;
                }
                if (repeat == 0) {
                    buttonView.setChecked(true);
                    //Toast.makeText(getApplicationContext(), "至少要有一天重复日期!", Toast.LENGTH_SHORT).show();
                    Snackbar.make(popupView, "至少要有一天重复日期!", Snackbar.LENGTH_LONG)
                            .show();
                    for (int i = tbtn_trigger_time_week.length; i > 0; i--) {
                        repeat = repeat << 1;
                        if (tbtn_trigger_time_week[i - 1].isChecked()) repeat |= 1;
                    }
                }
                tv_trigger_time_repeat.setText("重复:" + Function.getWeek(repeat));
            }
        };
        for (int i = 0; i < tbtn_trigger_time_week.length; i++) {
            tbtn_trigger_time_week[i].setOnCheckedChangeListener(weekCheckedChangeListener);
        }
        //endregion
        //region 定时触发部分 时间设置
        tv_trigger_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hour = 12;
                int minute = 0;
                String[] time = tv_trigger_time.getText().toString().split(":");
                hour = Integer.parseInt(time[0]);
                minute = Integer.parseInt(time[1]);
                TimePickerDialog timePickerDialog = new TimePickerDialog(UartToMqttTaskActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        tv_trigger_time.setText(String.format("%02d:%02d", hourOfDay, minute));
                    }
                }, hour, minute, true);
                timePickerDialog.show();
            }
        });
        //endregion
        //region 定时触发部分 当前时间/选择每天 快捷按钮
        btn_trigger_time_now.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv_trigger_time.setText(new SimpleDateFormat("HH:mm").format(new Date()));
            }
        });

        btn_trigger_time_repeat_everyday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < tbtn_trigger_time_week.length; i++) {
                    tbtn_trigger_time_week[i].setChecked(true);
                }
            }
        });
        //endregion
        //region 自动化部分 本地功能初始化

        //region mqtt部分 高级设置开关
        group_mqtt_advanced.setVisibility(View.GONE);
        chk_mqtt_advanced.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                group_mqtt_advanced.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        //endregion

        //region uart部分 高级设置开关
        group_uart_advanced.setVisibility(View.GONE);
        chk_uart_advanced.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                group_uart_advanced.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        //endregion
        //endregion

        btn_uart_get_last.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editLast = edt_uart_payload;
                handler.sendEmptyMessage(2);
            }
        });

        btn_trigger_uart_get_last.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editLast = edt_trigger_uart_payload;
                handler.sendEmptyMessage(2);
            }
        });

        tv_id.setText("任务" + (task_id + 1));
        edt_name.setText(task.name);
        popupwindowTask_show_task(popupView, task);

        //region 确认按钮初始化
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Send("{\"mac\": \"" + device.getMac() + "\",\"plug_" + plug_id + "\" : {\"setting\":{\"task_" + task_id + "\":{\"hour\":" + hour + ",\"minute\":" + minute + ",\"repeat\":" + repeat + ",\"action\":" + action + ",\"on\":" + on + "}}}}");
                window.dismiss();
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
        //endregion
        //region window初始化
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.alpha(0xffff0000)));
        //endregion
        //endregion
        window.update();
        window.showAtLocation(popupView, Gravity.CENTER, 0, 0);

    }

    private void popupwindowTask_show_task(View popupView, TaskItem task) {
        if (popupView == null || task == null) return;
        //region 获取各控件
        final TextView tv_id = popupView.findViewById(R.id.tv_id);
        final EditText edt_name = popupView.findViewById(R.id.edt_name);
        final Spinner spinner_type = popupView.findViewById(R.id.spinner_type);
        final ImageView img_help = popupView.findViewById(R.id.img_help);
        final Button btn_ok = popupView.findViewById(R.id.btn_ok);
        final Button btn_cancel = popupView.findViewById(R.id.btn_cancel);


        //region 触发页面
        final Group group_trigger_time = popupView.findViewById(R.id.group_trigger_time);
        final Group group_trigger_uart = popupView.findViewById(R.id.group_trigger_uart);
        final Group group_trigger_uart_advanced = popupView.findViewById(R.id.group_trigger_uart_advanced);

        final ConstraintLayout layout_mqtt = popupView.findViewById(R.id.layout_mqtt);
        final ConstraintLayout layout_wol = popupView.findViewById(R.id.layout_wol);
        final ConstraintLayout layout_uart = popupView.findViewById(R.id.layout_uart);
        final Group group_mqtt_advanced = popupView.findViewById(R.id.group_mqtt_advanced);
        final CheckBox chk_mqtt_advanced = popupView.findViewById(R.id.chk_mqtt_advanced);
        final Group group_uart_advanced = popupView.findViewById(R.id.group_uart_advanced);
        final CheckBox chk_uart_advanced = popupView.findViewById(R.id.chk_uart_advanced);


        final EditText edt_trigger_uart_payload = popupView.findViewById(R.id.edt_trigger_uart_payload);
        final EditText edit_trigger_uart_reserved_rec = popupView.findViewById(R.id.edit_trigger_uart_reserved_rec);
        final CheckBox chk_trigger_uart_mqtt_send = popupView.findViewById(R.id.chk_trigger_uart_mqtt_send);
        final CheckBox chk_trigger_uart_advanced = popupView.findViewById(R.id.chk_trigger_uart_advanced);

        final TextView tv_trigger_time = popupView.findViewById(R.id.tv_trigger_time);
        final TextView tv_trigger_time_repeat = popupView.findViewById(R.id.tv_trigger_time_repeat);
        final ToggleButton tbtn_trigger_time_week[] = {popupView.findViewById(R.id.tbtn_trigger_time_week_1),
                popupView.findViewById(R.id.tbtn_trigger_time_week_2), popupView.findViewById(R.id.tbtn_trigger_time_week_3),
                popupView.findViewById(R.id.tbtn_trigger_time_week_4), popupView.findViewById(R.id.tbtn_trigger_time_week_5),
                popupView.findViewById(R.id.tbtn_trigger_time_week_6), popupView.findViewById(R.id.tbtn_trigger_time_week_7),
        };
        final Button btn_trigger_time_now = popupView.findViewById(R.id.btn_trigger_time_now);
        final Button btn_trigger_time_repeat_everyday = popupView.findViewById(R.id.btn_trigger_time_repeat_everyday);
        //endregion

        //region 自动化mqtt部分
        final EditText edt_mqtt_topic = popupView.findViewById(R.id.edt_mqtt_topic);
        final EditText edt_mqtt_payload = popupView.findViewById(R.id.edt_mqtt_payload);
        final Spinner spinner_mqtt_qos = popupView.findViewById(R.id.spinner_mqtt_qos);
        final CheckBox chk_mqtt_retained = popupView.findViewById(R.id.chk_mqtt_retained);
        final EditText edt_mqtt_udp_ip = popupView.findViewById(R.id.edt_mqtt_udp_ip);
        final EditText edt_mqtt_udp_port = popupView.findViewById(R.id.edt_mqtt_udp_port);
        final EditText edit_mqtt_reserved_rec = popupView.findViewById(R.id.edit_mqtt_reserved_rec);
        final CheckBox chk_mqtt_udp = popupView.findViewById(R.id.chk_mqtt_udp);
        //endregion
        //region 自动化Wol部分
        final EditText edt_wol_mac = popupView.findViewById(R.id.edt_wol_mac);
        final EditText edt_wol_ip = popupView.findViewById(R.id.edt_wol_ip);
        final EditText edt_wol_port = popupView.findViewById(R.id.edt_wol_port);
        final EditText edt_wol_secure = popupView.findViewById(R.id.edt_wol_secure);
        //endregion

        //region 自动化uart部分
        final EditText edt_uart_payload = popupView.findViewById(R.id.edt_uart_payload);
        final EditText edit_uart_reserved_rec = popupView.findViewById(R.id.edit_uart_reserved_rec);
        final EditText edit_mqtt_reserved_send = popupView.findViewById(R.id.edit_mqtt_reserved_send);
        final Button btn_uart_get_last = popupView.findViewById(R.id.btn_uart_get_last);
        //endregion


        //endregion

        group_trigger_uart.setVisibility(View.GONE);
        group_trigger_time.setVisibility(View.GONE);
        layout_mqtt.setVisibility(View.GONE);
        layout_wol.setVisibility(View.GONE);
        layout_uart.setVisibility(View.GONE);
        switch (task.type) {    //切换spinner_type的选择项会自动切换ui界面,不需要手动处理
            case TaskItem.TASK_TYPE_MQTT:
                spinner_type.setSelection(0);
                break;
            case TaskItem.TASK_TYPE_WOL:
                spinner_type.setSelection(1);
                break;
            case TaskItem.TASK_TYPE_UART:
                spinner_type.setSelection(2);
                break;
            case TaskItem.TASK_TYPE_TIME_MQTT:
                spinner_type.setSelection(3);
                break;
            case TaskItem.TASK_TYPE_TIME_UART:
                spinner_type.setSelection(4);
                break;
            default:
                spinner_type.setSelection(-1);
                break;
        }

        //region 更新显示布局

        //region 触发部分更新显示
        switch (task.type) {
            case TaskItem.TASK_TYPE_MQTT:
            case TaskItem.TASK_TYPE_WOL:
            case TaskItem.TASK_TYPE_UART:
                edt_trigger_uart_payload.setText(task.condition_dat);
                chk_trigger_uart_mqtt_send.setChecked(task.mqtt_send != 0);
                if (task.reserved > 0 && task.reserved < 255) {
                    chk_trigger_uart_advanced.setChecked(true);
                    edit_trigger_uart_reserved_rec.setText(String.valueOf(task.reserved));
                }
                break;
            case TaskItem.TASK_TYPE_TIME_MQTT:
            case TaskItem.TASK_TYPE_TIME_UART:
                tv_trigger_time.setText(String.format("%02d:%02d", task.hour, task.minute));
                for (int i = 0; i < 7; i++) {
                    tbtn_trigger_time_week[i].setChecked((task.repeat & (1 << i)) != 0);
                }
                break;
        }
        //endregion
        //region 自动化部分更新显示
        switch (task.type) {
            case TaskItem.TASK_TYPE_MQTT:
            case TaskItem.TASK_TYPE_TIME_MQTT:
                edt_mqtt_topic.setText(task.mqtt.topic);
                edt_mqtt_payload.setText(task.mqtt.payload);
                spinner_mqtt_qos.setSelection(task.mqtt.qos);
                chk_mqtt_retained.setChecked(task.mqtt.retained != 0);
                edt_mqtt_udp_ip.setText(String.format(Locale.getDefault(), "%d.%d.%d.%d", task.mqtt.ip[0], task.mqtt.ip[1], task.mqtt.ip[2], task.mqtt.ip[3]));
                edt_mqtt_udp_port.setText(String.valueOf(task.mqtt.port));
                chk_mqtt_udp.setChecked(task.mqtt.udp != 0);
                if (task.mqtt.reserved > 0 && task.mqtt.reserved < 255) {
                    edit_mqtt_reserved_rec.setText(String.valueOf(task.mqtt.reserved));
                }

                if ((task.mqtt.reserved > 0 && task.mqtt.reserved < 255)
                        || task.mqtt.retained != 0 || task.mqtt.udp != 0) {
                    chk_mqtt_advanced.setChecked(true);
                }
                break;
            case TaskItem.TASK_TYPE_WOL:
                edt_wol_mac.setText(String.format("%02X:%02X:%02X:%02X:%02X:%02X", task.wol.mac[0], task.wol.mac[1], task.wol.mac[2], task.wol.mac[3], task.wol.mac[4], task.wol.mac[5]));
                edt_wol_ip.setText(String.format(Locale.getDefault(), "%d.%d.%d.%d", task.wol.ip[0], task.wol.ip[1], task.wol.ip[2], task.wol.ip[3]));
                edt_wol_secure.setText(String.format("%02X:%02X:%02X:%02X:%02X:%02X", task.wol.secure[0], task.wol.secure[1], task.wol.secure[2], task.wol.secure[3], task.wol.secure[4], task.wol.secure[5]));
                edt_wol_port.setText(String.valueOf(task.wol.port));
                break;
            case TaskItem.TASK_TYPE_UART:
            case TaskItem.TASK_TYPE_TIME_UART:
                edt_uart_payload.setText(task.uart.dat);
                if (task.uart.reserved_rec > 0 && task.uart.reserved_rec < 255
                        && task.uart.reserved_send > 0 && task.uart.reserved_send < 255) {
                    edit_uart_reserved_rec.setText(String.valueOf(task.uart.reserved_rec));
                    edit_mqtt_reserved_send.setText(String.valueOf(task.uart.reserved_send));
                    chk_uart_advanced.setChecked(true);
                }
                break;
        }
        //endregion
        //endregion

    }
    //endregion

    //region 数据接收发送处理函数
    void Send(String message) {
        boolean udp = getSharedPreferences("Setting_" + device.getMac(), 0).getBoolean("always_UDP", false);

        String topic = null;
        if (!udp) {
            topic = device.getSendMqttTopic();
        }
        super.Send(udp, topic, message);
    }


    public void Receive(String ip, int port, String topic, String message) {

        try {
            JSONObject jsonObject = new JSONObject(message);

            if (!jsonObject.has("mac") || !jsonObject.optString("mac").equals(device.getMac())) {
                return;
            }

            if (jsonObject.has("uart_last") && jsonObject.optString("uart_last").length() > 0) {
                if (editLast != null) {
                    editLast.setText(jsonObject.optString("uart_last"));
                }
                editLast = null;
            }

            //region 解析自动化任务
            boolean task_is_flash = false;
            for (int id = 0; id < 20; id++) {//共20组自动化任务
                if (!jsonObject.has("task_" + id)) continue;
                TaskItem task = data.get(id);
                if (task == null) continue;

                JSONObject jsonTask = jsonObject.getJSONObject("task_" + id);

                //region 任务基本信息
                if (jsonTask.has("name") && !jsonTask.optString("name").isEmpty()) {
                    task.name = jsonTask.optString("name");
                }
                if (jsonTask.has("type")) {
                    task.type = jsonTask.optInt("type");
                }

                if (jsonTask.has("on")) {
                    task.on = jsonTask.optInt("on");
                }
                //endregion

                //region 触发方式
                switch (task.type) {
                    case TaskItem.TASK_TYPE_MQTT:
                    case TaskItem.TASK_TYPE_WOL:
                    case TaskItem.TASK_TYPE_UART:
                        //串口触发方式
                        task.setTriggerUart(jsonTask.optString("uart_dat"), jsonTask.optInt("reserved"), jsonTask.optInt("mqtt_send"));
                        break;
                    case TaskItem.TASK_TYPE_TIME_MQTT:
                    case TaskItem.TASK_TYPE_TIME_UART:
                        task.setTriggerTime(jsonTask.optInt("hour", 12), jsonTask.optInt("minute", 0), jsonTask.optInt("repeat", 127));
                        break;
                }
                //endregion

                //region 自动化方式
                switch (task.type) {
                    case TaskItem.TASK_TYPE_MQTT:
                    case TaskItem.TASK_TYPE_TIME_MQTT: {
                        JSONObject jsonMqtt = jsonTask.getJSONObject("mqtt");
                        int[] mqtt_ip = new int[]{255, 255, 255, 255};
                        JSONArray jsonIP = jsonMqtt.optJSONArray("ip");
                        if (jsonIP != null && jsonIP.length() == 4) {
                            for (int i = 0; i < jsonIP.length(); i++) {
                                mqtt_ip[i] = jsonIP.optInt(i, 255);
                            }
                        }
                        task.setMqtt(jsonMqtt.optString("topic"), jsonMqtt.optString("payload"), jsonMqtt.optInt("qos"), jsonTask.optInt("retained"),
                                jsonMqtt.optInt("reserved"), jsonMqtt.optInt("udp"), mqtt_ip, jsonMqtt.optInt("port"));
                        break;
                    }
                    case TaskItem.TASK_TYPE_WOL: {
                        JSONObject jsonWol = jsonTask.getJSONObject("wol");
                        int[] wol_mac = new int[]{0, 0, 0, 0, 0, 0};
                        int[] wol_ip = new int[]{255, 255, 255, 255};
                        int[] wol_secure = new int[]{0, 0, 0, 0, 0, 0};

                        JSONArray jsonIP = jsonWol.optJSONArray("ip");
                        if (jsonIP != null && jsonIP.length() == 4) {
                            for (int i = 0; i < jsonIP.length(); i++) {
                                wol_ip[i] = jsonIP.optInt(i, 255);
                            }
                        }

                        JSONArray jsonMac = jsonWol.optJSONArray("mac");
                        if (jsonMac != null && jsonMac.length() == 6) {
                            for (int i = 0; i < jsonMac.length(); i++) {
                                wol_mac[i] = jsonMac.optInt(i);
                            }
                        }
                        JSONArray jsonSecure = jsonWol.optJSONArray("secure");
                        if (jsonSecure != null && jsonSecure.length() == 6) {
                            for (int i = 0; i < jsonSecure.length(); i++) {
                                wol_secure[i] = jsonSecure.optInt(i);
                            }
                        }
                        task.setWol(wol_mac, wol_ip, jsonWol.optInt("port", 9), wol_secure);
                        break;
                    }
                    case TaskItem.TASK_TYPE_UART:
                    case TaskItem.TASK_TYPE_TIME_UART: {
                        JSONObject jsonUart = jsonTask.getJSONObject("uart");
                        task.setUart(jsonUart.optString("uart"), jsonUart.optInt("reserved_rec"), jsonUart.optInt("reserved_send"));
                        break;
                    }

                }
                //endregion
                task_is_flash = true;
            }
            if (task_is_flash)
                adapter.notifyDataSetChanged();
           /* if (!jsonObject.has("task_" + plug_id)) return;
            JSONObject jsonPlug = jsonObject.getJSONObject("plug_" + plug_id);
            if (jsonPlug.has("on")) {
                int on = jsonPlug.getInt("on");
                tbt_button.setChecked(on != 0);
            }
            if (!jsonPlug.has("setting")) return;
            JSONObject jsonPlugSetting = jsonPlug.getJSONObject("setting");
            if (jsonPlugSetting.has("name")) {
                tv_name.setText(jsonPlugSetting.getString("name"));
            }

            //region 识别5组定时任务
            for (int i = 0; i < 5; i++) {
                if (!jsonPlugSetting.has("task_" + i)) continue;
                JSONObject jsonPlugTask = jsonPlugSetting.getJSONObject("task_" + i);
                if (!jsonPlugTask.has("hour") || !jsonPlugTask.has("minute") ||
                        !jsonPlugTask.has("repeat") || !jsonPlugTask.has("action") ||
                        !jsonPlugTask.has("on")) continue;

                /*adapter.setTask(i, jsonPlugTask.getInt("hour"),
                        jsonPlugTask.getInt("minute"),
                        jsonPlugTask.getInt("repeat"),
                        jsonPlugTask.getInt("action"),
                        jsonPlugTask.getInt("on"));*/
            // }
            //endregion
            //endregion


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    //endregion
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
