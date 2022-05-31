package com.presentio.handler;

import android.content.Context;

import com.presentio.R;
import com.presentio.js2p.structs.JsonFpost;
import com.presentio.view.PostFullView;

public class NoOpenMenuHandler extends PopupMenuHandler {
    public NoOpenMenuHandler(Context context) {
        super(context);
    }

    @Override
    protected int getMenuResId(JsonFpost post) {
        return post.own ? R.menu.noopen_own_post_menu : R.menu.noopen_post_menu;
    }

    @Override
    protected void handleClick(int itemId, JsonFpost post, PostFullView.EventHandler handler) {
        if (itemId == R.id.post_delete) {
            handler.onDelete(post);
        } else if (itemId == R.id.post_hide) {
            handler.onHide(post);
        } else if (itemId == R.id.post_not_interested) {
            handler.onNotInterested(post);
        }
    }
}
