package com.sarah.expensecontrol.statistics;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.sarah.expensecontrol.R;
import com.sarah.expensecontrol.model.ExpenseControlContentProvider;
import com.sarah.expensecontrol.util.TimeTracking;

import static com.sarah.expensecontrol.model.ExpenseControlContract.*;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Det här fragmentet används för att visa statistik över all data i applikationen, nu som då.
 */

public class StatisticsFragment extends Fragment {

    private static final String TAG = "StatisticsFragment";

    /* Definitioner för content provider interaktion */
    private static final String ONE_TIME_EXPENSE_WHERE =
            ExpenseEntry.COLUMN_NAME_RECURRING + "=? AND " +
            ExpenseEntry.COLUMN_NAME_TIMESTAMP + "<?";
    private static final String[] ONE_TIME_EXPENSE_WHERE_ARGS = {
            "0", String.valueOf(TimeTracking.getStartOfMonth(Calendar.getInstance()).getTimeInMillis())
    };
    private static final String RECORDS_DELETE_WHERE =
            RecordEntry.COLUMN_NAME_TIMESTAMP + "<?";
    /* Definitioner för content provider interaktion */

    private TableLayout mTableLayout;
    private ArrayList<TableRow> mTableLayoutRows;

    private String[] mMonthNames;

    // Offset från nuvarande månaden, alltså hur många månader tillbaka i tiden som ska visas.
    private int mCurrentOffset = 0;

    /**
     * Standard, skapar vyn för fragmentet.
     * @param inflater layoutinflater
     * @param container parent
     * @param savedInstanceState state
     * @return view för expense översikten
     */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        // Månaderna finns definierade som en strängresurs.
        mMonthNames = getResources().getStringArray(R.array.statistics_months);

        mTableLayout = (TableLayout) view.findViewById(R.id.statistics_table_layout);
        setUpTableRows(mTableLayout);

        // Knapp för att gå ännu längre tillbaka i tiden i statistiken.
        Button mButtonBackwards = (Button) view.findViewById(R.id.statistics_button_backwards);
        mButtonBackwards.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stepBackwards();
            }
        });
        // Knapp för att stega framåt i tiden.
        Button mButtonForwards = (Button) view.findViewById(R.id.statistics_button_forwards);
        mButtonForwards.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stepForwards();
            }
        });

        // Knapp för att rensa all historik längre tillbaka än den nuvarande månaden. Visar först
        // en dialog som ber användaren att bekräfta aktionen.
        Button mButtonClearHistory = (Button) view.findViewById(R.id.statistics_button_clear_history);
        mButtonClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder clearHistoryDialog = new AlertDialog.Builder(getContext());
                clearHistoryDialog.setTitle(R.string.clear_history);
                clearHistoryDialog.setMessage(R.string.dialog_statistics_clear_history_message);
                clearHistoryDialog.setNegativeButton(android.R.string.cancel, null);
                clearHistoryDialog.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                clearHistory();
                            }
                        }
                );
                clearHistoryDialog.show();
            }
        });

        return view;
    }

    /**
     * Rensar historik tidigare än den nuvarande månaden.
     */

    private void clearHistory() {
        getActivity().getContentResolver().delete(
                ExpenseControlContentProvider.RECORD_URI,
                RECORDS_DELETE_WHERE,
                new String[] {
                        String.valueOf(TimeTracking.getStartOfMonth(Calendar.getInstance()).getTimeInMillis())
                }
        );
        getActivity().getContentResolver().delete(
                ExpenseControlContentProvider.EXPENSE_URI,
                ONE_TIME_EXPENSE_WHERE,
                ONE_TIME_EXPENSE_WHERE_ARGS
        );

        mTableLayout.removeAllViews();
        setUpTableRows(mTableLayout);

        Toast.makeText(getContext(), "History was cleared successfully!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Kontrollerar först om stegningen kan tillåtas. Man kan tillexempel inte stega längre fram än
     * idag. Max 1 steg tillåts åt gången.
     * @param step, steg att ta
     * @return true om aktionen godkänns, false annars
     */

    private boolean stepMonthOffset(int step) {
        if (step > 1 || step < -1) {
            return false;
        } else if (step == -1 && mCurrentOffset == 0) {
            return false;
        }
        mCurrentOffset += step;
        return true;
    }

    /**
     * Stegar tillbaka i tiden för att visa statistik längre tillbaka.
     */

    private void stepBackwards() {
        // Öka offset med 1, om det tillåts.
        if (stepMonthOffset(1)) {
            // Ta bort den rad data som visas längst upp för att göra plats åt data längre tillbaka
            // i tiden.
            mTableLayout.removeView(mTableLayoutRows.remove(0));

            setUpNextRow(getNextRowDate()); // Dra igång processen med att hämta upp äldre data.
        }
    }

    /**
     * Räknar ut vilket datum som det ska hämtas data för.
     * @return ett par med <år, månad>
     */

    public Pair<Integer, Integer> getNextRowDate() {
        Calendar cal = Calendar.getInstance();
        /*
         Statisk uträkning för vilket år + månad som ska hämtas:
         Om nuvarande månad är 2017, september och statistiken per default visar 12 månader
         (alltså 2017 september -> 2016 oktober) så blir i detta fall resultatet:
         2017 september - 11 månader + (-1) = 2017 september - 12 månader = <2016, september>
         */
        cal.add(Calendar.MONTH, -11 + (-mCurrentOffset));

        return new Pair<>(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));
    }

    /**
     * Med ett datumpar som input så kan nästa rad sättas upp.
     * @param date, datumpar innehållande <år, månad>
     */

    private void setUpNextRow(Pair date) {
        final Context context = getContext();
        TableRow row = new TableRow(context);
        // Hjälpfunktion för att sätta rätt viewparametrar för en TableRow, definierad längre ned.
        setRowLayoutParams(row);

        TextView monthTextView = new TextView(context);
        // Hjälpfunktion för att sätta rätt viewparametrar för en TextView, definierad längre ned.
        setChildLayoutParams(monthTextView);
        monthTextView.setGravity(Gravity.CENTER);
        // Sätt det år och datum som ska visas så att datan visuella kan kopplas till ett datum.
        monthTextView.setText(String.valueOf(date.first) + " " + mMonthNames[(int) date.second]);

        row.addView(monthTextView);
        mTableLayout.addView(row);
        // Viktigt att hålla koll på raderna så att rätt rad kan läggas till/tas bort när det stegas
        // i historiken.
        mTableLayoutRows.add(row);

        int rowId = Integer.valueOf(String.valueOf((int) date.first + "" + (int) date.second));
        // Associera dynamiskt ett ID med raden (såsom det definieras i xml med android:id) för att
        // veta exakt vart den statistikdata som genereras ska sättas in när den laddats klart.
        row.setId(rowId);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, (int) date.first);
        cal.set(Calendar.MONTH, (int) date.second);

        Log.d(TAG, "New row id " + rowId);
        // Gå vidare till laddning av statistikdata.
        loadMonthSummary(new RowInfo(rowId, cal.getTimeInMillis()));
    }

    /**
     * Stegar framåt i tiden för att visa statistik längre fram.
     */

    private void stepForwards() {
        // Kontrollera offset så att vi inte försöker gå förbi dagens datum.
        if (stepMonthOffset(-1)) {
            // Ta bort raden längst ner för att visa en ny längst upp.
            mTableLayout.removeView(mTableLayoutRows.remove(mTableLayoutRows.size() - 1));

            setUpPreviousRow(getPreviousRowDate());
        }
    }

    /**
     * Hämta datum för raden som ska sättas in när det stegas framåt i tiden.
     * @return ett par med <år, månad> för datumet
     */

    public Pair<Integer, Integer> getPreviousRowDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -mCurrentOffset);

        return new Pair<>(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));
    }

    /**
     * Skapar en tableRow för parameterdatumet.
     * @param date att skapa en row för
     */

    private void setUpPreviousRow(Pair date) {
        final Context context = getContext();
        TableRow row = new TableRow(context);
        setRowLayoutParams(row);

        TextView monthTextView = new TextView(context);
        setChildLayoutParams(monthTextView);
        monthTextView.setGravity(Gravity.CENTER);
        monthTextView.setText(String.valueOf(date.first) + " " + mMonthNames[(int) date.second]);

        row.addView(monthTextView);
        // Denna gång läggs raden inte till sist utan först. Anledningen bakom att index 0 inte används
        // är för att tablelayouten har sin header på index 0, datumen visas från index 1 och framåt.
        mTableLayout.addView(row, 1);
        // Lägg även till TableRow referensen först i listan för att indikera att denna rad är den
        // som nu visas först.
        mTableLayoutRows.add(0, row);

        int rowId = Integer.valueOf(String.valueOf((int) date.first + "" + (int) date.second));
        row.setId(rowId);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, (int) date.first);
        cal.set(Calendar.MONTH, (int) date.second);

        Log.d(TAG, "New row id " + rowId);
        loadMonthSummary(new RowInfo(rowId, cal.getTimeInMillis()));
    }

    /**
     * Sätter upp alla TableRows för att visa standardvyn för statistik. Börjar med att skapa headern
     * och sedan de 12 rader som behövs för att visa alla månader.
     * @param tableLayout att lägga till TableRows till.
     */

    public void setUpTableRows(TableLayout tableLayout) {
        final Context context = getContext();
        ArrayList<RowInfo> rowIdentifiers = new ArrayList<>();
        TableRow row;
        mTableLayoutRows = new ArrayList<>();

        /* Lägg till alla headers */
        row = new TableRow(context);
        setRowLayoutParams(row);

        // Headertexterna är definierade som strängresurser.
        String[] headers = getResources().getStringArray(R.array.statistics_headers);
        for (String header : headers) {
            TextView headerTextView = new TextView(getContext());
            setChildLayoutParams(headerTextView);
            headerTextView.setGravity(Gravity.CENTER);
            headerTextView.setText(header);

            row.addView(headerTextView);
        }
        tableLayout.addView(row);
        /* Lägg till alla headers */

        /* Lägg till alla månader som ska visas */
        int addedMonths = 0, index = TimeTracking.getCurrentMonth();
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        String sYear = String.valueOf(year);

        // 12 månader ska läggas till.
        while (addedMonths < 12) {
            row = new TableRow(context);
            setRowLayoutParams(row);

            TextView monthTextView = new TextView(context);
            setChildLayoutParams(monthTextView);
            monthTextView.setGravity(Gravity.CENTER);
            monthTextView.setText(sYear + " " + mMonthNames[index]);

            row.addView(monthTextView);

            tableLayout.addView(row);
            /* Se till att hålla en referens till den TableRow som lades till. Inget index ska
             specas. Den ordning som rows läggs till är den ordning de ska visas i, ex:
             Sep, Aug, Jul, Jun, May, Apr, Mar, Feb, Jan, Dec, Nov, Oct
             Alltså inte Jan -> Dec.
             */
            mTableLayoutRows.add(row);

            // Igen, viktigt med unikt rowId, använd därför kombinationen år + månad i en sträng
            // varje rad ska visa en specifik månad under ett specifikt år, så detta är ett unikt id.
            int rowId = Integer.valueOf(String.valueOf(year + "" + index));
            row.setId(rowId);
            Log.d(TAG, "Initial row id: " + rowId);

            // Skapa en ny rowInfo som senare ska skickas vidare och populeras med data.
            rowIdentifiers.add(new RowInfo(rowId, cal.getTimeInMillis()));
            cal.add(Calendar.MONTH, -1);

            // En månad har lagts till
            addedMonths++;
            // Vi går tillbaka i tiden, så månader hämtas i omvänd riktning.
            index--;

            // Om index = 0, sätt till 11 så att december kan hämtas härnäst.
            if (index < 0) {
                index = 11;
                year--; // Ett år har traverserats.
                // Uppdatera årssträngen.
                sYear = String.valueOf(year);
            }
        }
        /* Lägg till alla månader som ska visas */

        loadMonthSummaries(rowIdentifiers);
    }

    /**
     * Centraliserad metod för att sätta de parametrar som behövs för TableRows.
     * @param row att sätta layoutparametrar till
     */

    private void setRowLayoutParams(TableRow row) {
        // Hämta dp så att layouten fungerar på olika skärmdensiteter.
        final float dp = getResources().getDisplayMetrics().density;
        TableRow.LayoutParams rowLayoutParams = new TableRow.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                (int) (48 * dp));
        row.setLayoutParams(rowLayoutParams);
    }

    /**
     * Centraliserad metod för att sätta de parametrar som behövs för TextViews.
     * @param view att sätta layoutparametrar till
     */


    private void setChildLayoutParams(View view) {
        // Hämta dp så att layouten fungerar på olika skärmdensiteter.
        final float dp = getResources().getDisplayMetrics().density;
        TableRow.LayoutParams childLayoutParams = new TableRow.LayoutParams(
                (int) getResources().getDimension(R.dimen.statistics_table_column),
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        int childMargins = (int) (8 * dp);

        view.setLayoutParams(childLayoutParams);
        childLayoutParams.setMargins(childMargins, childMargins, childMargins, childMargins);
    }

    /**
     * Reläfunktion för att skapa en enskilld summering för en månad.
     * @param rowIdentifier den rad som ska populeras med data
     */

    private void loadMonthSummary(RowInfo rowIdentifier) {
        // Custom Async Task för att ladda in data, definierad nedan.
        LoadMonthSummaryAsyncTask asyncTask = new LoadMonthSummaryAsyncTask();
        asyncTask.execute(rowIdentifier);
    }

    /**
     * Reläfunktion för att skapa en total summering för alla månader. Kallas endast vid fragmentets
     * skapande samt om clear history kallats.
     * @param rowIdentifiers lista med rader som ska populeras med data
     */

    private void loadMonthSummaries(ArrayList<RowInfo> rowIdentifiers) {
        // Custom Async Task för att ladda in data, definierad nedan.
        LoadMonthSummaryAsyncTask asyncTask = new LoadMonthSummaryAsyncTask();
        asyncTask.execute(rowIdentifiers.toArray(new RowInfo[rowIdentifiers.size()]));
    }

    /**
     * Kallas varje gång som Async Tasken har gjort färdigt en rad så att uppdateringen blir
     * progressiv och inte blockerar UI tråden när alla rader ska uppdateras samtidigt.
     * @param update en uppdatering för en rad
     */

    private void addMonthTotalsToTableLayout(RowInfo update) {
        // Här hämtas radens ID ut för att sätta uppdateringen till rätt rad!
        TableRow row = mTableLayout.findViewById(update.getRowId());
        final Context context = getContext();

        // Gå igenom alla totaler och visa dem i tabellen.
        /*
        Index   Innehåll
        0       Total
        1       Expenses
        2       Bills
        3       Loans
         */
        for (int index = 0; index < 4; index++) {
            TextView total = new TextView(context);
            setChildLayoutParams(total);
            total.setGravity(Gravity.CENTER);
            total.setText(String.valueOf(update.getTotal(index)));

            row.addView(total);
        }
    }

    /**
     * En custom implementation av Async Task för att ladda in statistik i TableLayoutens rader.
     */

    private class LoadMonthSummaryAsyncTask extends AsyncTask<RowInfo, RowInfo, String> {

        /* Definitioner för content provider interaktion */
        private final String[] expense_projection = {
                ExpenseEntry.COLUMN_NAME_AMOUNT
        };
        private final String one_time_expense_selection =
                ExpenseEntry.COLUMN_NAME_TIMESTAMP + ">=? AND " +
                ExpenseEntry.COLUMN_NAME_TIMESTAMP + "<=? AND " +
                ExpenseEntry.COLUMN_NAME_RECURRING + "=?";
        private final String recurring_expense_selection =
                ExpenseEntry.COLUMN_NAME_RECURRING + "=?";

        private final String[] loan_projection = {
                LoanEntry.COLUMN_NAME_AMOUNT,
                LoanEntry.COLUMN_NAME_INTEREST_RATE,
                LoanEntry.COLUMN_NAME_AMORTIZATION_AMOUNT
        };
        private final String[] record_projection = {
                RecordEntry.COLUMN_NAME_TYPE,
                RecordEntry.COLUMN_NAME_AMOUNT,
        };
        private final String record_selection =
                RecordEntry.COLUMN_NAME_TIMESTAMP + ">=? AND " +
                RecordEntry.COLUMN_NAME_TIMESTAMP + "<=?";
        /* Definitioner för content provider interaktion */

        /**
         * Går igenom alla parametrar som skickats till denn Async Task och skapar rad-data för dem.
         * @param params radinfo, datum, id och totaler för just den raden och datum
         * @return en beskrivande sträng som berättar om taskens arbete gick bra eller ej
         */

        @Override
        protected String doInBackground(RowInfo... params) {
            int totalTotal, expenseTotal, billTotal, loanTotal;
            Calendar cal = Calendar.getInstance();
            long start, end;

            // För varje radinfo...
            for (RowInfo row : params) {
                // Återställ alla totaler innan vi börjar.
                expenseTotal = 0;
                billTotal = 0;
                loanTotal = 0;
                // Vilket datum gäller det?
                cal.setTimeInMillis(row.getRowDate());

                /*
                Sätt start/end datum för att få med ALLA uppdateringar denna månaden.
                 */
                start = TimeTracking.getStartOfMonth(cal).getTimeInMillis();
                end = TimeTracking.getEndOfMonth(cal).getTimeInMillis();

                /* Engångsutgifter */
                Cursor oneTimeExpenses = getActivity().getContentResolver().query(
                        ExpenseControlContentProvider.EXPENSE_URI,
                        expense_projection,
                        one_time_expense_selection,
                        new String[] { String.valueOf(start), String.valueOf(end), "0" },
                        null
                );

                for (oneTimeExpenses.moveToFirst();
                     !oneTimeExpenses.isAfterLast();
                     oneTimeExpenses.moveToNext()) {
                    expenseTotal += oneTimeExpenses.getInt(0);
                }
                oneTimeExpenses.close();
                /* Engångsutgifter */

                /*
                Varför hantera den nuvarande månaden annorlunda? Det kan vara så att den nuvarande
                månaden har annorlunde status för lån och/eller räkningar är vad tidigare månader
                har haft.
                 */
                if (TimeTracking.isThisMonth(cal)) {
                    // Hämta nuvarande räkningar
                    Cursor recurringExpenses = getActivity().getContentResolver().query(
                            ExpenseControlContentProvider.EXPENSE_URI,
                            expense_projection,
                            recurring_expense_selection,
                            new String[] { "1" },
                            null
                    );

                    for (recurringExpenses.moveToFirst();
                         !recurringExpenses.isAfterLast();
                         recurringExpenses.moveToNext()) {
                        billTotal += recurringExpenses.getInt(0);
                    }
                    recurringExpenses.close();

                    // Hämta nuvarande lån.
                    Cursor loans = getActivity().getContentResolver().query(
                            ExpenseControlContentProvider.LOAN_URI,
                            loan_projection,
                            null,
                            null,
                            null
                    );

                    for (loans.moveToFirst();
                         !loans.isAfterLast();
                         loans.moveToNext()) {
                        int amount = loans.getInt(loans.getColumnIndex(LoanEntry.COLUMN_NAME_AMOUNT));
                        // Konvertera från procent till decimalt
                        double interest = loans.getDouble(
                                loans.getColumnIndex(LoanEntry.COLUMN_NAME_INTEREST_RATE)) * 0.01;
                        int amortization = loans.getInt(
                                loans.getColumnIndex(LoanEntry.COLUMN_NAME_AMORTIZATION_AMOUNT));
                        // Eftersom att vi endast är intresserade av denna månadens kostnader, dela
                        // med 12.
                        loanTotal += (int) (amount * interest / 12) + amortization;
                    }
                    loans.close();

                    // Hämta även specifikt lånbetalningar för den nuvarande månaden.
                    Cursor records = getActivity().getContentResolver().query(
                            ExpenseControlContentProvider.RECORD_URI,
                            record_projection,
                            record_selection,
                            new String[] { String.valueOf(start), String.valueOf(end) },
                            null
                    );

                    for (records.moveToFirst();
                         !records.isAfterLast();
                         records.moveToNext()) {
                        String type = records.getString(records.getColumnIndex(RecordEntry.COLUMN_NAME_TYPE));
                        int amount = records.getInt(records.getColumnIndex(RecordEntry.COLUMN_NAME_AMOUNT));

                        switch (type) {
                            case RecordEntry.TYPE_PARAM_LOAN_PAYMENT:
                                loanTotal += amount;
                                break;
                            default:
                                Log.d(TAG, "Unable to identify record type");
                                break;
                        }
                    }
                    records.close();

                // Om inte nuvarande månad, hämta alla records istället.
                } else {
                    Cursor records = getActivity().getContentResolver().query(
                            ExpenseControlContentProvider.RECORD_URI,
                            record_projection,
                            record_selection,
                            new String[] { String.valueOf(start), String.valueOf(end) },
                            null
                    );

                    for (records.moveToFirst();
                         !records.isAfterLast();
                         records.moveToNext()) {
                        String type = records.getString(records.getColumnIndex(RecordEntry.COLUMN_NAME_TYPE));
                        int amount = records.getInt(records.getColumnIndex(RecordEntry.COLUMN_NAME_AMOUNT));

                        // Kontrollera vilken typ av record det är för att uppdatera rätt totalsumma.
                        switch (type) {
                            case RecordEntry.TYPE_PARAM_RECURRING_EXPENSE:
                                billTotal += amount;
                                break;
                            case RecordEntry.TYPE_PARAM_LOAN:
                                loanTotal += amount;
                                break;
                            case RecordEntry.TYPE_PARAM_LOAN_PAYMENT:
                                loanTotal += amount;
                                break;
                            default:
                                Log.d(TAG, "Unable to identify record type");
                                break;
                        }
                    }
                    records.close();
                }

                totalTotal = expenseTotal + billTotal + loanTotal;
                /*
                KOM IHÅG! \/
                Index   Innehåll
                0       Total
                1       Expenses
                2       Bills
                3       Loans
                 */
                row.addToTotals(totalTotal, expenseTotal, billTotal, loanTotal);
                // Publicera progress för att uppdatera raderna i steg och inte allt på en gång.
                publishProgress(row);
            }

            return "Done!";
        }

        /**
         * Kallas när publishProgress kallats.
         * @param updates uppdateringar att hantera, i detta fall så kommer detta endast vara ett
         *                objekt vid varje kall
         */

        @Override
        protected void onProgressUpdate(RowInfo... updates) {
            // Kalla på för att uppdatera den associerade raden.
            addMonthTotalsToTableLayout(updates[0]);
        }
    }
}
