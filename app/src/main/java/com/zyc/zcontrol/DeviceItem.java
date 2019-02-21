package com.zyc.zcontrol;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;


public class DeviceItem {

    public DeviceItem(Context context,String name)
    {
        this.context=context;
        this.name=name;
    }

    public DeviceItem(Context context, String name, @DrawableRes int resId)
    {
        this.context=context;
        this.name=name;
        this.Icon=context.getResources().getDrawable(resId);
    }

    private Context context;
    public String name;
    public Drawable Icon=null;
    public int Group;


    public void setIcon(@DrawableRes int resId)
    {
        this.Icon=context.getResources().getDrawable(resId);
    }

}
