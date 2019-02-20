package com.zyc.zcontrol;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    public final static String Tag =
            "MainActivity";


    //region 使用本地广播与service通信
    LocalBroadcastManager localBroadcastManager;
    private MsgReceiver msgReceiver;
    //endregion


    Button startServiceButton;// 启动服务按钮
    Button shutDownServiceButton;// 关闭服务按钮
    Button startBindServiceButton;// 启动绑定服务按钮
    Button startunBindServiceButton;// 启动绑定服务按钮

    MQTTService mMQTTService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //region 侧边栏 悬浮按钮初始化
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        //endregion


        //region 动态注册接收mqtt服务的广播接收器,
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MQTTService.ACTION_MQTT_CONNECTED);
        intentFilter.addAction(MQTTService.ACTION_MQTT_DISCONNECTED);
        intentFilter.addAction(MQTTService.ACTION_DATA_AVAILABLE);
        localBroadcastManager.registerReceiver(msgReceiver, intentFilter);
        //endregion

        //region 启动MQTT服务
        Intent intent = new Intent(MainActivity.this, MQTTService.class);
        startService(intent);

        bindService(intent, mMQTTServiceConnection, BIND_AUTO_CREATE);

        //endregion

        startServiceButton = (Button) findViewById(R.id.startServerButton);
        startBindServiceButton = (Button) findViewById(R.id.startBindServerButton);
        shutDownServiceButton = (Button) findViewById(R.id.sutdownServerButton);
        startunBindServiceButton = (Button) findViewById(R.id.startunBindServerButton);
        //region 单击按钮时启动服务
        startServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("MainActivity", "start Service");
            }
        });
        //endregion


        //region 测试按钮
        findViewById(R.id.Button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMQTTService.Send("/test/Androdi", "test message", 0);
                //发送Action为com.zyc.zcontrol.MQTTRECEIVER的广播
            }
        });
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

        super.onDestroy();
    }

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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

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
                    mMQTTService.connect("tcp://47.112.16.98:1883", "mqtt_id_dasdf",
                            "z", "2633063");
                }
            } else if (MQTTService.ACTION_DATA_AVAILABLE.equals(action)) {  //接收到数据
                String topic = intent.getStringExtra(MQTTService.EXTRA_DATA_TOPIC);
                String str = intent.getStringExtra(MQTTService.EXTRA_DATA_CONTENT);
                Log.d(Tag, "RECV DATA,topic:" + topic + ",content:" + str);
            }
        }
    }
    //endregion

}
