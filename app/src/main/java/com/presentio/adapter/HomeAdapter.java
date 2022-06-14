package com.presentio.adapter;

import android.content.Context;
import android.widget.Toast;

import com.presentio.handler.AwareListEventHandler;
import com.presentio.handler.DefaultMenuHandler;
import com.presentio.handler.PostEventHandler;
import com.presentio.js2p.structs.JsonFpost;
import com.presentio.http.Api;
import com.presentio.requests.GetRecommendedPosts;
import com.presentio.util.ObservableUtil;
import com.presentio.view.InfiniteRecyclerView;
import com.presentio.view.PostFullView;
import com.presentio.view.PostGridView;

import java.util.ArrayList;

import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

public class HomeAdapter extends PostsAdapter {
    private CompositeDisposable disposable = new CompositeDisposable();
    private Context context;
    private PostEventHandler handler;

    public HomeAdapter(ArrayList<JsonFpost> posts, Context context, Api postsApi, PostEventHandler handler) {
        super(posts, postsApi);

        this.context = context;
        this.handler = handler;
    }

    @Override
    protected PostFullView.EventHandler getListEventHandler(PostsAdapter.ViewHolder holder, PostFullView fullPost) {
        return new AwareListEventHandler(context, postsApi, disposable, fullPost) {
            @Override
            protected void removeItem() {
                int pos = holder.getAdapterPosition();

                data.remove(pos);
                notifyItemRemoved(pos);
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
    protected PostFullView.MenuHandler getListMenuHandler(PostsAdapter.ViewHolder holder, PostFullView fullPost) {
        return new DefaultMenuHandler(context);
    }

    @Override
    protected PostGridView.EventHandler getGridEventHandler(PostsAdapter.ViewHolder holder, PostGridView gridPost) {
        return post -> handler.onOpen(post);
    }

    @Override
    protected void loadData(InfiniteRecyclerView view, int page) {
        ObservableUtil.singleIo(
                new GetRecommendedPosts(postsApi, page),
                new SingleObserver<ArrayList<JsonFpost>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(ArrayList<JsonFpost> jsonPosts) {
                        if (jsonPosts.size() == 0) {
                            view.finishLoading(true);
                            return;
                        }

                        int size = data.size();
                        data.addAll(jsonPosts);

                        notifyItemRangeInserted(size, jsonPosts.size());

                        view.finishLoading(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof Api.InvalidCredentialsException) {
                            Api.logout();
                            return;
                        }

                        view.finishLoading(true);
                        Toast.makeText(context, "Failed to fetch more posts", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public CompositeDisposable getDisposable() {
        return disposable;
    }
}
