
package com.github.arekolek.knurek.sync;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;

import com.github.arekolek.knurek.auth.AuthenticationPreferences;
import com.googlecode.androidannotations.annotations.AfterInject;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EBean;
import com.googlecode.androidannotations.annotations.rest.RestService;

import org.springframework.http.client.ClientHttpRequestInterceptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@EBean
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "SyncAdapter";

    @RestService
    FriendsClient client;

    @Bean
    AuthenticationPreferences authPrefs;

    @Bean
    CustomHeaderInterceptor authorizer;

    public SyncAdapter(Context context) {
        super(context, true);
    }

    @AfterInject
    void setupClient() {
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<ClientHttpRequestInterceptor>();
        interceptors.add(authorizer);
        client.getRestTemplate().setInterceptors(interceptors);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        try {

            Log.v("SYNC",
                    String.format("account=%s, extras=%s, authority=%s", account.toString(),
                            extras.toString(), authority));

            String name = account.name;
            String type = account.type;

            Uri contactsUri = ContactsContract.RawContacts.CONTENT_URI.buildUpon()
                    .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, name)
                    .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_TYPE, type).build();

            Cursor contacts = provider.query(contactsUri, new String[] {
                ContactsContract.RawContacts._ID
            }, null, null, null);

            ContactManager manager = new ContactManager(name, type, provider);

            try {
                while (contacts.moveToNext()) {
                    manager.deleteContact(contacts.getLong(0));
                }
            } finally {
                contacts.close();
            }

            authorizer.setIdentifier(authPrefs.getIdentifier(account));

            FriendList list = client.getFriends();

            Log.d(TAG, String.format("Downloaded %d friends", list.friends.size()));

            for (Friend friend : list.friends) {
                Log.d(TAG, String.format("Adding\t%s", friend.name));
                byte[] avatar = null;
                if (friend.image) {
                    Log.d(TAG, String.format("Downloading avatar for %s", friend.getDisplayName()));
                    avatar = client.getAvatar(friend.name);
                }
                manager.addContact(friend.getDisplayName(), avatar);
            }

            manager.apply();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        } catch (OperationCanceledException e) {
            e.printStackTrace();
        } catch (AuthenticatorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
