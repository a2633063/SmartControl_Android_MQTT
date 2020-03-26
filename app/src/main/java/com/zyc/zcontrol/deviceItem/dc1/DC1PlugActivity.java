package com.zyc.zcontrol.deviceItem.dc1;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.zyc.Function;
import com.zyc.zcontrol.MainApplication;
import com.zyc.zcontrol.R;
import com.zyc.zcontrol.ServiceActivity;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceDC1;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class DC1PlugActivity extends ServiceActivity {
    public final static String Tag = "DC1PlugActivity";

    private SwipeRefreshLayout mSwipeLayout;
    ListView lv_task;
    ArrayList<TaskItem> data = new ArrayList<>();
    DC1TaskListAdapter adapter;

    TextView tv_name;
    Switch tbt_button;
    Button btn_count_down;


    DeviceDC1 device;
    String plug_name = null;
    int plug_id = -1;

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {// handler接收到消息后就会执行此方法
            switch (msg.what) {
                case 1:
                    Send("{\"mac\": \"" + device.getMac() + "\",\"plug_" + plug_id + "\" : {\"on\" : null,\"setting\":{\"name\":null,\"task_0\":{},\"task_1\":{},\"task_2\":{},\"task_3\":{},\"task_4\":{}}}}");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dc1_plug);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//左侧添加一个默认的返回图标
        getSupportActionBar().setHomeButtonEnabled(true); //设置返回键可用

        Intent intent = this.getIntent();
        try {
            plug_name = intent.getStringExtra("plug_name");
            plug_id = intent.getIntExtra("plug_id", -1);
            device = (DeviceDC1) ((MainApplication) getApplication()).getDevice(intent.getStringExtra("mac"));
            if (device == null || plug_id < 0 || plug_id > 3) {
                throw new Exception("获取数据出错:" + intent.getStringExtra("mac") + "," + plug_id); // 异常信息
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(DC1PlugActivity.this, "数据错误!请联系开发者", Toast.LENGTH_SHORT).show();
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
        adapter = new DC1TaskListAdapter(DC1PlugActivity.this, data, new DC1TaskListAdapter.Callback() {
            @Override
            public void click(View v, int position) {
                TaskItem task = adapter.getItem(position);
                int hour = task.hour;
                int minute = task.minute;
                int action = task.action;
                int on = ((Switch) v).isChecked() ? 1 : 0;
                int repeat = task.repeat;

                Send("{\"mac\": \"" + device.getMac() + "\",\"plug_" + plug_id + "\" : {\"setting\":{\"task_" + position + "\":{\"hour\":" + hour + ",\"minute\":" + minute + ",\"repeat\":" + repeat + ",\"action\":" + action + ",\"on\":" + on + "}}}}");
            }
        });
        lv_task.setAdapter(adapter);

        //endregion


        //region 开关按键/名称
        tv_name = findViewById(R.id.tv_name);
        tbt_button = findViewById(R.id.tbtn_button);
        btn_count_down = findViewById(R.id.btn_count_down);
        tv_name.setText(plug_name);

        //region 控制开关
        tbt_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Send("{\"mac\":\"" + device.getMac() + "\",\"plug_" + plug_id + "\":{\"on\":" + String.valueOf(((Switch) v).isChecked() ? 1 : 0) + "}" + "}");

            }
        });
        //endregion

        //region 设置名称
        tv_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText mEditText = new EditText(DC1PlugActivity.this);
                mEditText.setText(tv_name.getText());
                mEditText.setHint("建议长度在8个字以内");
                AlertDialog.Builder builder = new AlertDialog.Builder(DC1PlugActivity.this);
                builder.setTitle("设置插口" + String.valueOf(plug_id + 1) + "名称")
                        .setView(mEditText)
                        .setMessage("自定义插口名称,建议长度在8个字以内")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String str = mEditText.getText().toString();
                                if (str.length() > 16)
                                    str = str.substring(0, 16);
                                else if (str.length() < 1) {
                                    Toast.makeText(DC1PlugActivity.this, "名称不能为空!", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Send("{\"mac\":\"" + device.getMac() + "\",\"plug_" + plug_id + "\":{\"setting\":{\"name\":\"" + str + "\"}}" + "}");
                            }
                        });
                builder.show();
            }
        });
        //endregion

        btn_count_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupwindowCountDown();
            }
        });
        //endregion
        //region 初始化下滑刷新功能
        AppBarLayout appBarLayout = findViewById(R.id.app_bar);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {

                if (verticalOffset >= 0) {
                    mSwipeLayout.setEnabled(true);
                } else {
                    mSwipeLayout.setEnabled(false);
                }
            }
        });
        //region 更新当前状态
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
        //endregion
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

        final View popupView = getLayoutInflater().inflate(R.layout.popupwindow_dc1_set_time, null);
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

                Send("{\"mac\": \"" + device.getMac() + "\",\"plug_" + plug_id + "\" : {\"setting\":{\"task_" + task_id + "\":{\"hour\":" + hour + ",\"minute\":" + minute + ",\"repeat\":" + repeat + ",\"action\":" + action + ",\"on\":" + on + "}}}}");
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

        final View popupView = getLayoutInflater().inflate(R.layout.popupwindow_dc1_set_time_count_down, null);
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

                Send("{\"mac\": \"" + device.getMac() + "\",\"plug_" + plug_id + "\" : {\"setting\":{\"task_" + task_id + "\":{\"hour\":" + hour + ",\"minute\":" + minute + ",\"repeat\":" + repeat + ",\"action\":" + action + ",\"on\":" + on + "}}}}");
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
        boolean udp = getSharedPreferences("Setting_" + device.getMac(), 0).getBoolean("always_UDP", false);
        boolean oldProtocol = getSharedPreferences("Setting_" + device.getMac(), 0).getBoolean("old_protocol", false);

        String topic = null;
        if (!udp) {
            if (oldProtocol) topic = "device/zdc1/set";
            else topic = device.getSendMqttTopic();
        }
        super.Send(udp, topic, message);
    }


    public void Receive(String ip, int port, String topic, String message) {

        try {
            JSONObject jsonObject = new JSONObject(message);

            if (!jsonObject.has("mac") || !jsonObject.getString("mac").equals(device.getMac())) {
                return;
            }

            //region 解析单个plug
            if (!jsonObject.has("plug_" + plug_id)) return;
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

                adapter.setTask(i, jsonPlugTask.getInt("hour"),
                        jsonPlugTask.getInt("minute"),
                        jsonPlugTask.getInt("repeat"),
                        jsonPlugTask.getInt("action"),
                        jsonPlugTask.getInt("on"));
            }
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
