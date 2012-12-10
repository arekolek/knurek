
package com.github.arekolek.knurek;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.github.arekolek.knurek.auth.AuthenticationPreferences;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.OptionsItem;
import com.googlecode.androidannotations.annotations.OptionsMenu;

@EActivity(R.layout.main)
@OptionsMenu(R.menu.main)
public class HelloAndroidActivity extends SherlockFragmentActivity {

    @Bean
    AuthenticationPreferences authPrefs;

    private boolean isAuthenticated;

    @Override
    protected void onResume() {
        super.onResume();

        isAuthenticated = authPrefs.isAuthenticated();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_login).setVisible(!isAuthenticated);
        return !isAuthenticated;
    }

    @OptionsItem
    void menuLoginSelected() {
        authPrefs.login();
    }

}
