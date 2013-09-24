
package com.github.arekolek.knurek.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.github.arekolek.knurek.auth.AuthenticationPreferences;
import com.googlecode.androidannotations.annotations.AfterInject;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EBean;
import com.googlecode.androidannotations.annotations.SystemService;
import com.googlecode.androidannotations.annotations.rest.RestService;

import org.apache.http.ParseException;
import org.springframework.http.client.ClientHttpRequestInterceptor;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@EBean
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "SyncAdapter";

    private static final String SYNC_MARKER_KEY = "com.github.arekolek.knurek.marker";

    @RestService
    FriendsClient client;

    @Bean
    AuthenticationPreferences authPrefs;

    @Bean
    CustomHeaderInterceptor authorizer;

    @SystemService
    AccountManager accountManager;

    private Context context;

    public SyncAdapter(Context context) {
        super(context, true);
        this.context = context;
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

        Log.v("SYNC",
                String.format("account=%s, extras=%s, authority=%s", account.toString(),
                        extras.toString(), authority));

        try {
            // see if we already have a sync-state attached to this account. By handing
            // This value to the server, we can just get the contacts that have
            // been updated on the server-side since our last sync-up
            String lastSyncMarker = getServerSyncMarker(account);

            // By default, contacts from a 3rd party provider are hidden in the contacts
            // list. So let's set the flag that causes them to be visible, so that users
            // can actually see these contacts.
            if (lastSyncMarker == null) {
                ContactManager.setAccountContactsVisibility(getContext(), account, true);
            }

            DirtyFriends dirtyContacts;
            FriendList updatedContacts;

            // Find the local 'dirty' contacts that we need to tell the server about...
            // Find the local users that need to be sync'd to the server...
            dirtyContacts = ContactManager.getDirtyContacts(context, account);

            // Send the dirty contacts to the server, and retrieve the server-side changes
            authorizer.setIdentifier(authPrefs.getIdentifier(account));
            updatedContacts = client.syncFriends(lastSyncMarker, dirtyContacts);

            // Update the local contacts database with the changes. updateContacts()
            // returns a syncState value that indicates the high-water-mark for
            // the changes we received.
            Log.d(TAG, "Calling contactManager's sync contacts");
            String newSyncState = ContactManager.updateContacts(context, account.name,
                    updatedContacts, client);

            // Save off the new sync marker. On our next sync, we only want to receive
            // contacts that have changed since this sync...
            setServerSyncMarker(account, newSyncState);

            if (dirtyContacts.size() > 0) {
                ContactManager.clearSyncFlags(context, dirtyContacts);
            }

        } catch (final AuthenticatorException e) {
            Log.e(TAG, "AuthenticatorException", e);
            syncResult.stats.numParseExceptions++;
        } catch (final OperationCanceledException e) {
            Log.e(TAG, "OperationCanceledExcetpion", e);
        } catch (final IOException e) {
            Log.e(TAG, "IOException", e);
            syncResult.stats.numIoExceptions++;
        } catch (final ParseException e) {
            Log.e(TAG, "ParseException", e);
            syncResult.stats.numParseExceptions++;
        }
    }

    /**
     * Save off the high-water-mark we receive back from the server.
     * 
     * @param account The account we're syncing
     * @param marker The high-water-mark we want to save.
     */
    private void setServerSyncMarker(Account account, String marker) {
        accountManager.setUserData(account, SYNC_MARKER_KEY, marker);
    }

    /**
     * This helper function fetches the last known high-water-mark we received
     * from the server - or empty string if we've never synced.
     * 
     * @param account the account we're syncing
     * @return the change high-water-mark
     */
    private String getServerSyncMarker(Account account) {
        String markerString = accountManager.getUserData(account, SYNC_MARKER_KEY);
        if (!TextUtils.isEmpty(markerString)) {
            return markerString;
        }
        return "";
    }

}
