package com.espressif;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import android.widget.Toast;

import com.espressif.iot.esptouch.EsptouchTask;
import com.espressif.iot.esptouch.IEsptouchListener;
import com.espressif.iot.esptouch.IEsptouchResult;
import com.espressif.iot.esptouch.IEsptouchTask;
import com.espressif.iot.esptouch.task.__IEsptouchTask;
import com.espressif.iot.esptouch.util.ByteUtil;
import com.espressif.iot.esptouch.util.EspNetUtil;
import com.zyc.Function;
import com.zyc.zcontrol.R;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class ESPtouchActivity extends AppCompatActivity implements View.OnClickListener {
    public final static String Tag = "ESPtouchActivity";

    private static final int REQUEST_PERMISSION = 0x01;

    private TextView mApSsidTV;
    private TextView mApBssidTV;
    private EditText mApPasswordET;
    private EditText mDeviceCountET;
    private RadioGroup mPackageModeGroup;
    private TextView mMessageTV;
    private Button mConfirmBtn;


    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.e(Tag, "handler:" + msg.what);
            if (msg.what == 1) {
                IEsptouchResult res = (IEsptouchResult) msg.obj;
                //返回数据
                Intent intent = new Intent();
                intent.putExtra("ip", res.getInetAddress().getHostAddress());
                intent.putExtra("mac", res.getBssid());
                setResult(RESULT_OK, intent);
                finish();
            }
        }

    };
    private IEsptouchListener myListener = new IEsptouchListener() {

        @Override
        public void onEsptouchResultAdded(final IEsptouchResult result) {
            onEsptoucResultAddedPerform(result);
        }
    };

    private EsptouchAsyncTask4 mTask;

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

        TextView versionTV = findViewById(R.id.version_tv);
        versionTV.setText("EspTouch Version:" + IEsptouchTask.ESPTOUCH_VERSION);

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

        } else {
            registerBroadcastReceiver();
        }
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
            mApSsidTV.setTag(null);
            mApBssidTV.setText("");
            mMessageTV.setText("");
            mConfirmBtn.setEnabled(false);

            if (mTask != null) {
                mTask.cancelEsptouch();
                mTask = null;
                new AlertDialog.Builder(ESPtouchActivity.this)
                        .setMessage("Wifi disconnected or changed")
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        } else {
            String ssid = Function.getSSID(ESPtouchActivity.this);
            if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }
            mApSsidTV.setText(ssid);
            mApSsidTV.setTag(ByteUtil.getBytesByString(ssid));
            byte[] ssidOriginalData = getOriginalSsidBytes(info);
            mApSsidTV.setTag(ssidOriginalData);

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
            byte[] ssid = mApSsidTV.getTag() == null ? ByteUtil.getBytesByString(mApSsidTV.getText().toString())
                    : (byte[]) mApSsidTV.getTag();
            byte[] password = ByteUtil.getBytesByString(mApPasswordET.getText().toString());
            byte[] bssid = EspNetUtil.parseBssid2bytes(mApBssidTV.getText().toString());
            byte[] deviceCount = mDeviceCountET.getText().toString().getBytes();
            byte[] broadcast = {(byte) (mPackageModeGroup.getCheckedRadioButtonId() == R.id.package_broadcast
                    ? 1 : 0)};

            if (mTask != null) {
                mTask.cancelEsptouch();
            }
            mTask = new EsptouchAsyncTask4(ESPtouchActivity.this, handler);
            mTask.execute(ssid, bssid, password, deviceCount, broadcast);
        }
    }

    private void onEsptoucResultAddedPerform(final IEsptouchResult result) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                String text = result.getBssid() + " is connected to the wifi";
                Toast.makeText(ESPtouchActivity.this, text,
                        Toast.LENGTH_LONG).show();
            }

        });
    }

    private static class EsptouchAsyncTask4 extends AsyncTask<byte[], Void, List<IEsptouchResult>> {
        private WeakReference<ESPtouchActivity> mActivity;

        // without the lock, if the user tap confirm and cancel quickly enough,
        // the bug will arise. the reason is follows:
        // 0. task is starting created, but not finished
        // 1. the task is cancel for the task hasn't been created, it do nothing
        // 2. task is created
        // 3. Oops, the task should be cancelled, but it is running
        private final Object mLock = new Object();
        private ProgressDialog mProgressDialog;
        private AlertDialog mResultDialog;
        private IEsptouchTask mEsptouchTask;
        private Handler handler = null;

        EsptouchAsyncTask4(ESPtouchActivity activity, Handler handler) {
            mActivity = new WeakReference<>(activity);
            this.handler = handler;
        }

        void cancelEsptouch() {
            cancel(true);
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            if (mResultDialog != null) {
                mResultDialog.dismiss();
            }
            if (mEsptouchTask != null) {
                mEsptouchTask.interrupt();
            }
        }

        @Override
        protected void onPreExecute() {
            Activity activity = mActivity.get();
            mProgressDialog = new ProgressDialog(activity);
            mProgressDialog.setMessage("正在配对,请稍后...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    synchronized (mLock) {
                        if (__IEsptouchTask.DEBUG) {
                            Log.i(Tag, "progress dialog back pressed canceled");
                        }
                        if (mEsptouchTask != null) {
                            mEsptouchTask.interrupt();
                        }
                    }
                }
            });
            mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getText(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            synchronized (mLock) {
                                if (__IEsptouchTask.DEBUG) {
                                    Log.i(Tag, "progress dialog cancel button canceled");
                                }
                                if (mEsptouchTask != null) {
                                    mEsptouchTask.interrupt();
                                }
                            }
                        }
                    });
            mProgressDialog.show();
        }

        @Override
        protected List<IEsptouchResult> doInBackground(byte[]... params) {
            ESPtouchActivity activity = mActivity.get();
            int taskResultCount;
            synchronized (mLock) {
                byte[] apSsid = params[0];
                byte[] apBssid = params[1];
                byte[] apPassword = params[2];
                byte[] deviceCountData = params[3];
                byte[] broadcastData = params[4];
                taskResultCount = deviceCountData.length == 0 ? -1 : Integer.parseInt(new String(deviceCountData));
                Context context = activity.getApplicationContext();
                mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword, context);
                mEsptouchTask.setPackageBroadcast(broadcastData[0] == 1);
                mEsptouchTask.setEsptouchListener(activity.myListener);
            }
            return mEsptouchTask.executeForResults(taskResultCount);
        }

        @Override
        protected void onPostExecute(List<IEsptouchResult> result) {
            Log.e(Tag, "onPostExecute");
            ESPtouchActivity activity = mActivity.get();
            mProgressDialog.dismiss();
            mResultDialog = new AlertDialog.Builder(activity)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
            mResultDialog.setCanceledOnTouchOutside(false);
            if (result == null) {
                mResultDialog.setMessage("Create Esptouch task failed, the esptouch port could be used by other thread");
                mResultDialog.show();
                return;
            }

            final IEsptouchResult firstResult = result.get(0);
            // check whether the task is cancelled and no results received
            if (!firstResult.isCancelled()) {
                int count = 0;
                // max results to be displayed, if it is more than maxDisplayCount,
                // just show the count of redundant ones
                final int maxDisplayCount = 5;
                // the task received some results including cancelled while
                // executing before receiving enough results
                if (firstResult.isSuc()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("配对成功:\n");
                    for (IEsptouchResult resultInList : result) {
                        sb.append(String.valueOf(count + 1) + ": ")
                                .append(resultInList.getInetAddress().getHostAddress())
                                .append("  |  ")
                                .append(resultInList.getBssid())
                                .append("\n");
                        count++;
                        if (count >= maxDisplayCount) {
                            break;
                        }
                    }
                    if (count < result.size()) {
                        sb.append("\n还有")
                                .append(result.size() - count)
                                .append("个结果未显示\n");
                    }
                    mResultDialog.setMessage(sb.toString());
                    mResultDialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
//                            handler.sendMessage(msg);
//                            //返回数据
//                            Intent intent = new Intent();
//                            intent.putExtra("ip", firstResult.getInetAddress().getHostAddress());
//                            intent.putExtra("mac", firstResult.getBssid());
//                            ESPtouchActivity.setResult(RESULT_OK, intent);
                            if (handler != null) {
                                Message msg = new Message();
                                msg.obj = firstResult;
                                msg.what = 1;
                                handler.sendMessage(msg);
                            }
                        }
                    });
                    mResultDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", (DialogInterface.OnClickListener) null);

                } else {
                    mResultDialog.setMessage("配对失败");
                }

                mResultDialog.show();
            }

            activity.mTask = null;
        }
    }


    public byte[] getOriginalSsidBytes(WifiInfo info) {
        try {
            Method method = info.getClass().getMethod("getWifiSsid");
            method.setAccessible(true);
            Object wifiSsid = method.invoke(info);
            method = wifiSsid.getClass().getMethod("getOctets");
            method.setAccessible(true);
            return (byte[]) method.invoke(wifiSsid);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }
}
