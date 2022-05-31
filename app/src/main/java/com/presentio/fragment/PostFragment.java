package com.presentio.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.presentio.R;
import com.presentio.adapter.CommentsAdapter;
import com.presentio.di.DaggerOkHttpComponent;
import com.presentio.di.OkHttpComponent;
import com.presentio.di.OkHttpModule;
import com.presentio.handler.AwareListEventHandler;
import com.presentio.handler.NoOpenMenuHandler;
import com.presentio.js2p.structs.JsonFpost;
import com.presentio.js2p.structs.JsonComment;
import com.presentio.js2p.structs.JsonUserInfo;
import com.presentio.requests.CreateCommentRequest;
import com.presentio.requests.DataHandler;
import com.presentio.requests.GetCommentsRequest;
import com.presentio.requests.GetPostRequest;
import com.presentio.requests.GetUserInfoRequest;
import com.presentio.requests.RepostRequest;
import com.presentio.http.Api;
import com.presentio.util.ObservableUtil;
import com.presentio.util.ViewUtil;
import com.presentio.view.InfiniteRecyclerView;
import com.presentio.view.PostFullView;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class PostFragment extends RefreshDataFragment<PostFragment.PostData> {
    private long postId;
    private final CompositeDisposable disposable = new CompositeDisposable();
    private Api postsApi, usersApi;

    @Inject
    OkHttpClient client;

    public static class PostData {
        JsonFpost post;
        ArrayList<JsonComment> comments = new ArrayList<>();
        JsonUserInfo userInfo;
    }

    @Override
    protected void initialize() {
        data = new PostData();

        Bundle args = getArguments();

        if (args == null) {
            throw new RuntimeException("Args expected in PostFragment");
        }
        
        postId = PostFragmentArgs.fromBundle(args).getPostId();

        OkHttpComponent component = DaggerOkHttpComponent.builder().okHttpModule(
                new OkHttpModule(getContext())
        ).build();

        component.inject(this);

        postsApi = new Api(getContext(), client, Api.HOST_POSTS_SERVICE);
        usersApi = new Api(getContext(), client, Api.HOST_USER_SERVICE);
    }

    @Override
    protected View getLoaderView(View view) {
        return view.findViewById(R.id.full_post_loader);
    }

    @Override
    protected View getDataView(View view) {
        return view.findViewById(R.id.full_post_wrapper);
    }

    private abstract static class PostDataHandler implements DataHandler {
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
        requestData(new PostDataHandler() {
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
        requestData(new PostDataHandler() {
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
                new GetPostRequest(handler, postsApi, postId),
                new SingleObserver<JsonFpost>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(JsonFpost jsonPost) {
                        data.post = jsonPost;
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
                new GetUserInfoRequest(handler, usersApi),
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
                new GetCommentsRequest(handler, postsApi, postId, 0),
                new SingleObserver<ArrayList<JsonComment>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(ArrayList<JsonComment> jsonComments) {
                        data.comments = jsonComments;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.post_fragment, container, false);
    }

    @Override
    protected void initializeView(View view, boolean success) {
        if (success) {
            fillView(view);
        } else {
            Toast.makeText(getContext(), getString(R.string.data_init_fail), Toast.LENGTH_SHORT).show();
        }

        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.full_post_wrapper);
        refreshLayout.setOnRefreshListener(() -> refreshLayout.setRefreshing(onRefresh()));
    }

    @Override
    protected void refreshView(View view, boolean success) {
        if (success) {
            fillView(view);
        } else {
            Toast.makeText(getContext(), getString(R.string.data_refresh_fail), Toast.LENGTH_SHORT).show();
        }

        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.full_post_wrapper);
        refreshLayout.setRefreshing(false);
    }

    private void fillView(View view) {
        PostFullView post = view.findViewById(R.id.full_post);

        post.fillView(data.post, new AwareListEventHandler(getContext(), postsApi, disposable, post) {
            @Override
            protected void removeItem() {
                getFragmentManager().popBackStack();
            }

            @Override
            public void onOpen(JsonFpost post) {
                // will never be called
            }

            @Override
            public void onHitRepost(JsonFpost post) {
                RepostSheetFragment fragment = new RepostSheetFragment(
                        data.userInfo.user,
                        post,
                        PostFragment.this::onRepost
                );
                fragment.show(getChildFragmentManager(), RepostSheetFragment.TAG);
            }
        }, new NoOpenMenuHandler(getContext()));

        fillComments(view);
    }

    private void fillComments(View view) {
        view.findViewById(R.id.full_post_comments_menu).setVisibility(View.VISIBLE);

        ViewUtil.setVisible(view.findViewById(R.id.full_post_no_comments), data.comments.size() == 0);

        InfiniteRecyclerView commentsView = view.findViewById(R.id.full_post_comments);

        commentsView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        commentsView.setPagingAdapter(new CommentsAdapter(data.comments, postsApi, postId));

        fillCommentBox(view);
    }

    private void fillCommentBox(View view) {
        ImageView userImage = view.findViewById(R.id.new_comment_user_image);
        TextView userUserName = view.findViewById(R.id.new_comment_user_username);
        ImageView commentAdd = view.findViewById(R.id.new_comment_send);

        userImage.setClipToOutline(true);
        Picasso.get().load(data.userInfo.user.pfpUrl).into(userImage);

        userUserName.setText(data.userInfo.user.name);

        commentAdd.setOnClickListener(v -> {
            TextView commentText = view.findViewById(R.id.new_comment_text);
            String text = commentText.getText().toString().trim();

            commentText.setText("");

            if (text.isEmpty()) {
                return;
            }

            uploadComment(text);
        });
    }

    private void addComment(JsonComment comment) {
        View view = getView();

        view.findViewById(R.id.full_post_no_comments).setVisibility(View.GONE);

        InfiniteRecyclerView commentsView = view.findViewById(R.id.full_post_comments);

        comment.user = data.userInfo.user;
        data.comments.add(0, comment);
        commentsView.getPagingAdapter().notifyItemInserted(0);
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
                        if (integer == 201) {
                            Toast.makeText(getContext(), "Repost made", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Toast.makeText(getContext(), "Unable to create post", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof Api.InvalidCredentialsException) {
                            Api.logout();
                            return;
                        }

                        Toast.makeText(getContext(), "Unable to connect to server", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void uploadComment(String text) {
        ObservableUtil.singleIo(
                new CreateCommentRequest(postsApi, text, postId),
                new SingleObserver<JsonComment>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(JsonComment comment) {
                        addComment(comment);
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof Api.InvalidCredentialsException) {
                            Api.logout();
                            return;
                        }

                        Toast.makeText(getContext(), "Unable to post comment", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        disposable.dispose();
    }
}
