package com.sarah.expensecontrol.expenses;

import android.support.v4.app.Fragment;

import com.sarah.expensecontrol.base.NavigationLessActivity;

/**
 * Aktivitet för utgiftsdetaljer. Wrapper-klass för att returnera ett ExpenseDetailFragment.
 */

public class ExpenseDetailActivity extends NavigationLessActivity {

    @Override
    public Fragment createFragment() {
        return new ExpenseDetailFragment();
    }
}
