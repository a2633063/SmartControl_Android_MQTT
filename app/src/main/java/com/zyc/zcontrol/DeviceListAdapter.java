package com.zyc.zcontrol;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

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
        convertView.setBackgroundColor(0x10000000);
        else convertView.setBackgroundColor(0xffffffff);
        holder.tv.setText(mdata.get(position).name);
        if (mdata.get(position).Icon != null) {
            holder.im.setImageDrawable(mdata.get(position).Icon);
            holder.im.setVisibility(View.VISIBLE);
        } else {
            holder.im.setVisibility(View.INVISIBLE);
        }


//        holder.tv.setText(((Map<String, Object>) mdata.get(position)).get("name").toString());

        return convertView;
    }

    public void setChoice(int c)
    {
        if(c>=getCount() || c<0) c=-1;
        this.choice=c;
        this.notifyDataSetChanged();
    }
    public int getChoice()
    {
        if(choice>=getCount() || choice<0) return -1;
        return choice;
    }

    class ViewHolder {
        ImageView im;
        TextView tv;
    }
}
