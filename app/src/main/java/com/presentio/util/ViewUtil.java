package com.presentio.util;

import android.view.View;

public class ViewUtil {
    public static void setVisible(View view, boolean expr) {
        view.setVisibility(expr ? View.VISIBLE : View.GONE);
    }
}
