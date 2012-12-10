
package com.github.arekolek.knurek.sync;

import android.text.TextUtils;

public class Friend {

    String realname;

    String name;

    String image;

    public String getDisplayName() {
        if (TextUtils.isEmpty(realname)) {
            return name;
        }
        return realname;
    }
}
