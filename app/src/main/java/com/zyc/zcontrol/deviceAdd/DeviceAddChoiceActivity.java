package com.zyc.zcontrol.deviceAdd;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zyc.zcontrol.MainApplication;
import com.zyc.zcontrol.R;
import com.zyc.zcontrol.deviceItem.DeviceClass.Device;

import java.util.ArrayList;
import java.util.List;

import static com.zyc.zcontrol.deviceItem.DeviceClass.Device.TYPE_COUNT;
import static com.zyc.zcontrol.deviceItem.DeviceClass.Device.TYPE_ICON;
import static com.zyc.zcontrol.deviceItem.DeviceClass.Device.TypeName;


public class DeviceAddChoiceActivity extends AppCompatActivity {
    public final static String Tag = "DeviceAddChoiceActivity";

    ListView lv_device;
    DeviceChoiceListAdapter adapter;
    RecyclerView mdns_recyclerView;
    DeviceAddMdnsAdapter mdnsAdapter;
    int device_type = -1;

    List<String> knownDevice = new ArrayList<>();

    ArrayList<Device> data = new ArrayList<Device>();
    //region Handler
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                //region mdns 刷新显示
                case 1:
                    //handler.removeMessages(1);
                    Device device = (Device) msg.obj;
                    if (!knownDevice.contains(device.getMac())) {
                        data.add(device);
                        if (data.size() > 0)
                            mdnsAdapter.notifyItemInserted(msg.arg1);
                    }
                    break;
                //endregion
                case 2:
                    discoverService(null);
                    break;
            }
        }
    };
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_add_choice);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        //region 获取已经存在的设备mac地址
        ArrayList<Device> deviceList = ((MainApplication) getApplication()).getDeviceList();
        for (int i = 0; i < deviceList.size(); i++)
            knownDevice.add(deviceList.get(i).getMac());
        //endregion

        //region listview及adapter

        lv_device = findViewById(R.id.lv_device);
        adapter = new DeviceChoiceListAdapter(DeviceAddChoiceActivity.this);

        lv_device.setAdapter(adapter);
        lv_device.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //TODO 配对页面设备点击逻辑处理
                device_type = position;
                if (Device.LinkActivity[device_type] != null) {
                    Intent intent = new Intent(DeviceAddChoiceActivity.this, Device.LinkActivity[device_type]);
                    startActivityForResult(intent, 1);
                } else {
                    Toast.makeText(DeviceAddChoiceActivity.this, "此设备无配对页面", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //endregion

        //region 按下获取局域网按钮
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //返回数据
                Intent intent = new Intent();
                intent.putExtra("type", -1);
                intent.putExtra("ip", "255.255.255.255");
                intent.putExtra("mac", "");
                setResult(RESULT_OK, intent);
                finish();

            }
        });
        //endregion
        //region 手动增加按钮
        findViewById(R.id.btn_customer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


//                    String ip ;
//                    String mac;
//
//                    //返回数据
//                    Intent intent = new Intent();
//                    intent.putExtra("type", device_type);
//                    intent.putExtra("ip", ip);
//                    intent.putExtra("mac", mac);
//                    setResult(RESULT_OK, intent);
//
//                    finish();
//                    Log.e(Tag, "customer device result:" + ip + "," + mac + "," + device_type);
            }
        });
        //endregion

        //region mdns扫描函数相关

        //region RecyclerView控件初始化
        mdns_recyclerView = findViewById(R.id.mdns_recyclerView);
        //创建一个layoutManager，这里使用LinearLayoutManager指定为线性，也就可以有ListView这样的效果
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);

        //完成layoutManager设置
        mdns_recyclerView.setLayoutManager(layoutManager);
        //创建IconAdapter的实例同时将iconList传入其构造函数
        mdnsAdapter = new DeviceAddMdnsAdapter(data);
        //完成adapter设置
        mdns_recyclerView.setAdapter(mdnsAdapter);

        mdnsAdapter.setOnItemClickListener(new DeviceAddMdnsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, Device device) {
                Toast.makeText(DeviceAddChoiceActivity.this, device.getName() + "," + device.getMac(), Toast.LENGTH_SHORT).show();
                Log.d(Tag, "ip:" + device.getIp() + "mac:" + device.getMac() + "type:" + device.getType());
                returnActivityDevice(device.getIp(), device.getMac(), device.getType());
            }
        });
        //endregion
        //endregion

        handler.sendEmptyMessageDelayed(2, 200);
//        discoverService(null);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != RESULT_OK) return;
        if (requestCode == 1) {
            String ip = data.getExtras().getString("ip");
            String mac = data.getExtras().getString("mac");
            returnActivityDevice(ip, mac, device_type);
            Log.e(Tag, "get device result:" + ip + "," + mac + "," + device_type);
        }
    }

    @Override
    protected void onDestroy() {
        NsdManager nsdManager = (NsdManager) getApplicationContext().getSystemService(Context.NSD_SERVICE);
        nsdManager.stopServiceDiscovery(nsDicListener); // 关闭网络发现
//        nsdManager.unregisterService(nsRegListener);    // 注销网络服务
        super.onDestroy();
    }

    //region mdns扫描函数相关
    NsdManager.DiscoveryListener nsDicListener;//发现网络服务

    //发现网络服务
    public void discoverService(View view) {
        final NsdManager nsdManager = (NsdManager) getApplicationContext().getSystemService(Context.NSD_SERVICE);
        nsDicListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.w("mdns", "onStopDiscoveryFailed: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.w("mdns", "onStartDiscoveryFailed: " + serviceType);
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.w("mdns", "onServiceLost: " + serviceInfo.toString());
                //Toast.makeText(getApplicationContext(), "Service Lost", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                // 发现网络服务时就会触发该事件
                // 可以通过switch或if获取那些你真正关心的服务
                Log.d("mdns", "onServiceFound: " + serviceInfo.toString());
                nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                    @Override
                    public void onServiceResolved(NsdServiceInfo arg0) {

                        Log.d("连接网络服务", "onServiceResolved: " + arg0.toString());
                        // 可以再这里获取相应网络服务的地址及端口信息，然后决定是否要与之建立连接。
                        // 之后就是一些socket操作了

                        int type = -1;
                        String mac = null;
                        if (arg0.getAttributes().containsKey("type")) {
                            try {
                                type = Integer.parseInt(new String(arg0.getAttributes().get("type")));
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                                type = -1;
                            }
                        }
                        if (arg0.getAttributes().containsKey("mac")) {
                            mac = new String(arg0.getAttributes().get("mac"));
                        }

                        Device d = new Device(type, arg0.getServiceName(), mac);
                        d.setIp(arg0.getHost().getHostAddress());
                        //进程原因,不可直接更新ui
                        //data.add(d);
                        //mdnsAdapter.notifyItemInserted(data.size()-1);

                        Message message = new Message();
                        message.arg1 = data.size() - 1;
                        message.what = 1;
                        message.obj = d;
                        handler.sendMessageDelayed(message, 200);
                    }

                    @Override
                    public void onResolveFailed(NsdServiceInfo arg0, int arg1) {
                        Log.d("连接网络服务", "onResolveFailed: " + arg0.toString() + ":" + arg1);
                    }
                });
                //Toast.makeText(getApplicationContext(), "Service Found", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.d("发现网络服务", "onDiscoveryStopped: " + serviceType);
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.d("发现网络服务", "onDiscoveryStarted: " + serviceType);
            }
        };

        nsdManager.discoverServices("_zcontrol._tcp", NsdManager.PROTOCOL_DNS_SD, nsDicListener);
    }
    //endregion


    void returnActivityDevice(String ip, String mac, int type) {
        //返回数据
        Intent intent = new Intent();
        intent.putExtra("type", type);
        intent.putExtra("ip", ip);
        intent.putExtra("mac", mac);
        setResult(RESULT_OK, intent);

        finish();

    }

    class DeviceChoiceListAdapter extends BaseAdapter {

        private Context context;
        //        private List<Device> mdata;
        private LayoutInflater inflater;

        public DeviceChoiceListAdapter(Context context) {
            this.context = context;

            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return TYPE_COUNT;
        }

        @Override
        public String getItem(int position) {
            return TypeName[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position1, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            View view = null;
            final int position = position1;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.list_item_main_device_list, null);
                holder = new ViewHolder();

                holder.tv = convertView.findViewById(R.id.textView);
                holder.im = convertView.findViewById(R.id.imageView);


                convertView.setTag(holder);
            } else {
                view = convertView;
                holder = (ViewHolder) view.getTag();
            }

            holder.tv.setText(TypeName[position]);
            holder.im.setImageResource(TYPE_ICON[position]);

            return convertView;
        }

        class ViewHolder {
            ImageView im;
            TextView tv;
        }
    }
}
