package com.zyc.devicesort;


import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;

import static com.zyc.zcontrol.ConnectService.ACTION_MAINACTIVITY_DEVICELISTUPDATE;

//Recycler 长按拖动排序功能
public class SortRecyclerItemTouchHelper extends ItemTouchHelper.Callback {
    private SortRecyclerAdapter adapter;
    LocalBroadcastManager localBroadcastManager;

    public SortRecyclerItemTouchHelper(Context content, SortRecyclerAdapter adapter) {
        this.adapter = adapter;
        localBroadcastManager = LocalBroadcastManager.getInstance(content);
    }

    /**
     * 返回滑动的方向
     *
     * @param recyclerView
     * @param viewHolder
     * @return
     */
    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlag;
        int swipeFlag;
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            //允许上下左右的拖动
            dragFlag = ItemTouchHelper.DOWN
                    | ItemTouchHelper.UP
                    | ItemTouchHelper.RIGHT
                    | ItemTouchHelper.LEFT;

            swipeFlag = 0;
        } else {
            dragFlag = ItemTouchHelper.DOWN | ItemTouchHelper.UP;
            swipeFlag = ItemTouchHelper.RIGHT;//只允许从右到左的侧滑
        }
        return makeMovementFlags(dragFlag, swipeFlag);
    }

    /**
     * 当用户拖动一个item从旧的位置移动到新的位置时会调用此方法
     *
     * @param recyclerView
     * @param viewHolder
     * @param target
     * @return
     */
    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {

        int fromPosition = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(adapter.getDataList(), i, i + 1);
//                Collections.swap(otherAdapter.getDataList(), i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(adapter.getDataList(), i, i - 1);
//                Collections.swap(otherAdapter.getDataList(), i, i - 1);
            }
        }


        recyclerView.getAdapter().notifyItemMoved(fromPosition, toPosition);
        localBroadcastManager.sendBroadcast(new Intent(ACTION_MAINACTIVITY_DEVICELISTUPDATE));
        return true;
    }

    /**
     * 当用户左右滑动item达到删除条件时会调用此方法
     * 一般达到item的一般宽度时才会删除，否则弹回原位置
     *
     * @param viewHolder
     * @param direction
     */
    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        if (direction == ItemTouchHelper.END) {
            adapter.getDataList().remove(position);
            adapter.notifyItemRemoved(position);
        }
        adapter.onItemDissmiss(position);
        localBroadcastManager.sendBroadcast(new Intent(ACTION_MAINACTIVITY_DEVICELISTUPDATE));
    }

    /**
     * 当某个item由静止状态变为滑动或拖动状态时调用此方法
     *
     * @param viewHolder
     * @param actionState
     */
    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
//            viewHolder.itemView.setBackgroundColor(Color.GRAY);

            viewHolder.itemView.setAlpha(0.5f);
        }
    }

    /**
     * 当用户操作完某个item动画结束时调用此方法
     *
     * @param recyclerView
     * @param viewHolder
     */
    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
//        viewHolder.itemView.setBackgroundColor(0);
        viewHolder.itemView.setAlpha(1.0f);
    }

    /**
     * 是否支持长按拖动
     * 默认返回true
     *
     * @return
     */
    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    /**
     * 是否支持侧滑删除
     * 默认返回true
     * 不支持侧滑删除返回false
     *
     * @return
     */
    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }
}
