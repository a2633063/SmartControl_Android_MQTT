package com.zyc.zcontrol.deviceItem.a1;

import android.annotation.SuppressLint;
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
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.zyc.Function;
import com.zyc.zcontrol.MainApplication;
import com.zyc.zcontrol.R;
import com.zyc.zcontrol.ServiceActivity;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceA1;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class A1PlugActivity extends ServiceActivity {
    public final static String Tag = "A1PlugActivity";

    private SwipeRefreshLayout mSwipeLayout;
    ListView lv_task;
    ArrayList<TaskItem> data = new ArrayList<>();
    A1TaskListAdapter adapter;

    Button btn_count_down;

    DeviceA1 device;

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {// handler接收到消息后就会执行此方法
            switch (msg.what) {
                case 1:
                    Send("{\"mac\": \"" + device.getMac() + "\",\"task_0\":{},\"task_1\":{},\"task_2\":{},\"task_3\":{},\"task_4\":{}}");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a1_activity_plug);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//左侧添加一个默认的返回图标
        getSupportActionBar().setHomeButtonEnabled(true); //设置返回键可用

        Intent intent = this.getIntent();
        try {
            device = (DeviceA1) ((MainApplication) getApplication()).getDevice(intent.getStringExtra("mac"));
            if (device == null) {
                throw new Exception("获取数据出错:" + intent.getStringExtra("mac")); // 异常信息
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(A1PlugActivity.this, "数据错误!请联系开发者", Toast.LENGTH_SHORT).show();
            finish();
        }


        data.add(new TaskItem(this));
        data.add(new TaskItem(this));
        data.add(new TaskItem(this));
        data.add(new TaskItem(this));
        data.add(new TaskItem(this));

        //region 控件初始化
        //region listview及adapter
        lv_task = findViewById(R.id.lv);
        lv_task.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                popupwindowTask(position);
            }
        });
        adapter = new A1TaskListAdapter(A1PlugActivity.this, data, new A1TaskListAdapter.Callback() {
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

        //endregion


        //region 开关按键/名称
        btn_count_down = findViewById(R.id.btn_count_down);

        btn_count_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupwindowCountDown();
            }
        });
        //endregion


        //region 初始化下滑刷新功能:更新当前状态

        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimaryDark, R.color.colorAccent, R.color.colorPrimary);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                handler.sendEmptyMessageDelayed(1, 0);
                mSwipeLayout.setRefreshing(false);
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


    //region 弹窗
    private void popupwindowTask(final int task_id) {

        final View popupView = getLayoutInflater().inflate(R.layout.a1_popupwindow_set_time, null);
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
        String[] action = {"关闭", "10","20","30","40","50","60","70","80","90","100"};
        action_picker.setDisplayedValues(action);
        action_picker.setMinValue(0);
        action_picker.setMaxValue(action.length - 1);
        action_picker.setValue(task.action/10);
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
                int action = action_picker.getValue()*10;
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

        final View popupView = getLayoutInflater().inflate(R.layout.a1_popupwindow_set_time_count_down, null);
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
        String[] action = {"关闭", "10","20","30","40","50","60","70","80","90","100"};
        action_picker.setDisplayedValues(action);
        action_picker.setMinValue(0);
        action_picker.setMaxValue(action.length - 1);
        action_picker.setValue(0);
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
                int action = action_picker.getValue()*10;
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

    //region 数据接收发送处理函数
    void Send(String message) {
        boolean b = getSharedPreferences("Setting_" + device.getMac(), 0).getBoolean("always_UDP", false);
        super.Send(b , device.getSendMqttTopic(), message);
    }

    public void Receive(String ip, int port, String topic, String message) {

        try {
            JSONObject jsonObject = new JSONObject(message);
            if (!jsonObject.has("mac") || !jsonObject.getString("mac").equals(device.getMac())) {
                return;
            }


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
