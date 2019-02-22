package com.zyc.zcontrol;

import android.content.Context;
import android.content.Intent;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.zyc.StaticVariable;

import java.util.List;

public class DeviceAddChoiceActivity extends AppCompatActivity {

    ListView lv_device;
    DeviceChoiceListAdapter adapter;

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
                //返回数据
                Intent intent = new Intent();
                intent.putExtra("type", 1);
                intent.putExtra("ip", "255.255.255.255");
                intent.putExtra("mac", "12:34:56:78:90");
                setResult(RESULT_OK, intent);
                finish();
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
    protected void onDestroy() {

        super.onDestroy();
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
