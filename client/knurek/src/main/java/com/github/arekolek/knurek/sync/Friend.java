
package com.github.arekolek.knurek.sync;

import android.text.TextUtils;

public class Friend {

    String real_name;

    String name;

    boolean image;

    private transient String first_name;
    private transient String last_name;

    private transient boolean dirty;
    private transient boolean deleted;

    private transient long rawContactId;

    public Friend() {
    }

    public Friend(String name, String fullName, String firstName, String lastName,
            String cellPhone, String officePhone, String homePhone, String email, boolean deleted,
            long rawContactId, long syncState, boolean dirty) {
        this.name = name;
        this.real_name = fullName;
        this.first_name = firstName;
        this.last_name = lastName;
        this.deleted = deleted;
        this.rawContactId = rawContactId;
        this.dirty = dirty;
    }

    public String getDisplayName() {
        if (TextUtils.isEmpty(real_name)) {
            return name;
        }
        return real_name;
    }

    /**
     * Creates and returns RawContact instance from all the supplied parameters.
     */
    public static Friend create(String fullName, String firstName, String lastName,
            String cellPhone, String officePhone, String homePhone, String email, boolean deleted,
            long rawContactId, String name) {
        return new Friend(name, fullName, firstName, lastName, cellPhone, officePhone, homePhone,
                email, deleted, rawContactId, -1, true);
    }

    /**
     * Creates and returns a User instance that represents a deleted user. Since
     * the user is deleted, all we need are the client/server IDs.
     * 
     * @param clientUserId The client-side ID for the contact
     * @param serverUserId The server-side ID for the contact
     * @return a minimal User object representing the deleted contact.
     */
    public static Friend createDeletedContact(long rawContactId, String name) {
        return new Friend(name, null, null, null, null, null, null, null, true, rawContactId, -1,
                true);
    }

    public String getBestName() {
        if (!TextUtils.isEmpty(real_name)) {
            return real_name;
        } else if (TextUtils.isEmpty(first_name)) {
            return last_name;
        } else {
            return first_name;
        }
    }

    public String getFullName() {
        return real_name;
    }

    public String getFirstName() {
        return first_name;
    }

    public String getLastName() {
        return last_name;
    }

    public String getEmail() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getCellPhone() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getHomePhone() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getOfficePhone() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getAvatarName() {
        return image ? name : null;
    }

    public String getServerContactId() {
        return name;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isDirty() {
        return dirty;
    }

    public long getRawContactId() {
        return rawContactId;
    }
}
