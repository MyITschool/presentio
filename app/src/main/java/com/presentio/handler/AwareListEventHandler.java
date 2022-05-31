package com.presentio.handler;

import android.content.Context;

import com.presentio.js2p.structs.JsonFpost;
import com.presentio.http.Api;
import com.presentio.view.PostFullView;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public abstract class AwareListEventHandler extends DefaultListEventHandler {
    private final PostFullView fullPost;

    protected AwareListEventHandler(
            Context context,
            Api postsApi,
            CompositeDisposable disposable,
            PostFullView fullPost
    ) {
        super(context, postsApi, disposable);

        this.fullPost = fullPost;
    }

    @Override
    protected void likeItem(JsonFpost post) {
        fullPost.likeView(post);
    }

    @Override
    protected void removeItemLike(JsonFpost post) {
        fullPost.removeViewLike(post);
    }

    @Override
    protected void addToFavorites(JsonFpost post) {
        fullPost.addToFavorites(post);
    }

    @Override
    protected void removeFromFavorites(JsonFpost post) {
        fullPost.removeFromFavorites(post);
    }
}
