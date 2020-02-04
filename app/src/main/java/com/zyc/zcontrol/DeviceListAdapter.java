package com.zyc.zcontrol;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

class DeviceListAdapter extends BaseAdapter {

    private Context context;
    private List<DeviceItem> mdata;
    private LayoutInflater inflater;
    private int choice=-1;

    public DeviceListAdapter(Context context, List data) {
        this.context = context;
        this.mdata = data;
        inflater = LayoutInflater.from(context);
    }

    public int getCount() {
        return mdata.size();
    }

    public DeviceItem getItem(int position) {
        return mdata.get(position);
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position1, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        View view = null;
        final int position = position1;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_main_device_list, null);
            holder = new ViewHolder();

            holder.tv = convertView.findViewById(R.id.textView);
            holder.im = convertView.findViewById(R.id.imageView);


            convertView.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }
        if(choice==position)
        convertView.setBackgroundColor(0x20ffffff);
        else convertView.setBackgroundColor(0x00000000);
        holder.tv.setText(mdata.get(position).name);
        if (mdata.get(position).getIcon() != null) {
            holder.im.setImageDrawable(mdata.get(position).getIcon());
            holder.im.setVisibility(View.VISIBLE);
        } else {
            holder.im.setVisibility(View.INVISIBLE);
        }


//        holder.tv.setText(((Map<String, Object>) mdata.get(position)).get("name").toString());

        return convertView;
    }

    public void add(DeviceItem d)
    {
        mdata.add(d);
        this.notifyDataSetChanged();
    }
    public void setChoice(int c)
    {
        if(c>=getCount() || c<0) c=-1;
        this.choice=c;
        this.notifyDataSetChanged();
    }
    public int getChoice()
    {
        if(mdata.size()<1) return -1;
        else if(choice>=getCount() || choice<0)
            return -1;
        return choice;
    }
    public DeviceItem getChoiceDevice()
    {
        if(choice>=getCount() || choice<0) {
            if (mdata.size() > 0) return mdata.get(0);
            else return null;
        }
        return mdata.get(choice);
    }
    public int contains(DeviceItem d)
    {
        for(int i=0;i<mdata.size();i++)
        {
            if(mdata.get(i).mac.equalsIgnoreCase(d.mac))
                return i;
        }
        return -1;
    }

    public int contains(String mac)
    {
        if(mac==null ||mac.length()==0) return -1;
        for(int i=0;i<mdata.size();i++)
        {
            if(mdata.get(i).mac.equalsIgnoreCase(mac))
                return i;
        }
        return -1;
    }

    class ViewHolder {
        ImageView im;
        TextView tv;
    }
}
