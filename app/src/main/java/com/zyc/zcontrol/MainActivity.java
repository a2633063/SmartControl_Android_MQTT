package com.zyc.zcontrol;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.zyc.Function;
import com.zyc.webservice.WebService;
import com.zyc.zcontrol.deviceItem.DeviceClass.Device;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceA1;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceButtonMate;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceDC1;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceM1;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceRGBW;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceTC1;
import com.zyc.zcontrol.deviceItem.SettingActivity;
import com.zyc.zcontrol.deviceAdd.DeviceAddChoiceActivity;
import com.zyc.zcontrol.mainActivity.MainDeviceFragmentAdapter;
import com.zyc.zcontrol.mainActivity.MainDeviceLanUdpScanListAdapter;
import com.zyc.zcontrol.mainActivity.MainDeviceListAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.zyc.Function.getLocalVersionName;

import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity {
    public final static String Tag = "MainActivity";

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;


    DrawerLayout drawerLayout;
    ListView lv_device;
    ArrayList<Device> deviceData;
    MainDeviceListAdapter mainDeviceListAdapter;
    MainDeviceLanUdpScanListAdapter mainDeviceLanUdpScanListAdapter = null;

    //region 使用本地广播与service通信
    LocalBroadcastManager localBroadcastManager;
    private MsgReceiver msgReceiver;
    WifiManager.MulticastLock wifiLock;
    //endregion

    private TextView tvDeviceSort;
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private MainDeviceFragmentAdapter mainDeviceFragmentAdapter;

    ConnectService mConnectService;
    boolean updateSqlFlag = false;


    int onPageScrolled = 0;   //viewpage滑动标志位,用于当viewpage滑到最左侧屏,依然继续向左侧滑动时打开侧边栏

    //region Handler
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                //region 连接MQTT服务器
                case 1:
                    if (mConnectService != null) {
                        handler.removeMessages(1);
                        String mqtt_uri = mSharedPreferences.getString("mqtt_uri", null);
                        String mqtt_id = mSharedPreferences.getString("mqtt_clientid", null);
                        String mqtt_user = mSharedPreferences.getString("mqtt_user", null);
                        String mqtt_password = mSharedPreferences.getString("mqtt_password", null);

                        Log.d(Tag, "mqtt connect: " + mqtt_uri + ",user:" + mqtt_user + "," + mqtt_password);
                        if (mqtt_id == null || mqtt_id.length() < 1)
                            mqtt_id = "Android_" + new Random().nextInt(10000);

                        Log.d(Tag, "mqtt_id:" + mqtt_id);
                        mConnectService.connect(mqtt_uri, mqtt_id, mqtt_user, mqtt_password);
                    }
                    break;
                //endregion
                //region 需要刷新设备adapter
                case 2:
                    handler.removeMessages(2);
                    if (deviceData.size() == 0) {
                        deviceData.add(new DeviceTC1("演示设备", "000000000000"));
                    } else if (deviceData.size() > 0 && deviceData.get(0).getMac().equals("000000000000")) {
                        deviceData.remove(0);
                    }

                    mainDeviceFragmentAdapter.notifyDataSetChanged();
                    mainDeviceListAdapter.notifyDataSetChanged();
                    if (msg.obj != null) {
                        mainDeviceListAdapter.setChoice((Integer) msg.obj);
                        viewPager.setCurrentItem((Integer) msg.obj);
                    }
                    toolbar.setTitle(mainDeviceListAdapter.getChoiceDevice().getName());
                    updateSqlFlag = true; //退出app时需要更新数据库
                    Log.d(Tag, "device list update");
                    break;
                //endregion
                //region 当获取局域网设备时,每隔2秒发送{"cmd":"device report"}
                case 3:
                    mConnectService.UDPsend("255.255.255.255", "{\"cmd\":\"device report\"}");
                    if (mainDeviceLanUdpScanListAdapter != null) {
                        handler.sendEmptyMessageDelayed(3, 1000);
                    }
                    break;
                //endregion

                //region 获取app最新版本
                case 100:
                    try {
                        if (msg.obj == null) throw new JSONException("获取版本信息失败,请重试");
                        JSONObject obj = new JSONObject((String) msg.obj);
                        if (!obj.has("tag_name")
                                || !obj.has("name")
                                || !obj.has("body")
                                || !obj.has("created_at")) throw new JSONException("获取最新版本信息失败");

                        String body = obj.getString("body");
                        String name = obj.getString("name");
                        String tag_name = obj.getString("tag_name");
                        String created_at = obj.getString("created_at");

                        String tag_name_old = getLocalVersionName(MainActivity.this);
                        if (tag_name.equals(tag_name_old)) {
                            Log.d(Tag, "已是最新版本");
                        } else {
                            Log.d(Tag, "当前版本:" + tag_name_old + ",发布版本:" + tag_name);
                            boolean show_ota = true;
                            String[] version_new = tag_name.replaceAll("[^.1234567890]", "").split("\\.");
                            String[] version_old = tag_name_old.replaceAll("[^.1234567890]", "").split("\\.");

                            for (int i = 0; i < version_new.length && i < version_old.length; i++) {
                                try {
                                    int a = Integer.parseInt(version_new[i]);
                                    int b = Integer.parseInt(version_old[i]);
                                    if (b < a) break;
                                    else if (b > a) {
                                        show_ota = false;
                                        break;
                                    }
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (!show_ota) {
                                Toast.makeText(MainActivity.this, "当前版本暂时未发布，测试中\n当前版本:" + tag_name_old + "\n发布版本:" + tag_name, Toast.LENGTH_LONG).show();
                                break;
                            }

                            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("请更新版本:" + tag_name)
                                    .setMessage(name + "\r\n" + body + "\r\n更新日期:" + created_at)
                                    .setPositiveButton("更新", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Uri uri = Uri.parse("https://www.coolapk.com/apk/com.zyc.zcontrol");
                                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                            startActivity(intent);
                                        }
                                    })
                                    .setNegativeButton("取消", null)
                                    .create();
                            alertDialog.show();

                            // 在dialog执行show之后才能来设置
                            TextView tvMsg = (TextView) alertDialog.findViewById(android.R.id.message);
                            String HtmlStr = String.format(getResources().getString(R.string.app_ota_message), name, body, created_at).replace("\n", "<br />");
                            Log.d(Tag, HtmlStr);
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                tvMsg.setText(Html.fromHtml(HtmlStr, Html.FROM_HTML_MODE_COMPACT));
                            } else {
                                tvMsg.setText(Html.fromHtml(HtmlStr));
                            }

                        }

                    } catch (JSONException e) {
//                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "获取最新版本失败,请在酷安搜索zConrotl更新最新版本", Toast.LENGTH_LONG).show();
                    }
                    break;
                //endregion
            }
        }
    };
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSharedPreferences = getSharedPreferences("Setting", 0);
        deviceData = ((MainApplication) getApplication()).getDeviceList();
        deviceData.clear();
        //region 数据库初始化
        SQLiteClass sqLite = new SQLiteClass(this, "device_list");
        //参数1：表名    参数2：要想显示的列    参数3：where子句   参数4：where子句对应的条件值
        // 参数5：分组方式  参数6：having条件  参数7：排序方式
        Cursor cursor = sqLite.Query("device_list", new String[]{"id", "name", "type", "mac", "sort"}, null, null, null, null, "sort");
        while (cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndex("id"));
            String name = cursor.getString(cursor.getColumnIndex("name"));
            int type = cursor.getInt(cursor.getColumnIndex("type"));
            String mac = cursor.getString(cursor.getColumnIndex("mac"));
            Log.d(Tag, "query------->" + "id：" + id + " " + "name：" + name + " " + "type：" + type + " " + "mac：" + mac);

            switch (type) {
                case Device.TYPE_BUTTON_MATE:
                    deviceData.add(new DeviceButtonMate(name, mac));
                    break;
                case Device.TYPE_TC1:
                    deviceData.add(new DeviceTC1(name, mac));
                    break;
                case Device.TYPE_DC1:
                    deviceData.add(new DeviceDC1(name, mac));
                    break;
                case Device.TYPE_A1:
                    deviceData.add(new DeviceA1(name, mac));
                    break;
                case Device.TYPE_M1:
                    deviceData.add(new DeviceM1(name, mac));
                    break;
                case Device.TYPE_RGBW:
                    deviceData.add(new DeviceRGBW(name, mac));
                    break;
            }
        }

        if (deviceData.size() < 1) {
            deviceData.add(new DeviceTC1("演示设备", "000000000000"));
//            deviceData.add(new DeviceButtonMate("演示设备1", "000000000001"));
//            deviceData.add(new DeviceDC1("演示设备2", "000000000002"));
//            deviceData.add(new DeviceA1("演示设备3", "000000000003"));
//            deviceData.add(new DeviceM1("演示设备4", "000000000004"));
//            deviceData.add(new DeviceRGBW("演示设备5", "000000000005"));
//            deviceData.add(new DeviceTC1("ztc18baa", "d0bae4638baa"));
        }
        //endregion


        //region 控件初始化

        //region 侧边栏 初始化
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        NavigationView navigationView = findViewById(R.id.nav_view);

        //endregion

        //region 排序提示初始化
        tvDeviceSort = findViewById(R.id.tv_device_list_tips);
        tvDeviceSort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deviceData.size() == 1 && deviceData.get(0).getMac().equals("000000000000")) {
                    Toast.makeText(MainActivity.this, "仅演示设备,无需排序", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(MainActivity.this, DeviceSortActivity.class);
                startActivity(intent);
                //finish();
            }
        });
        //endregion
        int page = mSharedPreferences.getInt("page", 0);
        //region listview及adapter

        lv_device = findViewById(R.id.lv_device);
        mainDeviceListAdapter = new MainDeviceListAdapter(MainActivity.this, deviceData);
        if (mainDeviceListAdapter.getCount() > 0) mainDeviceListAdapter.setChoice(0);

        Button b = new Button(this);
        b.setBackground(null);
        b.setTextColor(0xa0ffffff);
//        b.setBackgroundResource(R.drawable.background_gray_borders);
        b.setText("增加设备");
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(MainActivity.this, DeviceAddChoiceActivity.class), 1);
                drawerLayout.closeDrawer(GravityCompat.START);//关闭侧边栏

            }
        });
        View footView = (View) LayoutInflater.from(this).inflate(R.layout.nav_header_main, null);
        lv_device.addFooterView(b);
        lv_device.setAdapter(mainDeviceListAdapter);
        if (page < mainDeviceListAdapter.getCount()) mainDeviceListAdapter.setChoice(page);
        lv_device.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                drawerLayout.closeDrawer(GravityCompat.START);//关闭侧边栏
                mainDeviceListAdapter.setChoice(position);
                viewPager.setCurrentItem(position);
            }
        });
        //region 长按删除设备
        lv_device.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("配置设备:" + deviceData.get(position).getName())
                        .setMessage("设置桌面快捷方式请手动开启权限,否则会开启失败.")
                        .create();
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "删除设备", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (position == 0 && deviceData.get(position).getMac().equals("000000000000")) {
                            Toast.makeText(MainActivity.this, "演示设备在有设备后自动删除,无需手动删除", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        deviceData.remove(position);
                        handler.sendEmptyMessage(2);
                    }
                });
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", (DialogInterface.OnClickListener) null);
                alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "创建快捷方式", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Function.createShortCut(MainActivity.this, deviceData.get(position).getMac(), deviceData.get(position).getName());
                    }
                });
                alertDialog.show();
                return true;
            }
        });
        //endregion
        //endregion

        //region fragment显示相关
        tabLayout = (TabLayout) findViewById(R.id.tablayout);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        mainDeviceFragmentAdapter = new MainDeviceFragmentAdapter(getSupportFragmentManager(), deviceData);
        viewPager.setAdapter(mainDeviceFragmentAdapter);
        viewPager.setOffscreenPageLimit(deviceData.size() + 3);
        tabLayout.setupWithViewPager(viewPager);

        if (page < mainDeviceFragmentAdapter.getCount()) viewPager.setCurrentItem(page);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position == 0 && positionOffset == 0 && positionOffsetPixels == 0) {
                    onPageScrolled++;
                    if (onPageScrolled > 3) {
                        drawerLayout.openDrawer(GravityCompat.START);
                    }
                } else {
                    mEditor = getSharedPreferences("Setting", 0).edit();
                    mEditor.putInt("page", position);
                    mEditor.commit();
                    onPageScrolled = 0;
                }
            }

            @Override
            public void onPageSelected(int position) {
                mainDeviceListAdapter.setChoice(position);
                toolbar.setTitle(mainDeviceListAdapter.getChoiceDevice().getName());
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == 0 || state == 2)
                    onPageScrolled = 0;

            }
        });
        //endregion
        //region 打赏
        navigationView.getHeaderView(0).findViewById(R.id.tv_reward)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        drawerLayout.closeDrawer(GravityCompat.START);//关闭侧边栏
                        popupwindowInfo();
                    }
                });
        //endregion
        //region 打开网页
        final TextView nav_header_subtitle = navigationView.getHeaderView(0).findViewById(R.id.tv_nav_header_subtitle);
        nav_header_subtitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse(nav_header_subtitle.getText().toString());
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        //endregion

        //region 设置/关于/退出按钮
        //region 设置按钮
        findViewById(R.id.tv_setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);//关闭侧边栏

            }
        });
        //endregion
        //region 退出按钮
        findViewById(R.id.tv_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        //endregion
        //region 关于按钮
        findViewById(R.id.tv_info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawer(GravityCompat.START);//关闭侧边栏
                popupwindowInfo();
            }
        });
        //endregion
        //endregion

        //endregion

        //region MQTT服务有关
        //region 动态注册接收mqtt服务的广播接收器,
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectService.ACTION_MQTT_CONNECTED);
        intentFilter.addAction(ConnectService.ACTION_MQTT_DISCONNECTED);
        intentFilter.addAction(ConnectService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(ConnectService.ACTION_UDP_DATA_AVAILABLE);//UDP监听
        intentFilter.addAction(ConnectService.ACTION_MAINACTIVITY_DEVICELISTUPDATE); //device list更新
        localBroadcastManager.registerReceiver(msgReceiver, intentFilter);
        //endregion


        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = manager.createMulticastLock("localWifi");
        wifiLock.acquire();
        //region 启动MQTT服务
        Intent intent = new Intent(MainActivity.this, ConnectService.class);
        startService(intent);
        bindService(intent, mMQTTServiceConnection, BIND_AUTO_CREATE);

        //endregion
        //endregion
        //region 设置标题 无页面时导致闪退
        try {
            toolbar.setTitle(mainDeviceListAdapter.getChoiceDevice().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        //endregion

        //region 获取最新版本
        new Thread(new Runnable() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = 100;
                String res = WebService.WebConnect("https://gitee.com/api/v5/repos/a2633063/SmartControl_Android_MQTT/releases/latest");
                msg.obj = res;
                handler.sendMessageDelayed(msg, 0);// 执行耗时的方法之后发送消给handler
            }
        }).start();
        //endregion
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (resultCode != RESULT_OK) return;
        //region 新增设备返回
        if (requestCode == 1) {
            int type = intent.getIntExtra("type", Device.TYPE_UNKNOWN);
            String ip = intent.getExtras().getString("ip");
            String mac = intent.getExtras().getString("mac");
            Log.e(Tag, "get device result:" + ip + "," + mac + "," + type);
            popupwindowLanUdpScan();
//            if (ip != null && ip.equals("255.255.255.255")) {
//                popupwindowLanUdpScan();
//            } else {
//
//            }
        }
        //endregion
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra("mac") && intent.getStringExtra("mac") != null)
            setIntent(intent);// must store the new intent unless getIntent() will return the old one
    }

    @Override
    public void onResume() {
        Log.d(Tag, "onResume");
        super.onResume();
        Intent intentget = this.getIntent();
        if (intentget.hasExtra("mac") && intentget.getStringExtra("mac") != null)//判断是否有值传入,并判断是否有特定key
        {
            int position = mainDeviceListAdapter.contains(intentget.getStringExtra("mac"));
            Log.d(Tag, "mac:" + intentget.getStringExtra("mac") + "," + position);
            if (position >= 0 && position < mainDeviceListAdapter.getCount()) {
                mainDeviceListAdapter.setChoice(position);
                viewPager.setCurrentItem(position);
                Log.d(Tag, "set position:" + position);
            } else if (position == -1) {
                Toast.makeText(this, "设备不存在!", Toast.LENGTH_SHORT).show();
            }
            intentget.putExtra("mac", (String) null);
        }
        if (mConnectService != null) {

            String mqtt_uri = mSharedPreferences.getString("mqtt_uri", null);
            String mqtt_id = mSharedPreferences.getString("mqtt_clientid", null);
            String mqtt_user = mSharedPreferences.getString("mqtt_user", null);
            String mqtt_password = mSharedPreferences.getString("mqtt_password", null);

            if ((mqtt_uri != null) && mqtt_uri.length() > 3
                    && (
                    !mConnectService.mqtt_uri.equals(mqtt_uri)
                            || !mConnectService.mqtt_user.equals(mqtt_user)
                            || !mConnectService.mqtt_password.equals(mqtt_password)
            )) {
                Log.d(Tag, "onResume disconnect");
                mConnectService.disconnect();
                handler.sendEmptyMessageDelayed(1, 100);
            }
        }


    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(Tag, "onDestroy");
        //region 停止Service
        //注销广播
        localBroadcastManager.unregisterReceiver(msgReceiver);
        //停止服务
        Intent intent = new Intent(MainActivity.this, ConnectService.class);
        stopService(intent);
        unbindService(mMQTTServiceConnection);
        if (null != wifiLock) wifiLock.release();//必须调用
        //endregion
        //region 需要时更新数据库
        if (updateSqlFlag) {
            //删除数据库所有内容,根据排序重新写入
            SQLiteClass sqLite = new SQLiteClass(MainActivity.this, "device_list");
            sqLite.Delete("device_list", null, null);

            for (int i = 0; i < deviceData.size(); i++) {
                Device d = deviceData.get(i);
                ContentValues cv = new ContentValues();
                cv.put("name", d.getName());
                cv.put("type", d.getType());
                cv.put("mac", d.getMac());
                cv.put("sort", i);
                sqLite.Insert("device_list", cv);
            }
        }
        //endregion
        super.onDestroy();
    }

    //region toolbar 菜单栏
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_device_settings) {
            if (deviceData.size() > 0) {
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                intent.putExtra("index", mainDeviceListAdapter.getChoice());
                startActivity(intent);
            }
            return true;
        } else if (id == R.id.action_mqtt_send) {


            if (deviceData.size() < 1) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("设备列表为空")
                        .setMessage("请先添加设备")
                        .create();
                alertDialog.show();
                return true;
            }

            String mqtt_uri = mSharedPreferences.getString("mqtt_uri", null);
            String mqtt_user = mSharedPreferences.getString("mqtt_user", "");
            String mqtt_password = mSharedPreferences.getString("mqtt_password", "");

            if (mqtt_uri == null || mqtt_uri.length() < 1) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("未设置MQTT服务器")
                        .setMessage("继续会发送空数据,将删除固件的MQTT服务器设置!继续?")
                        .setPositiveButton("继续", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                Device d = mainDeviceListAdapter.getChoiceDevice();
                                JSONObject jsonObject = new JSONObject();
                                JSONObject jsonObject1 = new JSONObject();

                                try {
                                    jsonObject.put("name", d.getName());
                                    jsonObject.put("mac", d.getMac());

                                    jsonObject1.put("mqtt_uri", "");
                                    jsonObject1.put("mqtt_port", 0);
                                    jsonObject1.put("mqtt_user", "");
                                    jsonObject1.put("mqtt_password", "");
                                    jsonObject.put("setting", jsonObject1);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }

                                String message = null;
                                try {
                                    message = jsonObject.toString(0);
                                    message = message.replace("\r\n", "");

                                    Log.d(Tag, "message:" + message);
                                    mConnectService.UDPsend(message);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                        .setNegativeButton("取消", null)
                        .create();
                alertDialog.show();
                return true;
            }


            Device d = mainDeviceListAdapter.getChoiceDevice();
            JSONObject jsonObject = new JSONObject();
            JSONObject jsonObject1 = new JSONObject();

            try {
                jsonObject.put("name", d.getName());
                jsonObject.put("mac", d.getMac());

                String[] strArry = mqtt_uri.split(":");
                int port = parseInt(strArry[1]);

                jsonObject1.put("mqtt_uri", strArry[0]);
                jsonObject1.put("mqtt_port", port);
                jsonObject1.put("mqtt_user", mqtt_user);
                jsonObject1.put("mqtt_password", mqtt_password);
                jsonObject.put("setting", jsonObject1);

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            try {
                String message = jsonObject.toString(0);
                message = message.replace("\r\n", "");

                Log.d(Tag, "message:" + message);
                mConnectService.UDPsend(message);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //endregion

    //region 弹窗
    private void popupwindowInfo() {

        final View popupView = getLayoutInflater().inflate(R.layout.popupwindow_main_info, null);
        final PopupWindow window = new PopupWindow(popupView, MATCH_PARENT, MATCH_PARENT, true);//wrap_content,wrap_content


        //region 控件初始化

        TextView tv_version = popupView.findViewById(R.id.tv_version);
        tv_version.setText("APP当前版本:" + getLocalVersionName(this));

        //region 支付宝跳转
        ImageView imageView = popupView.findViewById(R.id.alipay);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String intentFullUrl = "intent://platformapi/startapp?saId=10000007&" +
                        "clientVersion=3.7.0.0718&qrcode=https%3A%2F%2Fqr.alipay.com%2Ffkx06093fjxuqmwbco9oka9%3F_s" +
                        "%3Dweb-other&_t=1472443966571#Intent;" +
                        "scheme=alipayqr;package=com.eg.android.AlipayGphone;end";
                Intent intent = null;
                try {
                    intent = Intent.parseUri(intentFullUrl, Intent.URI_INTENT_SCHEME);
                } catch (URISyntaxException e) {
//                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "失败,支付宝有安装?", Toast.LENGTH_SHORT).show();
                }


                startActivity(intent);
            }
        });
        //endregion
        //region 作者github跳转
        TextView tv_author = popupView.findViewById(R.id.tv_author);
        tv_author.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        tv_author.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "跳转作者github...", Toast.LENGTH_SHORT).show();
                Uri uri = Uri.parse("https://github.com/a2633063");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        //endregion
        //region app页面跳转
        popupView.findViewById(R.id.btn_app).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://github.com/a2633063/SmartControl_Android_MQTT");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        //endregion
        //region zTC1页面跳转
        popupView.findViewById(R.id.btn_ztc1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://github.com/a2633063/zTC1/");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        //endregion
        //region zDC1页面跳转
        popupView.findViewById(R.id.btn_zdc1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://github.com/a2633063/zDC1/");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        //endregion
        //region zA1页面跳转
        popupView.findViewById(R.id.btn_za1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://github.com/a2633063/zA1/");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        //endregion
        //region zM1页面跳转
        popupView.findViewById(R.id.btn_zm1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://github.com/a2633063/zM1/");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        //endregion
        //region window初始化
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

    private void popupwindowLanUdpScan() {

        final View popupView = getLayoutInflater().inflate(R.layout.popupwindow_main_lan_udp_scan, null);
        final PopupWindow window = new PopupWindow(popupView, MATCH_PARENT, MATCH_PARENT, true);//wrap_content,wrap_content

        //region 控件初始化
        ListView lv = popupView.findViewById(R.id.lv_device);
        List<Device> mdata = new ArrayList<>();
        //mdata.add(new DeviceButtonMate("演示设备1", "000000000001"));
        mainDeviceLanUdpScanListAdapter = new MainDeviceLanUdpScanListAdapter(MainActivity.this, mdata);
        lv.setAdapter(mainDeviceLanUdpScanListAdapter);


        Button btn_ok = popupView.findViewById(R.id.btn_ok);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mainDeviceLanUdpScanListAdapter.getCount() > 0) {
                    deviceData.addAll(mainDeviceLanUdpScanListAdapter.getData());
                    Message msg = new Message();
                    msg.what = 2;
                    msg.obj = deviceData.size() - 1;
                    handler.sendMessage(msg);
                    Toast.makeText(MainActivity.this, "设备已添加", Toast.LENGTH_SHORT).show();
                }
                window.dismiss();
            }
        });
        popupView.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                window.dismiss();
            }
        });
        //region window初始化
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.alpha(0xffff0000)));
        window.setOutsideTouchable(true);
        window.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                handler.removeMessages(3);
                mainDeviceLanUdpScanListAdapter = null;
            }
        });
        //endregion
        //endregion
        window.update();
        window.showAtLocation(popupView, Gravity.CENTER, 0, 0);
        handler.sendEmptyMessageDelayed(3, 0);

    }

    //endregion


    //数据接收处理函数
    void Receive(String ip, int port, String topic, String message) {
        Log.d(Tag, "RECV DATA,topic:" + topic + ",content:" + message);


        //region 接收availability数据,非Json,单独处理
        if (topic != null && topic.endsWith("availability")) {
            String regexp = "device/(.*?)/([0123456789abcdef]{12})/(.*)";
            Pattern pattern = Pattern.compile(regexp);
            Matcher matcher = pattern.matcher(topic);
            if (matcher.find() && matcher.groupCount() == 3) {
                String device_type = matcher.group(1);
                String device_mac = matcher.group(2);
                String device_state = matcher.group(3);
                broadcastUpdate(device_mac, ip, port, topic, message);
                return;
            }
        }
        //endregion
        try {

            JSONObject jsonObject = new JSONObject(message);
            String device_mac = null;
            if (jsonObject.has("mac")) device_mac = jsonObject.getString("mac");
            if (device_mac == null) return;

            if (jsonObject.has("mac") && jsonObject.has("type") && jsonObject.has("name")) {
                //region 设备回复 {"cmd":"device report"}的数据 处理
                if (mainDeviceLanUdpScanListAdapter != null) {  //但获取局域网设备时
                    String name = jsonObject.getString("name");
                    String mac = jsonObject.getString("mac");
                    int type = jsonObject.getInt("type");

                    if (mainDeviceListAdapter.contains(mac) > -1) { //设备之前就已添加
                        Log.d(Tag, "已添加的重复设备:" + mac);
                        return;
                    }
                    if (mainDeviceLanUdpScanListAdapter.contains(mac) > -1) {//设备已经在列表中
                        Log.d(Tag, "已扫描的重复设备:" + mac);
                        return;
                    }
                    switch (type) {
                        case Device.TYPE_BUTTON_MATE:
                            mainDeviceLanUdpScanListAdapter.add(new DeviceButtonMate(name, mac));
                            break;
                        case Device.TYPE_TC1:
                            mainDeviceLanUdpScanListAdapter.add(new DeviceTC1(name, mac));
                            break;
                        case Device.TYPE_DC1:
                            mainDeviceLanUdpScanListAdapter.add(new DeviceDC1(name, mac));
                            break;
                        case Device.TYPE_A1:
                            mainDeviceLanUdpScanListAdapter.add(new DeviceA1(name, mac));
                            break;
                        case Device.TYPE_M1:
                            mainDeviceLanUdpScanListAdapter.add(new DeviceM1(name, mac));
                            break;
                        case Device.TYPE_RGBW:
                            mainDeviceLanUdpScanListAdapter.add(new DeviceRGBW(name, mac));
                            break;
                    }
                    mainDeviceLanUdpScanListAdapter.notifyDataSetChanged();
                }
                //endregion
            } else {
                broadcastUpdate(device_mac, ip, port, topic, message);
            }
            //region 修改名称 只有当获取到的设备名称,与当前记录名称不同时,修改数据库内名称.
            //且为当前选择设备时,更新toolbar标题
            if (jsonObject.has("name")) {
                String name = jsonObject.getString("name");
                final int position = mainDeviceListAdapter.contains(device_mac);
                if (position >= 0 && name != null && !name.equals(deviceData.get(position).getName())) {
                    deviceData.get(position).setName(name);
                    handler.sendEmptyMessage(2);
                }
            }
            //endregion

            return;
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    //region MQTT服务有关

    private final ServiceConnection mMQTTServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mConnectService = ((ConnectService.LocalBinder) service).getService();
            // Automatically connects to the device upon successful start-up initialization.
            handler.sendEmptyMessageDelayed(1, 0);
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
                Receive(ip, port, null, message);
            } else if (ConnectService.ACTION_MQTT_CONNECTED.equals(action)) {  //连接成功
                Log.d(Tag, "ACTION_MQTT_CONNECTED");

                for (Device d : deviceData) {
                    String[] topic = d.getRecvMqttTopic();
                    if (topic != null) {
                        int[] qos = new int[topic.length];
                        for (int i = 0; i < qos.length; i++) qos[i] = 1;
                        mConnectService.subscribe(topic, qos);
//                        Log.d(Tag, "subscribe:" + d.getMqttStateTopic());
                    }
                }
            } else if (ConnectService.ACTION_MQTT_DISCONNECTED.equals(action)) {  //连接失败/断开,尝试重新连接
                Log.w(Tag, "ACTION_MQTT_DISCONNECTED");
                if (mConnectService != null) {
                    if (mConnectService.isConnected()) {
                        mConnectService.disconnect();
                    }
                    //1秒后重连
                    handler.sendEmptyMessageDelayed(1, 1000);
                }
            } else if (ConnectService.ACTION_DATA_AVAILABLE.equals(action)) {  //接收到数据
                String topic = intent.getStringExtra(ConnectService.EXTRA_DATA_TOPIC);
                String message = intent.getStringExtra(ConnectService.EXTRA_DATA_MESSAGE);
                Receive(null, -1, topic, message);
            } else if (ConnectService.ACTION_MAINACTIVITY_DEVICELISTUPDATE.equals(action)) {  //接收到数据
                handler.sendEmptyMessageDelayed(2, 0);
            }
        }
    }

    //endregion
//    void broadcastUpdate(String action) {
//        localBroadcastManager.sendBroadcast(new Intent(action));
//    }

    void broadcastUpdate(String action, String ip, int port, String topic, String message) {
        final Intent intent = new Intent(action);
        intent.putExtra(ConnectService.EXTRA_UDP_DATA_IP, ip);
        intent.putExtra(ConnectService.EXTRA_UDP_DATA_PORT, port);
        intent.putExtra(ConnectService.EXTRA_DATA_TOPIC, topic);
        intent.putExtra(ConnectService.EXTRA_DATA_MESSAGE, message);
        localBroadcastManager.sendBroadcast(intent);
    }

}
