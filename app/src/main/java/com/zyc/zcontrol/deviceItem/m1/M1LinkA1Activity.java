package com.zyc.zcontrol.deviceItem.m1;

import androidx.appcompat.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.zyc.zcontrol.MainApplication;
import com.zyc.zcontrol.ServiceActivity;
import com.zyc.zcontrol.R;
import com.zyc.zcontrol.deviceItem.DeviceClass.Device;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceA1;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceM1;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.zyc.zcontrol.deviceItem.DeviceClass.Device.TYPE_A1;

public class M1LinkA1Activity extends ServiceActivity {
    public final static String Tag = "M1LinkA1";

    //region 控件
    private SwipeRefreshLayout mSwipeLayout;
    DeviceM1 device;

    Switch switch_link;
    TextView tv_a1;
    TextView tv_start_time;
    TextView tv_end_time;
    TextView[] tv_formaldehyde = new TextView[3];
    TextView[] tv_pm = new TextView[3];
    TextView[] tv_speed = new TextView[3];
    //endregion

    ArrayList<Device> deviceData;

    String a1_mac = null;
    boolean switch_on = false;
    int start_time = 0;
    int end_time = 1440;
    int[] pm25 = {75, 100, 150};
    int[] formaldehyde = {10, 20, 30};
    int[] speed = {20, 50, 100};


    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {// handler接收到消息后就会执行此方法
            switch (msg.what) {
                case 1:
                    Send("{\"mac\": \"" + device.getMac() + "\",\"za1\":{}}");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.m1_activity_link_a1);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//左侧添加一个默认的返回图标
        getSupportActionBar().setHomeButtonEnabled(true); //设置返回键可用

        deviceData = ((MainApplication) getApplication()).getDeviceList();

        //region 获取设备信息
        Intent intent = this.getIntent();
        try {
            device = (DeviceM1) ((MainApplication) getApplication()).getDevice(intent.getStringExtra("mac"));
            if (device == null) {
                throw new Exception("获取数据出错:" + intent.getStringExtra("mac")); // 异常信息
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(M1LinkA1Activity.this, "数据错误!请联系开发者", Toast.LENGTH_SHORT).show();
            finish();
        }
        //endregion
        //region 控件初始化
        switch_link = findViewById(R.id.switch_link);
        tv_a1 = findViewById(R.id.tv_a1);
        tv_start_time = findViewById(R.id.tv_start_time);
        tv_end_time = findViewById(R.id.tv_end_time);
        tv_formaldehyde[0] = findViewById(R.id.tv_f_1);
        tv_formaldehyde[1] = findViewById(R.id.tv_f_2);
        tv_formaldehyde[2] = findViewById(R.id.tv_f_3);
        tv_pm[0] = findViewById(R.id.tv_pm_1);
        tv_pm[1] = findViewById(R.id.tv_pm_2);
        tv_pm[2] = findViewById(R.id.tv_pm_3);
        tv_speed[0] = findViewById(R.id.tv_speed_1);
        tv_speed[1] = findViewById(R.id.tv_speed_2);
        tv_speed[2] = findViewById(R.id.tv_speed_3);


        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupwindowTask();
            }
        });
        switch_link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (a1_mac == null) {
                    AlertDialog alertDialog = new AlertDialog.Builder(M1LinkA1Activity.this)
                            .setTitle("未获取到设备当前设置内容")
                            .setMessage("请下拉尝试重新获取.或检查您的网络状态")
                            .setNegativeButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    handler.sendEmptyMessageDelayed(1, 0);
                                    Toast.makeText(M1LinkA1Activity.this, "尝试请求数据...", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .create();
                    alertDialog.show();
                    switch_link.setChecked(!switch_link.isChecked());
                    return;
                }

                Send("{\"mac\": \"" + device.getMac() + "\",\"za1\":{\"on\":" + (switch_link.isChecked() ? "1" : "0") + "}}");
            }
        });
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
    private void popupwindowTask() {

        //region zA1设备读取初始化
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item);
        String[] itemNames;
        String select_a1 = a1_mac;
        if(select_a1==null)select_a1="虚拟设备|000000000000";
        dataAdapter.add("虚拟设备|000000000000");
        for (int i = 0; i < deviceData.size(); i++) // Maximum size of i upto --> Your Array Size
        {
            if (deviceData.get(i).getType() == TYPE_A1) {
                dataAdapter.add(deviceData.get(i).getName() + "|" + deviceData.get(i).getMac());
                if (deviceData.get(i).getMac().equals(select_a1.replaceAll(".*\\|", ""))) {
                    select_a1 = deviceData.get(i).getName() + "|" + deviceData.get(i).getMac();
                }
            }
        }
        if (dataAdapter.getCount()<2) {
            new AlertDialog.Builder(M1LinkA1Activity.this).setTitle("您当前无zA1设备")
                    .setMessage("请先将zA1设备添加至app中")
                    .setPositiveButton("确定", null).show();
            return;
        }
        //endregion


        final View popupView = getLayoutInflater().inflate(R.layout.m1_popupwindow_a1_link_set, null);
        final PopupWindow window = new PopupWindow(popupView, MATCH_PARENT, MATCH_PARENT, true);//wrap_content,wrap_content

        //region 控件初始化
        final Button btn_ok = popupView.findViewById(R.id.btn_ok);
        final Spinner spinner_a1 = popupView.findViewById(R.id.spinner_a1_device);
        final TextView tv_start_time = popupView.findViewById(R.id.tv_start_time);
        tv_start_time.setText(String.format("%02d", start_time / 60) + ":" + String.format("%02d", start_time % 60) +
                "-" + String.format("%02d", end_time / 60) + ":" + String.format("%02d", end_time % 60));

        //region 甲醛/pm2.5/速度输入框控件
        final EditText[] tv_popup_formaldehyde = {
                popupView.findViewById(R.id.tv_f_1),
                popupView.findViewById(R.id.tv_f_2),
                popupView.findViewById(R.id.tv_f_3)};
        final EditText[] tv_popup_pm = {
                popupView.findViewById(R.id.tv_pm_1),
                popupView.findViewById(R.id.tv_pm_2),
                popupView.findViewById(R.id.tv_pm_3)};
        final EditText[] tv_popup_speed = {
                popupView.findViewById(R.id.tv_speed_1),
                popupView.findViewById(R.id.tv_speed_2),
                popupView.findViewById(R.id.tv_speed_3)};

        for (int i = 0; i < 3; i++) {
            tv_popup_formaldehyde[i].setText(new DecimalFormat("0.00").format(formaldehyde[i] / 100));
            tv_popup_formaldehyde[i].setText(String.format("%d", formaldehyde[i] / 100) + "." + String.format("%02d", formaldehyde[i] % 100));
            tv_popup_pm[i].setText(String.valueOf(pm25[i]));
            tv_popup_speed[i].setText(String.valueOf(speed[i]));
        }
        //endregion

        //region 选择a1 下拉框初始化
        spinner_a1.setAdapter(dataAdapter);

        for (int i=0;i<spinner_a1.getCount();i++){
            if(select_a1.equals(spinner_a1.getItemAtPosition(i).toString())){
                spinner_a1.setSelection(i,true);
            }
        }

        //endregion
        final int[] popup_set_time = {0, 1440};

        //region 设置时间段
        tv_start_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                TimePickerDialog startTimerPickerDialog = new TimePickerDialog(M1LinkA1Activity.this, new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        // TODO Auto-generated method stub
                        Toast.makeText(M1LinkA1Activity.this, hourOfDay + "时" + minute + "分", Toast.LENGTH_LONG).show();
                        popup_set_time[0] = hourOfDay * 60 + minute;
                        TimePickerDialog endTimerPickerDialog = new TimePickerDialog(M1LinkA1Activity.this, new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                // TODO Auto-generated method stub
                                Toast.makeText(M1LinkA1Activity.this, hourOfDay + "时" + minute + "分", Toast.LENGTH_LONG).show();
                                popup_set_time[1] = hourOfDay * 60 + minute;
                                tv_start_time.setText(String.format("%02d", popup_set_time[0] / 60) + ":" + String.format("%02d", popup_set_time[0] % 60) +
                                        "-" + String.format("%02d", popup_set_time[1] / 60) + ":" + String.format("%02d", popup_set_time[1] % 60));

                            }
                        }, end_time / 60, end_time % 60, true);
                        endTimerPickerDialog.show();


                    }
                }, start_time / 60, start_time % 60, true);
                startTimerPickerDialog.show();

            }
        });
        //endregion
        //region 确认按钮初始化
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String popup_a1_mac = spinner_a1.getSelectedItem().toString().replaceAll(".*\\|", "");
                int popup__start_time = popup_set_time[0];
                int popup__end_time = popup_set_time[1];
                int[] popup_pm25 = {75, 100, 150};
                int[] popup_formaldehyde = {10, 20, 30};
                int[] popup_speed = {20, 50, 100};

                try {
                    for (int i = 0; i < 3; i++) {
                        if (tv_popup_pm[i].getText().length() > 0)
                            popup_pm25[i] = Integer.parseInt(tv_popup_pm[i].getText().toString());
                        if (tv_popup_speed[i].getText().length() > 0)
                            popup_speed[i] = Integer.parseInt(tv_popup_speed[i].getText().toString());
                        if (tv_popup_formaldehyde[i].getText().length() > 0)
                            popup_formaldehyde[i] = (int) (Float.parseFloat(tv_popup_formaldehyde[i].getText().toString()) * 100);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Toast.makeText(M1LinkA1Activity.this, "输入数据格式错误!请确认.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Send("{" +
                        "\"mac\" : \"" + device.getMac() + "\"," +
                        "\"za1\" : {" +
                        "\"on\" : 1," +
                        "\"za1_mac\" : \"" + popup_a1_mac + "\"," +
                        "\"start_time\" : " + popup__start_time + "," +
                        "\"end_time\" : " + popup__end_time + "," +
                        "\"PM25\" : [ " + popup_pm25[0] + ", " + popup_pm25[1] + ", " + popup_pm25[2] + " ]," +
                        "\"formaldehyde\" : [ " + popup_formaldehyde[0] + ", " + popup_formaldehyde[1] + ", " + popup_formaldehyde[2] + " ]," +
                        "\"speed\" : [ " + popup_speed[0] + ", " + popup_speed[1] + ", " + popup_speed[2] + " ]" +
                        "  }" +
                        "}");
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

            if (!jsonObject.has("za1")) return;

            JSONObject za1 = jsonObject.getJSONObject("za1");

//            if (!za1.has("on") || !za1.has("za1_mac")
//                    || !za1.has("start_time") || !za1.has("end_time")
//                    || !za1.has("PM25") || !za1.has("formaldehyde")
//                    || !za1.has("speed")
//
//            ) return;

            int json_on = za1.getInt("on");
            String json_za1_mac = za1.getString("za1_mac");
            int json_start_time = za1.getInt("start_time");
            int json_end_time = za1.getInt("end_time");
            JSONArray json_PM25 = za1.getJSONArray("PM25");
            JSONArray json_formaldehyde = za1.getJSONArray("formaldehyde");
            JSONArray json_speed = za1.getJSONArray("speed");


            DeviceA1 a1 = null;
            a1_mac = json_za1_mac;
            for (Device a : deviceData) {
                if (a.getType() == TYPE_A1 && a.getMac().equals(json_za1_mac)) {
                    a1 = (DeviceA1) a;
                    break;
                }
            }
            if (a1 == null)
                tv_a1.setText(a1_mac);
            else
                tv_a1.setText(a1.getName() + "|" + a1.getMac());

            switch_on = (json_on != 0);
            switch_link.setChecked(json_on != 0);
            start_time = json_start_time;
            end_time = json_end_time;
            tv_start_time.setText(String.format("%02d", start_time / 60) + ":" + String.format("%02d", start_time % 60));
            tv_end_time.setText(String.format("%02d", end_time / 60) + ":" + String.format("%02d", end_time % 60));

            for (int i = 0; i < 3; i++) {
                formaldehyde[i] = json_formaldehyde.getInt(i);
                pm25[i] = json_PM25.getInt(i);
                speed[i] = json_speed.getInt(i);

                tv_formaldehyde[i].setText(String.format("%d", formaldehyde[i] / 100) + "." + String.format("%02d", formaldehyde[i] % 100));
                tv_pm[i].setText(String.valueOf(pm25[i]));
                tv_speed[i].setText(String.valueOf(speed[i]));
            }

        } catch (
                JSONException e) {
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
