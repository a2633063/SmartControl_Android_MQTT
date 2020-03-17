package com.zyc.zcontrol.deviceItem.DeviceClass;


import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

public class SettingFragment extends PreferenceFragment implements AdapterView.OnItemLongClickListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(getActivity());
        setPreferenceScreen(root);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = super.onCreateView(inflater, container, savedInstanceState);
        //region 长按功能
        if (result != null) {
            View lv = result.findViewById (android.R.id.list);
            if (lv instanceof ListView) {
                ((ListView)lv).setOnItemLongClickListener(this);
            }
        }
        //endregion
        return result;
    }


    //region 配置长按功能
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return false;
    }
    //endregion

}