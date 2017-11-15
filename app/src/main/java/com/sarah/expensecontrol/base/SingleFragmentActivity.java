package com.sarah.expensecontrol.base;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sarah.expensecontrol.R;
import com.sarah.expensecontrol.expenses.ExpensesFragment;
import com.sarah.expensecontrol.home.HomeFragment;
import com.sarah.expensecontrol.loading.LoadingActivity;
import com.sarah.expensecontrol.loans.LoansFragment;
import com.sarah.expensecontrol.statistics.StatisticsFragment;
import com.sarah.expensecontrol.util.TimeTracking;

import java.util.Calendar;
import java.util.Date;

/**
 * Den här aktiviteten ligger som grund för de flesta fragmentaktiviteterna i applikationen.
 * Den använder sig av drawer layouten och agenerar som en gemensam grund som alla översikts-
 * aktiviteter kan ärva från.
 */

public abstract class SingleFragmentActivity extends AppCompatActivity {

    private static final String TAG = "SingleFragmentActivity";

    // Alla klassfält associerade med drawer layouten.
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawerList;
    private String[] mDrawerTitles;
    // Alla klassfält associerade med drawer layouten.

    // Titelfält.
    private CharSequence mTitle;
    private CharSequence mDrawerTitle;
    // Titelfält.

    // Abstrakt funktion som implementeras av alla översiktsaktiviteter, såsom ExpensesActivity.
    protected abstract Fragment createFragment();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);

        // Sätt alla titlas till applikationens titel till en början.
        mTitle = mDrawerTitle = getTitle();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.string.drawer_open,
                R.string.drawer_close
        ) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                // Sätt titeln till den nyligen valda sektionen (se setTitle).
                setTitle(mTitle);
            }

            public void onDrawerOpened(View view) {
                super.onDrawerOpened(view);
                // Sätt titeln till applikationens namn då drawern är öppen (se setTitle).
                setTitle(mDrawerTitle);
            }
        };
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        mDrawerList = (ListView) findViewById(R.id.drawer_list);
        mDrawerTitles = getResources().getStringArray(R.array.nav_drawer_options);
        mDrawerList.setAdapter(new ArrayAdapter<String>(
                this,
                R.layout.nav_drawer_list_item,
                mDrawerTitles)
        );
        // Se custom DrawerItemClickListener för implementation kring navigering i drawer layouten.
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // Inställningen för att få navigeringen via drawer layout.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Standard för abstrakt användning av fragment via denna basaktivitet.
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragmentContainer);

        if (fragment == null) {
            fragment = createFragment();
            fm.beginTransaction()
                    .add(R.id.fragmentContainer, fragment)
                    .commit();
        }
    }

    /**
     * onResume används för att kontrollera månadsskiften. Detta är viktigt för att kunna skapa
     * statistik till applikationen. Om ett månadsskifter tagit plats så måste statistik för den
     * föregående månaden skapas. Här kontrolleras det om senaste gången onResume kallades var
     * vid en tidigare månad än den nuvarande. Genom att inte bara göra detta vid uppstart så kan
     * man säkerställa att statistik hålls aktuell även när appen inte stängs ned totalt.
     */

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume running month change check...");
        final String lastAccessDate = "lastAccess";

        // Hämta shared preferences för att jämföra nuvarande månad.
        SharedPreferences sharedPrefs = getSharedPreferences(
                getString(R.string.app_name),
                Context.MODE_PRIVATE
        );
        Log.d("STATISTICS", "Got this date from shared prefs: " + new Date(sharedPrefs.getLong(lastAccessDate, new Date().getTime())));

        /*
        Flöde:

        1. Hämta kalenderinstans för att få nuvarande tid.
        2. Hämta lastAccess, returnera nuvarande tid vid fel.
        3. Sätt kalendern till lastAccess datumet.
        4. Kontrollera om år och månad överensstämmer mha. TimeTracking util-klassen.
        5a.Om inte, initiera laddningssekvens.
        5b.Om de överensstämmer, gör ingenting.
         */
        Calendar cal = Calendar.getInstance();
        long lastAccess = sharedPrefs.getLong(lastAccessDate, cal.getTimeInMillis());
        cal.setTimeInMillis(lastAccess);

        if (!TimeTracking.isThisMonth(cal)) {
            Intent intent = new Intent(getApplicationContext(), LoadingActivity.class);

            startActivity(intent);
            finish();
        }
    }

    /**
     * Används för att markera vilket alternativ i drawern som var aktiverat innan statet sparades.
     * @param savedInstanceState state
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    /**
     * Används för att indikera att drawermenyn valts.
     * @param item som valts
     * @return true, om options item blev valt
     */

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Används i samband med drawer layouten för att definiera vad som ska hända om ett alternativ
     * valts i menyn.
     */

    private class DrawerItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    /**
     * SelectItem sätter vilket menyalternativ som är valt just nu (för att kunna restora via
     * onPostCreate), sätter rätt titel för nuvarande sektion samt startar upp rätt fragment.
     * @param position som valts
     */

    private void selectItem(int position) {
        mDrawerList.setItemChecked(position, true);
        setTitle(mDrawerTitles[position]);
        // Stängs drawern när ett alternativ valts.
        mDrawerLayout.closeDrawer(mDrawerList);

        Fragment fragment;  // Fragmentet som kommer användas för det valda alternativet.
        switch (mDrawerTitles[position]) {
            case "Home":
                fragment = new HomeFragment();
                /*
                GÄLLER ALLA ALTERNATIV! ->
                    Fragmentet som just nu är aktivt byts ut mot det valda menyalternativet.
                 */
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .commit();
                break;
            case "Expenses":
                fragment = new ExpensesFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .commit();
                break;
            case "Loans":
                fragment = new LoansFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .commit();
                break;
            case "Statistics":
                fragment = new StatisticsFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .commit();
                break;
        }
    }

    /**
     * Sätt nuvarande klassfältet mTitle till newTitle samt ändra titeln för actionbaren.
     * @param newTitle, nya titeln för appen
     */

    @Override
    public void setTitle(CharSequence newTitle) {
        mTitle = newTitle;
        getSupportActionBar().setTitle(newTitle);
    }
}
