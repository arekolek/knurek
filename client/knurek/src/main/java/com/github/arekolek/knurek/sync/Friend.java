
package com.github.arekolek.knurek.sync;

import android.text.TextUtils;

public class Friend {

    String real_name;

    String name;

    boolean image;

    public String getDisplayName() {
        if (TextUtils.isEmpty(real_name)) {
            return name;
        }
        return real_name;
    }
}
