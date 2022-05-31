package com.presentio.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.Fragment;

public abstract class RefreshDataFragment<T> extends Fragment {
    protected T data;

    private boolean loading = false, creatingView = false;

    private boolean lastInitialLoadSuccess = true;

    private View createdView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initialize();

        loading = true;
        creatingView = true;
        loadInitialData();
    }

    protected void onInitialLoadFinished(boolean success) {
        loading = false;

        lastInitialLoadSuccess = success;

        if (!creatingView) {
            doInitializeView(createdView, success);
        }
    }

    protected void onRefreshFinished(boolean success) {
        loading = false;

        refreshView(getView(), success);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        createdView = view;
        creatingView = false;

        if (!loading) {
            doInitializeView(view, lastInitialLoadSuccess);
        }
    }

    protected boolean onRefresh() {
        if (!loading) {
            loading = true;
            refreshData();

            return true;
        }

        return false;
    }

    private void doInitializeView(View view, boolean success) {
        initializeView(view, success);

        getLoaderView(view).setVisibility(View.GONE);
        getDataView(view).setVisibility(View.VISIBLE);
    }

    protected void initialize() {}

    protected abstract View getLoaderView(View view);

    protected abstract View getDataView(View view);

    protected abstract void loadInitialData();

    protected abstract void refreshData();

    protected abstract void initializeView(View view, boolean success);

    protected abstract void refreshView(View view, boolean success);
}
