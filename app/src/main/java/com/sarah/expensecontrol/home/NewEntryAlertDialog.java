package com.sarah.expensecontrol.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import com.sarah.expensecontrol.R;
import com.sarah.expensecontrol.expenses.ExpenseDetailActivity;
import com.sarah.expensecontrol.expenses.ExpensesFragment;
import com.sarah.expensecontrol.loans.LoanDetailActivity;
import com.sarah.expensecontrol.loans.LoansFragment;

/**
 * Denna dialog används i homefragmentet för att visa alternativ för användaren när denne valt att
 * skapa ett nytt inlägg.
 */

public class NewEntryAlertDialog extends AlertDialog {

    public NewEntryAlertDialog(final Context context) {
        super(context);

        setTitle(getContext().getString(R.string.new_entry_alert_dialog_title));

        setMessage(getContext().getString(R.string.new_entry_alert_dialog_message));

        setButton(BUTTON_NEUTRAL, "Cancel", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dismiss();
            }
        });
        setButton(BUTTON_NEGATIVE, "Expense", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(context, ExpenseDetailActivity.class);
                intent.putExtra(ExpensesFragment.EXTRA_EDIT, false);
                dismiss();
                context.startActivity(intent);
            }
        });
        setButton(BUTTON_POSITIVE, "Loan", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(context, LoanDetailActivity.class);
                intent.putExtra(LoansFragment.EXTRA_EDIT, false);
                dismiss();
                context.startActivity(intent);
            }
        });
    }


}
