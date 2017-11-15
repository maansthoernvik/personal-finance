package com.sarah.expensecontrol.expenses;

import android.content.Intent;
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
import android.widget.AdapterView;
import android.widget.ListView;

import com.sarah.expensecontrol.R;
import com.sarah.expensecontrol.model.ExpenseControlContentProvider;

import static com.sarah.expensecontrol.util.LoaderManagerIDs.ONE_TIME_EXPENSES_LOADER_ID;
import static com.sarah.expensecontrol.util.LoaderManagerIDs.RECURRING_EXPENSES_LOADER_ID;
import static com.sarah.expensecontrol.model.ExpenseControlContract.*;

/**
 * Detta fragment visar information om alla expenses som just nu finns registrerade i applikationen.
 * Det finns även alternativ för att gå till en ny vy där expenses kan skapas.
 */

public class ExpensesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "ExpensesFragment";

    // Extras som behövs för att starta ExpenseDetailActivity, de indikerar om det är en editering eller ej.
    protected static final String EXTRA_EXPENSE_ID = "ExpensesFragment:expenseId";
    public static final String EXTRA_EDIT = "ExpensesFragment:editMode";

    /* Definitioner för content provider interaktion */
    private static final String[] EXPENSE_PROJECTION = {
            ExpenseEntry._ID,
            ExpenseEntry.COLUMN_NAME_NAME,
            ExpenseEntry.COLUMN_NAME_AMOUNT,
            ExpenseEntry.COLUMN_NAME_TIMESTAMP,
            ExpenseEntry.COLUMN_NAME_RECURRING,
            ExpenseEntry.COLUMN_NAME_CATEGORY,
    };
    private static final String EXPENSE_SELECTION =
            ExpenseEntry.COLUMN_NAME_RECURRING + "=?";

    private static final String RECURRING_EXPENSE_SORT_ORDER =
            ExpenseEntry.COLUMN_NAME_CATEGORY + " ASC";

    private static final String ONE_TIME_EXPENSE_SORT_ORDER =
            ExpenseEntry.COLUMN_NAME_TIMESTAMP + " DESC";

    // Binder kolumnnamn till layoutresursers idn. Index 0 -> 0, 1 -> 1 osv.
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

    // Adaptrar för de både listvyerna som visar expenseöversikterna.
    private SimpleCursorAdapter mRecurringExpenseListViewAdapter;
    private SimpleCursorAdapter mOneTimeExpenseListViewAdapter;

    /**
     * Standard, skapar vyn för fragmentet.
     * @param inflater layoutinflater
     * @param container parent
     * @param savedInstanceState state
     * @return view för expense översikten
     */

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);

        ListView mRecurringExpenseListView = (ListView) view.findViewById(R.id.expenses_recurring_list_view);
        mRecurringExpenseListViewAdapter = new SimpleCursorAdapter(
                getActivity().getApplicationContext(),
                R.layout.expense_list_item,
                null,
                EXPENSE_FROM_BINDING,
                EXPENSE_TO_BINDING,
                0
        );
        // Använd den custom view binder som skapats.
        mRecurringExpenseListViewAdapter.setViewBinder(new ExpenseCursorAdapterViewBinder());
        mRecurringExpenseListView.setAdapter(mRecurringExpenseListViewAdapter);
        // Custom ItemClickListener finns definierad längre ned som en inline klass.
        mRecurringExpenseListView.setOnItemClickListener(
                new ExpenseViewOnItemClickListener()
        );

        ListView mOneTimeExpenseListView = (ListView) view.findViewById(R.id.expenses_one_time_list_view);
        mOneTimeExpenseListViewAdapter = new SimpleCursorAdapter(
                getActivity().getApplicationContext(),
                R.layout.expense_list_item,
                null,
                EXPENSE_FROM_BINDING,
                EXPENSE_TO_BINDING,
                0
        );
        // Även här används förstås den custom view bindern.
        mOneTimeExpenseListViewAdapter.setViewBinder(new ExpenseCursorAdapterViewBinder());
        mOneTimeExpenseListView.setAdapter(mOneTimeExpenseListViewAdapter);
        mOneTimeExpenseListView.setOnItemClickListener(
                new ExpenseViewOnItemClickListener()
        );

        FloatingActionButton mFloatingActionButton = (FloatingActionButton) view.findViewById(R.id.expenses_add_button);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), ExpenseDetailActivity.class);
                    intent.putExtra(EXTRA_EDIT, false); // Flagga för att denna aktivitet inte startas
                    startActivity(intent);              // för att editera en existerande utgift
                }
        });

        // Starta de två loaders som behövs.
        getActivity().getSupportLoaderManager().initLoader(ONE_TIME_EXPENSES_LOADER_ID, null, this);
        getActivity().getSupportLoaderManager().initLoader(RECURRING_EXPENSES_LOADER_ID, null, this);

        return view;
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
            case RECURRING_EXPENSES_LOADER_ID:
                return new CursorLoader(
                        getActivity().getApplicationContext(),
                        ExpenseControlContentProvider.EXPENSE_URI,
                        EXPENSE_PROJECTION,
                        EXPENSE_SELECTION,
                        new String[] { "1" },           // Selection argumenten har inte fått sitt
                        RECURRING_EXPENSE_SORT_ORDER    // eget klassfält eftersom de endast har
                );                                      // ett argument.
            case ONE_TIME_EXPENSES_LOADER_ID:
                return new CursorLoader(
                        getActivity().getApplicationContext(),
                        ExpenseControlContentProvider.EXPENSE_URI,
                        EXPENSE_PROJECTION,
                        EXPENSE_SELECTION,
                        new String[] { "0" },           // Selection argumenten har inte fått sitt
                        ONE_TIME_EXPENSE_SORT_ORDER     // eget klassfält eftersom de endast har
                );                                      // ett argument.
            default:
                Log.d(TAG, "No loader id was matched.");
                break;
        }
        return null;
    }

    /**
     * Kallas när loadern hämtat sin associerade data länkar rätt adapter till rätt loader mha.
     * Loaderns id-nummer.
     * @param loader, den färdiga loadern
     * @param data, laddad data
     */

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        int id = loader.getId();

        switch (id) {
            case RECURRING_EXPENSES_LOADER_ID:
                mRecurringExpenseListViewAdapter.swapCursor(data);
                break;
            case ONE_TIME_EXPENSES_LOADER_ID:
                mOneTimeExpenseListViewAdapter.swapCursor(data);
                break;
        }
    }

    /**
     * Om en loader har återställts, resetta datan i den associerade adaptern så att ny data kan
     * ta dess plats.
     * @param loader som har resettats
     */

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        int id = loader.getId();

        switch (id) {
            case RECURRING_EXPENSES_LOADER_ID:
                mRecurringExpenseListViewAdapter.swapCursor(null);
                break;
            case ONE_TIME_EXPENSES_LOADER_ID:
                mOneTimeExpenseListViewAdapter.swapCursor(null);
                break;
        }
    }

    /**
     * Klass som implementerar AdapterViews OnItemClickListener för att starta editeringsaktiviteten
     * till det associerade list-itemet.
     */

    private class ExpenseViewOnItemClickListener implements AdapterView.OnItemClickListener {

        ExpenseViewOnItemClickListener() {}

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            Intent intent;

            intent = new Intent(getContext(), ExpenseDetailActivity.class);
            // Om ett list item klickats så måste idt komma med aktiviteten som startas.
            intent.putExtra(EXTRA_EXPENSE_ID, id);
            intent.putExtra(EXTRA_EDIT, true);
            startActivity(intent);
        }
    }
}
