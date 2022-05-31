package com.presentio.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.presentio.R;
import com.presentio.adapter.ImagePagerAdapter;
import com.presentio.js2p.structs.JsonFpost;
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator;

public class PostGridView extends ConstraintLayout {
    public interface EventHandler {
        void onOpen(JsonFpost post);
    }

    public PostGridView(Context context) {
        super(context);
        initialize(context);
    }

    public PostGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public PostGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    public PostGridView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initialize(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.post_grid_view, this);
    }

    public void fillView(JsonFpost post, EventHandler eventHandler) {
        // TODO extract with PostFullView
        RatioViewPager pager = findViewById(R.id.image_slider);

        pager.setClipToOutline(true);
        pager.WHRatio = post.photoRatio.floatValue();

        ImagePagerAdapter imageAdapter = new ImagePagerAdapter(post.attachments);
        pager.setAdapter(imageAdapter);

        SpringDotsIndicator pageIndicator = findViewById(R.id.image_indicator);

        pageIndicator.setViewPager(pager);

        setOnClickListener(v -> eventHandler.onOpen(post));
    }
}
