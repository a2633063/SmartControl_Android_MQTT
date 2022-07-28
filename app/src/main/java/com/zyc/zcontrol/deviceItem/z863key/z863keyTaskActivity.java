package com.zyc.zcontrol.deviceItem.z863key;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
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

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.zyc.Function;
import com.zyc.zcontrol.MainApplication;
import com.zyc.zcontrol.R;
import com.zyc.zcontrol.ServiceActivity;
import com.zyc.zcontrol.deviceItem.DeviceClass.Devicez863key;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class z863keyTaskActivity extends ServiceActivity {
    public final static String Tag = "z863keyTaskActivity";

    private SwipeRefreshLayout mSwipeLayout;
    ListView lv_task;
    ArrayList<TaskItem> data = new ArrayList<>();
    z863keyTaskListAdapter adapter;

    Devicez863key device;

    TaskItem task_last = null;
    //region Handler
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {// handler接收到消息后就会执行此方法
            switch (msg.what) {
                case 1: {
                    lv_task.setEnabled(false);
                    mSwipeLayout.setRefreshing(true);
                    if (msg.arg1 < 6) {
                        Send("{\"mac\": \"" + device.getMac() + "\",\"event_" + msg.arg1 + "\":null}");
                        // Send("{\"mac\": \"" + device.getMac() + "\",\"event_" + msg.arg1 + "\":null,\"event_" + (msg.arg1 + 1) + "\":null}");

                        if (msg.arg1 < 5) {
                            Message message = new Message();
                            message.what = 1;
                            message.arg1 = msg.arg1 + 1;
                            handler.sendMessageDelayed(message, message.arg1 % 4 == 0 ? 80 : 300);
                        } else {
                            if (mSwipeLayout.isRefreshing())
                                mSwipeLayout.setRefreshing(false);
                            lv_task.setEnabled(true);
                        }
                    }
                    break;
                }
            }
        }
    };
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.z863key_activity_task);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//左侧添加一个默认的返回图标
        getSupportActionBar().setHomeButtonEnabled(true); //设置返回键可用

        Intent intent = this.getIntent();

        try {
            device = (Devicez863key) ((MainApplication) getApplication()).getDevice(intent.getStringExtra("mac"));
            if (device == null) {
                throw new Exception("获取数据出错:" + intent.getStringExtra("mac")); // 异常信息
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(z863keyTaskActivity.this, "数据错误!请联系开发者", Toast.LENGTH_SHORT).show();
            finish();
        }

        for (int i = 0; i < 3; i++) {
            TaskItem t = new TaskItem();
            t.setBase("默认任务" + (i + 1), TaskItem.TASK_BUTTON_TYPE_TOGGLE, TaskItem.TASK_TYPE_RELAY);
            t.setColor(0, 0x00ff00);
            t.setRelay(0, i, -1);
            t.setRelay(1, i, -1);
            data.add(t);
        }
        for (int i = 3; i < 6; i++) {
            TaskItem t = new TaskItem();
            t.setBase("默认任务" + (i + 1), TaskItem.TASK_BUTTON_TYPE_TOGGLE, TaskItem.TASK_TYPE_RELAY);
            t.setColor(0x00ff00, 0);
            t.setRelay(0, i - 3, -1);
            t.setRelay(1, i - 3, -1);
            data.add(t);
        }

        //region 控件初始化
        mSwipeLayout = findViewById(R.id.swipeRefreshLayout);
        lv_task = findViewById(R.id.lv);
        //region 初始化下滑刷新功能
        mSwipeLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimaryDark, R.color.colorAccent, R.color.colorPrimary);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                handler.sendEmptyMessageDelayed(1, 0);
            }
        });
        //endregion
        //region listview及adapter
        lv_task.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                popupwindowTask(position);
            }
        });
        lv_task.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                View firstView = view.getChildAt(firstVisibleItem);
                if (firstVisibleItem == 0 && (firstView == null || firstView.getTop() == 0)) {
                    mSwipeLayout.setEnabled(true);
                } else
                    mSwipeLayout.setEnabled(false);
            }
        });
        adapter = new z863keyTaskListAdapter(z863keyTaskActivity.this, data, new z863keyTaskListAdapter.Callback() {
            @Override
            public void click(View v, int position) {
                TaskItem task = adapter.getItem(position);
                //Send("{\"mac\": \"" + device.getMac() + "\",\"task_" + position + "\":{\"on\":" + (task.on == 0 ? 1 : 0) + "}}");
            }
        });
        lv_task.setAdapter(adapter);
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

    //获取上次串口数据时记录需要显示在哪个EditText中
    EditText editLast = null;

    //region 弹窗
    private void popupwindowTask(final int task_id) {

        final View popupView = getLayoutInflater().inflate(R.layout.z863key_popupwindow_set_task, null);
        final PopupWindow window = new PopupWindow(popupView, MATCH_PARENT, MATCH_PARENT, true);//wrap_content,wrap_content

        final TaskItem task = adapter.getItem(task_id);

        //region 控件初始化
        //region 获取各控件
        final CardView cardview_color_0 = popupView.findViewById(R.id.cardview_color_0);
        final CardView cardview_color_1 = popupView.findViewById(R.id.cardview_color_1);

        final TextView tv_task_import = popupView.findViewById(R.id.tv_task_import);
        final TextView tv_id = popupView.findViewById(R.id.tv_id);
        final EditText edt_name = popupView.findViewById(R.id.edt_name);
        final Spinner spinner_button_type = popupView.findViewById(R.id.spinner_button_type);
        final Spinner spinner_type = popupView.findViewById(R.id.spinner_type);
        final ImageView img_help = popupView.findViewById(R.id.img_help);
        final Button btn_ok = popupView.findViewById(R.id.btn_ok);
        final Button btn_cancel = popupView.findViewById(R.id.btn_cancel);
        final TabLayout tablayout = popupView.findViewById(R.id.tablayout);

        final View[][] layout_event =
                {
                        {popupView.findViewById(R.id.layout_relay_0),
                                popupView.findViewById(R.id.layout_relay_1),},
                        {popupView.findViewById(R.id.layout_mqtt_0),
                                popupView.findViewById(R.id.layout_mqtt_1),}
                };
        final EditText[] edt_mqtt_topic = {layout_event[1][0].findViewById(R.id.edt_mqtt_topic), layout_event[1][1].findViewById(R.id.edt_mqtt_topic)};
        final EditText[] edt_mqtt_payload = {layout_event[1][0].findViewById(R.id.edt_mqtt_payload), layout_event[1][1].findViewById(R.id.edt_mqtt_payload)};
        final Spinner[] spinner_mqtt_qos = {layout_event[1][0].findViewById(R.id.spinner_mqtt_qos), layout_event[1][1].findViewById(R.id.spinner_mqtt_qos)};
        final CheckBox[] chk_mqtt_retained = {layout_event[1][0].findViewById(R.id.chk_mqtt_retained), layout_event[1][1].findViewById(R.id.chk_mqtt_retained)};

        final TextView[] tv_last_mqtt = {layout_event[1][0].findViewById(R.id.tv_last_mqtt), layout_event[1][1].findViewById(R.id.tv_last_mqtt)};

        final View function_select_color = popupView.findViewById(R.id.function_select_color);

        BitmapDrawable bitmapDrawable = (BitmapDrawable) getApplication().getResources().getDrawable(R.drawable.color_select);
        Bitmap bitmap = bitmapDrawable.getBitmap();
        Function.ShowColorSelectInit(popupView, cardview_color_0, bitmap);

        View.OnTouchListener colerSelectOnTouchListener = new View.OnTouchListener() {
            int color_temp;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                //Log.d(Tag, "motionEvent:" + motionEvent.toString());
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    color_temp = 0xffffff & ((CardView) view).getCardBackgroundColor().getDefaultColor();
                    Log.d(Tag, "color_temp:0x" + Integer.toString(color_temp, 16));
                }
                int color = Function.ShowColorSelect(view, popupView, function_select_color, motionEvent);
                Log.d(Tag, "color:0x" + Integer.toString(color, 16));
                if (color < 0) color = color_temp;
                ((CardView) view).setCardBackgroundColor(color | 0xff000000);
                return true;
            }
        };
        cardview_color_0.setOnTouchListener(colerSelectOnTouchListener);
        cardview_color_1.setOnTouchListener(colerSelectOnTouchListener);
        //endregion

        //region 根据任务类型切换显示页面
        final AdapterView.OnItemSelectedListener typeItemSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                layout_event[0][0].setVisibility(View.GONE);
                layout_event[0][1].setVisibility(View.GONE);
                layout_event[1][0].setVisibility(View.GONE);
                layout_event[1][1].setVisibility(View.GONE);

                int type = spinner_type.getSelectedItemPosition();
                int state = 0;
                if (spinner_button_type.getSelectedItemPosition() == 0) {
                    state = 0;
                    tablayout.setVisibility(View.GONE);
                    tablayout.selectTab(tablayout.getTabAt(0));
                } else {
                    state = tablayout.getSelectedTabPosition();
                    tablayout.setVisibility(View.VISIBLE);
                }
                Log.d(Tag, "type:" + type + ",state:" + state + " view:" + tablayout.getVisibility());
                layout_event[type][state].setVisibility(View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                layout_event[0][0].setVisibility(View.GONE);
                layout_event[0][1].setVisibility(View.GONE);
                layout_event[1][0].setVisibility(View.GONE);
                layout_event[1][1].setVisibility(View.GONE);
            }
        };
        spinner_type.setOnItemSelectedListener(typeItemSelectedListener);
        spinner_button_type.setOnItemSelectedListener(typeItemSelectedListener);
        //endregion

        //region tablayout
        tablayout.addTab(tablayout.newTab().setText("状态0"));
        tablayout.addTab(tablayout.newTab().setText("状态1"));
        tablayout.setSelected(false);
        tablayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                typeItemSelectedListener.onItemSelected(null, null, 0, 0);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        //endregion

        //region 导入上次设置的任务按钮
        if (task_last == null) tv_task_import.setVisibility(View.GONE);
        tv_task_import.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupwindowTask_show_task(popupView, task_last);
            }
        });
        //endregion
        //region 获取上次mqtt发送的数据
        View.OnClickListener lastMqttClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int index = (int) view.getTag();

                String[] mqtt_last = mConnectService.getSendLast();
                if (mqtt_last == null || mqtt_last.length < 2 || (mqtt_last[0] == null && mqtt_last[1] == null)) {
                    Toast.makeText(z863keyTaskActivity.this, "无法获取上次的命令,请操作控制后再尝试获取", Toast.LENGTH_SHORT).show();
                } else {
                    if (mqtt_last[0] != null) edt_mqtt_topic[index].setText(mqtt_last[0]);
                    if (mqtt_last[1] != null) edt_mqtt_payload[index].setText(mqtt_last[1]);
                }
            }
        };
        //endregion

        tv_last_mqtt[0].setTag(0);
        tv_last_mqtt[1].setTag(1);
        tv_last_mqtt[0].setOnClickListener(lastMqttClickListener);
        tv_last_mqtt[1].setOnClickListener(lastMqttClickListener);

        tv_id.setText("任务" + (task_id + 1));
        edt_name.setText(task.name);
        popupwindowTask_show_task(popupView, task);

        //region 确认按钮初始化
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Send("{\"mac\": \"" + device.getMac() + "\",\"plug_" + plug_id + "\" : {\"setting\":{\"task_" + task_id + "\":{\"hour\":" + hour + ",\"minute\":" + minute + ",\"repeat\":" + repeat + ",\"action\":" + action + ",\"on\":" + on + "}}}}");

                TaskItem t = popupwindowTask_analyse_to_task(popupView);
                if (t != null) {
                    if (task_last == null) task_last = new TaskItem();
                    task_last.Copy(t);
                    Log.d(Tag, "task json:" + t.getJson().toString());
                    Send("{\"mac\": \"" + device.getMac() + "\",\"event_" + task_id + "\" : " + t.getJson().toString() + "}");

                    window.dismiss();
                }

                //window.dismiss();
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

    //根据TaskItem,配置弹窗中控件显示的内容
    private void popupwindowTask_show_task(View popupView, TaskItem task) {
        if (popupView == null || task == null) return;
        //region 获取各控件
        final CardView cardview_color_0 = popupView.findViewById(R.id.cardview_color_0);
        final CardView cardview_color_1 = popupView.findViewById(R.id.cardview_color_1);

        final Spinner spinner_button_type = popupView.findViewById(R.id.spinner_button_type);
        final Spinner spinner_type = popupView.findViewById(R.id.spinner_type);

        final View[][] layout_event =
                {
                        {popupView.findViewById(R.id.layout_relay_0), popupView.findViewById(R.id.layout_relay_1),},
                        {popupView.findViewById(R.id.layout_mqtt_0), popupView.findViewById(R.id.layout_mqtt_1),}
                };
        //region 自动化mqtt部分
        final EditText[] edt_mqtt_topic = {layout_event[1][0].findViewById(R.id.edt_mqtt_topic), layout_event[1][1].findViewById(R.id.edt_mqtt_topic)};
        final EditText[] edt_mqtt_payload = {layout_event[1][0].findViewById(R.id.edt_mqtt_payload), layout_event[1][1].findViewById(R.id.edt_mqtt_payload)};
        final Spinner[] spinner_mqtt_qos = {layout_event[1][0].findViewById(R.id.spinner_mqtt_qos), layout_event[1][1].findViewById(R.id.spinner_mqtt_qos)};
        final CheckBox[] chk_mqtt_retained = {layout_event[1][0].findViewById(R.id.chk_mqtt_retained), layout_event[1][1].findViewById(R.id.chk_mqtt_retained)};
        //endregion

        //region 自动化relay部分
        final Spinner[] spinner_relay = {layout_event[0][0].findViewById(R.id.spinner_relay), layout_event[0][1].findViewById(R.id.spinner_relay)};
        final Spinner[] spinner_relay_onoff = {layout_event[0][0].findViewById(R.id.spinner_relay_onoff), layout_event[0][1].findViewById(R.id.spinner_relay_onoff)};
        //endregion

        //endregion
        cardview_color_0.setCardBackgroundColor(task.color[0] | 0xff000000);
        cardview_color_1.setCardBackgroundColor(task.color[1] | 0xff000000);
        spinner_type.setSelection(-1);
        spinner_button_type.setSelection(-1);
        if (task.type < spinner_type.getCount())
            spinner_type.setSelection(task.type);
        if (task.button_type < spinner_button_type.getCount())
            spinner_button_type.setSelection(task.button_type);
        //region 更新显示布局

        //region 自动化部分更新显示
        switch (task.type) {
            case TaskItem.TASK_TYPE_MQTT:
                for (int i = 0; i < 2; i++) {
                    edt_mqtt_topic[i].setText(task.data[i].mqtt.topic);
                    edt_mqtt_payload[i].setText(task.data[i].mqtt.payload);
                    spinner_mqtt_qos[i].setSelection(task.data[i].mqtt.qos);
                    chk_mqtt_retained[i].setChecked(task.data[i].mqtt.retained != 0);
                }
                break;
            case TaskItem.TASK_TYPE_RELAY:
                for (int i = 0; i < 2; i++) {
                    spinner_relay[i].setSelection(task.data[i].relay.index);
                    spinner_relay_onoff[i].setSelection(task.data[i].relay.on + 1);
                }
                break;
        }
        //endregion
        //endregion

    }

    //根据弹窗中控件的信息,配置TaskItem
    private TaskItem popupwindowTask_analyse_to_task(View popupView) {
        if (popupView == null) return null;

        TaskItem task = new TaskItem();

        //region 获取各控件
        final EditText edt_name = popupView.findViewById(R.id.edt_name);

        final CardView cardview_color_0 = popupView.findViewById(R.id.cardview_color_0);
        final CardView cardview_color_1 = popupView.findViewById(R.id.cardview_color_1);

        final Spinner spinner_button_type = popupView.findViewById(R.id.spinner_button_type);
        final Spinner spinner_type = popupView.findViewById(R.id.spinner_type);

        final View[][] layout_event =
                {
                        {popupView.findViewById(R.id.layout_relay_0), popupView.findViewById(R.id.layout_relay_1),},
                        {popupView.findViewById(R.id.layout_mqtt_0), popupView.findViewById(R.id.layout_mqtt_1),}
                };
        //region 自动化mqtt部分
        final EditText[] edt_mqtt_topic = {layout_event[1][0].findViewById(R.id.edt_mqtt_topic), layout_event[1][1].findViewById(R.id.edt_mqtt_topic)};
        final EditText[] edt_mqtt_payload = {layout_event[1][0].findViewById(R.id.edt_mqtt_payload), layout_event[1][1].findViewById(R.id.edt_mqtt_payload)};
        final Spinner[] spinner_mqtt_qos = {layout_event[1][0].findViewById(R.id.spinner_mqtt_qos), layout_event[1][1].findViewById(R.id.spinner_mqtt_qos)};
        final CheckBox[] chk_mqtt_retained = {layout_event[1][0].findViewById(R.id.chk_mqtt_retained), layout_event[1][1].findViewById(R.id.chk_mqtt_retained)};

        //endregion

        //region 自动化relay部分
        final Spinner[] spinner_relay = {layout_event[0][0].findViewById(R.id.spinner_relay), layout_event[0][1].findViewById(R.id.spinner_relay)};
        final Spinner[] spinner_relay_onoff = {layout_event[0][0].findViewById(R.id.spinner_relay_onoff), layout_event[0][1].findViewById(R.id.spinner_relay_onoff)};
        //endregion

        //endregion
        task.name = edt_name.getText().toString();

        task.color[0] = 0xffffff & cardview_color_0.getCardBackgroundColor().getDefaultColor();
        task.color[1] = 0xffffff & cardview_color_1.getCardBackgroundColor().getDefaultColor();
        task.type = spinner_type.getSelectedItemPosition();
        task.button_type = spinner_button_type.getSelectedItemPosition();
        //region 更新显示布局

        //region 自动化部分更新显示
        switch (task.type) {
            case TaskItem.TASK_TYPE_MQTT: {
                for (int i = 0; i < 2; i++) {
                    task.data[i].mqtt.topic = edt_mqtt_topic[i].getText().toString();
                    task.data[i].mqtt.payload = edt_mqtt_payload[i].getText().toString();
                    task.data[i].mqtt.qos = spinner_mqtt_qos[i].getSelectedItemPosition();
                    task.data[i].mqtt.retained = chk_mqtt_retained[i].isChecked() ? 1 : 0;
                }
                break;
            }
            case TaskItem.TASK_TYPE_RELAY: {
                for (int i = 0; i < 2; i++) {
                    task.data[i].relay.index = spinner_relay[i].getSelectedItemPosition();
                    task.data[i].relay.on = spinner_relay_onoff[i].getSelectedItemPosition() - 1;
                }
                break;
            }
        }
        //endregion
        //endregion
        return task;
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
            boolean task_is_flash = false;
            for (int id = 0; id < 6; id++) {//共20组自动化任务
                if (!jsonObject.has("event_" + id)) continue;
                TaskItem task = data.get(id);
                if (task == null) continue;

                JSONObject jsonTask = jsonObject.getJSONObject("event_" + id);

                //region 任务基本信息
                if (jsonTask.has("name") && !jsonTask.optString("name").isEmpty()) {
                    task.name = jsonTask.optString("name");
                }
                if (jsonTask.has("color")) {
                    JSONArray jsonColor = jsonTask.optJSONArray("color");
                    if (jsonColor != null && jsonColor.length() == 2) {
                        for (int i = 0; i < jsonColor.length(); i++) {
                            task.color[i] = jsonColor.optInt(i, 0);
                        }
                    }
                }
                if (jsonTask.has("type")) {
                    String s = jsonTask.optString("type", null);
                    if (s == null) task.type = jsonTask.optInt("type");
                    else if (s.equals("relay")) task.type = TaskItem.TASK_TYPE_RELAY;
                    else if (s.equals("mqtt")) task.type = TaskItem.TASK_TYPE_MQTT;
                }

                if (jsonTask.has("button_type")) {
                    String s = jsonTask.optString("button_type", null);
                    if (s == null) task.button_type = jsonTask.optInt("button_type");
                    else if (s.equals("button")) task.button_type = TaskItem.TASK_BUTTON_TYPE_BUTTON;
                    else if (s.equals("toggle")) task.button_type  = TaskItem.TASK_BUTTON_TYPE_TOGGLE;
                }
                //endregion

                //region 自动化方式
                switch (task.type) {
                    case TaskItem.TASK_TYPE_MQTT: {
                        JSONObject jsonMqtt = jsonTask.optJSONObject("mqtt");
                        if (jsonMqtt != null) {//非数组
                            task.setMqtt(0, jsonMqtt.optString("topic"), jsonMqtt.optString("payload"), jsonMqtt.optInt("qos"), jsonMqtt.optInt("retained"));
                            break;
                        }
                        //是数组
                        JSONArray jsonArrayMqtt = jsonTask.optJSONArray("mqtt");
                        if (jsonArrayMqtt == null) break;
                        for (int i = 0; i < 2 && i < jsonArrayMqtt.length(); i++) {
                            jsonMqtt = jsonArrayMqtt.optJSONObject(i);
                            if (jsonMqtt == null) break;
                            task.setMqtt(i, jsonMqtt.optString("topic"), jsonMqtt.optString("payload"), jsonMqtt.optInt("qos"), jsonMqtt.optInt("retained"));
                        }
                        break;
                    }
                    case TaskItem.TASK_TYPE_RELAY: {
                        JSONObject jsonRelay = jsonTask.optJSONObject("relay");
                        if (jsonRelay != null) {//非数组
                            task.setRelay(0, jsonRelay.optInt("index"), jsonRelay.optInt("on"));
                            break;
                        }
                        //是数组
                        JSONArray jsonArrayMqtt = jsonTask.optJSONArray("relay");
                        if (jsonArrayMqtt == null) break;
                        for (int i = 0; i < 2 && i < jsonArrayMqtt.length(); i++) {
                            jsonRelay = jsonArrayMqtt.optJSONObject(i);
                            if (jsonRelay == null) break;
                            task.setRelay(i, jsonRelay.optInt("index"), jsonRelay.optInt("on"));
                        }
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
