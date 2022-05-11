package com.zyc.zcontrol.deviceItem.rgbw;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Switch;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

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
            convertView = inflater.inflate(R.layout.rgbw_list_item_task_list, null);
            holder = new ViewHolder();
            holder.tv_time = convertView.findViewById(R.id.tv_time);
            holder.tv_action = convertView.findViewById(R.id.tv_action);
            holder.tv_repeat = convertView.findViewById(R.id.tv_repeat);
            holder.color_set = convertView.findViewById(R.id.color_set);
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
       // holder.tv_action.setText(mdata.get(position).getAction());
        holder.on.setChecked(mdata.get(position).getOn());
        holder.tv_repeat.setText(mdata.get(position).getRepeat());
        holder.color_set.setCardBackgroundColor(mdata.get(position).getColor());
        if(mdata.get(position).getColor()==0xff000000)
        {
            ((CardView)holder.color_set.getParent()).setVisibility(View.GONE);
        }else
            ((CardView)holder.color_set.getParent()).setVisibility(View.VISIBLE);


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

    public void setTask(int position, int on, int hour, int minute, int repeat, int[] rgb, int gradient) {
        mdata.get(position).on = on;
        mdata.get(position).hour = hour;
        mdata.get(position).minute = minute;
        mdata.get(position).repeat = repeat;
        mdata.get(position).gradient = gradient;
        for (int i = 0; i < 4; i++) {
            mdata.get(position).rgb[i] = rgb[i];
        }
        this.notifyDataSetChanged();
    }


    class ViewHolder {
        TextView tv_time;
        TextView tv_action;
        TextView tv_repeat;
        Switch on;
        CardView color_set;
    }
}
