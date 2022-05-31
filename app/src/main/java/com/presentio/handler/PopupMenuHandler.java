package com.presentio.handler;

import android.content.Context;
import android.widget.ImageView;
import android.widget.PopupMenu;

import com.presentio.js2p.structs.JsonFpost;
import com.presentio.view.PostFullView;

public abstract class PopupMenuHandler implements PostFullView.MenuHandler {
    private final Context context;

    protected PopupMenuHandler(Context context) {
        this.context = context;
    }

    @Override
    public void showMenu(ImageView threeDots, JsonFpost post, PostFullView.EventHandler handler) {
        PopupMenu popupMenu = new PopupMenu(context, threeDots);
        popupMenu.inflate(getMenuResId(post));

        popupMenu.setOnMenuItemClickListener(item -> {
            handleClick(item.getItemId(), post, handler);

            return true;
        });

        popupMenu.show();
    }

    protected abstract int getMenuResId(JsonFpost post);
    protected abstract void handleClick(int itemId, JsonFpost post, PostFullView.EventHandler handler);
}
