package com.presentio.adapter;

import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;

import com.presentio.js2p.structs.JsonFpost;
import com.presentio.http.Api;
import com.presentio.view.InfiniteRecyclerView;
import com.presentio.view.PostFullView;
import com.presentio.view.PostGridView;

import java.util.ArrayList;

public class HomeAdapter extends PostsAdapter {
    public HomeAdapter(ArrayList<JsonFpost> posts, Context context, Api postsApi) {
        super(posts, postsApi);
    }

    @Override
    protected PostFullView.EventHandler getListEventHandler(RecyclerView.ViewHolder holder, PostFullView fullPost) {
        return null;
    }

    @Override
    protected PostFullView.MenuHandler getListMenuHandler(RecyclerView.ViewHolder holder, PostFullView fullPost) {
        return null;
    }

    @Override
    protected PostGridView.EventHandler getGridEventHandler(RecyclerView.ViewHolder holder, PostGridView gridPost) {
        return null;
    }

    @Override
    protected void loadData(InfiniteRecyclerView view, int page) {

    }
}
