package com.zyc.zcontrol;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    //region 使用本地广播与service通信
    LocalBroadcastManager localBroadcastManager;
    private MsgReceiver msgReceiver;
    //endregion


    Button startServiceButton;// 启动服务按钮
    Button shutDownServiceButton;// 关闭服务按钮
    Button startBindServiceButton;// 启动绑定服务按钮
    Button startunBindServiceButton;// 启动绑定服务按钮

    MQTTService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //region 侧边栏 悬浮按钮初始化
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        //endregion


        //region 动态注册广播接收器
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.zyc.zcontrol.MQTTRECEIVER");
        localBroadcastManager.registerReceiver(msgReceiver, intentFilter);
        //endregion

        startServiceButton = (Button) findViewById(R.id.startServerButton);
        startBindServiceButton = (Button) findViewById(R.id.startBindServerButton);
        shutDownServiceButton = (Button) findViewById(R.id.sutdownServerButton);
        startunBindServiceButton = (Button) findViewById(R.id.startunBindServerButton);

        Intent intent = new Intent(MainActivity.this, MQTTService.class);
        intent.putExtra("mqtt_uri", "tcp://47.112.16.98:1883");
        intent.putExtra("mqtt_id", "asdf");
        intent.putExtra("mqtt_user", "z");
        intent.putExtra("mqtt_password", "2633063");
        startService(intent);

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

                //发送Action为com.zyc.zcontrol.MQTTRECEIVER的广播
                Intent intent = new Intent("com.zyc.zcontrol.MQTTSEND");
                intent.putExtra("string", "123");
                localBroadcastManager.sendBroadcast(intent);
//                if (mService != null)
//                    Log.d("MainActivity", "int:" + mService.getConunt());
//                else
//                    Toast.makeText(MainActivity.this, "null!", Toast.LENGTH_SHORT).show();
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
        getMenuInflater().inflate(R.menu.main, menu);
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //region 广播接收,用于处理接收到的数据
    public class MsgReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //拿到进度，更新UI
            String progress = intent.getStringExtra("string");
            Log.d("MainActivity", "MsgReceiver:" + progress);
        }
    }
    //endregion
}
