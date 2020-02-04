package com.zyc.zcontrol;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v4.app.Fragment;

import com.zyc.StaticVariable;
import com.zyc.zcontrol.controlItem.a1.A1Fragment;
import com.zyc.zcontrol.controlItem.buttonmate.ButtonMateFragment;
import com.zyc.zcontrol.controlItem.dc1.DC1Fragment;
import com.zyc.zcontrol.controlItem.m1.M1Fragment;
import com.zyc.zcontrol.controlItem.rgbw.RGBWFragment;
import com.zyc.zcontrol.controlItem.tc1.TC1Fragment;


public class DeviceItem {

    public DeviceItem(Context context, int type, String name, String mac) {
        this.context = context;
        this.name = name;
        this.type = type;
        this.mac = mac;
        init();
    }

    public DeviceItem(Context context, int type, String name, String mac, @DrawableRes int resId) {
        this.context = context;
        this.name = name;
        this.type = type;
        this.Icon = context.getResources().getDrawable(resId);
        this.mac = mac;
        init();
    }

    private Context context;

    public String name;
    public String mac;
    public String ip;

    public Drawable getIcon() {
        if (Icon == null) {
            try {
                setIcon(StaticVariable.TYPE_ICON[type]);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return Icon;
    }

    public Drawable Icon = null;
    public int Group;
    private Fragment fragment = null;
    public int type = -1;

    private void init() {

    }

    public void setIcon(@DrawableRes int resId) {
        this.Icon = context.getResources().getDrawable(resId);
    }

    public Fragment getFragment() {
        if (fragment == null) {
            switch (type) {
                case StaticVariable.TYPE_UNKNOWN:
//                throw
                    break;
                case StaticVariable.TYPE_BUTTON_MATE:
                    fragment = new ButtonMateFragment(name, mac);
                    break;
                case StaticVariable.TYPE_TC1:
                    fragment = new TC1Fragment(name, mac);
                    break;
                case StaticVariable.TYPE_DC1:
                    fragment = new DC1Fragment(name, mac);
                    break;
                case StaticVariable.TYPE_A1:
                    fragment = new A1Fragment(name, mac);
                    break;
                case StaticVariable.TYPE_M1:
                    fragment = new M1Fragment(name, mac);
                    break;
                case StaticVariable.TYPE_RGBW:
                    fragment = new RGBWFragment(name, mac);
                    break;
                default:
                    fragment = new ButtonMateFragment(name, mac);
            }
        }
        return fragment;
    }

}
