package com.presentio.fragment;

import android.os.Bundle;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.presentio.R;
import com.presentio.adapter.HomeAdapter;
import com.presentio.di.DaggerOkHttpComponent;
import com.presentio.di.OkHttpComponent;
import com.presentio.di.OkHttpModule;
import com.presentio.js2p.structs.JsonFpost;
import com.presentio.requests.DataHandler;
import com.presentio.http.Api;
import com.presentio.view.PostsView;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

import javax.inject.Inject;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class HomeFragment extends RefreshDataFragment<ArrayList<JsonFpost>> {
    private HomeAdapter adapter;
    private CompositeDisposable disposable = new CompositeDisposable();

    private boolean isList = true;

    @Inject
    OkHttpClient client;

    @Override
    protected void initialize() {
        data = new ArrayList<JsonFpost>();

        OkHttpComponent component = DaggerOkHttpComponent.builder().okHttpModule(
                new OkHttpModule(this.getContext())
        ).build();

        component.inject(this);

        adapter = new HomeAdapter(data, getContext(), new Api(getContext(), client, Api.HOST_POSTS_SERVICE));
    }

    @Override
    protected View getLoaderView(View view) {
        return view.findViewById(R.id.home_loader);
    }

    @Override
    protected View getDataView(View view) {
        return view.findViewById(R.id.refresh_feed);
    }

    @Override
    protected void initializeView(View view, boolean success) {
        if (success) {
            // fillView(view);
        } else {
            Toast.makeText(getContext(), getString(R.string.data_init_fail), Toast.LENGTH_SHORT).show();
        }

        SwipeRefreshLayout refreshFeed = view.findViewById(R.id.refresh_feed);
        refreshFeed.setOnRefreshListener(() -> refreshFeed.setRefreshing(onRefresh()));
    }

    @Override
    protected void refreshView(View view, boolean success) {
        if (success) {
            fillView(view);
        } else {
            Toast.makeText(getContext(), getString(R.string.data_refresh_fail), Toast.LENGTH_SHORT).show();
        }

        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.refresh_feed);

        swipeRefreshLayout.setRefreshing(false);
    }

    private void fillView(View view) {
        PostsView feed = view.findViewById(R.id.feed);

        feed.setPagingAdapter(adapter);
        feed.setList(isList);

        ImageButton listButton = view.findViewById(R.id.list_button);
        ImageButton gridButton = view.findViewById(R.id.grid_button);

        if (isList) {
            listButton.setBackgroundResource(R.drawable.bar_view_selection);
            gridButton.setBackground(null);
        } else {
            listButton.setBackground(null);
            gridButton.setBackgroundResource(R.drawable.bar_view_selection);
        }

        listButton.setOnClickListener(v -> {
            isList = true;
            adapter.isList = true;
            feed.setList(true);

            listButton.setBackgroundResource(R.drawable.bar_view_selection);
            gridButton.setBackground(null);
        });

        gridButton.setOnClickListener(v -> {
            isList = false;
            adapter.isList = false;
            feed.setList(false);

            gridButton.setBackgroundResource(R.drawable.bar_view_selection);
            listButton.setBackground(null);
        });
    }

    @Override
    protected void loadInitialData() {
        requestData(new DataHandler() {
            @Override
            public Response getResponse(Api api, String url) throws Api.ApiException, IOException {
                return api.request(url);
            }

            @Override
            public void finish(boolean success) {
                onInitialLoadFinished(success);
            }
        });
    }

    @Override
    protected void refreshData() {
        requestData(new DataHandler() {
            @Override
            public Response getResponse(Api api, String url) throws Api.ApiException, IOException {
                return api.requestForce(url);
            }

            @Override
            public void finish(boolean success) {
                onRefreshFinished(success);
            }
        });
    }

    private void requestData(DataHandler handler) {
        Single.fromCallable(() -> {
            Api api = new Api(getContext(), client, Api.HOST_POSTS_SERVICE);

            Response response = handler.getResponse(api, "/v0/recommended/0");

            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<JsonFpost>>() {}.getType();

            return gson.<ArrayList<JsonFpost>>fromJson(response.body().charStream(), type);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new SingleObserver<ArrayList<JsonFpost>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(ArrayList<JsonFpost> jsonPosts) {
                        int size = data.size();

                        data.clear();
                        data.addAll(jsonPosts);
                        adapter.notifyItemRangeRemoved(0, size);
                        adapter.notifyItemRangeInserted(0, data.size());

                        handler.finish(true);
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof Api.InvalidCredentialsException) {
                            Api.logout();
                            return;
                        }

                        handler.finish(false);
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.home_fragment, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        disposable.dispose();
    }
}