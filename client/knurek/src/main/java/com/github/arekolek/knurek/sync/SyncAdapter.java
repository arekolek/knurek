package com.github.arekolek.knurek.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;

import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EBean;

@EBean
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    @Bean
    Api api;

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

            for(Friend friend : api.getFriends(name)){
                manager.addContact(friend.realname, api.downloadAvatar(friend.image));
            }

            manager.apply(provider);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
    }


}
