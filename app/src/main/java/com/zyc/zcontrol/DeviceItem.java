package com.zyc.zcontrol;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v4.app.Fragment;

import com.zyc.zcontrol.controlItem.buttonmate.ButtonMateFragment;


public class DeviceItem {
    final static int TYPE_NULL=-1;
    final static int TYPE_BUTTON_MATE=1;
    final static int TYPE_BUTTON=2;
    final static int TYPE_RGB=3;
    final static int TYPE_CLOCK=4;



    public DeviceItem(Context context,int type,String name)
    {
        this.context=context;
        this.name=name;
        this.type=type;
        init();
    }

    public DeviceItem(Context context,int type, String name, @DrawableRes int resId)
    {
        this.context=context;
        this.name=name;
        this.type=type;
        this.Icon=context.getResources().getDrawable(resId);
        init();
    }

    private Context context;
    public String name;
    public Drawable Icon=null;
    public int Group;
    public Fragment fragment=null;
    int type=-1;

    private void init()
    {
        switch(type)
        {
            case TYPE_NULL:
//                throw
                break;
            case TYPE_BUTTON_MATE:
                fragment=new ButtonMateFragment();
                break;
        }
    }

    public void setIcon(@DrawableRes int resId)
    {
        this.Icon=context.getResources().getDrawable(resId);
    }



}
