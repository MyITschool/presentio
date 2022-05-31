package com.presentio.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.presentio.R;
import com.presentio.http.Api;
import com.presentio.js2p.structs.JsonUser;
import com.presentio.requests.UserSearchRequest;
import com.presentio.util.ObservableUtil;
import com.presentio.view.InfiniteRecyclerView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

public class UserSearchAdapter extends InfiniteRecyclerView.PagingAdapter<JsonUser, UserSearchAdapter.ViewHolder> {
    private final String query;
    private final Context context;
    private final Api usersApi;
    private final CompositeDisposable disposable = new CompositeDisposable();
    private final OnProfileOpenHandler handler;

    public interface OnProfileOpenHandler {
        void onOpen(JsonUser user);
    }
    
    public UserSearchAdapter(
            ArrayList<JsonUser> users,
            String query,
            Context context,
            Api usersApi,
            OnProfileOpenHandler handler
    ) {
        super(users);
        
        this.query = query;
        this.context = context;
        this.usersApi = usersApi;
        this.handler = handler;
    }
    
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        return new ViewHolder(inflater.inflate(R.layout.user_search_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        JsonUser user = data.get(position);

        Picasso.get().load(user.pfpUrl).into(holder.userImage);

        holder.userName.setText(user.name);

        holder.userImage.setOnClickListener(v -> handler.onOpen(data.get(holder.getAdapterPosition())));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public Disposable getDisposable() {
        return disposable;
    }

    @Override
    protected void loadData(InfiniteRecyclerView view, int page) {
        ObservableUtil.singleIo(
                new UserSearchRequest(usersApi, page, query),
                new SingleObserver<ArrayList<JsonUser>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(ArrayList<JsonUser> jsonUsers) {
                        boolean finished = false;

                        if (jsonUsers.size() == 0) {
                            finished = true;
                        } else {
                            int size = data.size();

                            data.addAll(jsonUsers);
                            notifyItemRangeInserted(size, jsonUsers.size());
                        }

                        view.finishLoading(finished);
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof Api.InvalidCredentialsException) {
                            Api.logout();
                            return;
                        }

                        Toast.makeText(context, "Failed to load more users", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView userImage;
        public final TextView userName;

        public ViewHolder(View itemView) {
            super(itemView);

            userImage = itemView.findViewById(R.id.search_user_image);
            userName = itemView.findViewById(R.id.search_user_name);
        }
    }
}
