package com.sarah.expensecontrol.loans;

import android.support.v4.app.Fragment;

import com.sarah.expensecontrol.base.NavigationLessActivity;

/**
 * Denna aktivitet startas när ett redan skapat lån ska editeras eller tas bort.
 */

public class LoanDetailActivity extends NavigationLessActivity {

    @Override
    protected Fragment createFragment() {
        return new LoanDetailFragment();
    }
}
