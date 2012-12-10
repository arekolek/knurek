
package com.github.arekolek.knurek.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EService;

@EService
public class AuthenticationService extends Service {

    @Bean
    Authenticator authenticator;

    @Override
    public IBinder onBind(Intent intent) {
        return authenticator.getIBinder();
    }

}
