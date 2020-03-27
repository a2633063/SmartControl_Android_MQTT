package com.zyc.zcontrol.mainActivity;

import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerAdapter;

import com.zyc.zcontrol.deviceItem.DeviceClass.Device;

import java.util.ArrayList;

public class MainDeviceFragmentAdapter extends FragmentPagerAdapter {

    private ArrayList<Device> data;

    public MainDeviceFragmentAdapter(FragmentManager fm, ArrayList<Device> fragmentArray) {
        this(fm);
        this.data = fragmentArray;
    }

    public MainDeviceFragmentAdapter(FragmentManager fm) {
        super(fm);
    }

    //这个函数的作用是当切换到第arg0个页面的时候调用。
    @Override
    public Fragment getItem(int arg0) {
        return this.data.get(arg0).getFragment();
    }

    @Override
    public long getItemId(int position) {
        return data.get(position).getFragment().hashCode();
    }

    @Override
    public int getItemPosition(Object object) {
        for (int i = 0; i < data.size(); i++) {
            if (object.hashCode() == data.get(i).getFragment().hashCode()) return i;
        }
        return PagerAdapter.POSITION_NONE;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        //int viewPagerId=container.getId();
        //makeFragmentName(viewPagerId,getItemId(position));
        return super.instantiateItem(container, position);
    }

    @Override
    public int getCount() {
        return this.data.size();
    }

    //重写这个方法，将设置每个Tab的标题
    @Override
    public CharSequence getPageTitle(int position) {
        if (data != null)
            return data.get(position).getName();
        else return "";
    }


}
