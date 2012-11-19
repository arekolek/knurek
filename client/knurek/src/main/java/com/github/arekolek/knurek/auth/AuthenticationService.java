package com.github.arekolek.knurek.auth;

import com.google.inject.Inject;

import android.content.Intent;
import android.os.IBinder;

import roboguice.service.RoboService;

public class AuthenticationService extends RoboService {

    @Inject
    private Authenticator authenticator;

    public IBinder onBind(Intent intent) {
        return authenticator.getIBinder();
    }

}
