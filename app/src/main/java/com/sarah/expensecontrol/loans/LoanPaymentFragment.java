package com.sarah.expensecontrol.loans;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.sarah.expensecontrol.Loan;
import com.sarah.expensecontrol.R;
import com.sarah.expensecontrol.util.TimeTracking;
import com.sarah.expensecontrol.model.ExpenseControlContentProvider;
import com.sarah.expensecontrol.model.ExpenseControlContract;

import java.util.Calendar;

import static com.sarah.expensecontrol.loans.LoanDetailFragment.EXTRA_LOAN;
import static com.sarah.expensecontrol.model.ExpenseControlContract.*;

/**
 * Fragment för avbetalningsvyn hos ett lån.
 */

public class LoanPaymentFragment extends DialogFragment {

    /* Definitioner för content provider interaktion */
    private static final String LOAN_SELECTION = ExpenseControlContract.LoanEntry._ID + "=?";
    /* Definitioner för content provider interaktion */

    private EditText mInput;    // Input för hur stor avbetalningen var.
    private Loan mLoan;         // Lånet i fråga.

    /**
     * Skapar en ny instans av LoanPaymentFragment med argument satt för vilket lån det gäller.
     * @param loan som avbetalningen gäller
     * @return ett LoanPaymentFragment
     */

    public static LoanPaymentFragment newInstance(Loan loan) {
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_LOAN, loan); // Det kommer att användas mer data än bara lånets
                                                // id, därav passeras hela objektet.
        LoanPaymentFragment paymentFragment = new LoanPaymentFragment();
        paymentFragment.setArguments(args);

        return paymentFragment;
    }

    /**
     * Skapar dialogrutan.
     * @param savedInstanceState state
     * @return en dialog att visa
     */

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Hämta ut lånet från fragmentets argument.
        mLoan = (Loan) getArguments().getSerializable(EXTRA_LOAN);

        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_loan_payment, null);

        mInput = (EditText) view.findViewById(R.id.dialog_loan_payment);

        return new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.dialog_loan_payment_amount))
                .setMessage(R.string.dialog_loan_make_payment_message)
                .setPositiveButton(
                        android.R.string.ok,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                if (makePayment()) sendResult(Activity.RESULT_OK);
                            }
                        }
                )
                .setNegativeButton(
                        android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                sendResult(Activity.RESULT_CANCELED);
                            }
                        }
                )
                .setView(view)
                .create();
    }

    private boolean makePayment() {
        // Om summan är lägre än den summa som vill betalas av så nekas aktionen. Samma sak om
        // inputsumman är tom.
        if (TextUtils.isEmpty(mInput.getText()) ||
                Integer.valueOf(mInput.getText().toString()) > mLoan.getAmount()) return false;

        ContentValues values = new ContentValues();
        int paymentAmount = Integer.valueOf(mInput.getText().toString()), newAmount;
        Calendar calendar = Calendar.getInstance(); // För tidsstämpel till det record som ska skapas.

        // Om allt ok, dra summan från lånet.
        newAmount = mLoan.getAmount() - paymentAmount;
        // Förbered values för uppdatering i databasen.
        values.put(LoanEntry.COLUMN_NAME_AMOUNT, newAmount);
        // Sätt lånet till den nya summan.
        mLoan.setAmount(newAmount);

        // Uppdatera det faktiska lånet i databasen.
        getActivity().getContentResolver().update(
                ExpenseControlContentProvider.LOAN_URI,
                values,
                LOAN_SELECTION,
                new String[] { String.valueOf(mLoan.getId()) }
        );

        // Cleara values och förbered för en ny insertion för att skapa ett record. Detta record
        // kommer att bidra med spårbarhet för avbetalningen så att denna syns i statistiken.
        values.clear();
        values.put(RecordEntry.COLUMN_NAME_TYPE, RecordEntry.TYPE_PARAM_LOAN_PAYMENT);
        values.put(RecordEntry.COLUMN_NAME_AMOUNT, paymentAmount);
        // Viktigt med korrekt tidsstämpel så att avbetalningen visas på rätt plats i statistiken.
        values.put(RecordEntry.COLUMN_NAME_TIMESTAMP, TimeTracking.getStartOfMonth(calendar).getTimeInMillis());

        getActivity().getContentResolver().insert(
                ExpenseControlContentProvider.RECORD_URI,
                values
        );

        return true;
    }

    /**
     * Skickar resultatet tillbaka till det fragment som är satt som target.
     * @param resultCode, antingen OK eller inte OK
     */

    private void sendResult(int resultCode) {
        if (getTargetFragment() == null) {
            return;
        }
        Intent intent = new Intent();

        // Skicka med det uppdaterade lånet.
        intent.putExtra(EXTRA_LOAN, mLoan);

        getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, intent);
    }
}
