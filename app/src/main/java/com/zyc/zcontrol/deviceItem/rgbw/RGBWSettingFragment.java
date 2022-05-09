package com.zyc.zcontrol.deviceItem.rgbw;


import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.zyc.Function.getLocalVersionName;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.zyc.webservice.WebService;
import com.zyc.zcontrol.MainActivity;
import com.zyc.zcontrol.R;
import com.zyc.zcontrol.deviceItem.DeviceClass.Device;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceRGBW;
import com.zyc.zcontrol.deviceItem.DeviceClass.SettingFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

@SuppressLint("ValidFragment")
public class RGBWSettingFragment extends SettingFragment {
    final static String Tag = "RGBWSettingFragment";


    Preference ssid;
    Preference fw_version;
    Preference restart;
    Preference regetdata;
    EditTextPreference name_preference;

    EditTextPreference auto_off;
    Preference gpio;

    DeviceRGBW device;

    boolean ota_flag = false;
    private ProgressDialog pd;

    int[] gpio_val = {-1, -1, -1, -1};

    private RGBWOTAInfo otaInfo = new RGBWOTAInfo();

    //region Handler
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {// handler接收到消息后就会执行此方法
            JSONObject obj = null;
            String JsonStr = null;
            switch (msg.what) {
                //region 获取最新版本信息
                case 0:
                    if (pd != null && pd.isShowing()) pd.dismiss();
                    JsonStr = (String) msg.obj;
                    Log.d(Tag, "result:" + JsonStr);
                    try {
                        if (JsonStr == null || JsonStr.length() < 3)
                            throw new JSONException("获取最新版本信息失败");
                        obj = new JSONObject(JsonStr);
                        if (obj.has("id") && obj.has("tag_name") && obj.has("target_commitish")
                                && obj.has("name") && obj.has("body") && obj.has("created_at")
                                && obj.has("assets")) {
                            otaInfo.title = obj.getString("name");
                            otaInfo.message = obj.getString("body");
                            otaInfo.tag_name = obj.getString("tag_name");
                            otaInfo.created_at = obj.getString("created_at");

                            String version = fw_version.getSummary().toString();
                            if (!version.equals(otaInfo.tag_name)) {
                                handler.sendEmptyMessage(1);
                            } else {
                                Toast.makeText(getActivity(), "已是最新版本", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            throw new JSONException("获取最新版本信息失败");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), "获取最新版本信息失败", Toast.LENGTH_SHORT).show();
                    }

                    break;
                //endregion
                //region 开始获取固件下载地址
                case 1:
                    pd.setMessage("正在获取固件地址,请稍后....");
                    pd.setCanceledOnTouchOutside(false);
                    pd.show();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Message msg = new Message();
                            msg.what = 2;
                            String res = WebService.WebConnect("https://gitee.com/api/v5/repos/a2633063/Release/releases/tags/zRGBW");
                            msg.obj = res;
                            handler.sendMessageDelayed(msg, 0);// 执行耗时的方法之后发送消给handler
                        }
                    }).start();
                    break;
                //endregion
                //region 已获取固件下载地址
                case 2:
                    if (pd != null && pd.isShowing()) pd.dismiss();
                    JsonStr = (String) msg.obj;
                    Log.d(Tag, "result:" + JsonStr);
                    try {
                        if (JsonStr == null || JsonStr.length() < 3)
                            throw new JSONException("获取固件下载地址失败");

                        obj = new JSONObject(JsonStr);

                        if (obj.getString("name").equals("zRGBW发布地址_" + otaInfo.tag_name)) {
                            String otauriAll = obj.getString("body");
                            otaInfo.ota = otauriAll.split("\r\n");

                            AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                                    .setTitle("获取到最新版本:" + otaInfo.tag_name)
                                    .setMessage(otaInfo.title + "\n" + otaInfo.message)
                                    .setPositiveButton("更新", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Send("{\"mac\":\"" + device.getMac() + "\",\"setting\":{\"ota1\":\"" + otaInfo.ota[0] + "\",\"ota2\":\"" + otaInfo.ota[1] + "\"}}");
                                        }
                                    })
                                    .setNegativeButton("取消", null)
                                    .create();
                            alertDialog.show();
                        } else
                            throw new JSONException("获取固件下载地址失败");

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), "固件下载地址获取失败", Toast.LENGTH_SHORT).show();

                    }

                    break;
                //endregion
                //region 发送请求数据
                case 3:
                    Send("{\"mac\":\"" + device.getMac() + "\",\"version\":null,\"ssid\":null,\"auto_off\":null,\"gpio\":null}");
                    break;
                //endregion
            }
        }
    };
    //endregion

    public RGBWSettingFragment(DeviceRGBW device) {
        super(device.getName(), device.getMac());
        this.device = device;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName("Setting_" + device.getMac());

        Log.d(Tag, "设置文件:" + "Setting" + device.getMac());
        addPreferencesFromResource(R.xml.rgbw_setting);


        ssid = findPreference("ssid");
        fw_version = findPreference("fw_version");
        restart = findPreference("restart");
        regetdata = findPreference("regetdata");
        name_preference = (EditTextPreference) findPreference("name");
        name_preference.setSummary(device.getName());
        auto_off = (EditTextPreference) findPreference("auto_off");
        gpio = findPreference("gpio");

        //region mac地址
        findPreference("mac").setSummary(device.getMac());
        findPreference("mac").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("text", device.getMac()));
                    Toast.makeText(getActivity(), "已复制mac地址", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(getActivity(), "复制mac地址失败", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
                return false;
            }
        });
        //endregion

        //region 设置名称
        name_preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Send("{\"mac\":\"" + device.getMac() + "\",\"setting\":{\"name\":\"" + (String) newValue + "\"}}");
                return false;
            }
        });
        //endregion
        //region 版本
        fw_version.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                //region 手动输入固件下载地址注释

//                final EditText et = new EditText(getActivity());
//                new AlertDialog.Builder(getActivity()).setTitle("请输入固件下载地址")
//                        .setView(et)
//                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialogInterface, int i) {
//                                String uri = et.getText().toString();
//                                if (uri.length() < 1) return;
//                                if (uri.startsWith("http")) {
//                                    Send("{\"mac\":\"" + device.getMac() + "\",\"setting\":{\"ota\":\"" + uri + "\"}}");
//                                }
//                            }
//                        }).setNegativeButton("取消", null).show();

                //endregion
                //未获取到当前版本信息
                if (!isGetVersion()) return false;


                String version = fw_version.getSummary().toString();
                //region 获取最新版本
                pd = new ProgressDialog(getActivity());
                pd.setMessage("正在获取最新固件版本,请稍后....");
                pd.setCanceledOnTouchOutside(false);
                pd.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Message msg = new Message();
                        msg.what = 0;
                        String res = WebService.WebConnect("https://gitee.com/api/v5/repos/a2633063/zRGBW/releases/latest");
                        msg.obj = res;
                        handler.sendMessageDelayed(msg, 0);// 执行耗时的方法之后发送消给handler
                    }
                }).start();


                //endregion
                return false;
            }
        });
        //endregion
        //region 重启设备
        restart.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                new AlertDialog.Builder(getActivity()).setTitle("重启设备?")
                        .setMessage("如果设备死机此处重启可能无效,依然需要手动断开电源才能重启设备")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Send("{\"mac\":\"" + device.getMac() + "\",\"cmd\":\"restart\"}");

                            }
                        }).setNegativeButton("取消", null).show();


                return false;
            }
        });
        //endregion
        //region 重新获取数据
        regetdata.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                handler.sendEmptyMessage(3);
                return false;
            }
        });
        //endregion
        //region 设置自动关闭时间
        auto_off.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int val = Integer.parseInt((String) newValue);
                if (val >= 0 ) {
                    Send("{\"mac\":\"" + device.getMac() + "\",\"auto_off\":" + (String) newValue + "}");
                } else {
                    Toast.makeText(getActivity(), "输入有误!", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });
        //endregion
        //region 设置gpio
        gpio.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                gpio_set_window();
                return false;
            }
        });
        //endregion
    }


    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(Tag, "longclick:" + position);
        if (position == fw_version.getOrder() + 1) {
            debugFWUpdate();
            return true;
        }
        return false;
    }

    //region 弹窗
    //region 判断是否获取当前版本号
    boolean isGetVersion() {
        //region 未获取到当前版本信息
        if (fw_version.getSummary() == null) {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                    .setTitle("未获取到当前设备版本")
                    .setMessage("请点击重新获取数据.获取到当前设备版本后重试.")
                    .setNegativeButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            handler.sendEmptyMessageDelayed(3, 0);
                            Toast.makeText(getActivity(), "请求版本数据...", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .create();
            alertDialog.show();
            return false;
        } else return true;
        //endregion
    }

    //endregion
    //region 手动输入固件下载地址
    void debugFWUpdate() {
        //未获取到当前版本信息
        if (!isGetVersion()) return;
        final EditText et = new EditText(getActivity());
        et.setMinLines(2);
        new AlertDialog.Builder(getActivity()).setTitle("请输入固件下载地址")
                .setMessage("需要输入2个ota地址,以换行隔开\n警告:输入错误的地址可能导致固件损坏!\n请勿谨慎使用此功能!")
                .setView(et)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String uri = et.getText().toString();
                        //if (uri.length() < 1) return;
                        String[] ota = uri.replace("\r\n", "\n").split("\n");
                        if (ota.length < 2
                                || (ota.length >= 2 && (!ota[0].startsWith("http") || !ota[1].startsWith("http")))
                        ) {
                            Toast.makeText(getActivity(), "填写链接错误!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Send("{\"mac\":\"" + device.getMac() + "\",\"setting\":{\"ota1\":\"" + ota[0] + "\",\"ota2\":\"" + ota[1] + "\"}}");

                    }
                }).setNegativeButton("取消", null).show();
    }

    //endregion
    //region 弹窗激活
    void unlock() {

        final EditText et = new EditText(getActivity());
        new AlertDialog.Builder(getActivity()).setTitle("请输入激活码")
                .setView(et)
                .setMessage("")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String lockStr = et.getText().toString();
                        lockStr = lockStr.replace("\r\n", "\n").replace("\n", "").trim();
                        Send("{\"mac\":\"" + device.getMac() + "\",\"lock\":\"" + lockStr + "\"}");
                    }
                }).setNegativeButton("取消", null).show();

    }

    //endregion

    void gpio_set_window() {
        if (!(gpio_val[0] >= 0 && gpio_val[0] <= 16 && gpio_val[1] >= 0 && gpio_val[1] <= 16
                && gpio_val[2] >= 0 && gpio_val[2] <= 16 && gpio_val[3] >= 0 && gpio_val[3] <= 16)
        ) {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                    .setTitle("未获取到设备当前配置")
                    .setMessage("请点击重新获取数据.获取到设备当前配置后重试.")
                    .setNegativeButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            handler.sendEmptyMessageDelayed(3, 0);
                            //Toast.makeText(getActivity(), "请求版本数据...", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .create();
            alertDialog.show();
            return;
        }


        final View popupView = getActivity().getLayoutInflater().inflate(R.layout.rgbw_popupwindow_set_gpio, null);
        final PopupWindow window = new PopupWindow(popupView, MATCH_PARENT, MATCH_PARENT, true);//wrap_content,wrap_content


        //region 控件初始化

        //region io口选择下拉框
        String[] mItems = getResources().getStringArray(R.array.rgbw_gpio);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.rgbw_popupwindow_set_gpio_spinner, mItems);
        final Spinner[] spinners = new Spinner[4];
        spinners[0] = popupView.findViewById(R.id.spinner_r);
        spinners[1] = popupView.findViewById(R.id.spinner_g);
        spinners[2] = popupView.findViewById(R.id.spinner_b);
        spinners[3] = popupView.findViewById(R.id.spinner_w);

        for (int i = 0; i < 4; i++) {
            spinners[i].setAdapter(adapter);
            for (int j = 0; j < spinners[i].getAdapter().getCount(); j++) {
                if (spinners[i].getAdapter().getItem(j).equals("IO" + gpio_val[i])) {
                    spinners[i].setSelection(j);
                    break;
                }
            }
        }


        //region 现有方案按钮
        popupView.findViewById(R.id.btn_mops).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] str = {"IO15", "IO5", "IO12", "IO14"};
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < spinners[i].getAdapter().getCount(); j++) {
                        if (spinners[i].getAdapter().getItem(j).equals(str[i])) {
                            spinners[i].setSelection(j);
                            break;
                        }
                    }
                }
            }
        });

        popupView.findViewById(R.id.btn_dohome).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] str = {"IO12", "IO5", "IO14", "IO4"};
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < spinners[i].getAdapter().getCount(); j++) {
                        if (spinners[i].getAdapter().getItem(j).equals(str[i])) {
                            spinners[i].setSelection(j);
                            break;
                        }
                    }
                }
            }
        });
        //endregion

        popupView.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (spinners[0].getSelectedItemPosition() == spinners[1].getSelectedItemPosition()
                        || spinners[0].getSelectedItemPosition() == spinners[2].getSelectedItemPosition()
                        || spinners[0].getSelectedItemPosition() == spinners[3].getSelectedItemPosition()
                        || spinners[1].getSelectedItemPosition() == spinners[2].getSelectedItemPosition()
                        || spinners[1].getSelectedItemPosition() == spinners[3].getSelectedItemPosition()
                        || spinners[2].getSelectedItemPosition() == spinners[3].getSelectedItemPosition()
                ) {
                    new AlertDialog.Builder(getActivity()).setTitle("错误,IO口重复")
                            .setMessage("不可以选择重复的IO口,请重新选择")
                            .setPositiveButton("确定", null)
                            .show();
                    return;
                }
                String gpios = "";
                for (int i = 0; i < 4; i++) {
                    gpios += spinners[i].getSelectedItem().toString().replaceAll("IO", "");
                    if (i < 3) gpios += ",";
                }

                Send("{\"mac\":\"" + device.getMac() + "\",\"gpio\":[" + gpios + "]}");
                window.dismiss();
            }
        });

        //endregion

        //region window初始化
        popupView.findViewById(R.id.constraintLayout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.alpha(0xffff0000)));
        window.setOutsideTouchable(true);
        window.getContentView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                window.dismiss();
                return true;
            }
        });
        //endregion
        //endregion
        window.update();
        window.showAtLocation(popupView, Gravity.CENTER, 0, 0);
    }

    //endregion

    void Send(String message) {
        boolean b = getActivity().getSharedPreferences("Setting_" + device.getMac(), 0).getBoolean("always_UDP", false);
        super.Send(b, device.getSendMqttTopic(), message);
    }

    //数据接收处理函数
    @Override
    public void Receive(String ip, int port, String topic, String message) {
        super.Receive(ip, port, topic, message);
        Log.d(Tag, "RECV DATA,topic:" + topic + ",content:" + message);

        try {
            JSONObject jsonObject = new JSONObject(message);
            if (!jsonObject.has("mac") || !jsonObject.getString("mac").equals(device.getMac())) {
                return;
            }
            //region 获取名称
            if (jsonObject.has("name")) {
                device.setName(jsonObject.getString("name"));
                name_preference.setSummary(device.getName());
                name_preference.setText(device.getName());
            }
            //endregion
            //region ssid
            if (jsonObject.has("ssid")) {
                String ssidString = jsonObject.getString("ssid");
                ssid.setSummary(ssidString);
                ssid.setSummary(jsonObject.getString("ssid"));
                if (jsonObject.has("rssi")) {
                    int rssi = jsonObject.getInt("rssi");
                    ssid.setSummary(ssid.getSummary() + " (" + rssi + "dBm)");

                }
            }
            //endregion
            //region 获取版本号
            if (jsonObject.has("version")) {
                String version = jsonObject.getString("version");
                fw_version.setSummary(version);
            }
            //endregion
            //region ota结果/进度
            if (jsonObject.has("ota_progress")) {
                int ota_progress = jsonObject.getInt("ota_progress");

                if (!(ota_progress >= 0 && ota_progress < 100) && pd != null && pd.isShowing()) {
                    pd.dismiss();

                    String m = "固件更新成功!";
                    if (ota_progress == -1) {
                        m = "固件更新失败!请重试";
                    }
                    if (ota_flag) {
                        ota_flag = false;
                        new android.app.AlertDialog.Builder(getActivity())
                                .setTitle("")
                                .setMessage(m)
                                .setPositiveButton("确定", null)
                                .show();
                    }
                } else {
                    if (ota_flag) {
                        //todo 显示更新进度

                        if (pd != null && pd.isShowing())
                            pd.setMessage("正在获取最新固件版本,请稍后....\n" + "进度:" + ota_progress + "%");
//                        Toast.makeText(getActivity(), "ota进度:"+ota_progress+"%", Toast.LENGTH_SHORT).show();
                    }
                }

            }
            //endregion
            //region 接收主机setting
            JSONObject jsonSetting = null;
            if (jsonObject.has("setting")) jsonSetting = jsonObject.getJSONObject("setting");
            if (jsonSetting != null) {

                //region ota
                if (jsonSetting.has("ota1") ||jsonSetting.has("ota2")) {
                    String ota_uri1 = jsonSetting.optString("ota1","http");
                    String ota_uri2 = jsonSetting.optString("ota2","http");
                    if (ota_uri1.startsWith("http") || ota_uri2.startsWith("http") ) {
                        ota_flag = true;
                        pd = new ProgressDialog(getActivity());
                        pd.setButton(DialogInterface.BUTTON_POSITIVE, "关闭窗口", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                pd.dismiss();// 关闭ProgressDialog
                                ota_flag = false;
                            }
                        });
                        pd.setCanceledOnTouchOutside(false);
                        pd.setMessage("正在更新固件,请勿断开设备电源!\n大约15秒左右,请稍后....\n若时间过长,请手动关闭此窗口\n此弹窗可以直接关闭,不影响ota结果");
                        pd.show();
//                        handler.sendEmptyMessageDelayed(0,5000);

                    }
                }
                //endregion
            }
            //endregion

            //region 获取自动关闭时间
            if (jsonObject.has("auto_off")) {
                int auto_off_time = jsonObject.getInt("auto_off");
                auto_off.setText(String.valueOf(auto_off_time));
                if(auto_off_time == 0)
                    auto_off.setSummary("开灯后延时自动关灯,功能已经关闭");
                else
                    auto_off.setSummary("开灯"+auto_off_time+"秒后自动关灯");
            }
            //endregion
            //region 获取GPIO设置
            if (jsonObject.has("gpio")) {
                gpio.setSummary("设置控制RGBW的IO口");
                JSONArray jsonGpio = jsonObject.optJSONArray("gpio");

                for (int i = 0; i < 4; i++)
                    gpio_val[i] = jsonGpio.optInt(i, -1);

                if (gpio_val[0] >= 0 && gpio_val[0] <= 16
                        && gpio_val[1] >= 0 && gpio_val[1] <= 16
                        && gpio_val[2] >= 0 && gpio_val[2] <= 16
                        && gpio_val[3] >= 0 && gpio_val[3] <= 16
                )
                    gpio.setSummary(gpio.getSummary() + " : [" + gpio_val[0] + "," + gpio_val[1] + "," + gpio_val[2] + "," + gpio_val[3] + "]");
            }
            //endregion

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //region 事件监听调用函数,主要为在子类中重写此函数实现在service建立成功/mqtt连接成功/失败时执行功能
    //Service建立成功时调用    此函数需要时在子类中重写
    public void ServiceConnected() {
        handler.sendEmptyMessageDelayed(3, 0);
    }

    //mqtt连接成功时调用    此函数需要时在子类中重写
    public void MqttConnected() {
        handler.sendEmptyMessageDelayed(3, 0);
    }

    //mqtt连接断开时调用    此函数需要时在子类中重写
    public void MqttDisconnected() {
        handler.sendEmptyMessageDelayed(3, 0);
    }
    //endregion

}
