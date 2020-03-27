package com.zyc.zcontrol;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.zyc.devicesort.SortRecyclerAdapter;
import com.zyc.devicesort.SortRecyclerItemTouchHelper;
import com.zyc.devicesort.SortRecyclerViewSpacesItemDecoration;
import com.zyc.zcontrol.deviceItem.DeviceClass.Device;

import java.util.ArrayList;
import java.util.List;

import static com.zyc.zcontrol.ConnectService.ACTION_MAINACTIVITY_DEVICELISTUPDATE;

public class DeviceSortActivity extends AppCompatActivity {
    public final static String Tag = "DeviceSortActivity";

    List<Device> mData;
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
        mData = ((MainApplication) getApplication()).getDeviceList();

        if (mData.size() > 1 && mData.get(0).getMac().equals("000000000000")) {
            mData.remove(0);
            Toast.makeText(this, "已删除演示设备", Toast.LENGTH_SHORT).show();
        }

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcast(new Intent(ACTION_MAINACTIVITY_DEVICELISTUPDATE));

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
//        //删除数据库所有内容,根据排序重新写入
//        SQLiteClass sqLite = new SQLiteClass(DeviceSortActivity.this, "device_list");
//        sqLite.Delete("device_list", null, null);
//
//        //SQLiteClass sqLite = new SQLiteClass(this, "device_list");
//        for (int i = 0; i < mData.size(); i++) {
//            Device d = mData.get(i);
//            ContentValues cv = new ContentValues();
//            cv.put("name", d.getName());
//            cv.put("type", d.getType());
//            cv.put("mac", d.getMac());
//            cv.put("sort", i);
//            sqLite.Insert("device_list", cv);
//        }
//        Intent intent = new Intent(DeviceSortActivity.this, MainActivity.class);
//        startActivity(intent);
        finish();
    }
}
