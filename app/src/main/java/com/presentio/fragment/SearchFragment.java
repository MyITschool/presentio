package com.presentio.fragment;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.tabs.TabLayout;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.SimpleOnSearchActionListener;
import com.presentio.R;
import com.presentio.adapter.SearchAdapter;
import com.presentio.adapter.ThemedSuggestionsAdapter;
import com.presentio.adapter.UserSearchAdapter;
import com.presentio.db.ObjectBox;
import com.presentio.di.DaggerOkHttpComponent;
import com.presentio.di.OkHttpComponent;
import com.presentio.di.OkHttpModule;
import com.presentio.handler.PostEventHandler;
import com.presentio.js2p.structs.JsonFpost;
import com.presentio.js2p.structs.JsonUser;
import com.presentio.js2p.structs.JsonUserInfo;
import com.presentio.models.SearchRequest;
import com.presentio.models.SearchRequest_;
import com.presentio.requests.DataHandler;
import com.presentio.requests.Finisher;
import com.presentio.requests.GetUserInfoRequest;
import com.presentio.requests.PostSearchRequest;
import com.presentio.requests.RepostRequest;
import com.presentio.http.Api;
import com.presentio.requests.UserSearchRequest;
import com.presentio.util.ObservableUtil;
import com.presentio.util.ViewUtil;
import com.presentio.view.CustomMaterialSearchBar;
import com.presentio.view.InfiniteRecyclerView;
import com.presentio.view.PostsView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.objectbox.Box;
import io.objectbox.query.Query;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

import okhttp3.OkHttpClient;
import okhttp3.Response;

public class SearchFragment extends RefreshDataFragment<SearchFragment.SearchData> {
    private enum Screen {
        DEFAULT,
        SEARCH
    }

    private static final int POST_TAB_POSITION = 0;
    private static final int USER_TAB_POSITION = 1;

    private static final int MAX_SUGGESTIONS_COUNT = 10;

    private final Box<SearchRequest> requestBox = ObjectBox.get().boxFor(SearchRequest.class);
    private List<SearchRequest> lastSuggestions = null;

    private final CompositeDisposable disposable = new CompositeDisposable();

    private Screen screen = Screen.DEFAULT;

    private Api postsApi, usersApi;

    @Inject
    OkHttpClient client;

    @Override
    protected void initialize() {
        OkHttpComponent component = DaggerOkHttpComponent.builder().okHttpModule(
                new OkHttpModule(getContext())
        ).build();

        component.inject(this);

        data = new SearchData();

        postsApi = new Api(getContext(), client, Api.HOST_POSTS_SERVICE);
        usersApi = new Api(getContext(), client, Api.HOST_USER_SERVICE);
    }

    public static class SearchData {
        public ArrayList<JsonFpost> posts = new ArrayList<>();
        public ArrayList<JsonUser> users = new ArrayList<>();
        public String query, postRequestParams, userRequestParams;
        public int postPage, userPage;
        public JsonUserInfo userInfo;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_fragment, container, false);

        CustomMaterialSearchBar searchBar = view.findViewById(R.id.search_input);

        ThemedSuggestionsAdapter suggestionsAdapter =
                new ThemedSuggestionsAdapter(inflater, requestBox, searchBar);

        searchBar.setSuggestionsAdapter(suggestionsAdapter);
        searchBar.setMaxSuggestionCount(MAX_SUGGESTIONS_COUNT);

        return view;
    }

    @Override
    protected View getLoaderView(View view) {
        return view.findViewById(R.id.explore_loader);
    }

    @Override
    protected View getDataView(View view) {
        return view.findViewById(R.id.explore_results);
    }

    private abstract static class SearchDataHandler implements DataHandler {
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
        requestData(new SearchDataHandler() {
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
        if (screen == Screen.DEFAULT) {
            refreshDefault();
        } else {
            refreshSearch();
        }
    }

    private void requestData(DataHandler handler) {
        ObservableUtil.singleIo(
                this::findQueries,
                new SingleObserver<List<SearchRequest>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(List<SearchRequest> searchRequests) {
                        lastSuggestions = searchRequests;
                        handler.finish(true);
                    }

                    @Override
                    public void onError(Throwable e) {
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
    }

    private void toExploreScreen() {
        View view = getView();

        view.findViewById(R.id.results_screen).setVisibility(View.GONE);
        view.findViewById(R.id.explore_screen).setVisibility(View.VISIBLE);

        screen = Screen.DEFAULT;
    }

    private void toSearchScreen() {
        View view = getView();

        view.findViewById(R.id.results_screen).setVisibility(View.VISIBLE);
        view.findViewById(R.id.explore_screen).setVisibility(View.GONE);

        screen = Screen.SEARCH;

        resetTab();
    }

    @Override
    protected void initializeView(View view, boolean success) {
        if (success) {
            fillViewInitial(view);
        } else {
            Toast.makeText(getContext(), "Failed to fetch data", Toast.LENGTH_SHORT).show();
        }

        CustomMaterialSearchBar searchBar = view.findViewById(R.id.search_input);

        ThemedSuggestionsAdapter suggestionsAdapter = getSuggestionsAdapter(searchBar);

        searchBar.setCustomSuggestionAdapter(suggestionsAdapter);
        searchBar.setMaxSuggestionCount(MAX_SUGGESTIONS_COUNT);

        searchBar.getPlaceHolderView().setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);

        if (lastSuggestions == null) {
            lastSuggestions = searchBar.getLastSuggestions();
        }

        searchBar.setLastSuggestions(lastSuggestions);

        searchBar.setOnSearchActionListener(getSearchListener(suggestionsAdapter));

        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.refresh_search);
        refreshLayout.setOnRefreshListener(() -> refreshLayout.setRefreshing(onRefresh()));

        setupTabListener(view);
    }

    @Override
    protected void refreshView(View view, boolean success) {
        if (success) {
            fillViewRefresh(view);
        }

        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.refresh_search);
        refreshLayout.setRefreshing(false);
    }

    private void fillViewInitial(View view) {
        if (screen == Screen.DEFAULT) {
            fillDefault(view);
        } else {
            fillSearch(view);
        }
    }

    private void fillViewRefresh(View view) {
        if (screen != Screen.DEFAULT) {
            return;
        }

        fillDefault(view);
    }

    private void refreshDefault() {
        onRefreshFinished(true);
    }

    private void refreshSearch() {
        performSearch(data.query, this::onRefreshFinished);
    }

    private void fillDefault(View view) {
        // TODO implement when getting data from server is implemented
    }

    private void fillSearch(View view) {
        toSearchScreen();

        finishSearchLoading();
        showResults(true);
    }

    private MaterialSearchBar.OnSearchActionListener getSearchListener(
            ThemedSuggestionsAdapter suggestionsAdapter
    ) {
        return new SimpleOnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                if (!enabled) {
                    toExploreScreen();
                }
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                initiateSearch(text.toString(), suggestionsAdapter);
            }
        };
    }

    private void initiateSearch(String text, ThemedSuggestionsAdapter suggestionsAdapter) {
        toSearchScreen();
        setSearchLoading();

        SearchRequest request = new SearchRequest();
        request.query = text;
        requestBox.put(request);

        suggestionsAdapter.addSuggestion(request);

        performSearch(text, this::onNewSearchDone);
    }

    private interface OnSearchDoneHandler {
        void onSearchDone(boolean success);
    }

    private void performSearch(String query, OnSearchDoneHandler callback) {
        data.query = query;
        data.postRequestParams = formPostRequestParameters(query);
        data.userRequestParams = formUserRequestParameters(query);

        Finisher finisher = new Finisher() {
            private static final int TOTAL = 2;
            private int completed = 0;
            private boolean success = true;
            
            @Override
            public void finish(boolean success) {
                this.success &= success;
                
                if (++completed == TOTAL) {
                    callback.onSearchDone(this.success);
                    
                    if (this.success) {
                        showResults(false);
                    } else {
                        Toast.makeText(getContext(), "Failed to load search results", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };

        ObservableUtil.singleIo(
                new PostSearchRequest(postsApi, 0, data.postRequestParams),
                new SingleObserver<ArrayList<JsonFpost>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(ArrayList<JsonFpost> jsonPosts) {
                        // Reference comparison as expected!
                        if (query != data.query) {
                            return;
                        }

                        data.posts = jsonPosts;
                        finisher.finish(true);
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof Api.InvalidCredentialsException) {
                            Api.logout();
                            return;
                        }

                        finisher.finish(false);
                    }
                }
        );

        ObservableUtil.singleIo(
                new UserSearchRequest(usersApi, 0, data.userRequestParams),
                new SingleObserver<ArrayList<JsonUser>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(ArrayList<JsonUser> jsonUsers) {
                        // Reference comparison as expected!
                        if (query != data.query) {
                            return;
                        }

                        data.users = jsonUsers;
                        finisher.finish(true);
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof Api.InvalidCredentialsException) {
                            Api.logout();
                            return;
                        }

                        finisher.finish(false);
                    }
                }
        );
    }

    private void onNewSearchDone(boolean success) {
        finishSearchLoading();
    }

    private void showResults(boolean restored) {
        View view = getView();

        ViewUtil.setVisible(view.findViewById(R.id.post_result_not_found), data.posts.size() == 0);

        PostsView postSearchResults = view.findViewById(R.id.post_search_results);

        if (restored) {
            postSearchResults.setPage(data.postPage);
        } else {
            postSearchResults.reset();
        }
        postSearchResults.reset();
        postSearchResults.setPagingAdapter(getPostPagingAdapter());

        ViewUtil.setVisible(view.findViewById(R.id.user_result_not_found), data.users.size() == 0);

        InfiniteRecyclerView userSearchResults = view.findViewById(R.id.user_search_results);

        if (restored) {
            userSearchResults.setPage(data.userPage);
        } else {
            userSearchResults.reset();
        }
        userSearchResults.setPagingAdapter(getUserPagingAdapter());
        userSearchResults.setLayoutManager(new LinearLayoutManager(
                getContext(),
                LinearLayoutManager.HORIZONTAL,
                false
        ));
    }

    private void setSearchLoading() {
        View view = getView();

        view.findViewById(R.id.post_search_results).setVisibility(View.GONE);
        view.findViewById(R.id.post_result_not_found).setVisibility(View.GONE);
        view.findViewById(R.id.user_search_results).setVisibility(View.GONE);
        view.findViewById(R.id.user_result_not_found).setVisibility(View.GONE);
        view.findViewById(R.id.results_loader).setVisibility(View.VISIBLE);
    }

    private void finishSearchLoading() {
        View view = getView();

        view.findViewById(R.id.post_search_results).setVisibility(View.VISIBLE);
        view.findViewById(R.id.post_result_not_found).setVisibility(View.GONE);
        view.findViewById(R.id.user_search_results).setVisibility(View.VISIBLE);
        view.findViewById(R.id.user_result_not_found).setVisibility(View.GONE);
        view.findViewById(R.id.results_loader).setVisibility(View.GONE);
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

    private List<SearchRequest> findQueries() {
        List<SearchRequest> lastRequests = requestBox
                .query()
                .orderDesc(SearchRequest_.id)
                .build()
                .find(0, MAX_SUGGESTIONS_COUNT);

        long minId = 0;

        if (lastRequests.size() > 0) {
            minId = lastRequests.get(lastRequests.size() - 1).id;
        }

        requestBox
                .query()
                .less(SearchRequest_.id, minId)
                .build()
                .remove();

        Query<SearchRequest> requestQuery = requestBox
                .query()
                .orderDesc(SearchRequest_.id)
                .build();

        return requestQuery.find(0, MAX_SUGGESTIONS_COUNT);
    }

    private String formPostRequestParameters(String query) {
        StringBuilder builder = new StringBuilder();

        boolean first = true;

        for (String part : query.split(" ")) {
            part = part.trim();

            if (part.isEmpty()) {
                continue;
            }

            String requestPart;

            if (part.charAt(0) == '#') {
                requestPart = "tag=" + part.substring(1);
            } else {
                requestPart = "keyword=" + part;
            }

            if (!first) {
                builder.append('&');
            }

            builder.append(requestPart);
            first = false;
        }

        return builder.toString();
    }
    
    private String formUserRequestParameters(String query) {
        StringBuilder builder = new StringBuilder();

        boolean first = true;

        for (String part : query.split(" ")) {
            part = part.trim();

            if (part.isEmpty()) {
                continue;
            }
            
            if (part.charAt(0) == '#') {
                continue;
            }

            if (!first) {
                builder.append('&');
            }
            
            builder.append("keyword=").append(part);
            first = false;
        }
        
        return builder.toString();
    }

    private ThemedSuggestionsAdapter getSuggestionsAdapter(CustomMaterialSearchBar searchBar) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        return new ThemedSuggestionsAdapter(inflater, requestBox, searchBar);
    }

    private SearchAdapter getPostPagingAdapter() {
        return new SearchAdapter(data, getContext(), postsApi, getPostEventHandler());
    }

    private UserSearchAdapter getUserPagingAdapter() {
        return new UserSearchAdapter(data.users, data.userRequestParams, getContext(), usersApi, user -> {
            NavHostFragment hostFragment = (NavHostFragment) getParentFragment();

            NavController controller = hostFragment.getNavController();

            savePages();

            controller.navigate(
                    SearchFragmentDirections.actionSearchFragmentToProfileFragment(user.id)
            );
        });
    }

    private PostEventHandler getPostEventHandler() {
        return new PostEventHandler() {
            @Override
            public void onOpen(JsonFpost post) {
                NavHostFragment hostFragment = (NavHostFragment) getParentFragment();

                NavController controller = hostFragment.getNavController();

                savePages();

                controller.navigate(SearchFragmentDirections.actionSearchFragmentToPostFragment(post.id));
            }

            @Override
            public void onRepost(JsonFpost post) {
                RepostSheetFragment fragment = new RepostSheetFragment(data.userInfo.user, post, SearchFragment.this::onRepost);
                fragment.show(getChildFragmentManager(), RepostSheetFragment.TAG);
            }
        };
    }

    private void savePages() {
        PostsView postsView = getView().findViewById(R.id.post_search_results);

        data.postPage = postsView.getPage();

        InfiniteRecyclerView usersView = getView().findViewById(R.id.user_search_results);

        data.userPage = usersView.getPage();
    }

    private void setupTabListener(View view) {
        TabLayout tabs = view.findViewById(R.id.search_tab_select);

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                ViewUtil.setVisible(
                        view.findViewById(R.id.post_result_wrapper),
                        tab.getPosition() == POST_TAB_POSITION
                );

                ViewUtil.setVisible(
                        view.findViewById(R.id.user_result_wrapper),
                        tab.getPosition() == USER_TAB_POSITION
                );
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // nothing to do in tab unselected
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

                SwipeRefreshLayout refreshLayout = view.findViewById(R.id.refresh_search);
                refreshLayout.setRefreshing(onRefresh());
            }
        });
    }

    private void resetTab() {
        View view = getView();

        TabLayout tabs = view.findViewById(R.id.search_tab_select);

        if (tabs.getSelectedTabPosition() == POST_TAB_POSITION) {
            return;
        }

        tabs.selectTab(tabs.getTabAt(POST_TAB_POSITION));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        disposable.dispose();
    }
}
