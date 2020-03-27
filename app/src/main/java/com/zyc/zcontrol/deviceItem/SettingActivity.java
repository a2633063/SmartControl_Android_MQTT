package com.zyc.zcontrol.deviceItem;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.zyc.zcontrol.MainApplication;
import com.zyc.zcontrol.R;
import com.zyc.zcontrol.SettingFragment;

import static com.zyc.zcontrol.deviceItem.DeviceClass.Device.TYPE_UNKNOWN;

public class SettingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        int type = TYPE_UNKNOWN;
        String name = null;
        String mac = null;

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        PreferenceFragment prefFragment = null;

        Intent intent = this.getIntent();
        if (intent.hasExtra("index"))//判断是否有值传入,并判断是否有特定key
        {
            try {
                int index = intent.getIntExtra("index", -1);
                prefFragment = (((MainApplication) getApplication()).getDeviceList()).get(index).getSettingFragment();
            } catch (Exception e) {
                e.printStackTrace();
                prefFragment = null;
            }
        }

        if (prefFragment == null)
            prefFragment = new SettingFragment();
        //加载PrefFragment
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
//        prefFragment = new ButtonSettingFragment(deviceNum);
        transaction.add(R.id.SettingFragment, prefFragment);
        transaction.commit();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
