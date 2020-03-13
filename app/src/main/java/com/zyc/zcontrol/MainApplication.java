package com.zyc.zcontrol;

import android.app.Application;

import com.zyc.zcontrol.deviceItem.DeviceClass.Device;

import java.util.ArrayList;

public class MainApplication extends Application {
    public ArrayList<Device> getDeviceList() {
        return mData;
    }

    private ArrayList<Device> mData = new ArrayList<>();
}