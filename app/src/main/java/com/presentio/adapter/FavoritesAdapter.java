package com.presentio.adapter;

import android.content.Context;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.presentio.handler.AwareListEventHandler;
import com.presentio.handler.DefaultMenuHandler;
import com.presentio.handler.PostEventHandler;
import com.presentio.http.Api;
import com.presentio.js2p.structs.JsonFpost;
import com.presentio.requests.GetUserFavorites;
import com.presentio.util.ObservableUtil;
import com.presentio.view.InfiniteRecyclerView;
import com.presentio.view.PostFullView;
import com.presentio.view.PostGridView;

import java.util.ArrayList;

import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;

public class FavoritesAdapter extends PostsAdapter {
    private final FavoritePostEventHandler handler;
    private final Context context;

    public interface FavoritePostEventHandler extends PostEventHandler {
        void onRemoveFavorites(JsonFpost post, int position);
    }

    public FavoritesAdapter(
            ArrayList<JsonFpost> posts,
            Context context,
            Api postsApi,
            FavoritePostEventHandler handler
    ) {
        super(posts, postsApi);
        this.handler = handler;
        this.context = context;
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

            @Override
            protected void removeFromFavorites(JsonFpost post) {
                super.removeFromFavorites(post);

                handler.onRemoveFavorites(post, holder.getAdapterPosition());
            }
        };
    }

    @Override
    protected PostFullView.MenuHandler getListMenuHandler(PostsAdapter.ViewHolder holder, PostFullView fullPost) {
        return new DefaultMenuHandler(context);
    }

    @Override
    protected PostGridView.EventHandler getGridEventHandler(PostsAdapter.ViewHolder holder, PostGridView gridPost) {
        return null;
    }

    @Override
    protected void loadData(InfiniteRecyclerView view, int page) {
        ObservableUtil.singleIo(
                new GetUserFavorites(postsApi, page),
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
                        Toast.makeText(context, "Failed to fetch more favorite posts", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}
