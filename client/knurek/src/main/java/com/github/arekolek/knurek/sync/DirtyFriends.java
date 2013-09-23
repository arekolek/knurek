
package com.github.arekolek.knurek.sync;

import java.util.ArrayList;
import java.util.List;

public class DirtyFriends {

    List<Friend> list = new ArrayList<Friend>();

    public void add(Friend friend) {
        list.add(friend);
    }

    public int size() {
        return list.size();
    }

    public List<Friend> get() {
        return list;
    }

}
