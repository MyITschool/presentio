package com.presentio.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;

public class CustomMaterialSearchBar extends MaterialSearchBar {
    private SuggestionsAdapter suggestionsAdapter;

    public CustomMaterialSearchBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomMaterialSearchBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomMaterialSearchBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setSuggestionsAdapter(SuggestionsAdapter suggestionsAdapter) {
        this.suggestionsAdapter = suggestionsAdapter;

        setCustomSuggestionAdapter(suggestionsAdapter);
    }

    /**
     * Works with any tag type
     */
    @Override
    public void OnItemClickListener(int position, View v) {
        getSearchEditText().setText(v.getTag().toString());
    }

    public SuggestionsAdapter getSuggestionsAdapter() {
        return suggestionsAdapter;
    }

    /**
     * Works with any tag type
     */
    @Override
    public void OnItemDeleteListener(int position, View v) {
        Object o = v.getTag();
        
        if (o instanceof String) {
            super.OnItemDeleteListener(position, v);
        } else {
            v.setTag(o.toString());
            super.OnItemDeleteListener(position, v);

            suggestionsAdapter.deleteSuggestion(position, o);
            v.setTag(o);
        }
    }
}
