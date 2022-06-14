package com.presentio.handler;

import android.content.Context;
import android.widget.Toast;

import com.presentio.js2p.structs.JsonFpost;
import com.presentio.requests.DeletePostRequest;
import com.presentio.requests.ToggleFavoritePostRequest;
import com.presentio.requests.ToggleLikeRequest;
import com.presentio.http.Api;
import com.presentio.util.ObservableUtil;
import com.presentio.view.PostFullView;

import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

public abstract class DefaultListEventHandler implements PostFullView.EventHandler {
    protected final Context context;
    protected final Api postsApi;
    protected final CompositeDisposable disposable;

    protected DefaultListEventHandler(
            Context context,
            Api postsApi,
            CompositeDisposable disposable
    ) {
        this.context = context;
        this.postsApi = postsApi;
        this.disposable = disposable;
    }

    @Override
    public void onDelete(JsonFpost post) {
        ObservableUtil.singleIo(
                new DeletePostRequest(postsApi, post.id),
                new SingleObserver<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(Integer integer) {
                        if (integer != 204) {
                            Toast.makeText(context, "Unable to delete post", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        removeItem();
                        Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof Api.InvalidCredentialsException) {
                            Api.logout();
                            return;
                        }

                        Toast.makeText(context, "Unable to delete post", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public void onHide(JsonFpost post) {
        removeItem();
    }

    @Override
    public void onHitLike(JsonFpost post) {
        ObservableUtil.singleIo(
                new ToggleLikeRequest(postsApi, post),
                new SingleObserver<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(Integer integer) {
                        if (integer == 404) {
                            Toast.makeText(context, "Post no longer exists", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (integer == 409) {
                            Toast.makeText(context, "Action already done", Toast.LENGTH_SHORT).show();
                        }

                        if (post.liked.id == 0) {
                            post.liked.id = 1L;
                            likeItem(post);
                        } else {
                            post.liked.id = 0L;
                            removeItemLike(post);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof Api.InvalidCredentialsException) {
                            Api.logout();
                            return;
                        }

                        Toast.makeText(context, "Failed to like post", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public void onHitComment(JsonFpost post) {
        onOpen(post);
    }

    @Override
    public void onHitFavorite(JsonFpost post) {
        ObservableUtil.singleIo(
                new ToggleFavoritePostRequest(postsApi, post),
                new SingleObserver<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(Integer integer) {
                        if (integer == 404) {
                            Toast.makeText(context, "Post no longer exists", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (integer == 409) {
                            Toast.makeText(context, "Action already done", Toast.LENGTH_SHORT).show();
                        }

                        if (post.favorite.id == 0) {
                            post.favorite.id = 1L;
                            addToFavorites(post);
                        } else {
                            post.favorite.id = 0L;
                            removeFromFavorites(post);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof Api.InvalidCredentialsException) {
                            Api.logout();
                            return;
                        }

                        Toast.makeText(context, "Failed to add post to favorites", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Should remove the item and notify the adapter
     */
    protected abstract void removeItem();

    /**
     * Should like the item
     */
    protected abstract void likeItem(JsonFpost post);

    /**
     * Should remove item like
     */
    protected abstract void removeItemLike(JsonFpost post);

    /**
     * Should add post to favorites
     */
    protected abstract void addToFavorites(JsonFpost post);

    /**
     * Should remove post from favorites
     */
    protected abstract void removeFromFavorites(JsonFpost post);
}
