package com.zyc.zcontrol.deviceItem.rgbw;


import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.zyc.webservice.WebService;
import com.zyc.zcontrol.R;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceRGBW;
import com.zyc.zcontrol.deviceItem.DeviceClass.SettingFragment;

import org.json.JSONException;
import org.json.JSONObject;

@SuppressLint("ValidFragment")
public class RGBWSettingFragment extends SettingFragment {
    final static String Tag = "RGBWSettingFragment";


    Preference ssid;
    Preference fw_version;
    Preference regetdata;
    EditTextPreference name_preference;


    DeviceRGBW device;

    boolean ota_flag = false;
    private ProgressDialog pd;

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
                    Send("{\"mac\":\"" + device.getMac() + "\",\"version\":null,\"ssid\":null}");
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
        regetdata = findPreference("regetdata");
        name_preference = (EditTextPreference) findPreference("name");
        name_preference.setSummary(device.getName());

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
        //region 重新获取数据
        regetdata.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                handler.sendEmptyMessage(3);
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
        new AlertDialog.Builder(getActivity()).setTitle("请输入固件下载地址")
                .setMessage("警告:输入错误的地址可能导致固件损坏!")
                .setView(et)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String uri = et.getText().toString();
                        if (uri.length() < 1) return;
                        if (uri.startsWith("http")) {
                            Send("{\"mac\":\"" + device.getMac() + "\",\"setting\":{\"ota\":\"" + uri + "\"}}");
                        }
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
                if (jsonSetting.has("ota")) {
                    String ota_uri = jsonSetting.getString("ota");
                    if (ota_uri.endsWith("ota.bin")) {
                        ota_flag = true;
                        pd = new ProgressDialog(getActivity());
                        pd.setButton(DialogInterface.BUTTON_POSITIVE, "取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                pd.dismiss();// 关闭ProgressDialog
                                ota_flag = false;
                            }
                        });
                        pd.setCanceledOnTouchOutside(false);
                        pd.setMessage("正在更新固件,请勿断开设备电源!\n大约1分钟左右,请稍后....");
                        pd.show();
//                        handler.sendEmptyMessageDelayed(0,5000);

                    }
                }
                //endregion
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
