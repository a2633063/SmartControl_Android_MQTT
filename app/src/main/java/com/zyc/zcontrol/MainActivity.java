package com.zyc.zcontrol;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

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


    MQTTService mMQTTService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //region 侧边栏 初始化
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        NavigationView navigationView =  findViewById(R.id.nav_view);        navigationView.getMenu().add(1,1,1,"dd");//需要获取id的话，id就等于1；

        //endregion

        //region 控件初始化
        data.add(new DeviceItem(MainActivity.this,DeviceItem.TYPE_BUTTON_MATE,"button1",R.drawable.ic_menu_manage));
        data.add(new DeviceItem(MainActivity.this,DeviceItem.TYPE_BUTTON_MATE,"标题2",R.drawable.ic_menu_gallery));
        data.add(new DeviceItem(MainActivity.this,DeviceItem.TYPE_BUTTON_MATE,"测试3",R.drawable.ic_menu_camera));
        //region listview及adapter

        lv_device=findViewById(R.id.lv_device);
        adapter = new DeviceListAdapter(MainActivity.this, data);
        if(adapter.getCount()>0) adapter.setChoice(0);
        lv_device.setAdapter(adapter);
        lv_device.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                drawerLayout.closeDrawer(GravityCompat.START);//关闭侧边栏
                adapter.setChoice(position);
                viewPager.setCurrentItem(position);
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

            }

            @Override
            public void onPageSelected(int position) {
                adapter.setChoice(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        //endregion

        final TextView nav_header_subtitle=navigationView.getHeaderView(0).findViewById(R.id.tv_nav_header_subtitle);
        nav_header_subtitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse(nav_header_subtitle.getText().toString());
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        //endregion

        //region MQTT服务有关
        //region 动态注册接收mqtt服务的广播接收器,
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MQTTService.ACTION_MQTT_CONNECTED);
        intentFilter.addAction(MQTTService.ACTION_MQTT_DISCONNECTED);
        intentFilter.addAction(MQTTService.ACTION_DATA_AVAILABLE);
        localBroadcastManager.registerReceiver(msgReceiver, intentFilter);
        //endregion

        //region 启动MQTT服务 不启动
        Intent intent = new Intent(MainActivity.this, MQTTService.class);
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
        Intent intent = new Intent(MainActivity.this, MQTTService.class);
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
        if (id == R.id.action_settings) {
            mMQTTService.Send("/test/Androdi", "test message", 0);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //endregion

    //region MQTT服务有关

    private final ServiceConnection mMQTTServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mMQTTService = ((MQTTService.LocalBinder) service).getService();
            // Automatically connects to the device upon successful start-up initialization.
            mMQTTService.connect("tcp://47.112.16.98:1883", "mqtt_id_dasdf",
                    "z", "2633063");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mMQTTService = null;
        }
    };

    //广播接收,用于处理接收到的数据
    public class MsgReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (MQTTService.ACTION_MQTT_CONNECTED.equals(action)) {  //连接成功
                Log.d(Tag, "ACTION_MQTT_CONNECTED");
            } else if (MQTTService.ACTION_MQTT_DISCONNECTED.equals(action)) {  //连接失败/断开
                Log.w(Tag, "ACTION_MQTT_DISCONNECTED");
                if (mMQTTService != null) {
                    if (mMQTTService.isConnected()) {
                        mMQTTService.disconnect();
                    }

                    //1秒后重连
                    new Handler().postDelayed(new Runnable(){
                        public void run() {
                            mMQTTService.connect("tcp://47.112.16.98:1883", "mqtt_id_dasdf",
                                    "z", "2633063");

                        }
                    }, 1000);

                }
            } else if (MQTTService.ACTION_DATA_AVAILABLE.equals(action)) {  //接收到数据
                String topic = intent.getStringExtra(MQTTService.EXTRA_DATA_TOPIC);
                String str = intent.getStringExtra(MQTTService.EXTRA_DATA_CONTENT);
                Log.d(Tag, "RECV DATA,topic:" + topic + ",content:" + str);
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
