package com.sarah.expensecontrol.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static com.sarah.expensecontrol.model.ExpenseControlContract.*;

/**
 * En hjälpklass för interagering med SQLiteDatabasen. Här definieras tabellerna med hjälp av
 * kontraktet samt olika typer av begränsningar på individuella kolumner.
 */

public class ExpenseControlDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "ExpenseControlDbHelper";

    private static final String CREATE_EXPENSES_TABLE = "CREATE TABLE IF NOT EXISTS " +
            ExpenseEntry.TABLE_NAME + " (" +
            ExpenseEntry._ID +                              " INTEGER PRIMARY KEY," +
            ExpenseEntry.COLUMN_NAME_NAME +                 " TEXT," +
            ExpenseEntry.COLUMN_NAME_AMOUNT +               " INTEGER NOT NULL," +
            ExpenseEntry.COLUMN_NAME_TIMESTAMP +            " INTEGER," +
            ExpenseEntry.COLUMN_NAME_RECURRING +            " BOOLEAN NOT NULL," +
            ExpenseEntry.COLUMN_NAME_CATEGORY +             " TEXT," +
            ExpenseEntry.COLUMN_NAME_PICTURE_URI +          " TEXT)";

    private static final String CREATE_LOAN_TABLE = "CREATE TABLE IF NOT EXISTS " +
            LoanEntry.TABLE_NAME + " (" +
            LoanEntry._ID +                                 " INTEGER PRIMARY KEY," +
            LoanEntry.COLUMN_NAME_NAME +                    " TEXT NOT NULL," +
            LoanEntry.COLUMN_NAME_AMOUNT +                  " INTEGER NOT NULL," +
            LoanEntry.COLUMN_NAME_INTEREST_RATE +           " REAL NOT NULL," +
            LoanEntry.COLUMN_NAME_AMORTIZATION_AMOUNT +     " INTEGER NOT NULL)";

    private static final String CREATE_CATEGORY_TABLE = "CREATE TABLE IF NOT EXISTS " +
            CategoryEntry.TABLE_NAME + " (" +
            CategoryEntry._ID +                             " INTEGER PRIMARY KEY," +
            CategoryEntry.COLUMN_NAME_NAME +                " TEXT UNIQUE NOT NULL)";

    private static final String CREATE_RECORD_TABLE = "CREATE TABLE IF NOT EXISTS " +
            RecordEntry.TABLE_NAME + " (" +
            RecordEntry._ID +                               " INTEGER PRIMARY KEY," +
            RecordEntry.COLUMN_NAME_TYPE +                  " TEXT NOT NULL," +
            RecordEntry.COLUMN_NAME_AMOUNT +                " INTEGER NOT NULL," +
            RecordEntry.COLUMN_NAME_TIMESTAMP +             " INTEGER NOT NULL)";

    private static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "ExpenseControl.db";

    /**
     * Standard constructor.
     * @param context för där db helpern skapats
     */

    public ExpenseControlDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * onCreate kallas BARA på om detta är första gången som databasen används. Den skapas alltså bara
     * första gången appen startas egentligen. Eller, såklart, om appen skulle avinstallerats och
     * installerats igen på nytt. Här skapas tabellerna där data ska sparas.
     * @param sqLiteDatabase att skapa
     */

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d(TAG, "onCreate() ExpenseControlDbHelper was called.");
        sqLiteDatabase.execSQL(CREATE_EXPENSES_TABLE);
        sqLiteDatabase.execSQL(CREATE_LOAN_TABLE);
        sqLiteDatabase.execSQL(CREATE_CATEGORY_TABLE);
        sqLiteDatabase.execSQL(CREATE_RECORD_TABLE);
    }

    /**
     * Uppdaterar databasen genom att slänga de gamla tabellerna och trigga en ny onCreate som skapar
     * dem på nytt med eventuella uppdateringar. Här har jag inte gjort så mycket. Ändringar i framtiden
     * skulle kräva att denna hook vidareutvecklades så att den inte bara slänger nuvarande data.s
     * @param sqLiteDatabase att uppdatera
     * @param oldVersion av databasen
     * @param newVersion av databasen
     */

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ExpenseEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + LoanEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + CategoryEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + RecordEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
