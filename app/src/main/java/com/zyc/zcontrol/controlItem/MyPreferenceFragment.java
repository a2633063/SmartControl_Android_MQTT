package com.zyc.zcontrol.controlItem;


import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

public class MyPreferenceFragment extends PreferenceFragment implements AdapterView.OnItemLongClickListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(getActivity());
        setPreferenceScreen(root);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = super.onCreateView(inflater, container, savedInstanceState);
        if (result != null) {
            View lv = result.findViewById (android.R.id.list);
            if (lv instanceof ListView) {
                ((ListView)lv).setOnItemLongClickListener(this);
            }
        }
        return result;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return false;
    }

//    private static abstract class LongClickableCheckBoxPreference extends CheckBoxPreference implements View.OnLongClickListener {
//        public LongClickableCheckBoxPreference(Context context) {
//            super(context);
//        }
//    }
}