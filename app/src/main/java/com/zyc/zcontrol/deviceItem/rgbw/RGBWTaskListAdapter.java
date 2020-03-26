package com.zyc.zcontrol.deviceItem.rgbw;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Switch;
import android.widget.TextView;

import com.zyc.zcontrol.R;

import java.util.List;

class RGBWTaskListAdapter extends BaseAdapter {

    private Context context;
    private List<TaskItem> mdata;
    private LayoutInflater inflater;
    private int choice = -1;
    private Callback mCallback = null;

    public RGBWTaskListAdapter(Context context, List data) {
        this.context = context;
        this.mdata = data;
        inflater = LayoutInflater.from(context);
    }

    public RGBWTaskListAdapter(Context context, List data, Callback callback) {
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
            convertView = inflater.inflate(R.layout.list_item_rgbw_task_list, null);
            holder = new ViewHolder();
            holder.tv_time = convertView.findViewById(R.id.tv_time);
            holder.tv_action = convertView.findViewById(R.id.tv_action);
            holder.on = convertView.findViewById(R.id.sw_on);
            convertView.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }
        if (choice == position)
            convertView.setBackgroundColor(0x20ffffff);
        else convertView.setBackgroundColor(0x00000000);

        holder.tv_time.setText(mdata.get(position).getTime());
        holder.tv_action.setText(mdata.get(position).getAction());
        holder.on.setChecked(mdata.get(position).getOn());

        if (mCallback != null) {
            holder.on.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCallback.click(v, position);
                }
            });
        }
        return convertView;
    }

    public void setTask(int position, int hour, int minute, int action, int on) {
        mdata.get(position).hour = hour;
        mdata.get(position).minute = minute;
        mdata.get(position).action = action;
        mdata.get(position).on = on;
        this.notifyDataSetChanged();
    }


    class ViewHolder {
        TextView tv_time;
        TextView tv_action;
        Switch on;
    }
}
