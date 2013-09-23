
package com.github.arekolek.knurek.sync;

import java.util.ArrayList;
import java.util.List;

public class DirtyFriends {

    List<Friend> deleted = new ArrayList<Friend>();
    List<Friend> updated = new ArrayList<Friend>();

    public void addDeleted(Friend friend) {
        deleted.add(friend);
    }

    public int size() {
        return updated.size() + deleted.size();
    }

    public void addUpdated(Friend friend) {
        updated.add(friend);
    }

}
