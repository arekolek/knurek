
package com.github.arekolek.knurek.sync;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.github.arekolek.knurek.auth.Constants;

public class ContactManager {

    private static final String TAG = "ContactManager";

    /**
     * When we first add a sync adapter to the system, the contacts from that
     * sync adapter will be hidden unless they're merged/grouped with an
     * existing contact. But typically we want to actually show those contacts,
     * so we need to mess with the Settings table to get them to show up.
     * 
     * @param context the Authenticator Activity context
     * @param account the Account who's visibility we're changing
     * @param visible true if we want the contacts visible, false for hidden
     */
    public static void setAccountContactsVisibility(Context context, Account account,
            boolean visible) {
        ContentValues values = new ContentValues();
        values.put(RawContacts.ACCOUNT_NAME, account.name);
        values.put(RawContacts.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
        values.put(Settings.UNGROUPED_VISIBLE, visible ? 1 : 0);

        context.getContentResolver().insert(Settings.CONTENT_URI, values);
    }

    /**
     * Return a list of the local contacts that have been marked as "dirty", and
     * need syncing to the SampleSync server.
     * 
     * @param context The context of Authenticator Activity
     * @param account The account that we're interested in syncing
     * @return a list of Users that are considered "dirty"
     */
    public static DirtyFriends getDirtyContacts(Context context, Account account) {
        Log.i(TAG, "*** Looking for local dirty contacts");
        DirtyFriends dirtyContacts = new DirtyFriends();

        final ContentResolver resolver = context.getContentResolver();
        final Cursor c = resolver.query(DirtyQuery.CONTENT_URI, DirtyQuery.PROJECTION,
                DirtyQuery.SELECTION, new String[] {
                    account.name
                }, null);
        try {
            while (c.moveToNext()) {
                final long rawContactId = c.getLong(DirtyQuery.COLUMN_RAW_CONTACT_ID);
                final String serverContactId = c.getString(DirtyQuery.COLUMN_SERVER_ID);
                final boolean isDirty = "1".equals(c.getString(DirtyQuery.COLUMN_DIRTY));
                final boolean isDeleted = "1".equals(c.getString(DirtyQuery.COLUMN_DELETED));

                // The system actually keeps track of a change version number for
                // each contact. It may be something you're interested in for your
                // client-server sync protocol. We're not using it in this example,
                // other than to log it.
                final long version = c.getLong(DirtyQuery.COLUMN_VERSION);

                Log.i(TAG, "Dirty Contact: " + serverContactId);
                Log.i(TAG, "Contact Version: " + Long.toString(version));

                if (isDeleted) {
                    Log.i(TAG, "Contact is marked for deletion");
                    Friend friend = Friend.createDeletedContact(rawContactId, serverContactId);
                    dirtyContacts.add(friend);
                } else if (isDirty) {
                    Friend rawContact = getRawContact(context, rawContactId);
                    Log.i(TAG, "Contact Name: " + rawContact.getBestName());
                    dirtyContacts.add(rawContact);
                }
            }

        } finally {
            if (c != null) {
                c.close();
            }
        }
        return dirtyContacts;
    }

    public static synchronized String updateContacts(Context context, String account,
            FriendList friends, FriendsClient client) {
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation = new BatchOperation(context, resolver);

        Log.d(TAG, "In SyncContacts");
        for (Friend friend : friends.created) {
            addContact(context, account, friend, true, batchOperation, client);
            // A sync adapter should batch operations on multiple contacts,
            // because it will make a dramatic performance difference.
            // (UI updates, etc)
            if (batchOperation.size() >= 50) {
                batchOperation.execute();
            }
        }

        long rawContactId;
        for (Friend friend : friends.updated) {
            rawContactId = lookupRawContact(resolver, friend.getServerContactId());
            updateContact(context, resolver, friend, true, true, true, rawContactId,
                    batchOperation, client);
            if (batchOperation.size() >= 50) {
                batchOperation.execute();
            }
        }

        for (Friend friend : friends.deleted) {
            rawContactId = lookupRawContact(resolver, friend.getServerContactId());
            deleteContact(context, rawContactId, batchOperation);
            if (batchOperation.size() >= 50) {
                batchOperation.execute();
            }
        }

        batchOperation.execute();

        return friends.timestamp;
    }

    /**
     * After we've finished up a sync operation, we want to clean up the
     * sync-state so that we're ready for the next time. This involves clearing
     * out the 'dirty' flag on the synced contacts - but we also have to finish
     * the DELETE operation on deleted contacts. When the user initially deletes
     * them on the client, they're marked for deletion - but they're not
     * actually deleted until we delete them again, and include the
     * ContactsContract.CALLER_IS_SYNCADAPTER parameter to tell the contacts
     * provider that we're really ready to let go of this contact.
     * 
     * @param context The context of Authenticator Activity
     * @param dirtyContacts The list of contacts that we're cleaning up
     */
    public static void clearSyncFlags(Context context, DirtyFriends dirtyContacts) {
        Log.i(TAG, "*** Clearing Sync-related Flags");
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation = new BatchOperation(context, resolver);
        for (Friend rawContact : dirtyContacts.get()) {
            if (rawContact.isDeleted()) {
                Log.i(TAG, "Deleting contact: " + Long.toString(rawContact.getRawContactId()));
                deleteContact(context, rawContact.getRawContactId(), batchOperation);
            } else if (rawContact.isDirty()) {
                Log.i(TAG, "Clearing dirty flag for: " + rawContact.getBestName());
                clearDirtyFlag(context, rawContact.getRawContactId(), batchOperation);
            }
        }
        batchOperation.execute();
    }

    /**
     * Adds a single contact to the platform contacts provider. This can be used
     * to respond to a new contact found as part of sync information returned
     * from the server, or because a user added a new contact.
     * 
     * @param context the Authenticator Activity context
     * @param accountName the account the contact belongs to
     * @param rawContact the sample SyncAdapter User object
     * @param inSync is the add part of a client-server sync?
     * @param batchOperation allow us to batch together multiple operations into
     *            a single provider call
     * @param client rest client to make http requests to fetch avatars
     */
    public static void addContact(Context context, String accountName, Friend rawContact,
            boolean inSync, BatchOperation batchOperation, FriendsClient client) {

        // Put the data in the contacts provider
        final ContactOperations contactOp = ContactOperations.createNewContact(context,
                rawContact.getServerContactId(), accountName, inSync, batchOperation);

        contactOp
                .addName(rawContact.getFullName(), rawContact.getFirstName(),
                        rawContact.getLastName()).addEmail(rawContact.getEmail())
                .addPhone(rawContact.getCellPhone(), Phone.TYPE_MOBILE)
                .addPhone(rawContact.getHomePhone(), Phone.TYPE_HOME)
                .addPhone(rawContact.getOfficePhone(), Phone.TYPE_WORK)
                .addAvatar(client, rawContact.getAvatarName());

        // If we have a serverId, then go ahead and create our status profile.
        // Otherwise skip it - and we'll create it after we sync-up to the
        // server later on.
        contactOp.addProfileAction(rawContact.getServerContactId());
    }

    /**
     * Updates a single contact to the platform contacts provider. This method
     * can be used to update a contact from a sync operation or as a result of a
     * user editing a contact record. This operation is actually relatively
     * complex. We query the database to find all the rows of info that already
     * exist for this Contact. For rows that exist (and thus we're modifying
     * existing fields), we create an update operation to change that field. But
     * for fields we're adding, we create "add" operations to create new rows
     * for those fields.
     * 
     * @param context the Authenticator Activity context
     * @param resolver the ContentResolver to use
     * @param rawContact the sample SyncAdapter contact object
     * @param updateStatus should we update this user's status
     * @param updateAvatar should we update this user's avatar image
     * @param inSync is the update part of a client-server sync?
     * @param rawContactId the unique Id for this rawContact in contacts
     *            provider
     * @param batchOperation allow us to batch together multiple operations into
     *            a single provider call
     * @param client rest client to fetch avatar
     */
    public static void updateContact(Context context, ContentResolver resolver, Friend rawContact,
            boolean updateStatus, boolean updateAvatar, boolean inSync, long rawContactId,
            BatchOperation batchOperation, FriendsClient client) {

        boolean existingCellPhone = false;
        boolean existingHomePhone = false;
        boolean existingWorkPhone = false;
        boolean existingEmail = false;
        boolean existingAvatar = false;

        final Cursor c = resolver.query(DataQuery.CONTENT_URI, DataQuery.PROJECTION,
                DataQuery.SELECTION, new String[] {
                    String.valueOf(rawContactId)
                }, null);
        final ContactOperations contactOp = ContactOperations.updateExistingContact(context,
                rawContactId, inSync, batchOperation);
        try {
            // Iterate over the existing rows of data, and update each one
            // with the information we received from the server.
            while (c.moveToNext()) {
                final long id = c.getLong(DataQuery.COLUMN_ID);
                final String mimeType = c.getString(DataQuery.COLUMN_MIMETYPE);
                final Uri uri = ContentUris.withAppendedId(Data.CONTENT_URI, id);
                if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
                    contactOp.updateName(uri, c.getString(DataQuery.COLUMN_GIVEN_NAME),
                            c.getString(DataQuery.COLUMN_FAMILY_NAME),
                            c.getString(DataQuery.COLUMN_FULL_NAME), rawContact.getFirstName(),
                            rawContact.getLastName(), rawContact.getFullName());
                } else if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                    final int type = c.getInt(DataQuery.COLUMN_PHONE_TYPE);
                    if (type == Phone.TYPE_MOBILE) {
                        existingCellPhone = true;
                        contactOp.updatePhone(c.getString(DataQuery.COLUMN_PHONE_NUMBER),
                                rawContact.getCellPhone(), uri);
                    } else if (type == Phone.TYPE_HOME) {
                        existingHomePhone = true;
                        contactOp.updatePhone(c.getString(DataQuery.COLUMN_PHONE_NUMBER),
                                rawContact.getHomePhone(), uri);
                    } else if (type == Phone.TYPE_WORK) {
                        existingWorkPhone = true;
                        contactOp.updatePhone(c.getString(DataQuery.COLUMN_PHONE_NUMBER),
                                rawContact.getOfficePhone(), uri);
                    }
                } else if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                    existingEmail = true;
                    contactOp.updateEmail(rawContact.getEmail(),
                            c.getString(DataQuery.COLUMN_EMAIL_ADDRESS), uri);
                } else if (mimeType.equals(Photo.CONTENT_ITEM_TYPE)) {
                    existingAvatar = true;
                    contactOp.updateAvatar(client, rawContact.getAvatarName(), uri);
                }
            } // while
        } finally {
            c.close();
        }

        // Add the cell phone, if present and not updated above
        if (!existingCellPhone) {
            contactOp.addPhone(rawContact.getCellPhone(), Phone.TYPE_MOBILE);
        }
        // Add the home phone, if present and not updated above
        if (!existingHomePhone) {
            contactOp.addPhone(rawContact.getHomePhone(), Phone.TYPE_HOME);
        }

        // Add the work phone, if present and not updated above
        if (!existingWorkPhone) {
            contactOp.addPhone(rawContact.getOfficePhone(), Phone.TYPE_WORK);
        }
        // Add the email address, if present and not updated above
        if (!existingEmail) {
            contactOp.addEmail(rawContact.getEmail());
        }
        // Add the avatar if we didn't update the existing avatar
        if (!existingAvatar) {
            contactOp.addAvatar(client, rawContact.getAvatarName());
        }
    }

    /**
     * Deletes a contact from the platform contacts provider. This method is
     * used both for contacts that were deleted locally and then that deletion
     * was synced to the server, and for contacts that were deleted on the
     * server and the deletion was synced to the client.
     * 
     * @param context the Authenticator Activity context
     * @param rawContactId the unique Id for this rawContact in contacts
     *            provider
     */
    private static void deleteContact(Context context, long rawContactId,
            BatchOperation batchOperation) {
        batchOperation.add(ContactOperations.newDeleteCpo(
                ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId), true, true)
                .build());
    }

    /**
     * Return a User object with data extracted from a contact stored in the
     * local contacts database. Because a contact is actually stored over
     * several rows in the database, our query will return those multiple rows
     * of information. We then iterate over the rows and build the User
     * structure from what we find.
     * 
     * @param context the Authenticator Activity context
     * @param rawContactId the unique ID for the local contact
     * @return a User object containing info on that contact
     */
    private static Friend getRawContact(Context context, long rawContactId) {
        String firstName = null;
        String lastName = null;
        String fullName = null;
        String cellPhone = null;
        String homePhone = null;
        String workPhone = null;
        String email = null;
        String serverId = null;

        final ContentResolver resolver = context.getContentResolver();
        final Cursor c = resolver.query(DataQuery.CONTENT_URI, DataQuery.PROJECTION,
                DataQuery.SELECTION, new String[] {
                    String.valueOf(rawContactId)
                }, null);
        try {
            while (c.moveToNext()) {
                final String mimeType = c.getString(DataQuery.COLUMN_MIMETYPE);
                final String tempServerId = c.getString(DataQuery.COLUMN_SERVER_ID);
                if (!TextUtils.isEmpty(tempServerId)) {
                    serverId = tempServerId;
                }
                if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
                    lastName = c.getString(DataQuery.COLUMN_FAMILY_NAME);
                    firstName = c.getString(DataQuery.COLUMN_GIVEN_NAME);
                    fullName = c.getString(DataQuery.COLUMN_FULL_NAME);
                } else if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                    final int type = c.getInt(DataQuery.COLUMN_PHONE_TYPE);
                    if (type == Phone.TYPE_MOBILE) {
                        cellPhone = c.getString(DataQuery.COLUMN_PHONE_NUMBER);
                    } else if (type == Phone.TYPE_HOME) {
                        homePhone = c.getString(DataQuery.COLUMN_PHONE_NUMBER);
                    } else if (type == Phone.TYPE_WORK) {
                        workPhone = c.getString(DataQuery.COLUMN_PHONE_NUMBER);
                    }
                } else if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                    email = c.getString(DataQuery.COLUMN_EMAIL_ADDRESS);
                }
            } // while
        } finally {
            c.close();
        }

        // Now that we've extracted all the information we care about,
        // create the actual User object.
        Friend rawContact = Friend.create(fullName, firstName, lastName, cellPhone, workPhone,
                homePhone, email, false, rawContactId, serverId);

        return rawContact;
    }

    /**
     * Returns the RawContact id for a Knurek contact, or 0 if the Knurek user
     * isn't found.
     * 
     * @param resolver the content resolver to use
     * @param serverContactId the Knurek user ID to lookup
     * @return the RawContact id, or 0 if not found
     */
    private static long lookupRawContact(ContentResolver resolver, String serverContactId) {
        long rawContactId = 0;
        final Cursor c = resolver.query(UserIdQuery.CONTENT_URI, UserIdQuery.PROJECTION,
                UserIdQuery.SELECTION, new String[] {
                    serverContactId
                }, null);
        try {
            if ((c != null) && c.moveToFirst()) {
                rawContactId = c.getLong(UserIdQuery.COLUMN_RAW_CONTACT_ID);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return rawContactId;
    }

    /**
     * Clear the local system 'dirty' flag for a contact.
     * 
     * @param context the Authenticator Activity context
     * @param rawContactId the id of the contact update
     * @param batchOperation allow us to batch together multiple operations
     */
    private static void clearDirtyFlag(Context context, long rawContactId,
            BatchOperation batchOperation) {
        final ContactOperations contactOp = ContactOperations.updateExistingContact(context,
                rawContactId, true, batchOperation);

        final Uri uri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);
        contactOp.updateDirtyFlag(false, uri);
    }

    /**
     * Constants for a query to get contact data for a given rawContactId
     */
    final private static class DataQuery {

        private DataQuery() {
        }

        public static final String[] PROJECTION = new String[] {
                Data._ID, RawContacts.SOURCE_ID, Data.MIMETYPE, Data.DATA1, Data.DATA2, Data.DATA3,
                Data.DATA15, Data.SYNC1
        };

        public static final int COLUMN_ID = 0;
        public static final int COLUMN_SERVER_ID = 1;
        public static final int COLUMN_MIMETYPE = 2;
        public static final int COLUMN_DATA1 = 3;
        public static final int COLUMN_DATA2 = 4;
        public static final int COLUMN_DATA3 = 5;
        public static final int COLUMN_DATA15 = 6;
        public static final int COLUMN_SYNC1 = 7;

        public static final Uri CONTENT_URI = Data.CONTENT_URI;

        public static final int COLUMN_PHONE_NUMBER = COLUMN_DATA1;
        public static final int COLUMN_PHONE_TYPE = COLUMN_DATA2;
        public static final int COLUMN_EMAIL_ADDRESS = COLUMN_DATA1;
        public static final int COLUMN_EMAIL_TYPE = COLUMN_DATA2;
        public static final int COLUMN_FULL_NAME = COLUMN_DATA1;
        public static final int COLUMN_GIVEN_NAME = COLUMN_DATA2;
        public static final int COLUMN_FAMILY_NAME = COLUMN_DATA3;
        public static final int COLUMN_AVATAR_IMAGE = COLUMN_DATA15;
        public static final int COLUMN_SYNC_DIRTY = COLUMN_SYNC1;

        public static final String SELECTION = Data.RAW_CONTACT_ID + "=?";
    }

    /**
     * Constants for a query to find contacts that are in need of syncing to the
     * server. This should cover new, edited, and deleted contacts.
     */
    final private static class DirtyQuery {

        private DirtyQuery() {
        }

        public final static String[] PROJECTION = new String[] {
                RawContacts._ID, RawContacts.SOURCE_ID, RawContacts.DIRTY, RawContacts.DELETED,
                RawContacts.VERSION
        };

        public final static int COLUMN_RAW_CONTACT_ID = 0;
        public final static int COLUMN_SERVER_ID = 1;
        public final static int COLUMN_DIRTY = 2;
        public final static int COLUMN_DELETED = 3;
        public final static int COLUMN_VERSION = 4;

        public static final Uri CONTENT_URI = RawContacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();

        public static final String SELECTION = RawContacts.DIRTY + "=1 AND "
                + RawContacts.ACCOUNT_TYPE + "='" + Constants.ACCOUNT_TYPE + "' AND "
                + RawContacts.ACCOUNT_NAME + "=?";
    }

    /**
     * Constants for a query to find a contact given a sample SyncAdapter user
     * ID.
     */
    final private static class UserIdQuery {

        private UserIdQuery() {
        }

        public final static String[] PROJECTION = new String[] {
                RawContacts._ID, RawContacts.CONTACT_ID
        };

        public final static int COLUMN_RAW_CONTACT_ID = 0;
        public final static int COLUMN_LINKED_CONTACT_ID = 1;

        public final static Uri CONTENT_URI = RawContacts.CONTENT_URI;

        public static final String SELECTION = RawContacts.ACCOUNT_TYPE + "='"
                + Constants.ACCOUNT_TYPE + "' AND " + RawContacts.SOURCE_ID + "=?";
    }
}
