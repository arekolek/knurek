package com.github.arekolek.knurek.sync;

import com.google.inject.Inject;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

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
        Log.v("SYNC", String.format("account=%s, extras=%s, authority=%s", account.toString(),
                extras.toString(), authority));
    }

}
