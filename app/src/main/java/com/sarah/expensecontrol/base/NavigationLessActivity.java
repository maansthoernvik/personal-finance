package com.sarah.expensecontrol.base;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.sarah.expensecontrol.R;

/**
 * Den här aktiviteten är gjord som grund för de fragment som inte skall visa
 * någon navigeringsmeny, till skillnad från SingleFragmentActivity.
 */

public abstract class NavigationLessActivity extends AppCompatActivity {

    @SuppressWarnings("unused")
    private static final String TAG = "NavigationLessActivity";

    protected abstract Fragment createFragment();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_navigation);

        /*
        En annan skillnad mot SingleFragment Activity är denna aktivitets custom action bar.
        Denna aktivitet används för att visa detaljer om olika objekt, såsom expenses och loans,
        därför behövs det speciella kontroller för att spara, ta bort eller gå ut ur editeringsvyn.
         */
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.custom_action_bar);

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragmentContainer);

        if (fragment == null) {
            fragment = createFragment();
            fm.beginTransaction()
                    .add(R.id.fragmentContainer, fragment)
                    .commit();
        }
    }
}
