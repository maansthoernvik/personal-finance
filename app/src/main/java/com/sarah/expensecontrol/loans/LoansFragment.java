package com.sarah.expensecontrol.loans;

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

import static com.sarah.expensecontrol.util.LoaderManagerIDs.LOANS_LOADER_ID;
import static com.sarah.expensecontrol.model.ExpenseControlContract.*;

/**
 * Fragmentet som visar översikten på alla nuvarande lån samt tillåter skapandet av nya lån.
 */

public class LoansFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "LoansFragment";

    // Extras som behövs för editering av existerande lån.
    public static final String EXTRA_LOAN_ID = "LoansFragment:LoanID";
    public static final String EXTRA_EDIT = "LoansFragment:isEdit";

    /* Definitioner för content provider interaktion */
    private static final String[] LOAN_PROJECTION = {
            LoanEntry._ID,
            LoanEntry.COLUMN_NAME_NAME,
            LoanEntry.COLUMN_NAME_AMOUNT,
            LoanEntry.COLUMN_NAME_INTEREST_RATE,
            LoanEntry.COLUMN_NAME_AMORTIZATION_AMOUNT
    };
    private static final String[] LOAN_FROM_BINDING = {
            LoanEntry.COLUMN_NAME_NAME,
            LoanEntry.COLUMN_NAME_AMOUNT,
            LoanEntry.COLUMN_NAME_INTEREST_RATE,
            LoanEntry.COLUMN_NAME_AMORTIZATION_AMOUNT
    };
    private static final int[] LOAN_TO_BINDING = {
            R.id.loan_list_name,
            R.id.loan_list_amount,
            R.id.loan_list_interest,
            R.id.loan_list_amortization
    };
    /* Definitioner för content provider interaktion */

    private SimpleCursorAdapter mLoansListViewAdapter;

    /**
     * Standard, skapar vyn för fragmentet.
     * @param inflater layoutinflater
     * @param container parent
     * @param savedInstanceState state
     * @return view för expense översikten
     */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_loans, container, false);

        ListView mLoansListView = (ListView) view.findViewById(R.id.loans_list_view);
        mLoansListViewAdapter = new SimpleCursorAdapter(
                getActivity().getApplicationContext(),
                R.layout.loan_list_item,
                null,
                LOAN_FROM_BINDING,
                LOAN_TO_BINDING,
                0
        );
        mLoansListView.setAdapter(mLoansListViewAdapter);
        // Custom on item click listener som finns definierad längre ner som en inline klass.
        mLoansListView.setOnItemClickListener(new LoanViewOnItemClickListener());

        FloatingActionButton mFloatingActionButton = (FloatingActionButton) view.findViewById(R.id.loans_add_button);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Såsom i alla andra fragment som hanterar floatingActionButtons, starta aktiviteten
                // associerad med den typ av data du vill skapa, ej editering.
                Intent intent = new Intent(getActivity(), LoanDetailActivity.class);
                intent.putExtra(EXTRA_EDIT, false);
                startActivity(intent);
            }
        });

        // Dra igång lån loadern.
        getActivity().getSupportLoaderManager().initLoader(LOANS_LOADER_ID, null, this);

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
            case LOANS_LOADER_ID:
                return new CursorLoader(
                        getActivity().getApplicationContext(),
                        ExpenseControlContentProvider.LOAN_URI,
                        LOAN_PROJECTION,
                        null,
                        null,
                        null    // Ingen selection, argument eller sortorder krävs, standardsortering
                );              // görs med A -> Z.
            default:
                Log.d(TAG, "No loader id was matched.");
                break;
        }
        return null;
    }

    /**
     * Kallas när loadern hämtat sin associerade data och uppdaterar listvyadapterns cursor.
     * som blivit klar med laddningen.
     * @param loader, den färdiga loadern
     * @param data, laddad data
     */

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mLoansListViewAdapter.swapCursor(data);
    }

    /**
     * Tar helt enkelt bort referensen till den cursor som fanns i adaptern i väntan på uppdaterad data.
     * @param loader som har resettats
     */

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mLoansListViewAdapter.swapCursor(null);
    }

    /**
     * Klass som implementerar AdapterViews OnItemClickListener för att starta editeringsaktiviteten
     * till det associerade list-itemet.
     */

    private class LoanViewOnItemClickListener implements AdapterView.OnItemClickListener {

        LoanViewOnItemClickListener() {}

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            Intent intent;

            intent = new Intent(getContext(), LoanDetailActivity.class);
            // Om ett list item klickats så måste idt komma med aktiviteten som startas.
            intent.putExtra(EXTRA_LOAN_ID, id);
            intent.putExtra(EXTRA_EDIT, true);
            startActivity(intent);
        }
    }
}
