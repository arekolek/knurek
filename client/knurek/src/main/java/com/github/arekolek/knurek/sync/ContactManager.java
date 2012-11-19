package com.github.arekolek.knurek.sync;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;

import java.util.ArrayList;

public class ContactManager {

    private String name;

    private String type;

    private ArrayList<ContentProviderOperation> ops;

    public ContactManager(String name, String type) {
        this.name = name;
        this.type = type;
        this.ops = new ArrayList<ContentProviderOperation>();
    }

    public void addContact(String displayName, byte[] avatar) {
        ContentProviderOperation.Builder op = ContentProviderOperation
                .newInsert(syncUri(ContactsContract.RawContacts.CONTENT_URI))
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, type)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, name);
        ops.add(op.build());

        int backref = ops.size() - 1;

        op = ContentProviderOperation.newInsert(syncUri(ContactsContract.Data.CONTENT_URI))
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, backref)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, avatar);

        ops.add(op.build());

        op = ContentProviderOperation.newInsert(syncUri(ContactsContract.Data.CONTENT_URI))
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, backref)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                        displayName);
        op.withYieldAllowed(true);
        ops.add(op.build());
    }

    public void deleteContact(long id) {
        ContentProviderOperation.Builder op = ContentProviderOperation.newDelete(
                syncUri(ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, id)));
        op.withYieldAllowed(true);
        ops.add(op.build());
    }

    public void apply(ContentProviderClient provider)
            throws OperationApplicationException, RemoteException {
        provider.applyBatch(ops);
        ops.clear();
    }

    private Uri syncUri(Uri uri) {
        return uri.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                .build();
    }
}
