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
    protected final CompositeDisposable disposable = new CompositeDisposable();
    protected final Api postsApi;

    public boolean isList = true;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final PostFullView fullPost;
        final PostGridView gridPost;

        public ViewHolder(View itemView) {
            super(itemView);

            fullPost = itemView.findViewById(R.id.item_full_post);
            gridPost = itemView.findViewById(R.id.item_grid_post);
        }
    }

    protected PostsAdapter(ArrayList<JsonFpost> posts, Api postsApi) {
        super(posts);
        this.postsApi = postsApi;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        return new ViewHolder(inflater.inflate(R.layout.post_item, parent, false));
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
        holder.gridPost.setVisibility(View.GONE);
        holder.fullPost.setVisibility(View.VISIBLE);

        holder.fullPost.fillView(
                post,
                getListEventHandler(holder, holder.fullPost),
                getListMenuHandler(holder, holder.fullPost)
        );
    }

    private void bindGridPost(ViewHolder holder, JsonFpost post) {
        holder.fullPost.setVisibility(View.GONE);
        holder.gridPost.setVisibility(View.VISIBLE);

        holder.gridPost.fillView(
                post,
                getGridEventHandler(holder, holder.gridPost)
        );
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

    protected abstract PostFullView.EventHandler getListEventHandler(PostsAdapter.ViewHolder holder, PostFullView fullPost);

    protected abstract PostFullView.MenuHandler getListMenuHandler(PostsAdapter.ViewHolder holder, PostFullView fullPost);

    protected abstract PostGridView.EventHandler getGridEventHandler(PostsAdapter.ViewHolder holder, PostGridView gridPost);
}
