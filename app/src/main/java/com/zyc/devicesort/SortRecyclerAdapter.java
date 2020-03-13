package com.zyc.devicesort;


import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.zyc.zcontrol.deviceItem.DeviceClass.Device;
import com.zyc.zcontrol.R;

import java.util.List;


public class SortRecyclerAdapter extends RecyclerView.Adapter<SortRecyclerAdapter.ViewHolder> {

    private List<Device> mData;

    public List<Device> getDataList() {
        return mData;
    }

    public SortRecyclerAdapter(List<Device> mData) {
        this.mData = mData;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.list_item_device_sort_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        ViewHolder mHolder = holder;
        mHolder.name.setText(mData.get(position).getName());
        holder.icon.setImageResource(mData.get(position).getIcon());
        holder.icon.setVisibility(View.VISIBLE);

//        mHolder.itemView.setBackgroundColor(0);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView icon;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textView);
            icon = itemView.findViewById(R.id.imageView);
        }
    }

    public void onItemDissmiss(int position) {
        //移除数据
        mData.remove(position);
        notifyItemRemoved(position);
    }

}