package com.presentio.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.presentio.R;
import com.presentio.adapter.ImagePagerAdapter;
import com.presentio.adapter.TagAdapter;
import com.presentio.js2p.structs.JsonFpost;
import com.presentio.js2p.structs.JsonUser;
import com.presentio.util.StringFormatUtil;
import com.presentio.util.ViewUtil;
import com.squareup.picasso.Picasso;
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator;

public class PostFullView extends ConstraintLayout {
    public interface EventHandler {
        void onDelete(JsonFpost post);
        void onOpen(JsonFpost post);
        void onHide(JsonFpost post);
        void onHitLike(JsonFpost post);
        void onHitComment(JsonFpost post);
        void onHitRepost(JsonFpost post);
        void onHitFavorite(JsonFpost post);
    }

    public interface MenuHandler {
        void showMenu(ImageView threeDots, JsonFpost post, EventHandler handler);
    }

    public PostFullView(Context context) {
        super(context);
        initialize(context);
    }

    public PostFullView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public PostFullView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    public PostFullView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initialize(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.post_full_view, this);
    }

    public void fillView(JsonFpost post, EventHandler eventHandler, MenuHandler menuHandler) {
        fillIndicators(post);

        JsonFpost originalPost = post;

        setupIndicatorListeners(post, eventHandler);

        ImageView threeDots;
        String pfpUrl;

        ViewUtil.setVisible(findViewById(R.id.original_post_wrapper), post.sourceId != null);
        ViewUtil.setVisible(findViewById(R.id.post_three_dots), post.sourceId == null);

        if (post.sourceId != null) {
            fillPostHeader(post, post.user);

            findViewById(R.id.post_wrapper).setPadding(getResources().getDimensionPixelSize(R.dimen.repostPaddingDimen), 0, 0, 0);

            threeDots = findViewById(R.id.original_post_three_dots);

            pfpUrl = post.sourceUser.pfpUrl;
            post = post.source;
        } else {
            fillText(post, post.user);

            findViewById(R.id.post_wrapper).setPadding(0, 0, 0, 0);

            threeDots = findViewById(R.id.post_three_dots);

            pfpUrl = post.user.pfpUrl;
        }

        threeDots.setOnClickListener(v -> menuHandler.showMenu(threeDots, originalPost, eventHandler));
        fillImage(findViewById(R.id.user_image), pfpUrl);

        fillDetails(post);
    }

    public void likeView(JsonFpost post) {
        ImageView likeButton = findViewById(R.id.like_button);

        likeButton.setImageTintList(ColorStateList.valueOf(Color.RED));
        likeButton.setImageResource(R.drawable.ic_like_filled);

        TextView likeCounter = findViewById(R.id.like_counter);
        likeCounter.setText(StringFormatUtil.format(++post.likes));
    }

    public void removeViewLike(JsonFpost post) {
        int color = MaterialColors.getColor(getContext(), R.attr.lowestAttentionTextColor, Color.GRAY);

        ImageView likeButton = findViewById(R.id.like_button);

        likeButton.setImageTintList(ColorStateList.valueOf(color));
        likeButton.setImageResource(R.drawable.ic_like_outlined);

        TextView likeCounter = findViewById(R.id.like_counter);
        likeCounter.setText(StringFormatUtil.format(--post.likes));
    }

    public void addToFavorites(JsonFpost post) {
        ImageView favoriteButton = findViewById(R.id.favorite_button);

        favoriteButton.setImageResource(R.drawable.ic_bookmark_filled);
    }

    public void removeFromFavorites(JsonFpost post) {
        ImageView favoriteButton = findViewById(R.id.favorite_button);

        favoriteButton.setImageResource(R.drawable.ic_bookmark_outlined);
    }

    private void fillDetails(JsonFpost post) {
        ViewUtil.setVisible(findViewById(R.id.image_slider), post.sourceId == null);
        ViewUtil.setVisible(findViewById(R.id.tag_slider), post.sourceId == null);
        ViewUtil.setVisible(findViewById(R.id.image_indicator), post.sourceId == null);

        if (post.sourceId == null) {
            fillPager(post);
            fillTags(post);
        }
    }

    private void fillImage(ImageView view, String pfpUrl) {
        view.setClipToOutline(true);
        Picasso.get().load(pfpUrl).into(view);
    }

    private void fillPostHeader(JsonFpost post, JsonUser user) {
        TextView postText = findViewById(R.id.original_post_text),
                userUsername = findViewById(R.id.original_user_username);

        postText.setText(post.text);
        userUsername.setText(user.name);

        fillImage(findViewById(R.id.original_user_image), user.pfpUrl);

        fillText(post.source, post.sourceUser);
    }

    private void fillPager(JsonFpost post) {
        RatioViewPager pager = findViewById(R.id.image_slider);

        pager.setClipToOutline(true);
        pager.WHRatio = post.photoRatio.floatValue();

        ImagePagerAdapter imageAdapter = new ImagePagerAdapter(post.attachments);
        pager.setAdapter(imageAdapter);

        SpringDotsIndicator pageIndicator = findViewById(R.id.image_indicator);

        pageIndicator.setViewPager(pager);
    }

    private void fillText(JsonFpost post, JsonUser user) {
        TextView postText = findViewById(R.id.post_text),
                userUsername = findViewById(R.id.user_username);

        postText.setText(post.text);
        userUsername.setText(user.name);
    }

    private void fillIndicators(JsonFpost post) {
        TextView likeCounter = findViewById(R.id.like_counter),
                commentCounter = findViewById(R.id.comment_counter),
                repostCounter = findViewById(R.id.repost_counter);

        likeCounter.setText(StringFormatUtil.format(post.likes));
        commentCounter.setText(StringFormatUtil.format(post.comments));
        repostCounter.setText(StringFormatUtil.format(post.reposts));

        if (post.liked.id != 0) {
            --post.likes;
            likeView(post);
        }

        if (post.favorite.id != 0) {
            addToFavorites(post);
        }
    }

    private void fillTags(JsonFpost post) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);

        RecyclerView tags = findViewById(R.id.tag_slider);

        TagAdapter tagAdapter = new TagAdapter(post.tags, getContext());
        tags.setAdapter(tagAdapter);
        tags.setLayoutManager(layoutManager);
    }

    private void setupIndicatorListeners(JsonFpost post, EventHandler eventHandler) {
        ImageView likeButton = findViewById(R.id.like_button),
                commentButton = findViewById(R.id.comment_button),
                repostButton = findViewById(R.id.repost_button),
                favoriteButton = findViewById(R.id.favorite_button);

        likeButton.setOnClickListener(v -> eventHandler.onHitLike(post));
        commentButton.setOnClickListener(v -> eventHandler.onHitComment(post));
        repostButton.setOnClickListener(v -> eventHandler.onHitRepost(post));
        favoriteButton.setOnClickListener(v -> eventHandler.onHitFavorite(post));
    }
}