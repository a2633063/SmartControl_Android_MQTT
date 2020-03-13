package com.zyc.zcontrol;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import android.util.Log;
import android.view.MenuItem;

import com.zyc.devicesort.SortRecyclerAdapter;
import com.zyc.devicesort.SortRecyclerItemTouchHelper;
import com.zyc.devicesort.SortRecyclerViewSpacesItemDecoration;
import com.zyc.zcontrol.deviceItem.DeviceClass.Device;

import java.util.ArrayList;
import java.util.List;

public class DeviceSortActivity extends AppCompatActivity {
    public final static String Tag = "DeviceSortActivity";

    List<Device> mData = new ArrayList<>();
    private SortRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_sort);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
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

            mData.add(new Device(type, name, mac));
        }

//        if (mData.size() < 1) {
////            deviceData.add(new Device(MainActivity.this, StaticVariable.TYPE_M1, "演示设备", "b0f8932234f4"));
//            mData.add(new Device(MainActivity.this, StaticVariable.TYPE_TC1, "演示设备", "000000000000"));
//        }
        //endregion

        //region 侧边RecyclerView初始化
        RecyclerView sideRecyclerView = findViewById(R.id.side_recyclerView);
        LinearLayoutManager sideLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
//      GridLayoutManager layoutManager=new GridLayoutManager(this,3,GridLayoutManager.VERTICAL,false);
        adapter = new SortRecyclerAdapter(mData);
        sideRecyclerView.setLayoutManager(sideLayoutManager);
        sideRecyclerView.setAdapter(adapter);
        // 设置RecyclerView Item边距
        sideRecyclerView.addItemDecoration(new SortRecyclerViewSpacesItemDecoration(10, 10, 10, 20));
        //设置长按拖动排序
        ItemTouchHelper sideHelper = new ItemTouchHelper(new SortRecyclerItemTouchHelper(adapter));
        sideHelper.attachToRecyclerView(sideRecyclerView);

        //endregion

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                SaveExit();
//                Intent intent = new Intent(DeviceSortActivity.this, MainActivity.class);
//                startActivity(intent);
//                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        SaveExit();
        //super.onBackPressed();

    }

    void SaveExit() {
        //删除数据库所有内容
        SQLiteClass sqLite = new SQLiteClass(DeviceSortActivity.this, "device_list");
        sqLite.Delete("device_list", null, null);

        //SQLiteClass sqLite = new SQLiteClass(this, "device_list");
        for (int i = 0; i < mData.size(); i++) {
            Device d = mData.get(i);
            ContentValues cv = new ContentValues();
            cv.put("name", d.getName());
            cv.put("type", d.getType());
            cv.put("mac", d.getMac());
            cv.put("sort", i);
            sqLite.Insert("device_list", cv);
        }
        Intent intent = new Intent(DeviceSortActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
