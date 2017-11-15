package com.sarah.expensecontrol.home;

import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.sarah.expensecontrol.expenses.ExpenseCursorAdapterViewBinder;
import com.sarah.expensecontrol.util.TimeTracking;
import com.sarah.expensecontrol.model.ExpenseControlContentProvider;
import com.sarah.expensecontrol.R;

import java.util.Calendar;

import static com.sarah.expensecontrol.model.ExpenseControlContract.*;
import static com.sarah.expensecontrol.util.LoaderManagerIDs.*;

/**
 * Detta fragment styr över de summor som visas på Home-vyn.
 */

public class HomeFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "HomeFragment";

    private static final String CURRENT_MONTH_START = String.valueOf(
            TimeTracking.getStartOfMonth(Calendar.getInstance()).getTimeInMillis()
    );
    private static final String CURRENT_MONTH_END = String.valueOf(
            TimeTracking.getEndOfMonth(Calendar.getInstance()).getTimeInMillis()
    );

    /* Definitioner för content provider interaktion */
    private static final String[] EXPENSE_PROJECTION = {
            ExpenseEntry._ID,
            ExpenseEntry.COLUMN_NAME_NAME,
            ExpenseEntry.COLUMN_NAME_AMOUNT,
            ExpenseEntry.COLUMN_NAME_TIMESTAMP,
            ExpenseEntry.COLUMN_NAME_RECURRING,
            ExpenseEntry.COLUMN_NAME_CATEGORY,
    };
    private static final String RECENT_EXPENSES_SELECTION =
            ExpenseEntry.COLUMN_NAME_RECURRING + "=? AND " +
            ExpenseEntry.COLUMN_NAME_TIMESTAMP + ">=? AND " +
            ExpenseEntry.COLUMN_NAME_TIMESTAMP + "<=?";
    private static final String[] RECENT_EXPENSES_SELECTION_ARGS = {
            "0", CURRENT_MONTH_START, CURRENT_MONTH_END
    };
    private static final String CURRENT_MONTH_EXPENSES_SELECTION = "(" +
            ExpenseEntry.COLUMN_NAME_TIMESTAMP + ">=? AND " +
            ExpenseEntry.COLUMN_NAME_TIMESTAMP + "<=?) OR " +
            ExpenseEntry.COLUMN_NAME_RECURRING + "=?";
    private static final String[] CURRENT_MONTH_EXPENSES_SELECTION_ARGS = {
            CURRENT_MONTH_START, CURRENT_MONTH_END, "1"
    };
    private static final String[] LOAN_PROJECTION = {
            LoanEntry._ID,
            LoanEntry.COLUMN_NAME_NAME,
            LoanEntry.COLUMN_NAME_AMOUNT,
            LoanEntry.COLUMN_NAME_INTEREST_RATE,
            LoanEntry.COLUMN_NAME_AMORTIZATION_AMOUNT
    };
    private final String[] RECORD_PROJECTION = {
            RecordEntry.COLUMN_NAME_AMOUNT,
    };
    private final String RECORD_SELECTION =
            RecordEntry.COLUMN_NAME_TYPE + "=? AND " +
            RecordEntry.COLUMN_NAME_TIMESTAMP + ">=? AND " +
            RecordEntry.COLUMN_NAME_TIMESTAMP + "<=?";
    private static final String[] EXPENSE_FROM_BINDING = {
            ExpenseEntry.COLUMN_NAME_NAME,
            ExpenseEntry.COLUMN_NAME_AMOUNT,
            ExpenseEntry.COLUMN_NAME_TIMESTAMP,
            ExpenseEntry.COLUMN_NAME_CATEGORY,
    };
    private static final int[] EXPENSE_TO_BINDING = {
            R.id.expense_list_name,
            R.id.expense_list_amount,
            R.id.expense_list_timestamp,
            R.id.expense_list_category
    };
    /* Definitioner för content provider interaktion */

    private TextView mTotalExpensesTextView;
    private SimpleCursorAdapter mRecentExpensesListViewAdapter;

    // Används för att räkna ut och visa det totala beloppet som spenderats för den nuvarande månaden.
    private int mTotalExpenses = 0;
    private int mTotalLoans = 0;

    /**
     * Standard, skapar vyn för fragmentet.
     * @param inflater layoutinflater
     * @param container parent
     * @param savedInstanceState state
     * @return view för expense översikten
     */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mTotalExpensesTextView = (TextView) view.findViewById(R.id.home_total_expenses_text_view);
        updateTotal();  // Detta görs för att inte lämna total-fältet helt blankt, det kommer initiellt
                        // att visa 0 kr till loaders har laddat klart sin data.
        ListView mRecentExpensesListView = (ListView) view.findViewById(R.id.home_recent_expenses_list_view);
        mRecentExpensesListViewAdapter = new SimpleCursorAdapter(
                getActivity().getApplicationContext(),
                R.layout.expense_list_item,
                null,
                EXPENSE_FROM_BINDING,
                EXPENSE_TO_BINDING,
                0                       // Det är viktigt att flagga 0, annars kommer adaptern att
        );                              // begära data på nytt i UI tråden. Detta ska skötas av
                                        // loaders som har sin egen tråd.
        mRecentExpensesListViewAdapter.setViewBinder(new ExpenseCursorAdapterViewBinder());
        mRecentExpensesListView.setAdapter(mRecentExpensesListViewAdapter);

        FloatingActionButton mFloatingActionButton = (FloatingActionButton) view.findViewById(R.id.home_add_button);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Dialogen visar tre alternativ, lån, expense eller cancel.
                new NewEntryAlertDialog(getContext()).show();
            }
        });

        // Dra igång alla loaders som behövs. Notera att om det redan finns loaders av de spesade id
        // numren igång, så kommer dessa hämtas istället för att nya startas.
        getActivity().getSupportLoaderManager().initLoader(RECENT_ONE_TIME_EXPENSES_LOADER_ID, null, this);
        getActivity().getSupportLoaderManager().initLoader(ALL_EXPENSES_LOADER_ID, null, this);
        getActivity().getSupportLoaderManager().initLoader(LOANS_LOADER_ID, null, this);

        return view;
    }

    /**
     * Räkna ut totalen och sätt textviewen till resultatet.
     */

    private void updateTotal() {
        int mTotal = mTotalExpenses + mTotalLoans;
        mTotalExpensesTextView.setText(String.valueOf(mTotal) + " kr");
    }

    /**
     * Räkna ut hur mycket expensetotalen är för data som Cursorn håller.
     * @param data, cursor som ska innehålla expensedata
     */

    private void calculateExpenseTotal(Cursor data) {
        mTotalExpenses = 0;

        for (data.moveToFirst(); !data.isAfterLast(); data.moveToNext()) {
            mTotalExpenses += data.getInt(data.getColumnIndex(ExpenseEntry.COLUMN_NAME_AMOUNT));
        }
    }

    /**
     * Räkna ut hur mycket låntotalen är för den data som Cursorn håller.
     * @param data, cursor som ska innehålla låndata.
     */

    private void calculateLoanTotal(Cursor data) {
        mTotalLoans = 0;

        for (data.moveToFirst(); !data.isAfterLast(); data.moveToNext()) {
            // Konvertera räntefältet till decimalt för att få rätt resultat (ex. 1.5 -> 0.015).
            double interestRate = data.getDouble(data.getColumnIndex(LoanEntry.COLUMN_NAME_INTEREST_RATE)) * 0.01;
            int amount = data.getInt(data.getColumnIndex(LoanEntry.COLUMN_NAME_AMOUNT));
            int interestPayment = (int) (interestRate * amount) / 12;
            int amortization = data.getInt(
                    data.getColumnIndex(LoanEntry.COLUMN_NAME_AMORTIZATION_AMOUNT)
            );
            mTotalLoans += (interestPayment + amortization);
        }

        addLoanPaymentTotal();
    }

    /**
     * Eftersom att det är möjligt att extra avbetalningar har gjorts på lån för den nuvarande
     * månaden så måste även dessa records hämtas fram.
     */

    private void addLoanPaymentTotal() {
        Cursor records = getActivity().getContentResolver().query(
                ExpenseControlContentProvider.RECORD_URI,
                RECORD_PROJECTION,
                RECORD_SELECTION,
                new String[] {
                        RecordEntry.TYPE_PARAM_LOAN_PAYMENT,    // Bara lånbetalningar.
                        CURRENT_MONTH_START,                    // Från starten av denna månad
                        CURRENT_MONTH_END },                    // till slutet av denna månad.
                null
        );

        for (records.moveToFirst();
             !records.isAfterLast();
             records.moveToNext()) {

            mTotalLoans += records.getInt(records.getColumnIndex(RecordEntry.COLUMN_NAME_AMOUNT));
        }
        records.close();
    }

    /**
     * Skapar en loader och utför en laddning baserat på vilket loader id som används.
     * @param id, loaderns id-nummer
     * @param args, extra argument till loadern
     * @return en loader som håller en cursor med data
     */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case RECENT_ONE_TIME_EXPENSES_LOADER_ID:
                return new CursorLoader(
                        getActivity().getApplicationContext(),
                        ExpenseControlContentProvider.EXPENSE_URI,
                        EXPENSE_PROJECTION,
                        RECENT_EXPENSES_SELECTION,
                        RECENT_EXPENSES_SELECTION_ARGS,
                        ExpenseEntry.COLUMN_NAME_TIMESTAMP + " DESC LIMIT 30"   // 30 senaste inköpen
                );
            case ALL_EXPENSES_LOADER_ID:
                return new CursorLoader(
                        getActivity().getApplicationContext(),
                        ExpenseControlContentProvider.EXPENSE_URI,
                        EXPENSE_PROJECTION,
                        CURRENT_MONTH_EXPENSES_SELECTION,
                        CURRENT_MONTH_EXPENSES_SELECTION_ARGS,
                        null
                );
            case LOANS_LOADER_ID:
                return new CursorLoader(
                        getActivity().getApplicationContext(),
                        ExpenseControlContentProvider.LOAN_URI,
                        LOAN_PROJECTION,
                        null,
                        null,
                        null
                );
            default:
                Log.d(TAG, "Failed to match a loader ID.");
                break;
        }
        return null;
    }

    /**
     * Kallas när loadern hämtat sin associerade data och uppdaterar totalen för den loaders data
     * som blivit klar med laddningen.
     * @param loader, den färdiga loadern
     * @param data, laddad data
     */

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        int id = loader.getId();

        switch (id) {
            case RECENT_ONE_TIME_EXPENSES_LOADER_ID:
                mRecentExpensesListViewAdapter.swapCursor(data);
                break;
            case ALL_EXPENSES_LOADER_ID:
                calculateExpenseTotal(data);
                updateTotal();
                break;
            case LOANS_LOADER_ID:
                calculateLoanTotal(data);
                updateTotal();
                break;

        }
    }

    /**
     * Om en loader har återställts, se till att totaln återställs så att den kan uppdateras med den
     * nya korrekta datan. Återställ även recent expenses listan om denna loadern blivit påverkad.
     * @param loader som har resettats
     */

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        int id = loader.getId();

        switch (id) {
            case RECENT_ONE_TIME_EXPENSES_LOADER_ID:
                mRecentExpensesListViewAdapter.swapCursor(null);
                break;
            case ALL_EXPENSES_LOADER_ID:
                mTotalExpenses = 0;
                updateTotal();
                break;
            case LOANS_LOADER_ID:
                mTotalLoans = 0;
                updateTotal();
                break;
        }
    }
}
