package com.zyc.zcontrol.controlItem;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;

import android.preference.PreferenceFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import com.zyc.StaticVariable;

import com.zyc.zcontrol.controlItem.buttonmate.ButtonMateSettingFragment;
import com.zyc.zcontrol.R;
public class SettingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        int type=StaticVariable.TYPE_UNKNOWN;
        String name=null;
        String mac=null;
        PreferenceFragment prefFragment=null;

        Intent intent = this.getIntent();
        if (intent.hasExtra("name") && intent.hasExtra("type")
                && intent.hasExtra("mac"))//判断是否有值传入,并判断是否有特定key
        {
            type=intent.getIntExtra("type",StaticVariable.TYPE_UNKNOWN);
            name=intent.getStringExtra("name");
            mac=intent.getStringExtra("mac");

            switch(type)
            {
                case StaticVariable.TYPE_UNKNOWN:prefFragment=null;break;
                case StaticVariable.TYPE_BUTTON_MATE:prefFragment=new ButtonMateSettingFragment(name,mac);
                    break;
            }
        }

        if(prefFragment==null)
            prefFragment=new SettingFragment();
        //加载PrefFragment
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
//        prefFragment = new ButtonSettingFragment(deviceNum);
        transaction.add(R.id.SettingFragment, prefFragment);
        transaction.commit();
    }
}
