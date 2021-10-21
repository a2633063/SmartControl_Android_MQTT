package com.zyc.zcontrol.deviceItem.key51;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Switch;
import android.widget.TextView;

import com.zyc.zcontrol.R;


import java.util.List;

class Key51TaskListAdapter extends BaseAdapter {

    private Context context;
    private List<TaskItem> mdata;
    private LayoutInflater inflater;
    private int choice = -1;
    private Callback mCallback = null;

    public Key51TaskListAdapter(Context context, List data) {
        this.context = context;
        this.mdata = data;
        inflater = LayoutInflater.from(context);
    }

    public Key51TaskListAdapter(Context context, List data, Callback callback) {
        this.context = context;
        this.mdata = data;
        inflater = LayoutInflater.from(context);
        mCallback = callback;
    }

    /**
     * 自定义接口，回调点击事件到Activity
     */
    public interface Callback {
        public void click(View v, int position);
    }


    public int getCount() {
        return mdata.size();
    }

    public TaskItem getItem(int position) {
        return mdata.get(position);
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(final int position1, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        View view = null;
        final int position = position1;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.key51_list_item_task_list, null);
            holder = new ViewHolder();
            holder.tv_name = convertView.findViewById(R.id.tv_name);
            holder.tv_key = convertView.findViewById(R.id.tv_key);
            holder.on = convertView.findViewById(R.id.sw_on);
            convertView.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }
//        if (choice == position)
//            convertView.setBackgroundColor(0x20ffffff);
//        else convertView.setBackgroundColor(0x00000000);
        holder.tv_name.setText(mdata.get(position).name);
        holder.tv_key.setText(mdata.get(position).key+"");

        holder.on.setChecked(mdata.get(position).getOn());

        if (mCallback!=null){
            holder.on.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCallback.click(v,position);
                }
            });
        }
        return convertView;
    }

    class ViewHolder {
        TextView tv_name;
        TextView tv_key;
        Switch on;
    }
}
