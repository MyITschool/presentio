package com.presentio.adapter;

import android.content.Context;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.presentio.handler.AwareListEventHandler;
import com.presentio.handler.DefaultMenuHandler;
import com.presentio.handler.PostEventHandler;
import com.presentio.js2p.structs.JsonFpost;
import com.presentio.requests.GetUserPostsRequest;
import com.presentio.http.Api;
import com.presentio.util.ObservableUtil;
import com.presentio.view.InfiniteRecyclerView;
import com.presentio.view.PostFullView;
import com.presentio.view.PostGridView;

import java.util.ArrayList;

import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;

public class ProfileAdapter extends PostsAdapter {
    private final PostEventHandler handler;
    private final Context context;
    private final long userId;

    public ProfileAdapter(
            ArrayList<JsonFpost> posts,
            Context context,
            Api postsApi,
            PostEventHandler handler,
            long userId
    ) {
        super(posts, postsApi);
        this.handler = handler;
        this.context = context;
        this.userId = userId;
    }

    @Override
    protected PostFullView.EventHandler getListEventHandler(RecyclerView.ViewHolder holder, PostFullView fullPost) {
        return new AwareListEventHandler(context, postsApi, disposable, fullPost) {
            @Override
            protected void removeItem() {
                data.remove(holder.getAdapterPosition());
                notifyItemRemoved(holder.getAdapterPosition());
            }

            @Override
            public void onOpen(JsonFpost post) {
                handler.onOpen(post);
            }

            @Override
            public void onHitRepost(JsonFpost post) {
                handler.onRepost(post);
            }
        };
    }

    @Override
    protected PostFullView.MenuHandler getListMenuHandler(RecyclerView.ViewHolder holder, PostFullView fullPost) {
        return new DefaultMenuHandler(context);
    }

    @Override
    protected void loadData(InfiniteRecyclerView view, int page) {
        ObservableUtil.singleIo(
                new GetUserPostsRequest(postsApi, page, userId),
                new SingleObserver<ArrayList<JsonFpost>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(ArrayList<JsonFpost> jsonPosts) {
                        boolean postsFinished = false;

                        if (jsonPosts.size() == 0) {
                            postsFinished = true;
                        } else {
                            int size = data.size();

                            data.addAll(jsonPosts);

                            notifyItemRangeInserted(size, jsonPosts.size());
                        }

                        view.finishLoading(postsFinished);
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof Api.InvalidCredentialsException) {
                            Api.logout();
                            return;
                        }

                        Toast.makeText(context, "Failed to fetch more posts", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    protected PostGridView.EventHandler getGridEventHandler(RecyclerView.ViewHolder holder, PostGridView gridPost) {
        return null;
    }
}
