package com.presentio.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;
import com.presentio.R;
import com.presentio.models.SearchRequest;

import java.util.List;

import io.objectbox.Box;

/**
 * This class is nearly copypasted from
 * com.mancj.materialsearchbar.adapter.DefaultSuggestionsAdapter
 * since that library didn't allow me to extend from it and override the
 * only necessary method
 */
public class ThemedSuggestionsAdapter extends SuggestionsAdapter<SearchRequest, ThemedSuggestionsAdapter.SuggestionHolder> {
    private final SuggestionsAdapter.OnItemViewClickListener listener;
    private final Box<SearchRequest> requests;

    public ThemedSuggestionsAdapter(
            LayoutInflater inflater,
            Box<SearchRequest> requests,
            MaterialSearchBar searchBar
    ) {
        super(inflater);

        this.requests = requests;
        listener = searchBar;
    }

    @Override
    public int getSingleViewHeight() {
        return 50;
    }

    @Override
    public SuggestionHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = getLayoutInflater().inflate(R.layout.themed_item_last_request, parent, false);
        return new SuggestionHolder(view);
    }

    @Override
    public void onBindSuggestionHolder(SearchRequest request, SuggestionHolder holder, int position) {
        holder.text.setText(getSuggestions().get(position).query);
    }

    private void removeSuggestionFromBox(SearchRequest request) {
        requests.remove(request);
    }

    class SuggestionHolder extends RecyclerView.ViewHolder {
        private final TextView text;

        public SuggestionHolder(final View itemView) {
            super(itemView);

            text = itemView.findViewById(R.id.text);
            ImageView ivDelete = itemView.findViewById(R.id.iv_delete);

            List<SearchRequest> suggestions = getSuggestions();

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();

                v.setTag(suggestions.get(position));
                listener.OnItemClickListener(position, v);
            });

            ivDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();

                if (position > 0 && position < suggestions.size()) {
                    v.setTag(suggestions.get(position));

                    removeSuggestionFromBox(suggestions.get(position));
                    listener.OnItemDeleteListener(position, v);
                }
            });
        }
    }
}
