package com.sarah.expensecontrol.loans;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.sarah.expensecontrol.Loan;
import com.sarah.expensecontrol.R;
import com.sarah.expensecontrol.model.ExpenseControlContentProvider;

import static android.app.Activity.RESULT_OK;
import static com.sarah.expensecontrol.model.ExpenseControlContract.*;

/**
 * Fragment för detaljvyn hos ett lån.
 */

public class LoanDetailFragment extends Fragment {

    private static final String TAG = "LoanDetailFragment";

    // Det extra som behövs för lånavbetalningar
    protected static final String EXTRA_LOAN = "LoanDetailFragment:loan_id";

    /* Definitioner för content provider interaktion */
    private static final String[] LOAN_PROJECTION = {
            LoanEntry._ID,
            LoanEntry.COLUMN_NAME_NAME,
            LoanEntry.COLUMN_NAME_AMOUNT,
            LoanEntry.COLUMN_NAME_INTEREST_RATE,
            LoanEntry.COLUMN_NAME_AMORTIZATION_AMOUNT
    };
    private static final String LOAN_SELECTION = LoanEntry._ID + "=?";
    /* Definitioner för content provider interaktion */

    // Requestkod för att göra en lånavbetalning
    private static final int MAKE_PAYMENT_REQUEST_CODE = 0;

    private EditText mLoanName;
    private EditText mLoanAmount;
    private EditText mLoanInterest;
    private EditText mLoanAmortization;

    private Loan mLoan;
    private boolean mIsEdit;

    /**
     * Standard, skapar vyn för fragmentet.
     * @param inflater layoutinflater
     * @param container parent
     * @param savedInstanceState state
     * @return view för expense översikten
     */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_loan_detail, container, false);

        loadExtras();   // Laddar in alla extras som skickades med när fragmentets aktivitet startades.

        mLoanName = (EditText) view.findViewById(R.id.loan_detail_name);
        mLoanAmount = (EditText) view.findViewById(R.id.loan_detail_amount);
        mLoanInterest = (EditText) view.findViewById(R.id.loan_detail_interest);
        mLoanAmortization = (EditText) view.findViewById(R.id.loan_detail_amortization);

        TextView loanMakePaymentDesc = (TextView) view.findViewById(R.id.loan_detail_make_payment_message);
        loanMakePaymentDesc.setVisibility(mIsEdit ? View.VISIBLE : View.INVISIBLE);

        Button mMakePayment = (Button) view.findViewById(R.id.loan_detail_make_payment);
        mMakePayment.setVisibility(mIsEdit ? View.VISIBLE : View.INVISIBLE);
        mMakePayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Startar lånavbetalningsdialogen.
                FragmentManager fm = getActivity().getSupportFragmentManager();

                LoanPaymentFragment dialog = LoanPaymentFragment.newInstance(mLoan);

                dialog.setTargetFragment(LoanDetailFragment.this, MAKE_PAYMENT_REQUEST_CODE);
                dialog.show(fm, TAG);
            }
        });

        // Om ett lån just nu editeras, lägg in värden för det lånet i textfälten.
        if (mIsEdit) {
            populateFields();
        }

        // Sätt precis som för expense detaljer upp en custom action bar som har en delete, save och
        // cancel knapp.
        setUpCustomActionBar();

        return view;
    }

    /**
     * I detta fall hämtas information om lånavbetalningen in i fragmentet för att sätta rätt lån-
     * summa efter avbetalningen.
     * @param requestCode för förfrågan, så att det går att spåra vad som startats
     * @param resultCode om resultatet var OK eller inte
     * @param intent intent data från förfrågan
     */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode != RESULT_OK) return;

        if (requestCode == MAKE_PAYMENT_REQUEST_CODE) {
            // Uppdatera det nuvarande lånet med summan efter avbetalningen och ladda om lånets fält.
            mLoan.setAmount(((Loan) intent.getSerializableExtra(EXTRA_LOAN)).getAmount());
            populateFields();
        }
    }

    /**
     * Försöker spara ändringarna men kontrollerar först så att alla fält är ifyllda.
     * @return true om ändringarna kan sparas
     */

    private boolean saveChanges() {
        ContentValues values = new ContentValues();

        if (TextUtils.isEmpty(mLoanName.getText()) ||
                TextUtils.isEmpty(mLoanAmount.getText()) ||
                TextUtils.isEmpty(mLoanInterest.getText()) ||
                TextUtils.isEmpty(mLoanAmortization.getText())) {
            Toast.makeText(getContext(), "Could not save, fill out the fields!", Toast.LENGTH_SHORT).show();
            return false;
        }

        values.put(LoanEntry.COLUMN_NAME_NAME, mLoanName.getText().toString());
        values.put(LoanEntry.COLUMN_NAME_AMOUNT, Integer.valueOf(mLoanAmount.getText().toString()));
        values.put(LoanEntry.COLUMN_NAME_INTEREST_RATE, Double.valueOf(mLoanInterest.getText().toString()));
        values.put(LoanEntry.COLUMN_NAME_AMORTIZATION_AMOUNT, Integer.valueOf(mLoanAmortization.getText().toString()));

        // Om editeras, uppdatera, annars insert.
        if (mIsEdit) {
            getActivity().getContentResolver().update(
                    ExpenseControlContentProvider.LOAN_URI,
                    values,
                    LOAN_SELECTION,
                    new String[] { String.valueOf(mLoan.getId()) }
            );
        } else {
            getActivity().getContentResolver().insert(
                    ExpenseControlContentProvider.LOAN_URI,
                    values
            );
        }

        return true;
    }

    /**
     * Tar bort (om ett existerade lån editeras) ett lån.
     */

    private void delete() {
        getActivity().getContentResolver().delete(
                ExpenseControlContentProvider.LOAN_URI,
                LOAN_SELECTION,
                new String[] { String.valueOf(mLoan.getId()) }
        );
    }

    /**
     * Laddar in extras som kommit med då fragmentets aktivitet startades.
     */

    private void loadExtras() {
        Intent intent = getActivity().getIntent();
        mIsEdit = intent.getBooleanExtra(LoansFragment.EXTRA_EDIT, false);

        // Om ett lån editeras, hämta låndata.
        if (mIsEdit) {
            long loanId = intent.getLongExtra(LoansFragment.EXTRA_LOAN_ID, -1);
            Cursor cursor = getActivity().getContentResolver().query(
                    ExpenseControlContentProvider.LOAN_URI,
                    LOAN_PROJECTION,
                    LOAN_SELECTION,
                    new String[] { String.valueOf(loanId) },
                    null
            );
            if (cursor.moveToFirst()) {
                mLoan = new Loan(
                        cursor.getLong(cursor.getColumnIndex(LoanEntry._ID)),
                        cursor.getString(cursor.getColumnIndex(LoanEntry.COLUMN_NAME_NAME)),
                        cursor.getInt(cursor.getColumnIndex(LoanEntry.COLUMN_NAME_AMOUNT)),
                        cursor.getDouble(cursor.getColumnIndex(LoanEntry.COLUMN_NAME_INTEREST_RATE)),
                        cursor.getInt(cursor.getColumnIndex(LoanEntry.COLUMN_NAME_AMORTIZATION_AMOUNT))
                );
            }
            cursor.close();
        }
    }

    /**
     * Sätter lånets värden till de olika textfälten.
     */

    private void populateFields() {
        mLoanName.setText(mLoan.getName());
        mLoanAmount.setText(String.valueOf(mLoan.getAmount()));
        mLoanInterest.setText(String.valueOf(mLoan.getInterest()));
        mLoanAmortization.setText(String.valueOf(mLoan.getAmortization()));
    }

    /**
     * Sätter upp knapparna i den custom action bar som skapats i den underliggande aktiviteten
     * NavigationLessActivity.
     */

    private void setUpCustomActionBar() {
        /*
        Likadant utformat som ExpenseDetailFragment. Jag har försökt använda ett sorts interface
        med lika namngivna metoder. Tillexempel saveChanges, delete, populate fields osv.
         */
        View view = ((AppCompatActivity) getActivity()).getSupportActionBar().getCustomView();
        ImageButton btnClose = (ImageButton) view.findViewById(R.id.custom_action_bar_close);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finish();
            }
        });
        ImageButton btnDelete = (ImageButton) view.findViewById(R.id.custom_action_bar_delete);
        btnDelete.setVisibility(mIsEdit ? View.VISIBLE : View.INVISIBLE);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delete();
                getActivity().finish();
            }
        });
        ImageButton btnDone = (ImageButton) view.findViewById(R.id.custom_action_bar_done);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (saveChanges()) getActivity().finish();
            }
        });
    }
}
