package com.zyc;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import com.zyc.zcontrol.MainActivity;
import com.zyc.zcontrol.R;
import com.zyc.zcontrol.deviceItem.DeviceClass.Device;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceA1;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceButtonMate;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceC1;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceClock;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceClockMatrix;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceDC1;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceKey51;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceM1;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceMOPS;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceRGBW;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceS7;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceTC1;
import com.zyc.zcontrol.deviceItem.DeviceClass.DeviceUartToMqtt;

public class Function {


    public static String getSSID(Context context) {
        String ssid = "unknown id";
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1) {

            ConnectivityManager connManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            assert connManager != null;
            NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
            if (networkInfo.isConnected()) {
                if (networkInfo.getExtraInfo() != null) {
                    return networkInfo.getExtraInfo().replace("\"", "");
                }
            }
        } else //if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O || Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
        {
            WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            assert mWifiManager != null;
            WifiInfo info = mWifiManager.getConnectionInfo();
            return info.getSSID().replace("\"", "");

        }
        return ssid;
    }

    public static String getWeek(int repeat) {
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

    public static String getLocalVersionName(Context ctx) {
        String localVersion = "获取app版本失败";
        try {
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            localVersion = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return localVersion;

    }

    public static void createShortCut(Context context, String mac, String name) {
        //创建Intent对象

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);
            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
                Intent intent = new Intent(context, MainActivity.class);
                intent.setAction("com.zyc.zcontrol.MainActivity");
                //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("mac", mac);
                ShortcutInfo info = new ShortcutInfo.Builder(context, mac)
                        .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher_round))
                        .setShortLabel(name)
                        .setIntent(intent)
                        .build();

                //当添加快捷方式的确认弹框弹出来时，将被回调CallBackReceiver里面的onReceive方法
                PendingIntent shortcutCallbackIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, CallBackReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
                shortcutManager.requestPinShortcut(info, shortcutCallbackIntent.getIntentSender());
            }
        } else {
            //region android8以下版本

            Intent shortcutIntent = new Intent();
            shortcutIntent.setComponent(new ComponentName(context.getPackageName(), "com.zyc.zcontrol.MainActivity"));
            shortcutIntent.putExtra("mac", mac);

            //设置点击快捷方式，进入指定的Activity
            Intent resultIntent = new Intent();
            //设置快捷方式的图标
            resultIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_launcher_round));
            resultIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);//启动的Intent
            resultIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);//设置快捷方式的名称
            resultIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");//设置Action
            context.sendBroadcast(resultIntent);
            //endregion
        }
    }

    class CallBackReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("test", "onReceive: 固定快捷方式的回调");
        }
    }


    public static Device returnDeviceClass(String name, String mac, int type) {
        switch (type) {
            case Device.TYPE_BUTTON_MATE:
                return new DeviceButtonMate(name, mac);
            case Device.TYPE_TC1:
                return new DeviceTC1(name, mac);
            case Device.TYPE_DC1:
                return new DeviceDC1(name, mac);
            case Device.TYPE_S7:
                return new DeviceS7(name, mac);
            case Device.TYPE_A1:
                return new DeviceA1(name, mac);
            case Device.TYPE_M1:
                return new DeviceM1(name, mac);
            case Device.TYPE_RGBW:
                return new DeviceRGBW(name, mac);
            case Device.TYPE_CLOCK:
                return new DeviceClock(name, mac);
            case Device.TYPE_MOPS:
                return new DeviceMOPS(name, mac);
            case Device.TYPE_CLOCK_MATRIX:
                return new DeviceClockMatrix(name, mac);
            case Device.TYPE_KEY51:
                return new DeviceKey51(name, mac);
            case Device.TYPE_C1:
                return new DeviceC1(name, mac);
            case Device.TYPE_UARTTOMQTT:
                return new DeviceUartToMqtt(name, mac);
        }
        return null;
    }

    public static String getMacString(int[] mac) {
        if (mac.length != 6) return null;
        return Integer.toHexString(mac[0]) + ":"
                + Integer.toHexString(mac[1]) + ":"
                + Integer.toHexString(mac[2]) + ":"
                + Integer.toHexString(mac[3]) + ":"
                + Integer.toHexString(mac[4]) + ":"
                + Integer.toHexString(mac[5]);
    }
}
