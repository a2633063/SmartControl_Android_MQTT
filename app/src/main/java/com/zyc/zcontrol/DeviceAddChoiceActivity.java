package com.zyc.zcontrol;

import android.content.Context;
import android.content.Intent;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import com.espressif.ESPtouchActivity;
import com.zyc.StaticVariable;

import java.util.List;

public class DeviceAddChoiceActivity extends AppCompatActivity {
    public final static String Tag = "DeviceAddChoiceActivity";

    ListView lv_device;
    DeviceChoiceListAdapter adapter;

    int device_type = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_add_choice);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
//            actionBar.setTitle(StaticVariable.Item_PC_Text);
        }

        //region listview及adapter

        lv_device = findViewById(R.id.lv_device);
        adapter = new DeviceChoiceListAdapter(DeviceAddChoiceActivity.this);

        lv_device.setAdapter(adapter);
        lv_device.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                device_type = position;
                Intent intent = new Intent(DeviceAddChoiceActivity.this, ESPtouchActivity.class);
                startActivityForResult(intent, 1);
            }
        });
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != RESULT_OK) return;
        if (requestCode == 1) {
            String ip = data.getExtras().getString("ip");
            String mac = data.getExtras().getString("mac");

            //返回数据
            Intent intent = new Intent();
            intent.putExtra("type", device_type);
            intent.putExtra("ip", ip);
            intent.putExtra("mac", mac);
            setResult(RESULT_OK, intent);

            finish();
            Log.e(Tag, "get device result:" + ip + "," + mac + "," + device_type);
        }
    }


    class DeviceChoiceListAdapter extends BaseAdapter {

        private Context context;
        //        private List<DeviceItem> mdata;
        private LayoutInflater inflater;

        public DeviceChoiceListAdapter(Context context) {
            this.context = context;

            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return StaticVariable.TYPE_NAME.length;
        }

        @Override
        public String getItem(int position) {
            return StaticVariable.TYPE_NAME[position];
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

            holder.tv.setText(StaticVariable.TYPE_NAME[position]);
            holder.im.setImageResource(StaticVariable.TYPE_ICON[position]);

            return convertView;
        }

        class ViewHolder {
            ImageView im;
            TextView tv;
        }
    }
}
