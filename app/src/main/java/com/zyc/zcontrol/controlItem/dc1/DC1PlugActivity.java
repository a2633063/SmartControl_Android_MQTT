package com.zyc.zcontrol.controlItem.dc1;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import com.google.android.material.appbar.AppBarLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
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

import com.zyc.Function;
import com.zyc.zcontrol.ConnectService;
import com.zyc.zcontrol.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class DC1PlugActivity extends AppCompatActivity {
    public final static String Tag = "DC1PlugActivity";

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    //region 使用本地广播与service通信
    LocalBroadcastManager localBroadcastManager;
    private MsgReceiver msgReceiver;
    //endregion
    ConnectService mConnectService;

    private SwipeRefreshLayout mSwipeLayout;
    ListView lv_task;
    ArrayList<TaskItem> data = new ArrayList<>();
    DC1TaskListAdapter adapter;

    TextView tv_name;
    Switch tbt_button;
    Button btn_count_down;

    String device_mac = null;
    String plug_name = null;
    String device_name = null;
    int plug_id = -1;

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {// handler接收到消息后就会执行此方法
            switch (msg.what) {
                case 1:
                    Send("{\"mac\": \"" + device_mac + "\",\"plug_" + plug_id + "\" : {\"on\" : null,\"setting\":{\"name\":null,\"task_0\":{},\"task_1\":{},\"task_2\":{},\"task_3\":{},\"task_4\":{}}}}");
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
        if (!intent.hasExtra("name") || !intent.hasExtra("mac")
                || !intent.hasExtra("plug_id") || !intent.hasExtra("plug_name"))//判断是否有值传入,并判断是否有特定key
        {
            Toast.makeText(DC1PlugActivity.this, "数据错误!请联系开发者", Toast.LENGTH_SHORT).show();
            finish();
        }

        device_name = intent.getStringExtra("name");
        plug_name = intent.getStringExtra("plug_name");
        device_mac = intent.getStringExtra("mac");
        plug_id = intent.getIntExtra("plug_id", -1);

        if (device_mac.length() < 1 || plug_name.length() < 1 || device_name.length() < 1 || plug_id == -1) {
            Toast.makeText(DC1PlugActivity.this, "数据错误!请联系开发者", Toast.LENGTH_SHORT).show();
            finish();
        }
        TaskItem task = new TaskItem(this);
        data.add(task);
        TaskItem task1 = new TaskItem(this);
        data.add(task1);
        TaskItem task2 = new TaskItem(this);
        data.add(task2);
        TaskItem task3 = new TaskItem(this);
        data.add(task3);
        TaskItem task4 = new TaskItem(this);
        data.add(task4);
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

                Send("{\"mac\": \"" + device_mac + "\",\"plug_" + plug_id + "\" : {\"setting\":{\"task_" + position + "\":{\"hour\":" + hour + ",\"minute\":" + minute + ",\"repeat\":" + repeat + ",\"action\":" + action + ",\"on\":" + on + "}}}}");
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
                Send("{\"mac\":\"" + device_mac + "\",\"plug_" + plug_id + "\":{\"on\":" + String.valueOf(((Switch) v).isChecked() ? 1 : 0) + "}" + "}");

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
                                Send("{\"mac\":\"" + device_mac + "\",\"plug_" + plug_id + "\":{\"setting\":{\"name\":\"" + str + "\"}}" + "}");
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

        //region MQTT服务有关
        //region 动态注册接收mqtt服务的广播接收器,
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(ConnectService.ACTION_UDP_DATA_AVAILABLE);//UDP监听
        localBroadcastManager.registerReceiver(msgReceiver, intentFilter);
        //endregion

        //region 启动MQTT服务
        Intent serverIntent = new Intent(DC1PlugActivity.this, ConnectService.class);
        bindService(serverIntent, mMQTTServiceConnection, BIND_AUTO_CREATE);

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


    @Override
    protected void onDestroy() {
        //注销广播
        localBroadcastManager.unregisterReceiver(msgReceiver);
        //停止服务
        unbindService(mMQTTServiceConnection);
        super.onDestroy();
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

                Send("{\"mac\": \"" + device_mac + "\",\"plug_" + plug_id + "\" : {\"setting\":{\"task_" + task_id + "\":{\"hour\":" + hour + ",\"minute\":" + minute + ",\"repeat\":" + repeat + ",\"action\":" + action + ",\"on\":" + on + "}}}}");
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

                Send("{\"mac\": \"" + device_mac + "\",\"plug_" + plug_id + "\" : {\"setting\":{\"task_" + task_id + "\":{\"hour\":" + hour + ",\"minute\":" + minute + ",\"repeat\":" + repeat + ",\"action\":" + action + ",\"on\":" + on + "}}}}");
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
        if (mConnectService == null) return;
        boolean udp = getSharedPreferences("Setting_" + device_mac, 0).getBoolean("always_UDP", false);
        boolean oldProtocol = getSharedPreferences("Setting_" + device_mac, 0).getBoolean("old_protocol", false);

        String topic = null;
        if (!udp) {
            if (oldProtocol) topic = "device/zdc1/set";
            else topic = "device/zdc1/" + device_mac + "/set";
        }
        mConnectService.Send(topic, message);    }

    void Receive(String ip, int port, String message) {
        //TODO 数据接收处理
        Receive(null, message);
    }

    void Receive(String topic, String message) {
        //TODO 数据接收处理
        Log.d(Tag, "RECV DATA,topic:" + topic + ",content:" + message);

        try {
            JSONObject jsonObject = new JSONObject(message);
            String name = null;
            String mac = null;

            if (jsonObject.has("name")) name = jsonObject.getString("name");
            if (jsonObject.has("mac")) mac = jsonObject.getString("mac");
            if (mac == null || !mac.equals(device_mac)) return;

            //region 解析当个plug
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
    //region MQTT服务有关

    private final ServiceConnection mMQTTServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mConnectService = ((ConnectService.LocalBinder) service).getService();
            handler.sendEmptyMessageDelayed(1, 0);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mConnectService = null;
        }
    };

    //广播接收,用于处理接收到的数据
    public class MsgReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (ConnectService.ACTION_UDP_DATA_AVAILABLE.equals(action)) {
                String ip = intent.getStringExtra(ConnectService.EXTRA_UDP_DATA_IP);
                String message = intent.getStringExtra(ConnectService.EXTRA_UDP_DATA_MESSAGE);
                int port = intent.getIntExtra(ConnectService.EXTRA_UDP_DATA_PORT, -1);
                Receive(ip, port, message);
            } else if (ConnectService.ACTION_DATA_AVAILABLE.equals(action)) {  //接收到数据
                String topic = intent.getStringExtra(ConnectService.EXTRA_DATA_TOPIC);
                String message = intent.getStringExtra(ConnectService.EXTRA_DATA_MESSAGE);
                Receive(topic, message);

            }
        }
    }
    //endregion

}
