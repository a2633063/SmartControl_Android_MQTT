package com.easylink;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.zyc.Function;
import com.zyc.zcontrol.ConnectService;
import com.zyc.zcontrol.R;

import org.json.JSONException;
import org.json.JSONObject;

import io.fogcloud.sdk.easylink.api.EasylinkP2P;
import io.fogcloud.sdk.easylink.helper.EasyLinkCallBack;
import io.fogcloud.sdk.easylink.helper.EasyLinkParams;

public class EasylinkActivity extends AppCompatActivity implements View.OnClickListener {
    public final static String Tag = "EasylinkActivity";

    private static final int REQUEST_PERMISSION = 0x01;

    //region 使用本地广播与service通信
    LocalBroadcastManager localBroadcastManager;
    private MsgReceiver msgReceiver;
    //endregion


    private TextView mApSsidTV;
    private TextView mApBssidTV;
    private EditText mApPasswordET;
    private EditText mDeviceCountET;
    private RadioGroup mPackageModeGroup;
    private TextView mMessageTV;
    private Button mConfirmBtn;

    private ProgressDialog mProgressDialog;
    private AlertDialog mResultDialog;

    EasylinkP2P elp2p;


    //region wifi广播状态监听
    private boolean mReceiverRegistered = false;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }

            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(WIFI_SERVICE);
            assert wifiManager != null;

            switch (action) {
                case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                    WifiInfo wifiInfo;
                    if (intent.hasExtra(WifiManager.EXTRA_WIFI_INFO)) {
                        wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                    } else {
                        wifiInfo = wifiManager.getConnectionInfo();
                    }
                    onWifiChanged(wifiInfo);
                    break;
                case LocationManager.PROVIDERS_CHANGED_ACTION:
                    onWifiChanged(wifiManager.getConnectionInfo());
                    onLocationChanged();
                    break;
            }
        }
    };
    //endregion

    private boolean mDestroyed = false;


    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.e(Tag, "handler:" + msg.what);
            if (msg.what == 0) {

                //返回数据
//                Intent intent = new Intent();
//                intent.putExtra("ip", res.getInetAddress().getHostAddress());
//                intent.putExtra("mac", res.getBssid());
//                setResult(RESULT_OK, intent);
                finish();
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_esptouch);

        elp2p = new EasylinkP2P(this);

        Log.d("EasylinkActivity", "ssid:" + Function.getSSID(EasylinkActivity.this));

        //region 顶部返回按钮
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        //endregion

        //region MQTT服务有关
        //region 动态注册接收mqtt服务的广播接收器,
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectService.ACTION_UDP_DATA_AVAILABLE);
        localBroadcastManager.registerReceiver(msgReceiver, intentFilter);
        //endregion
        //endregion

        //region 控件初始化
        mApSsidTV = findViewById(R.id.ap_ssid_text);
        mApBssidTV = findViewById(R.id.ap_bssid_text);
        mApPasswordET = findViewById(R.id.ap_password_edit);
        mDeviceCountET = findViewById(R.id.device_count_edit);
        mDeviceCountET.setText("1");
        mPackageModeGroup = findViewById(R.id.package_mode_group);
        mMessageTV = findViewById(R.id.message);
        mConfirmBtn = findViewById(R.id.confirm_btn);
        mConfirmBtn.setEnabled(false);
        mConfirmBtn.setOnClickListener(this);

        mPackageModeGroup.setVisibility(View.GONE);
        mDeviceCountET.setVisibility(View.GONE);
        //endregion

//        TextView versionTV = findViewById(R.id.version_tv);
//        versionTV.setText("EspTouch Version:" + IEsptouchTask.ESPTOUCH_VERSION);

        //region 权限判断
        if (Build.VERSION.SDK_INT >= 28) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                String[] permissions = {
                        Manifest.permission.ACCESS_COARSE_LOCATION
                };

                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION);
            } else {
                registerBroadcastReceiver();
            }
            onLocationChanged();
        } else {
            registerBroadcastReceiver();
        }
        //endregion
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (!mDestroyed) {
                        registerBroadcastReceiver();
                    }
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mDestroyed = true;
        if (mReceiverRegistered) {
            unregisterReceiver(mReceiver);
        }
        //注销广播
        localBroadcastManager.unregisterReceiver(msgReceiver);

    }

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        if (Build.VERSION.SDK_INT >= 28) {
            filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        }
        registerReceiver(mReceiver, filter);
        mReceiverRegistered = true;
    }

    private void onWifiChanged(WifiInfo info) {
        boolean connected = info != null && info.getNetworkId() != -1;
        if (!connected) {
            mApSsidTV.setText("");
            mApBssidTV.setText("");
            mMessageTV.setText("");
            mConfirmBtn.setEnabled(false);


        } else {
            String ssid = Function.getSSID(EasylinkActivity.this);
            if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }
            mApSsidTV.setText(ssid);

            String bssid = info.getBSSID();
            mApBssidTV.setText(bssid);

            mConfirmBtn.setEnabled(true);
            mMessageTV.setText("");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int frequence = info.getFrequency();
                if (frequence > 4900 && frequence < 5900) {
                    // Connected 5G wifi. Device does not support 5G
                    mMessageTV.setText("不支持5G网络,请切换为2.4G网络或重试");
                }
            }
            if ((Build.VERSION.SDK_INT >= 28))
                onLocationChanged();
        }
    }

    private void onLocationChanged() {
        boolean enable;
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            enable = false;
        } else {
            boolean locationGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean locationNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            enable = locationGPS || locationNetwork;
        }

        if (!enable) {
            mMessageTV.setText("GPS关闭! 安卓9.0必须打开gps且授权才能获取到ssid!");
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mConfirmBtn) {
            mProgressDialog = new ProgressDialog(EasylinkActivity.this);
            mProgressDialog.setMessage("正在配对,请稍后...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            elp2p.stopEasyLink(new EasyLinkCallBack() {
                                @Override
                                public void onSuccess(int code, String message) {
                                    Log.d(Tag, message);
                                }

                                @Override
                                public void onFailure(int code, String message) {
                                    Log.d(Tag, message);
                                }
                            });
                        }
                    });
            mProgressDialog.show();

            EasyLinkParams elp = new EasyLinkParams();
            elp.ssid = mApSsidTV.getText().toString();
            elp.password = mApPasswordET.getText().toString();
            elp.sleeptime = 50;
            elp.runSecond = 60000;
            Log.d(Tag, "ssid:" + mApSsidTV.getText().toString() + ",password:" + mApPasswordET.getText().toString());
            elp2p.startEasyLink(elp, new EasyLinkCallBack() {
                @Override
                public void onSuccess(int code, String message) {
                    Log.d(Tag, message);
//                    send2handler(1, message);
                }

                @Override
                public void onFailure(int code, String message) {
                    Log.d(Tag, message);
//                    Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    void Receive(String ip, int port, String message) {
        //TODO 数据接收处理
        Receive(null, message);
    }

    void Receive(String topic, String message) {
        //TODO 数据接收处理
        Log.d(Tag, "RECV DATA,topic:" + topic + ",content:" + message);

        try {
            JSONObject jsonObject = new JSONObject(message);

            if (jsonObject.has("name") && jsonObject.has("mac") && jsonObject.has("type_name")) {
                final String name = jsonObject.getString("name");
                final String mac = jsonObject.getString("mac");
                final String type_name = jsonObject.getString("type_name");
                final String ip = jsonObject.getString("ip");
                if (type_name.equals("zTC1")) {
                    elp2p.stopEasyLink(new EasyLinkCallBack() {
                        @Override
                        public void onSuccess(int code, String message) {
                            Log.d(Tag, message);
                        }

                        @Override
                        public void onFailure(int code, String message) {
                            Log.d(Tag, message);
                        }
                    });
                    mResultDialog = new AlertDialog.Builder(EasylinkActivity.this).create();
                    mResultDialog.setCanceledOnTouchOutside(false);
                    mResultDialog.setMessage("配对成功:\n1:" + ip + "  |  " + mac + "\n");
                    mResultDialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //返回数据
                            Intent intent = new Intent();
                            intent.putExtra("ip", ip);
                            intent.putExtra("mac", mac);
                            setResult(RESULT_OK, intent);
                            finish();
//                            handler.sendEmptyMessageDelayed(0, 50);
                        }
                    });
                    mResultDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", (DialogInterface.OnClickListener) null);
                    mResultDialog.show();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

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
            }
        }
    }
}
