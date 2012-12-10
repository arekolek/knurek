
package com.github.arekolek.knurek.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;

import com.github.arekolek.knurek.R;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.InstanceState;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;
import com.googlecode.androidannotations.annotations.rest.RestService;

import org.springframework.web.client.RestClientException;

@EActivity(R.layout.activity_login)
public class AuthenticatorActivity extends AccountAuthenticatorActivity {

    public static Intent intent(Context context, AccountAuthenticatorResponse response) {
        Intent intent = AuthenticatorActivity_.intent(context).get();
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        return intent;
    }

    @InstanceState
    String identifier;

    @RestService
    KnurekRestClient client;

    @ViewById
    View loading;

    @ViewById
    View step1;

    @ViewById
    View step2;

    @Override
    protected void onResume() {
        super.onResume();

        showView(loading);

        if (identifier == null) {
            getIdentifier();
        } else {
            getUsername();
        }
    }

    @UiThread
    void showView(View view) {
        loading.setVisibility(View.GONE);
        step1.setVisibility(View.GONE);
        step2.setVisibility(View.GONE);

        view.setVisibility(View.VISIBLE);
    }

    @Background
    void getUsername() {
        try {
            Auth auth = client.getUsername(identifier);
            if (auth != null && !TextUtils.isEmpty(auth.name)) {
                setUsername(auth.name);
                return;
            }
        } catch (RestClientException e) {
        }
        showView(step1);
    }

    @UiThread
    void setUsername(String name) {
        String type = Constants.ACCOUNT_TYPE;

        AccountManager.get(this).addAccountExplicitly(new Account(name, type), identifier, null);

        Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, name);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, type);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);

        showView(step2);
    }

    @Background
    void getIdentifier() {
        Auth auth = client.getIdentifier();
        setIdentifier(auth.identifier);
    }

    @UiThread
    void setIdentifier(String identifier) {
        this.identifier = identifier;
        showView(step1);
    }

    @Click(R.id.submit)
    void onSubmit() {
        Uri uri = Uri.parse("http://192.168.1.2:8080/api/auth/?identifier=" +
                identifier);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    @Click(R.id.finish)
    void onFinish() {
        finish();
    }

}
