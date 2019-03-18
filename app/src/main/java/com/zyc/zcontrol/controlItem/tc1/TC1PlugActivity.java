package com.zyc.zcontrol.controlItem.tc1;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.zyc.zcontrol.ConnectService;
import com.zyc.zcontrol.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class TC1PlugActivity extends AppCompatActivity {
    public final static String Tag = "TC1PlugActivity";

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    //region 使用本地广播与service通信
    LocalBroadcastManager localBroadcastManager;
    private MsgReceiver msgReceiver;
    //endregion
    ConnectService mConnectService;

    ListView lv_task;
    ArrayList<TaskItem> data = new ArrayList<>();
    TC1TaskListAdapter adapter;

    TextView tv_name;
    ToggleButton tbt_button;
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
                case 100:
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tc1_plug);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//左侧添加一个默认的返回图标
        getSupportActionBar().setHomeButtonEnabled(true); //设置返回键可用

        Intent intent = this.getIntent();
        if (!intent.hasExtra("name") || !intent.hasExtra("mac")
                || !intent.hasExtra("plug_id") || !intent.hasExtra("plug_name"))//判断是否有值传入,并判断是否有特定key
        {
            Toast.makeText(TC1PlugActivity.this, "数据错误!请联系开发者", Toast.LENGTH_SHORT).show();
            finish();
        }

        device_name = intent.getStringExtra("name");
        plug_name = intent.getStringExtra("plug_name");
        device_mac = intent.getStringExtra("mac");
        plug_id = intent.getIntExtra("plug_id", -1);

        if (device_mac.length() < 1 || plug_name.length() < 1 || device_name.length() < 1 || plug_id == -1) {
            Toast.makeText(TC1PlugActivity.this, "数据错误!请联系开发者", Toast.LENGTH_SHORT).show();
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
        adapter = new TC1TaskListAdapter(TC1PlugActivity.this, data);
        lv_task.setAdapter(adapter);
        //endregion


        //region 开关按键/名称
        tv_name = findViewById(R.id.tv_name);
        tbt_button = findViewById(R.id.tbtn_button);
        btn_count_down=findViewById(R.id.btn_count_down);
        tv_name.setText(plug_name);

        //region 控制开关
        tbt_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Send("{\"name\":\"" + device_name + "\",\"mac\":\"" + device_mac + "\",\"plug_" + plug_id + "\":{\"on\":" + String.valueOf(((ToggleButton) v).isChecked() ? 1 : 0) + "}" + "}");

            }
        });
        //endregion

        //region 设置名称
        tv_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText mEditText = new EditText(TC1PlugActivity.this);
                mEditText.setText(tv_name.getText());
                mEditText.setHint("建议长度在8个字以内");
                AlertDialog.Builder builder = new AlertDialog.Builder(TC1PlugActivity.this);
                builder.setTitle("设置插口" + String.valueOf(plug_id + 1) + "名称")
                        .setView(mEditText)
                        .setMessage("自定义插口名称,建议长度在8个字以内")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String str = mEditText.getText().toString();
                                if (str.length() < 1 || str.length() > 16)
                                    str = str.substring(0, 16);
                                Send("{\"name\":\"" + device_name + "\",\"mac\":\"" + device_mac + "\",\"plug_" + plug_id + "\":{\"setting\":{\"name\":\"" + str + "\"}}" + "}");
                            }
                        });
                builder.show();
            }
        });
        //endregion

        btn_count_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
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
        Intent serverIntent = new Intent(TC1PlugActivity.this, ConnectService.class);
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

    void Send(String message) {
        boolean b = getSharedPreferences("Setting_" + device_mac, 0).getBoolean("always_UDP", false);
        mConnectService.Send(b ? null : "domoticz/out", message);
    }

    //数据接收处理函数
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

    //region MQTT服务有关

    private final ServiceConnection mMQTTServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mConnectService = ((ConnectService.LocalBinder) service).getService();
            Send("{\"mac\": \"" + device_mac + "\",\"plug_" + plug_id + "\" : {\"on\" : null,\"setting\":{\"name\":null,\"task_0\":{},\"task_1\":{},\"task_2\":{},\"task_3\":{},\"task_4\":{}}}}");
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
