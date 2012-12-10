
package com.github.arekolek.knurek.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;

import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.EBean;
import com.googlecode.androidannotations.annotations.RootContext;
import com.googlecode.androidannotations.annotations.SystemService;

import java.io.IOException;

@EBean
public class AuthenticationPreferences {

    public interface Callback {

        void onResult(boolean isAuthenticated);

        void onReady();

    }

    @SystemService
    AccountManager accountManager;

    @RootContext
    Activity activity;

    @Background
    public void isAuthenticated(Callback callback) {
        Account[] accounts = accountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
        callback.onResult(accounts.length > 0);
    }

    public String getRecentUser() {
        // TODO Auto-generated method stub
        return null;
    }

    @Background
    public void login(Callback callback) {
        accountManager.addAccount(Constants.ACCOUNT_TYPE, null, null, null, activity, null, null);
        callback.onReady();
    }

    public String getIdentifier(Account account) throws OperationCanceledException,
            AuthenticatorException, IOException {
        return accountManager.blockingGetAuthToken(account, Constants.AUTH_TOKEN_TYPE, false);
    }

}
