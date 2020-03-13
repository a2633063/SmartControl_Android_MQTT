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
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.view.GravityCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.zyc.Function;
import com.zyc.StaticVariable;
import com.zyc.webservice.WebService;
import com.zyc.zcontrol.controlItem.SettingActivity;
import com.zyc.zcontrol.deviceScan.DeviceAddChoiceActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Random;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.zyc.Function.getLocalVersionName;
import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity {
    public final static String Tag = "MainActivity";

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;


    DrawerLayout drawerLayout;
    ListView lv_device;
    ArrayList<DeviceItem> data = new ArrayList<DeviceItem>();
    DeviceListAdapter adapter;

    //region 使用本地广播与service通信
    LocalBroadcastManager localBroadcastManager;
    private MsgReceiver msgReceiver;
    WifiManager.MulticastLock wifiLock;
    //endregion

    private TextView tvDeviceSort;
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FragmentAdapter fragmentAdapter;

    ConnectService mConnectService;
    boolean newDeviceFlag = false;
    boolean getDeviceFlag = false;


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
                //region 获取设备标志位
                case 2:
                    newDeviceFlag = false;
                    break;
                case 3:
                    getDeviceFlag = false;
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

            data.add(new DeviceItem(MainActivity.this, type, name, mac));
        }

        if (data.size() < 1) {
//            data.add(new DeviceItem(MainActivity.this, StaticVariable.TYPE_M1, "演示设备", "b0f8932234f4"));
            data.add(new DeviceItem(MainActivity.this, StaticVariable.TYPE_TC1, "演示设备", "000000000000"));
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
                Intent intent = new Intent(MainActivity.this, DeviceSortActivity.class);
                startActivity(intent);
                finish();
            }
        });
        //endregion
        int page = mSharedPreferences.getInt("page", 0);
        //region listview及adapter

        lv_device = findViewById(R.id.lv_device);
        adapter = new DeviceListAdapter(MainActivity.this, data);
        if (adapter.getCount() > 0) adapter.setChoice(0);

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
        lv_device.setAdapter(adapter);
        if (page < adapter.getCount()) adapter.setChoice(page);
        lv_device.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                drawerLayout.closeDrawer(GravityCompat.START);//关闭侧边栏
                adapter.setChoice(position);
                viewPager.setCurrentItem(position);
            }
        });
        //region 长按删除设备
        lv_device.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("配置设备:" + data.get(position).getName())
                        .setMessage("设置桌面快捷方式请手动开启权限,否则会开启失败.")
                        .create();
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "删除设备", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteClass sqLite = new SQLiteClass(MainActivity.this);
                        String whereClauses = "mac=?";
                        String[] whereArgs = {data.get(position).getMac()};
                        sqLite.Delete("device_list", whereClauses, whereArgs);

                        data.remove(position);
                        adapter.notifyDataSetChanged();
                        fragmentAdapter.notifyDataSetChanged();
                    }
                });
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", (DialogInterface.OnClickListener) null);
                alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "创建快捷方式", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Function.createShortCut(MainActivity.this, data.get(position).getMac(), data.get(position).getName());
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
        fragmentAdapter = new FragmentAdapter(getSupportFragmentManager(), data);
        viewPager.setAdapter(fragmentAdapter);
        viewPager.setOffscreenPageLimit(data.size());
        tabLayout.setupWithViewPager(viewPager);

        if (page < fragmentAdapter.data.size()) viewPager.setCurrentItem(page);
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
                adapter.setChoice(position);
                toolbar.setTitle(adapter.getChoiceDevice().getName());
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
            toolbar.setTitle(adapter.getChoiceDevice().getName());
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
                if (res == null || res.length() < 100)
                    res = WebService.WebConnect("https://gitee.com/api/v5/repos/zhangyichen/SmartControl_Android_MQTT/releases/latest");
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
            int type = intent.getIntExtra("type", StaticVariable.TYPE_UNKNOWN);
            String ip = intent.getExtras().getString("ip");
            String mac = intent.getExtras().getString("mac");
            Log.e(Tag, "get device result:" + ip + "," + mac + "," + type);

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("cmd", "device report");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (ip != null && ip.equals("255.255.255.255")) {
                getDeviceFlag = true;
                handler.sendEmptyMessageDelayed(3, 2000);
            } else {
                handler.sendEmptyMessageDelayed(2, 1000);
                newDeviceFlag = true;
            }
            String message = jsonObject.toString();

            mConnectService.UDPsend(ip, message);

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
            int position = adapter.contains(intentget.getStringExtra("mac"));
            Log.d(Tag, "mac:" + intentget.getStringExtra("mac") + "," + position);
            if (position >= 0 && position < adapter.getCount()) {
                adapter.setChoice(position);
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
        //注销广播
        localBroadcastManager.unregisterReceiver(msgReceiver);
        //停止服务
        Intent intent = new Intent(MainActivity.this, ConnectService.class);
        stopService(intent);
        unbindService(mMQTTServiceConnection);
        if (null != wifiLock) wifiLock.release();//必须调用
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
//            mConnectService.Send("/test/Androd", "test message");
//            mConnectService.Send(null, "UDP TEST");


            if (data.size() > 0) {
                DeviceItem d = adapter.getChoiceDevice();
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                intent.putExtra("name", d.getName());
                intent.putExtra("mac", d.getMac());
                intent.putExtra("type", d.getType());
                startActivity(intent);
            }
            return true;
        } else if (id == R.id.action_mqtt_send) {


            if (data.size() < 1) {
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

                                DeviceItem d = adapter.getChoiceDevice();
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


            DeviceItem d = adapter.getChoiceDevice();
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

            String message = null;
            try {
                message = jsonObject.toString(0);
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
    //endregion


    //数据接收处理函数
    void Receive(String ip, int port, String message) {
        //TODO 数据接收处理
        Receive(null, message);
    }

    void Receive(String topic, String message) {
        //TODO 数据接收处理
        Log.d(Tag, "RECV DATA,topic:" + topic + ",content:" + message);

        try {
            JSONObject jsonObject = new JSONObject(message);
            String name = null;
            String mac = null;
            int type = -2;
            String type_name = null;
            JSONObject jsonSetting = null;
            if (jsonObject.has("name")) name = jsonObject.getString("name");
            if (jsonObject.has("mac")) mac = jsonObject.getString("mac");
            if (jsonObject.has("type_name")) type_name = jsonObject.getString("type_name");
            if (jsonObject.has("type")) type = jsonObject.getInt("type");
            if (jsonObject.has("setting")) jsonSetting = jsonObject.getJSONObject("setting");
            if (mac == null) return;
            final int position = adapter.contains(mac);
            //region 根据收到的消息更改显示列表
            if (position >= 0) {//设备已存在

                //region 修改名称
                if (name != null && !name.equals(data.get(position).getName())) {
                    SQLiteClass sqLite = new SQLiteClass(this, "device_list");
                    ContentValues cv = new ContentValues();
                    cv.put("name", name);
                    if (type >= 0) cv.put("type", type);
                    sqLite.Modify("device_list", cv, "mac=?", new String[]{mac});

                    data.get(position).setName(name);
                    fragmentAdapter.notifyDataSetChanged();
                    adapter.notifyDataSetChanged();
                    if (position == adapter.getChoice())
                        toolbar.setTitle(name);
                }
                //endregion

                //region 反馈mqtt设置回应
                if (jsonSetting != null && jsonSetting.has("mqtt_uri")
                        && jsonSetting.has("mqtt_port") && jsonSetting.has("mqtt_user")
                        && jsonSetting.has("mqtt_password")) {
                    String toastStr = "已设置\"" + data.get(position).getName() + "\"mqtt服务器:\r\n"
                            + jsonSetting.getString("mqtt_uri") + ":" + jsonSetting.getInt("mqtt_port")
                            + "\n" + jsonSetting.getString("mqtt_user");
                    Toast.makeText(MainActivity.this, toastStr, Toast.LENGTH_SHORT).show();
                }
                //endregion

                //region 获取 局域网设备 状态
                if (newDeviceFlag) {
                    if (name == null || type_name == null || type == -1 || jsonSetting != null)
                        return;
                    handler.removeMessages(2);
                    newDeviceFlag = false;
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                            .setTitle("设备重复添加")
                            .setMessage("设备已在列表中")
                            .create();
                    alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            viewPager.setCurrentItem(position);

                        }
                    });
                    alertDialog.show();
                }
                //endregion
            } else {//设备不存在
                if (newDeviceFlag || getDeviceFlag) {
                    if (name == null || type_name == null || type == -1 || jsonSetting != null)
                        return;
                    handler.removeMessages(2);
                    newDeviceFlag = false;
                    DeviceItem d = new DeviceItem(MainActivity.this, type, name, mac);
                    SQLiteClass sqLite = new SQLiteClass(this, "device_list");
                    ContentValues cv = new ContentValues();
                    cv.put("name", name);
                    cv.put("type", type);
                    cv.put("mac", mac);
                    cv.put("sort", data.size());
                    sqLite.Insert("device_list", cv);
                    data.add(d);
                    fragmentAdapter.notifyDataSetChanged();
                    adapter.notifyDataSetChanged();
                    viewPager.setCurrentItem(adapter.getCount() - 1);
                }
            }
            //endregion


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
            handler.sendEmptyMessage(1);
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
                Receive(ip, port, message);
            } else if (ConnectService.ACTION_MQTT_CONNECTED.equals(action)) {  //连接成功
                Log.d(Tag, "ACTION_MQTT_CONNECTED");
            } else if (ConnectService.ACTION_MQTT_DISCONNECTED.equals(action)) {  //连接失败/断开
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
                Receive(topic, message);

            }
        }
    }
    //endregion


    //region FragmentPagerAdapter
    class FragmentAdapter extends FragmentPagerAdapter {

        private ArrayList<DeviceItem> data;

        public FragmentAdapter(FragmentManager fm, ArrayList<DeviceItem> fragmentArray) {
            this(fm);
            this.data = fragmentArray;
        }

        public FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        //这个函数的作用是当切换到第arg0个页面的时候调用。
        @Override
        public Fragment getItem(int arg0) {
            return this.data.get(arg0).getFragment();
        }

        @Override
        public long getItemId(int position) {
            return data.get(position).getFragment().hashCode();
        }

        @Override
        public int getItemPosition(Object object) {
            // TODO Auto-generated method stub
            return PagerAdapter.POSITION_NONE;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            //int viewPagerId=container.getId();
            //makeFragmentName(viewPagerId,getItemId(position));
            return super.instantiateItem(container, position);
        }

        @Override
        public int getCount() {
            return this.data.size();
        }

        //重写这个方法，将设置每个Tab的标题
        @Override
        public CharSequence getPageTitle(int position) {
            if (data != null)
                return data.get(position).getName();
            else return "";
        }


    }
    //endregion

}
