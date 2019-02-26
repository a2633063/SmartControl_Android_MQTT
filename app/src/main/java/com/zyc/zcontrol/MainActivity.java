package com.zyc.zcontrol;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.zyc.StaticVariable;
import com.zyc.zcontrol.controlItem.SettingActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public final static String Tag = "MainActivity";

    DrawerLayout drawerLayout;
    ListView lv_device;
    ArrayList<DeviceItem> data = new ArrayList<DeviceItem>();
    DeviceListAdapter adapter;

    //region 使用本地广播与service通信
    LocalBroadcastManager localBroadcastManager;
    private MsgReceiver msgReceiver;
    //endregion

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FragmentAdapter fragmentAdapter;

    ConnectService mConnectService;
    boolean newDeviceFlag = false;


    int onPageScrolled = 0;   //viewpage滑动标志位,用于当viewpage滑到最左侧屏,依然继续向左侧滑动时打开侧边栏


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //region 数据库初始化
        SQLiteClass sqLite = new SQLiteClass(this, "device_list");
        //参数1：表名    参数2：要想显示的列    参数3：where子句   参数4：where子句对应的条件值
        // 参数5：分组方式  参数6：having条件  参数7：排序方式
        Cursor cursor = sqLite.Query("device_list", new String[]{"id", "name", "type", "mac"}, null, null, null, null, null);
        while (cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndex("id"));
            String name = cursor.getString(cursor.getColumnIndex("name"));
            int type = cursor.getInt(cursor.getColumnIndex("type"));
            String mac = cursor.getString(cursor.getColumnIndex("mac"));
            Log.d(Tag, "query------->" + "id：" + id + " " + "name：" + name + " " + "type：" + type + " " + "mac：" + mac);

            data.add(new DeviceItem(MainActivity.this, type, name, mac));
        }

        if (data.size() < 1) {
            data.add(new DeviceItem(MainActivity.this, StaticVariable.TYPE_BUTTON_MATE, "button1", "123456789abcde"));
            data.add(new DeviceItem(MainActivity.this, StaticVariable.TYPE_BUTTON_MATE, "测试3", "123456789abcde"));
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
        lv_device.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                drawerLayout.closeDrawer(GravityCompat.START);//关闭侧边栏
                adapter.setChoice(position);
                viewPager.setCurrentItem(position);
            }
        });
        lv_device.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("")
                        .setMessage("删除设备 " + data.get(position).name + " ?")
                        .create();
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "删除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteClass sqLite = new SQLiteClass(MainActivity.this);
                        String whereClauses = "mac=?";
                        String[] whereArgs = {data.get(position).mac};
                        sqLite.Delete("device_list", whereClauses, whereArgs);

                        data.remove(position);
                        adapter.notifyDataSetChanged();
                        fragmentAdapter.notifyDataSetChanged();
                    }
                });
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", (DialogInterface.OnClickListener) null);
                alertDialog.show();
                return true;
            }
        });
        //endregion

        //region fragment显示相关
        tabLayout = (TabLayout) findViewById(R.id.tablayout);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        fragmentAdapter = new FragmentAdapter(getSupportFragmentManager(), data);
        viewPager.setAdapter(fragmentAdapter);
        viewPager.setOffscreenPageLimit(data.size());
        tabLayout.setupWithViewPager(viewPager);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position == 0 && positionOffset == 0 && positionOffsetPixels == 0) {
                    onPageScrolled++;
                    if (onPageScrolled > 3) {
                        drawerLayout.openDrawer(GravityCompat.START);
                    }
                } else
                    onPageScrolled = 0;
            }

            @Override
            public void onPageSelected(int position) {
                adapter.setChoice(position);
                toolbar.setTitle(adapter.getChoiceDevice().name);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == 0 || state == 2)
                    onPageScrolled = 0;

            }
        });
        //endregion

//        Button btn_device_add = findViewById(R.id.btn_device_add);
//        btn_device_add.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startActivityForResult(new Intent(MainActivity.this, DeviceAddChoiceActivity.class), 1);
//                drawerLayout.closeDrawer(GravityCompat.START);//关闭侧边栏
//            }
//        });

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
        findViewById(R.id.tv_setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingActivity.class));
            }
        });
        findViewById(R.id.tv_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.tv_info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
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

        //region 启动MQTT服务
        Intent intent = new Intent(MainActivity.this, ConnectService.class);
        startService(intent);
        bindService(intent, mMQTTServiceConnection, BIND_AUTO_CREATE);

        //endregion
        //endregion

        //region json测试
/*
        String Data = "{\n" +
                "   \"Battery\" : 255,\n" +
                "   \"RSSI\" : 12,\n" +
                "   \"description\" : \"\",\n" +
                "   \"dtype\" : \"Light/Switch\",\n" +
                "   \"id\" : \"00014052\",\n" +


                "   \"nvalue\" : 0,\n" +
                "   \"stype\" : \"Switch\",\n" +
                "   \"svalue1\" : \"0\",\n" +
                "   \"switchType\" : \"On/Off\",\n" +
                "   \"unit\" : 1\n" +
                "}\n";


        try {
            JSONObject jsonArray=new JSONObject(Data);
        } catch (JSONException e) {
            e.printStackTrace();
        }*/
        //endregion
        toolbar.setTitle(adapter.getChoiceDevice().name);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (resultCode != RESULT_OK) return;
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

            String message = jsonObject.toString();
            newDeviceFlag = true;
            mConnectService.UDPsend(ip, message);

        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
            mConnectService.Send("/test/Androdi", "test message");

            mConnectService.Send(null, "UDP TEST");


            if (data.size() > 0) {
                DeviceItem d = adapter.getChoiceDevice();
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                intent.putExtra("name", d.name);
                intent.putExtra("mac", d.mac);
                intent.putExtra("type", d.type);
                startActivity(intent);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
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
        if (newDeviceFlag) {
            try {
                JSONObject jsonObject = new JSONObject(message);
                if (jsonObject.has("name") && jsonObject.has("mac")
                        && jsonObject.has("type") && jsonObject.has("type_name")) {
                    newDeviceFlag = false;
                    final int type = jsonObject.getInt("type");
                    final String name = jsonObject.getString("name");
                    final String mac = jsonObject.getString("mac");

                    final int id = adapter.contains(mac);
                    if (id >= 0) {
                        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                                .setTitle("设备重复添加")
                                .setMessage("设备已在列表中")
                                .create();
                        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                viewPager.setCurrentItem(id);

                            }
                        });
                        alertDialog.show();
                    } else {
                        DeviceItem d = new DeviceItem(MainActivity.this, type, name, mac);
                        SQLiteClass sqLite = new SQLiteClass(this, "device_list");
                        ContentValues cv = new ContentValues();
                        cv.put("name", name);
                        cv.put("type", type);
                        cv.put("mac", mac);
                        sqLite.Insert("device_list", cv);
                        data.add(d);
                        fragmentAdapter.notifyDataSetChanged();
                        adapter.notifyDataSetChanged();

                        viewPager.setCurrentItem(adapter.getCount() - 1);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    //region MQTT服务有关

    private final ServiceConnection mMQTTServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mConnectService = ((ConnectService.LocalBinder) service).getService();
            // Automatically connects to the device upon successful start-up initialization.
            mConnectService.connect("tcp://47.112.16.98:1883", "mqtt_id_dasdf",
                    "z", "2633063");
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
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            mConnectService.connect("tcp://47.112.16.98:1883", "mqtt_id_dasdf",
                                    "z", "2633063");

                        }
                    }, 1000);

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
            return this.data.get(arg0).fragment;
        }

        @Override
        public long getItemId(int position) {

            return data.get(position).fragment.hashCode();

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
                return data.get(position).name;
            else return "";
        }


    }
    //endregion

}
