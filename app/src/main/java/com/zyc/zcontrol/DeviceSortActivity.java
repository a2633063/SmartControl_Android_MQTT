package com.zyc.zcontrol;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import android.view.MenuItem;

import com.zyc.devicesort.SortRecyclerAdapter;
import com.zyc.devicesort.SortRecyclerItemTouchHelper;
import com.zyc.devicesort.SortRecyclerViewSpacesItemDecoration;
import com.zyc.zcontrol.deviceItem.DeviceClass.Device;

import java.util.List;

public class DeviceSortActivity extends AppCompatActivity {
    public final static String Tag = "DeviceSortActivity";

    List<Device> mData;
    private SortRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_activity_device_sort);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        //region 数据库初始化
        mData = ((MainApplication) getApplication()).getDeviceList();

//        if (mData.size() > 1 && mData.get(0).getMac().equals("000000000000")) {
//            mData.remove(0);
//            Toast.makeText(this, "已删除演示设备", Toast.LENGTH_SHORT).show();
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
        ItemTouchHelper sideHelper = new ItemTouchHelper(new SortRecyclerItemTouchHelper(this,adapter));
        sideHelper.attachToRecyclerView(sideRecyclerView);

        //endregion

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
//                Intent intent = new Intent(DeviceSortActivity.this, MainActivity.class);
//                startActivity(intent);
//                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
