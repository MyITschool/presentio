package com.presentio.handler;

import android.content.Context;

import com.presentio.R;
import com.presentio.js2p.structs.JsonFpost;
import com.presentio.view.PostFullView;

public class DefaultMenuHandler extends NoOpenMenuHandler {
    public DefaultMenuHandler(Context context) {
        super(context);
    }

    @Override
    protected int getMenuResId(JsonFpost post) {
        return post.own ? R.menu.own_post_menu : R.menu.post_menu;
    }

    @Override
    protected void handleClick(int itemId, JsonFpost post, PostFullView.EventHandler handler) {
        if (itemId == R.id.post_open) {
            handler.onOpen(post);
            return;
        }

        super.handleClick(itemId, post, handler);
    }
}
