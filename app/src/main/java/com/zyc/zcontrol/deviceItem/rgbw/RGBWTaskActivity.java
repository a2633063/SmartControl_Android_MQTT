package com.zyc.zcontrol.deviceItem.rgbw;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.zyc.Function;
import com.zyc.zcontrol.MainApplication;
import com.zyc.zcontrol.R;
import com.zyc.zcontrol.ServiceActivity;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceRGBW;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class RGBWTaskActivity extends ServiceActivity {
    public final static String Tag = "RGBWPlugActivity";


    private SwipeRefreshLayout mSwipeLayout;
    ListView lv_task;
    ArrayList<TaskItem> data = new ArrayList<>();
    RGBWTaskListAdapter adapter;


    Button btn_count_down;


    DeviceRGBW device;


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
        setContentView(R.layout.rgbw_activity_plug);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//左侧添加一个默认的返回图标
        getSupportActionBar().setHomeButtonEnabled(true); //设置返回键可用

        Intent intent = this.getIntent();
        try {
            device = (DeviceRGBW) ((MainApplication) getApplication()).getDevice(intent.getStringExtra("mac"));
            if (device == null) {
                throw new Exception("获取数据出错:" + intent.getStringExtra("mac")); // 异常信息
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(RGBWTaskActivity.this, "数据错误!请联系开发者", Toast.LENGTH_SHORT).show();
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
        adapter = new RGBWTaskListAdapter(RGBWTaskActivity.this, data, new RGBWTaskListAdapter.Callback() {
            @Override
            public void click(View v, int position) {
                TaskItem task = adapter.getItem(position);
                int hour = task.hour;
                int minute = task.minute;
                int repeat = task.repeat;
                int on = ((Switch) v).isChecked() ? 1 : 0;

                Send("{\"mac\": \"" + device.getMac() + "\",\"task_" + position + "\":{\"hour\":" + hour + ",\"minute\":" + minute + ",\"repeat\":" + repeat + ",\"on\":" + on + "}}");
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

        final View popupView = getLayoutInflater().inflate(R.layout.rgbw_popupwindow_set_time, null);
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
        //action_picker.setValue(task.action);
        //endregion
        //endregion

        //region touch测试代码
        final ImageView img_get_color = popupView.findViewById(R.id.img_get_color);
        img_get_color.bringToFront();

        final Bitmap bitmap = ((BitmapDrawable) (img_get_color.getDrawable())).getBitmap();

        action_picker.setOnTouchListener(new View.OnTouchListener() {
            float image_x, image_y;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        image_x = -v.getX();//+event.getX() - img_get_color.getWidth() / 2.0f;
                        image_y = -v.getX()+event.getY() - img_get_color.getHeight() / 2.0f;
                        img_get_color.setVisibility(View.VISIBLE);
                        img_get_color.setX(v.getX() + image_x);
                        //img_get_color.setY(v.getY() + image_y);
                        break;
                    case MotionEvent.ACTION_UP:
                        img_get_color.setVisibility(View.GONE);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int x = (int) ((event.getX() - image_x + 0.5f) * bitmap.getWidth() / img_get_color.getWidth());
                        int y = (int) ((event.getY() - image_y + 0.5f) * bitmap.getHeight() / img_get_color.getHeight());
                        if (x < 0 || y < 0) break;
                        try {
                            int color = bitmap.getPixel(x, y);
                            if(color < 0xff000000 ||color == 0) break;
                            Log.d(Tag,"color:"+Integer.toHexString(color));
                            btn_ok.setTextColor(color);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
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

                if (tbtn_week[0].isChecked() && tbtn_week[1].isChecked() &&
                        tbtn_week[2].isChecked() && tbtn_week[3].isChecked() &&
                        tbtn_week[4].isChecked() && tbtn_week[5].isChecked() &&
                        tbtn_week[6].isChecked()) {
                    for (int i = 0; i < tbtn_week.length; i++)
                        tbtn_week[i].setChecked(false);
                } else {

                    for (int i = 0; i < tbtn_week.length; i++)
                        tbtn_week[i].setChecked(true);
                }
                TimePickerDialog timePickerDialog = new TimePickerDialog(RGBWTaskActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int i, int i1) {
                        Log.d(Tag,"time:"+i+":"+i1);
                    }
                },12,0,true);

                timePickerDialog.show();
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

        final View popupView = getLayoutInflater().inflate(R.layout.rgbw_popupwindow_set_time_count_down, null);
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
        String[] action = {"关闭", "1", "2", "3", "4"};
        action_picker.setDisplayedValues(action);
        action_picker.setMinValue(0);
        action_picker.setMaxValue(action.length - 1);
        action_picker.setValue(0);
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

                Calendar c = Calendar.getInstance();
                c.add(Calendar.HOUR_OF_DAY, hour);
                c.add(Calendar.MINUTE, minute);
                hour = c.get(Calendar.HOUR_OF_DAY);
                minute = c.get(Calendar.MINUTE);

                Send("{\"mac\": \"" + device.getMac() + "\",\"task_" + task_id + "\":{\"hour\":" + hour + ",\"minute\":" + minute + ",\"brightness\":" + action + ",\"on\":" + on + "}}");
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
        super.Send(b, device.getSendMqttTopic(), message);
    }

    public void Receive(String ip, int port, String topic, String message) {

        try {
            JSONObject jsonObject = new JSONObject(message);
            if (!jsonObject.has("mac") || !jsonObject.getString("mac").equals(device.getMac())) {
                return;
            }
            //region 解析当个plug
//            JSONObject jsonPlug = jsonObject.getJSONObject("plug_" + plug_id);
//            if (!jsonPlug.has("setting")) return;
//            JSONObject jsonPlugSetting = jsonPlug.getJSONObject("setting");

            //region 识别5组定时任务
            for (int i = 0; i < 5; i++) {
                if (!jsonObject.has("task_" + i)) continue;
                JSONObject jsonPlugTask = jsonObject.getJSONObject("task_" + i);
                if (!jsonPlugTask.has("hour") || !jsonPlugTask.has("minute") ||
                        !jsonPlugTask.has("repeat") ||
                        !jsonPlugTask.has("rgb") ||
                        !jsonPlugTask.has("on")) continue;
                JSONArray jsonRGBW = jsonPlugTask.optJSONArray("rgb");
                if (jsonRGBW == null || jsonRGBW.length() < 4) continue;


                int[] rgb = new int[4];
                for (int j = 0; j < 4; j++) {
                    rgb[j] = jsonRGBW.optInt(j, 0);
                }
                adapter.setTask(i, jsonPlugTask.getInt("on"), jsonPlugTask.getInt("hour"),
                        jsonPlugTask.getInt("minute"), jsonPlugTask.getInt("repeat"),
                        rgb, jsonPlugTask.getInt("gradient"));
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
