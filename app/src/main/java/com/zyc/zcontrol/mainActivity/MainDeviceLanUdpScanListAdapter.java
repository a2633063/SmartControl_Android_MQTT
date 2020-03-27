package com.zyc.zcontrol.mainActivity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.zyc.zcontrol.R;
import com.zyc.zcontrol.deviceItem.DeviceClass.Device;

import java.util.List;

public class MainDeviceLanUdpScanListAdapter extends BaseAdapter {

    private Context context;
    private List<Device> mdata;
    private LayoutInflater inflater;

    public MainDeviceLanUdpScanListAdapter(Context context, List data) {
        this.context = context;
        this.mdata = data;
        inflater = LayoutInflater.from(context);
    }

    public int getCount() {
        return mdata.size();
    }

    public List<Device> getData() {
        return mdata;
    }

    public Device getItem(int position) {
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
            convertView = inflater.inflate(R.layout.list_item_main_device_udp_scan_list, null);
            holder = new ViewHolder();

            holder.name = convertView.findViewById(R.id.textView);
            holder.mac = convertView.findViewById(R.id.tv_mac);
            holder.im = convertView.findViewById(R.id.imageView);

            convertView.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }

        holder.name.setText(mdata.get(position).getName());
        holder.mac.setText(mdata.get(position).getMac());
        holder.im.setImageResource(mdata.get(position).getIcon());
        holder.im.setVisibility(View.VISIBLE);


//        holder.tv.setText(((Map<String, Object>) mdata.get(position)).get("name").toString());

        return convertView;
    }

    public void add(Device d) {
        mdata.add(d);
        this.notifyDataSetChanged();
    }

    public int contains(Device d) {
        for (int i = 0; i < mdata.size(); i++) {
            if (mdata.get(i).getMac().equalsIgnoreCase(d.getMac()))
                return i;
        }
        return -1;
    }

    public int contains(String mac) {
        if (mac == null || mac.length() == 0) return -1;
        for (int i = 0; i < mdata.size(); i++) {
            if (mdata.get(i).getMac().equalsIgnoreCase(mac))
                return i;
        }
        return -1;
    }

    class ViewHolder {
        ImageView im;
        TextView name;
        TextView mac;
    }
}
