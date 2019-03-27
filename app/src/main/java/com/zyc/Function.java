package com.zyc;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

public class Function {


    public static String getSSID(Context context)
    {
        String ssid="unknown id";

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O||Build.VERSION.SDK_INT== Build.VERSION_CODES.P) {

            WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            assert mWifiManager != null;
            WifiInfo info = mWifiManager.getConnectionInfo();

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                return info.getSSID();
            } else {
                return info.getSSID().replace("\"", "");
            }
        } else if (Build.VERSION.SDK_INT==Build.VERSION_CODES.O_MR1){

            ConnectivityManager connManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            assert connManager != null;
            NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
            if (networkInfo.isConnected()) {
                if (networkInfo.getExtraInfo()!=null){
                    return networkInfo.getExtraInfo().replace("\"","");
                }
            }
        }
        return ssid;
    }
    public static String getWeek(int repeat){
        String str = "";
        String Week[] = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        repeat &= 0x7f;
        if (repeat == 0) str = "一次";
        else if ((repeat & 0x7f) == 0x7f) str = "每天";
        else {
            for (int i = 0; i < 7; i++) {
                if ((repeat & (1 << i)) != 0) {
                    str = str + "," + Week[i];
                }
            }
            str = str.replaceFirst(",", "");
        }

        return str;
    }
}
