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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.cardview.widget.CardView;

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
import com.zyc.zcontrol.deviceItem.DeviceClass.Devicez863key;

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
        String[] Week = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
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

    public static void createShortCut(Context context, String mac, String name, @DrawableRes int resId) {
        //创建Intent对象

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);
            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
                Intent intent = new Intent(context, MainActivity.class);
                intent.setAction("com.zyc.zcontrol.MainActivity");
                //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("mac", mac);
                ShortcutInfo info = new ShortcutInfo.Builder(context, mac)
                        .setIcon(Icon.createWithResource(context, resId))
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
            case Device.TYPE_Z863KEY:
                return new Devicez863key(name, mac);
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

    public static void ShowColorSelectInit(View parent, View view, Bitmap bitmap) {
        if (parent == null) return;
        if (view == null) return;
        if (bitmap == null) return;
        ImageView imageView = parent.findViewById(R.id.imageView);
        CardView color_set = parent.findViewById(R.id.color_set);
        color_set.setVisibility(View.GONE);
        imageView.setImageBitmap(bitmap);
        imageView.setTag(bitmap);
    }

    public static int ShowColorSelect(View obj,View parent, View view, MotionEvent motionEvent) {
        if (parent == null) return -1;
        ImageView imageView = parent.findViewById(R.id.imageView);
        CardView color_set = parent.findViewById(R.id.color_set);

        if (imageView == null) return -1;
        if (color_set == null) return -1;
        Bitmap bitmap = (Bitmap) imageView.getTag();
        if (bitmap == null) return -1;

        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            view.setVisibility(View.VISIBLE);
        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP
                || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
            view.setVisibility(View.GONE);
        }

        //imageView.getParent().getParent().getParent().getParent().requestDisallowInterceptTouchEvent(true);
        //imageView.getParent().getParent().getParent().requestDisallowInterceptTouchEvent(true);
        int[] location_obj = new int[2] ;
        int[] location_image = new int[2] ;
        obj.getLocationInWindow(location_obj);
        imageView.getLocationInWindow(location_image);
        int x = (int) motionEvent.getX()+location_obj[0]-location_image[0];
        int y = (int) motionEvent.getY()+location_obj[1]-location_image[1];

        if (x < 0 || y < 0 || y > imageView.getHeight() || x > imageView.getWidth()) {
            return -1;
        }
        try {
            color_set.setX(x + imageView.getX() - color_set.getWidth() / 2);
            color_set.setY(y + imageView.getY() - color_set.getHeight() / 2);
            color_set.setVisibility(View.VISIBLE);

            int pixel = bitmap.getPixel(x*bitmap.getWidth()/imageView.getWidth(), y*bitmap.getHeight()/imageView.getHeight());//获取颜色
            ((CardView) color_set.getChildAt(0)).setCardBackgroundColor(pixel);
            return pixel&0xffffff;
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Log.d("function",(x + imageView.getX() - color_set.getWidth() / 2)+","+(y + imageView.getY() - color_set.getHeight() / 2));

        return -1;
    }
}
