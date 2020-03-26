package com.zyc.zcontrol;

import android.app.Application;

import com.zyc.zcontrol.deviceItem.DeviceClass.Device;

import java.util.ArrayList;

public class MainApplication extends Application {
    public ArrayList<Device> getDeviceList() {
        return mData;
    }

    private ArrayList<Device> mData = new ArrayList<>();


    public Device getDevice(String mac) {
        for (int i = 0; i < mData.size(); i++) {
            if (mac.equals(mData.get(i).getMac())) return mData.get(i);
        }
        return null;
    }

    public int getDeviceIndex(String mac) {
        for (int i = 0; i < mData.size(); i++) {
            if (mac.equals(mData.get(i).getMac())) return i;
        }
        return -1;
    }
}