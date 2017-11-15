package com.sarah.expensecontrol.loading;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sarah.expensecontrol.Loan;
import com.sarah.expensecontrol.R;
import com.sarah.expensecontrol.Record;
import com.sarah.expensecontrol.util.TimeTracking;
import com.sarah.expensecontrol.home.HomeActivity;
import com.sarah.expensecontrol.model.ExpenseControlContentProvider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static com.sarah.expensecontrol.util.LoaderManagerIDs.*;
import static com.sarah.expensecontrol.model.ExpenseControlContract.*;

/**
 * Denna aktivitet kommer att startas först av alla då applikationen startas. Den kommer att
 * kontrollera senaste uppstarten av applikationen och se om några månadsskiften skett, om så
 * så kommer en procedur dra igång som genererar statistisk data för de gångna månaderna.
 *
 * NOTERA! Se även onResume i SingleFragmentActivity för hantering av inladdning av statistik.
 */

public class LoadingActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    @SuppressWarnings("unused")
    private static final String TAG = "LoadingActivity";

    /* Definitioner för content provider interaktion */
    private static final String[] EXPENSE_PROJECTION = {
            ExpenseEntry.COLUMN_NAME_AMOUNT
    };
    private static final String EXPENSE_SELECTION = ExpenseEntry.COLUMN_NAME_RECURRING + "=?";
    private static final String[] RECURRING_EXPENSE_SELECTION_ARG = { "1" };

    private static final String[] LOAN_PROJECTION = {
            LoanEntry._ID,
            LoanEntry.COLUMN_NAME_AMOUNT,
            LoanEntry.COLUMN_NAME_INTEREST_RATE,
            LoanEntry.COLUMN_NAME_AMORTIZATION_AMOUNT
    };
    /* Definitioner för content provider interaktion */

    private ArrayList<Record> mRecords;             // Lista med inlägg som ska skapas i databasen
    private int mMonthDiff;                         // Hur många månader som gått sedan senaste uppstart
    private long mLastAccess;                       // Senaste gången applikationen användes

    private boolean mExpensesLoaded, mLoansLoaded;  // Flaggor för vilka delar av datan som ska skapas
    {                                               // statistik för som hittills har laddat klart.
        mExpensesLoaded = false;                    // Laddningen är asynkron, därav flaggor
        mLoansLoaded = false;
    }

    private ProgressBar mProgressBar;               // Indikerar hur långt laddningen kommit
    private TextView mLoadingProgress;              // Text för laddningsprocessen

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        mProgressBar = (ProgressBar) findViewById(R.id.loading_progress_bar);
        mLoadingProgress = (TextView) findViewById(R.id.loading_current_progress_message);

        // Kontrollera om något månadsskifte hänt, och i så fall hur många.
        if ((mMonthDiff = getMonthDiffSinceLastLogin(getLastAccessDate())) == 0) {
            mProgressBar.setProgress(mProgressBar.getMax());    // Om inget, indikera 100 % klart
            mLoadingProgress.setText(getString(R.string.loading_completed));              // och visa ett meddelande
            goToHome();                                         // Starta hemaktiviteten
        } else {
            Log.d(TAG, "Month diff was greater than 0, initiating record sequencing.");
            mRecords = new ArrayList<>();                       // Records kommer behöva skapas
            mProgressBar.setMax(mMonthDiff);                    // Sätt progressbar till antal månadskiften

            // Dra igång loaders för data som måste hämtas. Notera att endast recurring expenses (räkningar)
            // och lån behöver genereras data för.
            getSupportLoaderManager().initLoader(RECURRING_EXPENSES_LOADER_ID, null, this);
            getSupportLoaderManager().initLoader(LOANS_LOADER_ID, null, this);
        }
    }

    /**
     * Sparar nuvarande tid som lastAccess (dvs. senaste gången som applikationen användes) och
     * starta hemaktiviteten.
     */

    private void goToHome() {
        // Spara lastAccess.
        final String lastAccessDate = "lastAccess";
        SharedPreferences sharedPrefs = getSharedPreferences(
                getString(R.string.app_name),
                Context.MODE_PRIVATE
        );
        sharedPrefs.edit().putLong(lastAccessDate, new Date().getTime()).apply();
        Log.d("STATISTICS", "Put current date into shared prefs: " + new Date().toString());
        // Spara lastAccess.

        /*
        Runnable används för att på ett säkert sätt fördröja starten av den nya aktiviteten. Det är
        inte en bra idé att stanna huvudtråden.
         */
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(), HomeActivity.class);

                startActivity(intent);
                finish();
            }
        };
        // En handler används för att starta den skapade runnablen.
        Handler h = new Handler();
        // Med ett 750ms delay.
        h.postDelayed(r, 750);
    }

    /**
     * Returnerar antalet månader mellan senaste månaden och idag.
     * @param lastAccess, senaste användningen i millisekunder sedan epoch
     * @return int, antal månader mellan senaste användningen och idag
     */

    private int getMonthDiffSinceLastLogin(long lastAccess) {
        int monthDiff = 0;
        /*
        Både dagens och senaste användningens datum återställs till början av respektive månad.
        Detta eftersom att månadsskiften är det enda vi är intresserade av. Vi vill inte starta
        en inmatningssekvens i onödan.
         */
        Calendar lastAccessDate = Calendar.getInstance();
        lastAccessDate.setTimeInMillis(lastAccess);

        final Calendar currentDate = TimeTracking.getStartOfMonth(Calendar.getInstance());

        // Kontrollera om lastAccess var före dagens datum även efter datumåterställning.
        while (lastAccessDate.before(currentDate)) {
            monthDiff++;
            TimeTracking.addOneMonth(lastAccessDate);
        }

        Log.d(TAG, "Monthdiff was: " + monthDiff);
        return monthDiff;
    }

    /**
     * Hämtar senaste gången applikationen användes från shared preferences och returnerar resultatet.
     * @return long, senaste accessdatumet för applikationen
     */

    public long getLastAccessDate() {
        final String lastAccessDate = "lastAccess";
        // Öppnas med privat åtkomst.
        SharedPreferences sharedPreferences = this.getSharedPreferences(
                getString(R.string.app_name),
                MODE_PRIVATE
        );
        // Hämta shared preferences lastAccess, om inget finns där, ta dagens datum. Det är viktigt
        // att hämta dagens datum som default, eftersom att om det är första gången applikationen används
        // så ska inga records skapas utan den sekvensen ska skippas.
        long lastAccess = sharedPreferences.getLong(lastAccessDate, new Date().getTime());
        //long lastAccess = 1500000000000L; // INFO debug long date = july 2017.
        Log.d(TAG, new Date(lastAccess).toString() + " was the last access date.");

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(lastAccess);
        TimeTracking.getStartOfMonth(calendar);             // Se getMonthDiff... ovan, kalendern
        return mLastAccess = calendar.getTimeInMillis();    // återställs till början av månaden
    }                                                       // för att endast kontrollera månadsskiften.

    /**
     * Ersätter helt laddningsmeddelandet.
     * @param message, att ersätta nuvarande text med för laddningsmeddelandet
     */

    private void overrideLoadingProgressMessage(String message) {
        mLoadingProgress.setText(message);
    }

    /**
     * Stegar fram progress baren genom att kontrollera nuvarande antal steg och lägger till step
     * antal steg i meddelandet.
     * @param step, antal steg progress meddelandet med
     */

    private void incrementRecordProgressMessage(int step) {
        mLoadingProgress.setText(
                "Creating record " + (mProgressBar.getProgress() + step) + " / " + mMonthDiff
        );
    }

    /**
     * Stegar fram progress baren med steps antal steg.
     * @param steps, antal steg att öka progress baren med
     */

    private void incrementProgressBar(int steps) {
        mProgressBar.setProgress(mProgressBar.getProgress() + steps);
    }

    /**
     * Skapar Records av den expense data som finns i cursorn. Eftersom att bara räkningar skall skapas
     * records för så kommer denna cursorn bara att innehålla räknings-expenses (se selection för
     * expenses i klassfälten).
     * @param data, cursor data att omvandla till records
     */

    private void convertExpensesToRecords(Cursor data) {
        int total = 0;
        Calendar calendar = Calendar.getInstance();
        // Kalendern initieras med lastAccess då detta datumet skall skapas records för FÖRST. Denna
        // kalender ökas sedan med en månad i taget tills monthDiff antal records har skapats.
        calendar.setTimeInMillis(mLastAccess);

        // Hämta expense-totalen för den aktuella månaden. Alla räkningars summa tas ut och räknas ihop.
        for (data.moveToFirst(); !data.isAfterLast(); data.moveToNext()) {
            total += data.getInt(data.getColumnIndex(ExpenseEntry.COLUMN_NAME_AMOUNT));
        }

        // Skapa monthDiff antal records. Öka kalendern för varje loop-traversal för att inte duplicera
        // records för samma månad utan endast ett per månad.
        for (int index = 0; index < mMonthDiff; index++) {
            mRecords.add(new Record(
                    RecordEntry.TYPE_PARAM_RECURRING_EXPENSE,
                    total,
                    calendar.getTimeInMillis()
                    )
            );
            TimeTracking.addOneMonth(calendar);
        }

        mExpensesLoaded = true;     // Sätt expenses loaded flaggan för att indikera att alla expenses
                                    // laddats klart, se vidare i onLoadFinished om varför.
    }

    /**
     * Omvandlar såsom ovan cursor data till records, denna tar dock loans och inte expenses.
     * @param data, cursor data att omvandla till records
     */

    private void convertLoansToRecords(Cursor data) {
        ArrayList<Loan> loans = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(mLastAccess);

        // Hämta först ut alla aktiva lån.
        for (data.moveToFirst(); !data.isAfterLast(); data.moveToNext()) {
            loans.add(new Loan(
                    data.getLong(data.getColumnIndex(LoanEntry._ID)),
                    data.getInt(data.getColumnIndex(LoanEntry.COLUMN_NAME_AMOUNT)),
                    // Sätt till * 0.01 för enklare uträkning senare och för att slippa omvandlingen då.
                    // Från procentbaserad till decimal alltså.
                    data.getDouble(data.getColumnIndex(LoanEntry.COLUMN_NAME_INTEREST_RATE)) * 0.01,
                    data.getInt(data.getColumnIndex(LoanEntry.COLUMN_NAME_AMORTIZATION_AMOUNT))
                    )
            );
        }

        /*
        Skapa precis som tidigare monthDiff antal records:
        1. Extrahera ett lån.
        2. Kalkylera kostnaden per månad för lånet, ränta och amortering.
        3. Dra amorteringskostnaden för lånet från lånets totala summa. Detta för att få korrekt räntesumme
           vid nästa iteration av loopen.
        4. Skapa ett record när alla lån gåtts igenom och gå vidare till nästa månad.
         */
        for (int index = 0; index < mMonthDiff; index++) {
            int monthTotal = 0;

            for (Loan loan : loans) {
                int interestPayment = (int) (loan.getAmount() * loan.getInterest()) / 12;

                // Så länge summan för lånet är större än amorteringsmängden, gå vidare.
                if (loan.getAmount() > loan.getAmortization()) {
                    monthTotal += interestPayment + loan.getAmortization();
                    // Dra amorteringen från summan.
                    loan.setAmount(loan.getAmount() - loan.getAmortization());
                } else {
                    // Skulle amorteringsmängden vara större, lägg helt enkelt till summan istället.
                    monthTotal += interestPayment + loan.getAmount();
                    // Sätt i detta fallet både summan och amorteringen till 0.
                    loan.setAmount(0);
                    loan.setAmortization(0);
                }
            }

            mRecords.add(new Record(
                    RecordEntry.TYPE_PARAM_LOAN,
                    monthTotal,
                    calendar.getTimeInMillis()
                    )
            );

            TimeTracking.addOneMonth(calendar);
        }

        /*
        Nu när lånen laddats klart och records skapats så måste lånen även uppdateras i databasen.
        Detta eftersom att flera månader gått och amorteringssumman då dragits monthDiff antal gånger.
        Görs inte detta så kommer fortfarande originalsumman för lånet att visas när appen startas och
        fel kostnad kommer att visas för det. De enda två fält som kan ha uppdaterats är summan
        och amorteringssumman.
         */
        ContentValues values;
        for (Loan loan : loans) {
            values = new ContentValues();
            // Sätt fälten som ev. uppdaterats.
            values.put(LoanEntry.COLUMN_NAME_AMOUNT, loan.getAmount());
            values.put(LoanEntry.COLUMN_NAME_AMORTIZATION_AMOUNT, loan.getAmortization());

            // Uppdatera lånet i databasen.
            getContentResolver().update(
                    ExpenseControlContentProvider.LOAN_URI,
                    values,
                    LoanEntry._ID + "=?",
                    new String[] { String.valueOf(loan.getId()) }   // Se till att uppdatera rätt id.
            );
        }

        mLoansLoaded = true;    // Lånen är färdigladdade, flagga för det.
    }

    /**
     * Kontrollerar flaggorna för om lån och utgifter laddats klart, alltså om records skapats för
     * dessa.
     * @return true, om både lån och utgifter laddats klart
     */

    private boolean isLoadingCompleted() {
        return mExpensesLoaded && mLoansLoaded;
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
                Log.d(TAG, "Creating loader for Recurring expenses");
                // Hämta all data som associeras med räkningar
                return new CursorLoader(
                        getApplicationContext(),
                        ExpenseControlContentProvider.EXPENSE_URI,
                        EXPENSE_PROJECTION,
                        EXPENSE_SELECTION,
                        RECURRING_EXPENSE_SELECTION_ARG,
                        null
                );
            case LOANS_LOADER_ID:
                Log.d(TAG, "Creating loader for loans");
                // Hämta all låndata.
                return new CursorLoader(
                        getApplicationContext(),
                        ExpenseControlContentProvider.LOAN_URI,
                        LOAN_PROJECTION,
                        null,
                        null,
                        null
                );
            default:
                Log.d(TAG, "No loader ID was matched.");
                break;
        }
        // null bör returneras om loader idt inte kan länkas till någon känd data.
        return null;
    }

    /**
     * Kallas när loadern hämtat sin associerade data.
     * @param loader, den färdiga loadern
     * @param data, laddad data
     */

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (!isLoadingCompleted()) {
            int id = loader.getId();
            Log.d(TAG, "Finished loading");

            // Kontrollera vilken loader som blev klar och skapa records för den data som kommit in.
            switch (id) {
                case RECURRING_EXPENSES_LOADER_ID:
                    convertExpensesToRecords(data);
                    overrideLoadingProgressMessage("Expenses prepared...");
                    break;
                case LOANS_LOADER_ID:
                    convertLoansToRecords(data);
                    overrideLoadingProgressMessage("Loans prepared...");
                    break;
            }

            // Denna kommer kallas två gånger, först när antingen lån eller räkningar laddat klart och
            // sedan när den andra blev klar. Notera att det inte går att förutse vilken av dem som blir
            // klar först då det sker asynkront.
            if (isLoadingCompleted()) {
                Log.d(TAG, "Loading completed, preparing records...");
                overrideLoadingProgressMessage("Starting record creation...");
                // Skapa en specialiserad async task som tar hand om det faktiska recordskapandet, detta
                // också för att kunna sköta det asynkront och inte låsa UI tråden.
                CreateRecordsAsyncTask asyncTask = new CreateRecordsAsyncTask();
                asyncTask.execute(mRecords.toArray(new Record[mRecords.size()]));
            }
        }
    }

    /**
     * ANVÄNDS EJ DÅ DATAN INTE KOMMER ATT ÅTERSTÄLLAS! AKTIVITETEN STÄNGS EFTER ATT RECORDS SKAPATS!
     * @param loader, loader som ska resettas
     */

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // EJ IMPLEMENTERAD
    }

    /**
     * En subklass från Async task som tar hand om att skapa input records, uppdatera den visuella
     * statusen och till sist kalla på metoden som avslutar laddningsaktiviteten.
     */

    private class CreateRecordsAsyncTask extends AsyncTask<Record, Integer, String> {

        /**
         * Sätter laddningsmeddelandet till 0/monthDiff.
         */

        @Override
        protected void onPreExecute() {
            incrementRecordProgressMessage(0);
        }

        /**
         * Skapar records för alla de Record-object som funktionen tar som argument. Skickar
         * även kontinuerligt uppdateringar till onProgressUpdate för att uppdatera laddningsmeddelandet.
         * @param records att skapa inlägg för
         * @return String, representerar success-fallet
         */

        @Override
        protected String doInBackground(Record... records) {
            ContentValues values = new ContentValues();
            Log.d(TAG, "Inserting new records...");

            for (Record record : records) {
                values.put(RecordEntry.COLUMN_NAME_TYPE, record.getType());
                values.put(RecordEntry.COLUMN_NAME_AMOUNT, record.getAmount());
                values.put(RecordEntry.COLUMN_NAME_TIMESTAMP, record.getTimestamp());

                getContentResolver().insert(
                        ExpenseControlContentProvider.RECORD_URI,
                        values
                );

                publishProgress(1);
            }

            return "Record insertion completed!";
        }

        /**
         * Uppdaterar laddningsmeddelandet.
         * @param integers, input för med hur många steg laddningsmeddelandet ska uppdateras med
         */

        @Override
        protected void onProgressUpdate(Integer... integers) {
            incrementProgressBar(integers[0]);
            incrementRecordProgressMessage(integers[0]);
        }

        /**
         * Anropas när hela processen är klar. goToHome() avslutar aktiviteten och visar Home istället.
         * @param result, resultatet av doInBackground().
         */

        @Override
        public void onPostExecute(String result) {
            Log.d(TAG, "Record creating sequence completed successfully");
            overrideLoadingProgressMessage(result);
            goToHome();
        }
    }
}
