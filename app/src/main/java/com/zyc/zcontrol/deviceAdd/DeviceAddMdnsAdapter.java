package com.zyc.zcontrol.deviceAdd;


import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.zyc.zcontrol.deviceItem.DeviceClass.Device;
import com.zyc.zcontrol.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DeviceAddMdnsAdapter extends RecyclerView.Adapter<DeviceAddMdnsAdapter.ViewHolder> {

    private List<Device> mList;


    //记录不同字数时的字体大小
    Map<Integer, Float> textSizeHashMap = new HashMap<Integer, Float>();

    //region 单击回调事件
    //点击 RecyclerView 某条的监听
    public interface OnItemClickListener {
        /**
         * 当RecyclerView某个被点击的时候回调
         *
         * @param view     点击item的视图
         * @param position 在adapter中的位置
         * @param device   点击的设备
         */
        void onItemClick(View view, int position, Device device);
    }

    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    //endregion
    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView name;


        public ViewHolder(View view) {
            super(view);

            name = view.findViewById(R.id.name);
            imageView = view.findViewById(R.id.imageView);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onItemClick(v, getLayoutPosition(), mList.get(getLayoutPosition()));
                    }
                }
            });
        }
    }

    public DeviceAddMdnsAdapter(List<Device> iconList) {
        mList = iconList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_device_add_mdns_list, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Device device = mList.get(position);

        holder.imageView.setImageResource(device.getIcon());
        holder.imageView.setVisibility(View.VISIBLE);

        //region 显示名称,并自动调整字体大小
        holder.name.setText(device.getName());
        final TextView v = holder.name;
        if (textSizeHashMap.get(v.getText().length()) != null)
            v.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeHashMap.get(v.getText().length()));
        else {
            v.setTextSize(TypedValue.COMPLEX_UNIT_PX, 25);

            holder.name.post(new Runnable() {
                public void run() {
                    if (v.getLeft() + v.getLeft() / 2 >= ((ConstraintLayout) v.getParent()).getWidth()) {
                        v.setTextSize(TypedValue.COMPLEX_UNIT_PX, v.getTextSize() - 1);
                        v.post(this);
                    } else if (!textSizeHashMap.containsKey(v.getText().length())) {
                        textSizeHashMap.put(v.getText().length(), v.getTextSize());
                    }
                }
            });
        }
        //endregion


        holder.name.setTextColor(0xfffe871f);


    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public void clear() {
        mList.clear();
    }

    public Device getItem(int position) {
        return mList.get(position);
    }


}