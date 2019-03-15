package com.zyc.zcontrol.controlItem.tc1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Switch;
import android.widget.TextView;

import com.zyc.zcontrol.R;

import java.util.List;

class TC1TaskListAdapter extends BaseAdapter {

    private Context context;
    private List<TaskItem> mdata;
    private LayoutInflater inflater;
    private int choice = -1;

    public TC1TaskListAdapter(Context context, List data) {
        this.context = context;
        this.mdata = data;
        inflater = LayoutInflater.from(context);
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

    public View getView(int position1, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        View view = null;
        final int position = position1;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_tc1_task_list, null);
            holder = new ViewHolder();
            holder.tv_time = convertView.findViewById(R.id.tv_time);
            holder.tv_repeat = convertView.findViewById(R.id.tv_repeat);
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
        holder.tv_repeat.setText(mdata.get(position).getRepeat());
        holder.tv_action.setText(mdata.get(position).getAction());
        holder.on.setChecked(mdata.get(position).getOn());

        return convertView;
    }

    public void setTask(int position, int hour, int minute, int repeat, int action, int on) {
        mdata.get(position).hour = hour;
        mdata.get(position).minute = minute;
        mdata.get(position).repeat = repeat;
        mdata.get(position).action = action;
        mdata.get(position).on = on;
        this.notifyDataSetChanged();
    }


    class ViewHolder {
        TextView tv_time;
        TextView tv_repeat;
        TextView tv_action;
        Switch on;
    }
}
