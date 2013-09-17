
package com.github.arekolek.knurek;

import android.support.v4.app.ActivityCompat;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.github.arekolek.knurek.auth.AuthenticationPreferences;
import com.github.arekolek.knurek.auth.AuthenticationPreferences.Callback;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.OptionsItem;
import com.googlecode.androidannotations.annotations.OptionsMenu;
import com.googlecode.androidannotations.annotations.UiThread;

@EActivity(R.layout.main)
@OptionsMenu(R.menu.main)
public class HelloAndroidActivity extends SherlockFragmentActivity implements Callback {

    @Bean
    AuthenticationPreferences authPrefs;

    private boolean isAuthenticated = true;

    @Override
    protected void onResume() {
        super.onResume();

        authPrefs.isAuthenticated(this);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_login).setVisible(!isAuthenticated);
        // return false to hide the menu when it's not needed
        return !isAuthenticated;
    }

    @OptionsItem
    void menuLoginSelected() {
        authPrefs.login(this);
    }

    @Override
    @UiThread
    public void onResult(boolean isAuthenticated) {
        this.isAuthenticated = isAuthenticated;
        ActivityCompat.invalidateOptionsMenu(this);
    }

    @Override
    public void onReady() {
    }

}
