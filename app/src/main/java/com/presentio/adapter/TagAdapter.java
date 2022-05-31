package com.presentio.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.presentio.R;
import com.presentio.js2p.structs.JsonTag;
import com.presentio.util.DoubleClickListener;

import java.util.List;
import java.util.concurrent.Callable;

public class TagAdapter extends RecyclerView.Adapter<TagAdapter.ViewHolder> {
    private List<JsonTag> tags;
    private Context context;
    private OnClickHandler handler;

    public interface OnClickHandler {
        void onClick(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public TextView tag;

        public ViewHolder(View itemView) {
            super(itemView);

            tag = itemView.findViewById(R.id.tag_item);
        }
    }

    public TagAdapter(List<JsonTag> tags, Context context) {
        this(tags, context, null);
    }

    public TagAdapter(List<JsonTag> tags, Context context, OnClickHandler handler) {
        this.tags = tags;
        this.context = context;
        this.handler = handler;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tag_item, parent,false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String tag = tags.get(position).name;

        int color = MaterialColors.getColor(context, R.attr.inactiveTextColor, Color.GRAY);

        SpannableString text = new SpannableString("#" + tag);
        text.setSpan(new ForegroundColorSpan(color), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new RelativeSizeSpan(1.2f), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        holder.tag.setText(text);

        holder.tag.setOnClickListener(new DoubleClickListener() {
            @Override
            public void onDoubleClick() {
                if (handler != null) {
                    handler.onClick(holder.getAdapterPosition());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    public void setHandler(OnClickHandler handler) {
        this.handler = handler;
    }
}
