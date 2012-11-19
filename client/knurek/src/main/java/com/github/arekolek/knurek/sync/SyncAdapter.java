package com.github.arekolek.knurek.sync;

import com.google.inject.Inject;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;

import roboguice.inject.ContextSingleton;

@ContextSingleton
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    @Inject
    public SyncAdapter(Context context) {
        super(context, true);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        try {

            Log.v("SYNC", String.format("account=%s, extras=%s, authority=%s", account.toString(),
                    extras.toString(), authority));

            String name = account.name;
            String type = account.type;

            ContactManager manager = new ContactManager(name, type);

            Uri contactsUri = ContactsContract.RawContacts.CONTENT_URI.buildUpon()
                    .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, name)
                    .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_TYPE, type).build();

            Cursor contacts = provider
                    .query(contactsUri, new String[]{ContactsContract.RawContacts._ID}, null, null,
                            null);

            ContentProviderOperation.Builder op;

            try {
                while (contacts.moveToNext()) {
                    manager.deleteContact(contacts.getLong(0));
                }
            } finally {
                contacts.close();
            }

            manager.addContact("Jan Makaron");

            manager.apply(provider);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
    }

    public static class ContactManager {

        private String name;

        private String type;

        private ArrayList<ContentProviderOperation> ops;

        public ContactManager(String name, String type) {
            this.name = name;
            this.type = type;
            this.ops = new ArrayList<ContentProviderOperation>();
        }

        public void addContact(String displayName) {
            ContentProviderOperation.Builder op = ContentProviderOperation
                    .newInsert(syncUri(ContactsContract.RawContacts.CONTENT_URI))
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, type)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, name);
            ops.add(op.build());

            op = ContentProviderOperation.newInsert(syncUri(ContactsContract.Data.CONTENT_URI))
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, ops.size() - 1)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                            displayName);
            op.withYieldAllowed(true);
            ops.add(op.build());
        }

        public void deleteContact(long id) {
            ContentProviderOperation.Builder op = ContentProviderOperation.newDelete(
                    syncUri(ContentUris
                            .withAppendedId(ContactsContract.RawContacts.CONTENT_URI, id)));
            op.withYieldAllowed(true);
            ops.add(op.build());
        }

        public void apply(ContentProviderClient provider)
                throws OperationApplicationException, RemoteException {
            provider.applyBatch(ops);
            ops.clear();
        }

        private Uri syncUri(Uri uri) {
            return uri.buildUpon()
                    .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
        }
    }


}
