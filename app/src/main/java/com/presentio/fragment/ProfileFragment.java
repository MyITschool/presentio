package com.presentio.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.presentio.R;
import com.presentio.adapter.ProfileAdapter;
import com.presentio.di.DaggerOkHttpComponent;
import com.presentio.di.OkHttpComponent;
import com.presentio.di.OkHttpModule;
import com.presentio.handler.PostEventHandler;
import com.presentio.js2p.structs.JsonFpost;
import com.presentio.js2p.structs.JsonUserInfo;
import com.presentio.requests.DataHandler;
import com.presentio.requests.GetUserInfoRequest;
import com.presentio.requests.GetUserPostsRequest;
import com.presentio.requests.RepostRequest;
import com.presentio.http.Api;
import com.presentio.requests.ToggleFollowRequest;
import com.presentio.util.ObservableUtil;
import com.presentio.util.ViewUtil;
import com.presentio.view.PostsView;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import okhttp3.OkHttpClient;
import okhttp3.Response;



public class ProfileFragment extends RefreshDataFragment<ProfileFragment.ProfileData>{
    private final CompositeDisposable disposable = new CompositeDisposable();
    private Api postsApi, usersApi;
    private long userId;

    @Inject
    OkHttpClient client;

    public static class ProfileData {
        public JsonUserInfo userInfo, myUserInfo;
        public ArrayList<JsonFpost> posts = new ArrayList<>();
    }

    @Override
    protected void initialize() {
        data = new ProfileData();

        Bundle args = getArguments();

        userId = ProfileFragmentArgs.fromBundle(args).getUserId();

        OkHttpComponent component = DaggerOkHttpComponent.builder().okHttpModule(
                new OkHttpModule(getContext())
        ).build();

        component.inject(this);

        postsApi = new Api(getContext(), client, Api.HOST_POSTS_SERVICE);
        usersApi = new Api(getContext(), client, Api.HOST_USER_SERVICE);
    }

    @Override
    protected View getLoaderView(View view) {
        return view.findViewById(R.id.profile_loader_wrapper);
    }

    @Override
    protected View getDataView(View view) {
        return view.findViewById(R.id.refresh_profile);
    }

    private abstract static class ProfileDataHandler implements DataHandler {
        private static final int TOTAL = 3;
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
        requestData(new ProfileDataHandler() {
            @Override
            protected void doFinish(boolean success) {
                onInitialLoadFinished(success);
            }

            @Override
            public Response getResponse(Api api, String url) throws Api.ApiException, IOException {
                return api.request(url);
            }
        });
    }

    @Override
    protected void refreshData() {
        requestData(new ProfileDataHandler() {
            @Override
            protected void doFinish(boolean success) {
                onRefreshFinished(success);
            }

            @Override
            public Response getResponse(Api api, String url) throws Api.ApiException, IOException {
                return api.requestForce(url);
            }
        });
    }

    private void requestData(DataHandler handler) {
        ObservableUtil.singleIo(
                new GetUserInfoRequest(handler, usersApi, userId),
                new SingleObserver<JsonUserInfo>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(JsonUserInfo jsonUserInfo) {
                        data.userInfo = jsonUserInfo;

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
                new GetUserPostsRequest(handler, postsApi, 0, userId),
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
    }

    @Override
    protected void initializeView(View view, boolean success) {
        if (success) {
            fillView(view);
        } else {
            Toast.makeText(getContext(), getString(R.string.data_init_fail), Toast.LENGTH_SHORT).show();
        }

        SwipeRefreshLayout refreshProfile = view.findViewById(R.id.refresh_profile);
        refreshProfile.setOnRefreshListener(() -> refreshProfile.setRefreshing(onRefresh()));
    }

    @Override
    protected void refreshView(View view, boolean success) {
        if (success) {
            fillView(view);
        } else {
            Toast.makeText(getContext(), getString(R.string.data_refresh_fail), Toast.LENGTH_SHORT).show();
        }

        SwipeRefreshLayout refreshProfile = view.findViewById(R.id.refresh_profile);
        refreshProfile.setRefreshing(false);
    }

    private void fillView(View view) {
        fillUserInfo(view);

        ImageView logout = view.findViewById(R.id.profile_logout);
        ViewUtil.setVisible(logout, data.userInfo.self);

        logout.setOnClickListener(v -> Api.logout());

        ImageView favorites = view.findViewById(R.id.profile_favorites);
        ViewUtil.setVisible(favorites, data.userInfo.self);

        ViewUtil.setVisible(view.findViewById(R.id.profile_create), data.userInfo.self);

        favorites.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(
                    ProfileFragmentDirections.actionProfileFragmentToFavoritesFragment()
            );
        });

        setupCreateListener(view);

        fillFollows(view);

        fillPosts(view);
    }

    private void setupCreateListener(View view) {
        view.findViewById(R.id.profile_create).setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(
                    ProfileFragmentDirections.actionProfileFragmentToCreatePostFragment()
            );
        });
    }

    private void fillUserInfo(View view) {
        ImageView profileImage = view.findViewById(R.id.profile_image);

        profileImage.setClipToOutline(true);
        Picasso.get().load(data.userInfo.user.pfpUrl).into(profileImage);

        TextView nameView = view.findViewById(R.id.profile_name);

        nameView.setText(data.userInfo.user.name);

        TextView bioView = view.findViewById(R.id.profile_bio);

        String bio = data.userInfo.user.bio;

        if (bio.isEmpty()) {
            bio = "No status..";
        }

        bioView.setText(bio);
    }

    private void fillFollows(View view) {
        TextView followers = view.findViewById(R.id.followers_counter),
                following = view.findViewById(R.id.following_counter);

        following.setText("Following\n" + data.userInfo.user.following.longValue());
        followers.setText("Followers\n" + data.userInfo.user.followers.longValue());

        setupFollowButton(view);
    }

    private void setupFollowButton(View view) {
        MaterialButton followButton = view.findViewById(R.id.profile_follow);

        ViewUtil.setVisible(followButton, !data.userInfo.self);

        int iconId;

        if (data.userInfo.user.follow.id != 0) {
            iconId = R.drawable.ic_check;
            followButton.setText("following");
        } else {
            iconId = R.drawable.ic_follow;
            followButton.setText("Follow");
        }

        followButton.setIcon(ContextCompat.getDrawable(getContext(), iconId));

        followButton.setOnClickListener(this::onFollowClick);
    }

    private void onFollowClick(View view) {
        ObservableUtil.singleIo(
                new ToggleFollowRequest(usersApi, data.userInfo),
                new SingleObserver<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(Integer integer) {
                        if (integer == 404) {
                            Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (integer == 409) {
                            Toast.makeText(getContext(), "Action already done", Toast.LENGTH_SHORT).show();
                        }

                        data.userInfo.user.follow.id = data.userInfo.user.follow.id == 0 ? 1L : 0L;

                        setupFollowButton(getView());
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof Api.InvalidCredentialsException) {
                            Api.logout();
                            return;
                        }

                        Toast.makeText(getContext(), "Failed to follow user", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void fillPosts(View view) {
        ViewUtil.setVisible(view.findViewById(R.id.profile_no_posts), data.posts.size() == 0);

        PostsView profilePosts = view.findViewById(R.id.profile_posts);

        ProfileAdapter adapter = getProfileAdapter();

        profilePosts.reset();
        profilePosts.setPagingAdapter(adapter);
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.profile_fragment, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        disposable.dispose();
    }

    private PostEventHandler getPostEventHandler() {
        return new PostEventHandler() {
            @Override
            public void onOpen(JsonFpost post) {

                NavHostFragment.findNavController(ProfileFragment.this).navigate(
                        ProfileFragmentDirections.actionProfileFragmentToPostFragment(post.id)
                );
            }

            @Override
            public void onRepost(JsonFpost post) {
                RepostSheetFragment fragment = new RepostSheetFragment(
                        data.myUserInfo.user,
                        post,
                        ProfileFragment.this::onRepost
                        );
                fragment.show(getChildFragmentManager(), RepostSheetFragment.TAG);
            }
        };
    }

    private ProfileAdapter getProfileAdapter() {
        return new ProfileAdapter(
                data.posts,
                getContext(),
                postsApi,
                getPostEventHandler(),
                userId
        );
    }
}
