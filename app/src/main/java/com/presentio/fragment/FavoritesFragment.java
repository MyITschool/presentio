package com.presentio.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.presentio.R;
import com.presentio.adapter.FavoritesAdapter;
import com.presentio.di.DaggerOkHttpComponent;
import com.presentio.di.OkHttpComponent;
import com.presentio.di.OkHttpModule;
import com.presentio.http.Api;
import com.presentio.js2p.structs.JsonFpost;
import com.presentio.js2p.structs.JsonUserInfo;
import com.presentio.requests.DataHandler;
import com.presentio.requests.GetUserFavorites;
import com.presentio.requests.GetUserInfoRequest;
import com.presentio.requests.RepostRequest;
import com.presentio.util.ObservableUtil;
import com.presentio.util.ViewUtil;
import com.presentio.view.PostsView;

import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class FavoritesFragment extends RefreshDataFragment<FavoritesFragment.FavoritesData> {
    private Api postsApi, usersApi;
    private CompositeDisposable disposable = new CompositeDisposable();

    @Inject
    OkHttpClient client;

    @Override
    protected View getLoaderView(View view) {
        return view.findViewById(R.id.favorites_loader);
    }

    public static class FavoritesData {
        public JsonUserInfo myUserInfo;
        public ArrayList<JsonFpost> favorites = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.favorites_fragment, container, false);
    }

    @Override
    protected void initialize() {
        data = new FavoritesData();

        OkHttpComponent component = DaggerOkHttpComponent.builder().okHttpModule(
                new OkHttpModule(getContext())
        ).build();

        component.inject(this);

        postsApi = new Api(getContext(), client, Api.HOST_POSTS_SERVICE);
        usersApi = new Api(getContext(), client, Api.HOST_USER_SERVICE);
    }

    @Override
    protected View getDataView(View view) {
        return view.findViewById(R.id.favorites_posts_wrapper);
    }

    private abstract static class FavoritesDataHandler implements DataHandler {
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
        requestData(new FavoritesDataHandler() {
            @Override
            protected void doFinish(boolean success) {
                onInitialLoadFinished(success);
            }

            @Override
            public Response getResponse(Api api, String url) throws IOException, Api.ApiException {
                return api.request(url);
            }
        });
    }

    @Override
    protected void refreshData() {
        requestData(new FavoritesDataHandler() {
            @Override
            protected void doFinish(boolean success) {
                onRefreshFinished(success);
            }

            @Override
            public Response getResponse(Api api, String url) throws IOException, Api.ApiException {
                return api.requestForce(url);
            }
        });
    }

    protected void requestData(DataHandler handler) {
        ObservableUtil.singleIo(
                new GetUserInfoRequest(handler, usersApi, -1),
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

        ObservableUtil.singleIo(
                new GetUserFavorites(postsApi, handler, 0),
                new SingleObserver<ArrayList<JsonFpost>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(ArrayList<JsonFpost> jsonPosts) {
                        data.favorites = jsonPosts;
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
    protected void initializeView(View view, boolean success) {
        if (success) {
            fillView(view);
        } else {
            Toast.makeText(getContext(), "Failed to fetch data", Toast.LENGTH_SHORT).show();
        }

        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.favorites_posts_wrapper);
        refreshLayout.setOnRefreshListener(() -> refreshLayout.setRefreshing(onRefresh()));
    }

    @Override
    protected void refreshView(View view, boolean success) {
        if (success) {
            fillView(view);
        } else {
            Toast.makeText(getContext(), "Failed to refresh data", Toast.LENGTH_SHORT).show();
        }

        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.favorites_posts_wrapper);
        refreshLayout.setRefreshing(false);
    }

    private void fillView(View view) {
        setupNoPosts(view);

        PostsView postsView = view.findViewById(R.id.favorites_posts);

        postsView.setPagingAdapter(new FavoritesAdapter(
                data.favorites,
                getContext(),
                postsApi,
                new FavoritesAdapter.FavoritePostEventHandler() {
                    @Override
                    public void onRemoveFavorites(JsonFpost post, int position) {
                        data.favorites.remove(position);
                        postsView.getPagingAdapter().notifyItemRemoved(position);

                        setupNoPosts(getView());
                    }

                    @Override
                    public void onOpen(JsonFpost post) {
                        NavHostFragment hostFragment = (NavHostFragment) getParentFragment();

                        NavController controller = hostFragment.getNavController();

                        controller.navigate(
                                FavoritesFragmentDirections.actionFavoritesFragmentToPostFragment(post.id)
                        );
                    }

                    @Override
                    public void onRepost(JsonFpost post) {
                        RepostSheetFragment fragment = new RepostSheetFragment(
                                data.myUserInfo.user,
                                post,
                                FavoritesFragment.this::onRepost
                        );
                        fragment.show(getChildFragmentManager(), RepostSheetFragment.TAG);
                    }
                }
        ));
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

    private void setupNoPosts(View view) {
        ViewUtil.setVisible(view.findViewById(R.id.favorites_no_posts), data.favorites.isEmpty());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        disposable.dispose();
    }
}
