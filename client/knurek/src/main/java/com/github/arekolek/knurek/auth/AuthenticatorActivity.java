package com.github.arekolek.knurek.auth;

import com.github.arekolek.knurek.R;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;

import roboguice.activity.RoboAccountAuthenticatorActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

@ContentView(R.layout.activity_login)
public class AuthenticatorActivity extends RoboAccountAuthenticatorActivity {

    @InjectView(R.id.login)
    private EditText accountName;

    public static Intent createIntent(Context context, AccountAuthenticatorResponse response) {
        Intent intent = new Intent(context, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        return intent;
    }

    public void onSubmit(View view) {
        String name = accountName.getText().toString();
        String type = Constants.ACCOUNT_TYPE;

        AccountManager.get(this).addAccountExplicitly(new Account(name, type), null, null);

        Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, name);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, type);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

}
