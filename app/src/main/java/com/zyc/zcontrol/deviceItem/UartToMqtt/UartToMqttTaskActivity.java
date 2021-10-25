package com.zyc.zcontrol.deviceItem.UartToMqtt;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.zyc.zcontrol.MainApplication;
import com.zyc.zcontrol.R;
import com.zyc.zcontrol.ServiceActivity;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceTC1;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceUartToMqtt;
import com.zyc.zcontrol.deviceItem.tc1.TC1PlugActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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
                case 1:
                    Send("{\"mac\": \"" + device.getMac() + "\"}");
                    break;
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
            t.setBase("" + (i + 1), 0, TaskItem.TASK_TYPE_MQTT);
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

                //Send("{\"mac\": \"" + device.getMac() + "\",\"plug_" + plug_id + "\" : {\"setting\":{\"task_" + position + "\":{\"hour\":" + hour + ",\"minute\":" + minute + ",\"repeat\":" + repeat + ",\"action\":" + action + ",\"on\":" + on + "}}}}");
            }
        });
        lv_task.setAdapter(adapter);

        //endregion

        //endregion


        //region 初始化下滑刷新功能
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

        final View popupView = getLayoutInflater().inflate(R.layout.uarttomqtt_popupwindow_set_task, null);
        final PopupWindow window = new PopupWindow(popupView, MATCH_PARENT, MATCH_PARENT, true);//wrap_content,wrap_content

        final TaskItem task = adapter.getItem(task_id);

        //region 控件初始化


        //region 确认按钮初始化
        /*btn_ok.setOnClickListener(new View.OnClickListener() {
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
        });*/
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

            //region 解析自动化任务
            boolean task_is_flash=false;
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
                        task.setWol(wol_mac,wol_ip,jsonWol.optInt("port",9),wol_secure);
                        break;
                    }
                    case TaskItem.TASK_TYPE_UART:
                    case TaskItem.TASK_TYPE_TIME_UART: {
                        JSONObject jsonUart=jsonTask.getJSONObject("uart");
                        task.setUart(jsonUart.optString("uart"),jsonUart.optInt("reserved_rec"),jsonUart.optInt("reserved_send"));
                        break;
                    }

                }
                //endregion
                task_is_flash=true;
            }
            if(task_is_flash)
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
