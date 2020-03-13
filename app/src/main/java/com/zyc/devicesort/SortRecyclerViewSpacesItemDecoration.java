package com.zyc.devicesort;


import android.graphics.Rect;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

public class SortRecyclerViewSpacesItemDecoration extends RecyclerView.ItemDecoration {

    public int left;
    public int top;
    public int right;
    public int bottom;

    public SortRecyclerViewSpacesItemDecoration(int left, int top, int right, int bottom) {
        this.left=left;
        this.top=top;
        this.right=right;
        this.bottom=bottom;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.left = left;
            outRect.right = right;
            outRect.bottom = bottom;
            outRect.top = top;
    }
}
