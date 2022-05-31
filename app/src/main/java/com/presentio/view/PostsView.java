package com.presentio.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public class PostsView extends InfiniteRecyclerView {
    private boolean isList = true;

    public PostsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutManager(createListLayoutManager());
        setItemViewCacheSize(4);
    }

    public PostsView(Context context, AttributeSet attrs) {
        this(context, attrs, androidx.recyclerview.R.attr.recyclerViewStyle);
    }

    public PostsView(Context context) {
        this(context, null);
    }

    public void setList(boolean list) {
        if (list == isList) {
            return;
        }

        isList = list;
        setLayoutManager(list ? createListLayoutManager() : createGridLayoutManager());
    }

    private LinearLayoutManager createListLayoutManager() {
        return new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
    }

    private StaggeredGridLayoutManager createGridLayoutManager() {
        return new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
    }
}
