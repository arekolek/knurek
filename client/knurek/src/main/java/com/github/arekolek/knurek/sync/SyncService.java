package com.github.arekolek.knurek.sync;

import com.google.inject.Inject;

import android.content.Intent;
import android.os.IBinder;

import roboguice.service.RoboService;

public class SyncService extends RoboService {

    @Inject
    private SyncAdapter syncAdapter;

    public IBinder onBind(Intent intent) {
        return syncAdapter.getSyncAdapterBinder();
    }

}
