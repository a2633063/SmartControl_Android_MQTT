package com.easylink;

import android.Manifest;
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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.zyc.Function;
import com.zyc.zcontrol.R;

import io.fogcloud.sdk.easylink.api.EasylinkP2P;
import io.fogcloud.sdk.easylink.helper.EasyLinkCallBack;
import io.fogcloud.sdk.easylink.helper.EasyLinkParams;

public class EasylinkActivity extends AppCompatActivity implements View.OnClickListener {
    public final static String Tag = "EasylinkActivity";

    private static final int REQUEST_PERMISSION = 0x01;

    private TextView mApSsidTV;
    private TextView mApBssidTV;
    private EditText mApPasswordET;
    private EditText mDeviceCountET;
    private RadioGroup mPackageModeGroup;
    private TextView mMessageTV;
    private Button mConfirmBtn;

    private ProgressDialog mProgressDialog;

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
}
