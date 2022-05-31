package com.presentio.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.presentio.R;
import com.presentio.js2p.structs.JsonFpost;
import com.presentio.http.Api;
import com.presentio.view.InfiniteRecyclerView;
import com.presentio.view.PostFullView;
import com.presentio.view.PostGridView;

import java.util.ArrayList;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public abstract class PostsAdapter extends InfiniteRecyclerView.PagingAdapter<JsonFpost, PostsAdapter.ViewHolder> {
    protected CompositeDisposable disposable = new CompositeDisposable();
    protected Api postsApi;

    public boolean isList = true;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View v;

        public ViewHolder(View itemView) {
            super(itemView);

            v = itemView.findViewById(R.id.item_post);
        }
    }

    protected PostsAdapter(ArrayList<JsonFpost> posts, Api postsApi) {
        super(posts);
        this.postsApi = postsApi;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (isList) {
            view = inflater.inflate(R.layout.post_list_item, parent, false);
        } else {
            view = inflater.inflate(R.layout.post_grid_item, parent, false);
        }

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        JsonFpost post = data.get(position);

        if (isList) {
            bindListPost(holder, post);
        } else {
            bindGridPost(holder, post);
        }
    }

    private void bindListPost(ViewHolder holder, JsonFpost post) {
        PostFullView fullPost = (PostFullView) holder.v;

        fullPost.fillView(post, getListEventHandler(holder, fullPost), getListMenuHandler(holder, fullPost));
    }

    private void bindGridPost(ViewHolder holder, JsonFpost post) {
        PostGridView gridPost = (PostGridView) holder.v;

        gridPost.fillView(post, getGridEventHandler(holder, gridPost));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public ArrayList<JsonFpost> getPosts() {
        return data;
    }

    @Override
    public CompositeDisposable getDisposable() {
        return disposable;
    }

    protected abstract PostFullView.EventHandler getListEventHandler(RecyclerView.ViewHolder holder, PostFullView fullPost);

    protected abstract PostFullView.MenuHandler getListMenuHandler(RecyclerView.ViewHolder holder, PostFullView fullPost);

    protected abstract PostGridView.EventHandler getGridEventHandler(RecyclerView.ViewHolder holder, PostGridView gridPost);
}
