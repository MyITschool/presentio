package com.presentio.fragment;

import android.os.Bundle;

import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.presentio.R;
import com.presentio.adapter.HomeAdapter;
import com.presentio.di.DaggerOkHttpComponent;
import com.presentio.di.OkHttpComponent;
import com.presentio.di.OkHttpModule;
import com.presentio.handler.PostEventHandler;
import com.presentio.js2p.structs.JsonFpost;
import com.presentio.js2p.structs.JsonUserInfo;
import com.presentio.requests.DataHandler;
import com.presentio.http.Api;
import com.presentio.requests.GetUserInfoRequest;
import com.presentio.requests.PostSearchRequest;
import com.presentio.requests.RepostRequest;
import com.presentio.util.ObservableUtil;
import com.presentio.view.PostsView;

import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

import io.reactivex.rxjava3.disposables.Disposable;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class HomeFragment extends RefreshDataFragment<HomeFragment.HomeData> {
    private final CompositeDisposable disposable = new CompositeDisposable();
    private boolean isList = true;
    private Api postsApi, userApi;

    @Inject
    OkHttpClient client;

    public static class HomeData {
        public ArrayList<JsonFpost> posts;
        public JsonUserInfo myUserInfo;
    }
    
    @Override
    protected void initialize() {
        data = new HomeData();

        OkHttpComponent component = DaggerOkHttpComponent.builder().okHttpModule(
                new OkHttpModule(this.getContext())
        ).build();

        component.inject(this);

        postsApi = new Api(getContext(), client, Api.HOST_POSTS_SERVICE);
        userApi = new Api(getContext(), client, Api.HOST_USER_SERVICE);
    }

    private abstract static class HomeDataHandler implements DataHandler {
        private static final int TOTAL = 2;
        private int completed = 0;
        private boolean success = true;

        @Override
        public void finish(boolean success) {
            this.success &= success;
            completed++;

            if (completed == TOTAL) {
                doFinish(this.success);
            }
        }

        protected abstract void doFinish(boolean success);
    }
    
    @Override
    protected void loadInitialData() {
        requestData(new HomeDataHandler() {
            @Override
            public Response getResponse(Api api, String url) throws Api.ApiException, IOException {
                return api.request(url);
            }

            @Override
            protected void doFinish(boolean success) {
                onInitialLoadFinished(success);
            }
        });
    }

    @Override
    protected void refreshData() {
        requestData(new HomeDataHandler() {
            @Override
            public Response getResponse(Api api, String url) throws Api.ApiException, IOException {
                return api.requestForce(url);
            }

            @Override
            protected void doFinish(boolean success) {
                onRefreshFinished(success);
            }
        });
    }
    
    private void requestData(DataHandler handler) {
        ObservableUtil.singleIo(
                new PostSearchRequest(postsApi, 0, "tag=подарок"),
                new SingleObserver<ArrayList<JsonFpost>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(ArrayList<JsonFpost> jsonPosts) {
                        data.posts = jsonPosts;

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
                }
        );
        
        ObservableUtil.singleIo(
                new GetUserInfoRequest(handler, userApi),
                new SingleObserver<JsonUserInfo>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(JsonUserInfo jsonUserInfo) {
                        data.myUserInfo = jsonUserInfo;
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
                }
        );
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
             fillView(view);
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

        HomeAdapter adapter = getHomeAdapter(data.posts);

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.home_fragment, container, false);
    }

    private void onRepost(JsonFpost post, String text) {
        ObservableUtil.singleIo(
                new RepostRequest(postsApi, text, post),
                new SingleObserver<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(Integer integer) {
                        Toast.makeText(getContext(), "Repost was made", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof Api.InvalidCredentialsException) {
                            Api.logout();
                            return;
                        }

                        Toast.makeText(getContext(), "Failed to create post", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public HomeAdapter getHomeAdapter(ArrayList<JsonFpost> posts) {
        return new HomeAdapter(posts, getContext(), postsApi, new PostEventHandler() {
            @Override
            public void onOpen(JsonFpost post) {
                NavHostFragment.findNavController(HomeFragment.this).navigate(
                        HomeFragmentDirections.actionHomeFragmentToPostFragment(post.id)
                );
            }

            @Override
            public void onRepost(JsonFpost post) {
                RepostSheetFragment fragment = new RepostSheetFragment(
                        data.myUserInfo.user,
                        post,
                        HomeFragment.this::onRepost
                );

                fragment.show(getChildFragmentManager(), RepostSheetFragment.TAG);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        disposable.dispose();
    }
}