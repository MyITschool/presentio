package com.presentio.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.presentio.R;
import com.presentio.js2p.structs.JsonComment;
import com.presentio.requests.GetCommentsRequest;
import com.presentio.http.Api;
import com.presentio.util.ObservableUtil;
import com.presentio.view.InfiniteRecyclerView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;


public class CommentsAdapter extends InfiniteRecyclerView.PagingAdapter<JsonComment, CommentsAdapter.ViewHolder> {
    private final long postId;
    private final Api postsApi;
    private final CompositeDisposable disposable = new CompositeDisposable();

    public CommentsAdapter(
            ArrayList<JsonComment> comments,
            Api postsApi,
            long postId
    ) {
        super(comments);
        this.postsApi = postsApi;
        this.postId = postId;
    }

    @Override
    protected void loadData(InfiniteRecyclerView view, int page) {
        ObservableUtil.singleIo(
                new GetCommentsRequest(postsApi, postId, page),
                new SingleObserver<ArrayList<JsonComment>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(ArrayList<JsonComment> jsonComments) {
                        if (jsonComments.size() == 0) {
                            view.finishLoading(true);
                            return;
                        }

                        int size = data.size();

                        data.addAll(jsonComments);

                        notifyItemRangeInserted(size, jsonComments.size());

                        view.finishLoading(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof Api.InvalidCredentialsException) {
                            Api.logout();
                            return;
                        }

                        view.finishLoading(true);
                        Toast.makeText(view.getContext(), "Failed to fetch more comments", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public CompositeDisposable getDisposable() {
        return disposable;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        return new ViewHolder(inflater.inflate(R.layout.comment_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        JsonComment comment = data.get(position);

        holder.userUsername.setText(comment.user.name);

        holder.userImage.setClipToOutline(true);
        Picasso.get().load(comment.user.pfpUrl).into(holder.userImage);

        holder.userComment.setText(comment.text);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView userImage;
        public final TextView userUsername;
        public final TextView userComment;

        public ViewHolder(View itemView) {
            super(itemView);

            userImage = itemView.findViewById(R.id.user_image);
            userUsername = itemView.findViewById(R.id.user_username);
            userComment = itemView.findViewById(R.id.user_comment);
        }
    }
}
