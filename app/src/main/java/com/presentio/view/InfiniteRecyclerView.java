package com.presentio.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.util.ArrayList;

import io.reactivex.rxjava3.disposables.Disposable;

public class InfiniteRecyclerView extends RecyclerView {
    private int page = 0;
    private boolean finished = false, loading = false;
    private PagingAdapter pagingAdapter;

    public final double threshold = 0.75;

    public abstract static class PagingAdapter<T, VH extends ViewHolder> extends Adapter<VH> {
        protected final ArrayList<T> data;

        protected PagingAdapter(ArrayList<T> data) {
            this.data = data;
        }

        public abstract Disposable getDisposable();
        protected abstract void loadData(InfiniteRecyclerView view, int page);
    }

    public InfiniteRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LayoutManager manager = getLayoutManager();

                int items = getAdapter().getItemCount(), position = -2;

                if (items == 0) {
                    return;
                }

                if (manager instanceof LinearLayoutManager) {
                    position = ((LinearLayoutManager) manager).findLastVisibleItemPosition();
                } else if (manager instanceof StaggeredGridLayoutManager) {
                    position = ((StaggeredGridLayoutManager) manager).findLastVisibleItemPositions(null)[0];
                }

                if (position == NO_POSITION) {
                    return;
                }

                if (position == -2) {
                    throw new RuntimeException("Unexpected layout manager type");
                }

                if ((double) position / items >= threshold && !finished && !loading) {
                    loading = true;
                    pagingAdapter.loadData(InfiniteRecyclerView.this, ++page);
                }
            }
        });
    }

    public InfiniteRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, androidx.recyclerview.R.attr.recyclerViewStyle);
    }

    public InfiniteRecyclerView(Context context) {
        this(context, null);
    }

    public void finishLoading(boolean finished) {
        this.finished = finished;
        this.loading = false;
    }

    public PagingAdapter getPagingAdapter() {
        return pagingAdapter;
    }

    public void setPagingAdapter(PagingAdapter pagingAdapter) {
        this.pagingAdapter = pagingAdapter;
        setAdapter(pagingAdapter);
    }

    public void reset() {
        page = 0;
        finished = false;

        PagingAdapter prevAdapter = getPagingAdapter();

        if (prevAdapter != null && prevAdapter.getDisposable() != null) {
            prevAdapter.getDisposable().dispose();
        }
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
