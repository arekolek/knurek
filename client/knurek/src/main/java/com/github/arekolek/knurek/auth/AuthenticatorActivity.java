package com.github.arekolek.knurek.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;

import com.github.arekolek.knurek.R;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.ViewById;

@EActivity(R.layout.activity_login)
public class AuthenticatorActivity extends AccountAuthenticatorActivity {

    @ViewById(R.id.login)
    EditText accountName;

    public static Intent intent(Context context, AccountAuthenticatorResponse response) {
        Intent intent = AuthenticatorActivity_.intent(context).get();
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
