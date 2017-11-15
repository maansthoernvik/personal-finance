package com.sarah.expensecontrol.home;

import com.sarah.expensecontrol.base.SingleFragmentActivity;

/**
 * Den aktivitet som startas efter laddningsaktiviteten.
 */

public class HomeActivity extends SingleFragmentActivity {

    @Override
    protected HomeFragment createFragment() {
        return new HomeFragment();
    }
}
