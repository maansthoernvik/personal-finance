package com.sarah.expensecontrol.model;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import static com.sarah.expensecontrol.model.ExpenseControlContract.*;

/**
 * ExpenseControlContentProvider sköter kommunikationen med den underliggande SQLite-databasen.
 * Och ser till att datan hålls till denna och ingen annan applikation (se manifestet för export
 * deklarationen).
 */

public class ExpenseControlContentProvider extends ContentProvider {

    @SuppressWarnings("unused")
    private static final String TAG = "ContentProvider";

    private ExpenseControlDbHelper mDbHelper;   // Wrapper-klass för interaktion med databasen.
    private SQLiteDatabase mDb;                 // SQLite-databasen som interageras med.
    private boolean mLoaded = false;            // Indikerar om databasen är öppnad för transaktioner.

    private static final String PROVIDER_NAME =
            "com.sarah.expensecontrol.model.ExpenseControlContentProvider";

    /* URIs för olika typer av data */
    public static final Uri EXPENSE_URI = Uri.parse(
            "content://" + PROVIDER_NAME + "/" + ExpenseEntry.TABLE_NAME
    );
    public static final Uri LOAN_URI = Uri.parse(
            "content://" + PROVIDER_NAME + "/" + LoanEntry.TABLE_NAME
    );
    public static final Uri CATEGORY_URI = Uri.parse(
            "content://" + PROVIDER_NAME + "/" + CategoryEntry.TABLE_NAME
    );
    public static final Uri RECORD_URI = Uri.parse(
            "content://" + PROVIDER_NAME + "/" + RecordEntry.TABLE_NAME
    );
    /* URIs för olika typer av data */

    /* URI associerade ID-nummer som används för att veta vilken typ av data som ska returneras */
    private static final int EXPENSES       = 1;
    private static final int EXPENSES_ID    = 2;
    private static final int LOANS          = 3;
    private static final int LOANS_ID       = 4;
    private static final int CATEGORIES     = 5;
    private static final int CATEGORIES_ID  = 6;
    private static final int RECORDS        = 7;
    private static final int RECORDS_ID     = 8;
    /* URI associerade ID-nummer som används för att veta vilken typ av data som ska returneras */

    /*
    URI-matchningen sker via detta objekt. Ett inkommande URI matchas mot detta och det associerade
    ID-numret returneras. Dessa IDn används i switch statements där varje case returnerar rätt typ av
    data.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(
                // Providern   DB-tabellen                      ID-numret som länkas till
                PROVIDER_NAME, ExpenseEntry.TABLE_NAME,         EXPENSES
        );
        sUriMatcher.addURI(
                PROVIDER_NAME, ExpenseEntry.TABLE_NAME + "/#",  EXPENSES_ID
        );
        sUriMatcher.addURI(
                PROVIDER_NAME, LoanEntry.TABLE_NAME,            LOANS
        );
        sUriMatcher.addURI(
                PROVIDER_NAME, LoanEntry.TABLE_NAME + "/#",     LOANS_ID
        );
        sUriMatcher.addURI(
                PROVIDER_NAME, CategoryEntry.TABLE_NAME,        CATEGORIES
        );
        sUriMatcher.addURI(
                PROVIDER_NAME, CategoryEntry.TABLE_NAME + "/#", CATEGORIES_ID
        );
        sUriMatcher.addURI(
                PROVIDER_NAME, RecordEntry.TABLE_NAME,          RECORDS
        );
        sUriMatcher.addURI(
                PROVIDER_NAME, RecordEntry.TABLE_NAME + "/#",   RECORDS_ID
        );
    }

    /**
     * onCreate för content providern, skapar DB helpern men öppnar inte anslutningen till databasen
     * förrän den första queryn/inserten/uppdateringen kommer.
     * @return true
     */

    @Override
    public boolean onCreate() {
        mDbHelper = new ExpenseControlDbHelper(getContext());
        return true;
    }

    /**
     * Begär URI-typ av data från Content Providern.
     * @param uri, typ av data som har begärts
     * @param projection, vilka fält som ska returneras
     * @param selection, vilka rows som ska returneras
     * @param selectionArgs, i och med att preparedstatements används för att undvika SQL injection-
     *                       attacker så måste argument till selection definieras
     * @param sortOrder, hur resultatet ska sorteras
     * @return en Cursor med data relaterat till queryns URI
     * @throws IllegalArgumentException om inget URI kunde matchas
     */

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri,
                        @Nullable String[] projection,
                        @Nullable String selection,
                        @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {
        checkLoaded();  // Kontrollera om databasen är öppnad för en ny transaktion.

        // Hjälpobjekt för att bygga queries.
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        // Cursor som ska hålla resultatet.
        Cursor cursor;

        // Matchar på det inkommande URIt
        switch (sUriMatcher.match(uri)) {
            /*
            Beroende på den inkommande URIt som matchats så kommer det nummer som associerats med det
            i URI-matcharens statiska initializer ovan att returneras vid matchning.
             */
            case EXPENSES:
                // Sätt rätt table att querya.
                queryBuilder.setTables(ExpenseEntry.TABLE_NAME);
                // Default sortOrder är alltid på namn, om sortOrder skulle vara tom.
                if (TextUtils.isEmpty(sortOrder)) sortOrder = ExpenseEntry.COLUMN_NAME_NAME;
                cursor = queryBuilder.query(
                        mDb,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                /*
                Det är viktigt att definiera ett notifikationsURI för den skapade cursorn. Detta är
                vad som gör att listorna i de olika fragmentvyerna uppdateras direkt vid ändringar.
                Om en ändring gjorts på ett visst URI så kommer cursorn att veta om det genom detta
                och kan då trigga sin loader att uppdatera datan.
                 */
                cursor.setNotificationUri(getContext().getContentResolver(), EXPENSE_URI);
                return cursor;
            case LOANS:
                queryBuilder.setTables(LoanEntry.TABLE_NAME);
                if (TextUtils.isEmpty(sortOrder)) sortOrder = LoanEntry.COLUMN_NAME_NAME;
                cursor = queryBuilder.query(
                        mDb,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                cursor.setNotificationUri(getContext().getContentResolver(), LOAN_URI);
                return cursor;
            case CATEGORIES:
                queryBuilder.setTables(CategoryEntry.TABLE_NAME);
                if (TextUtils.isEmpty(sortOrder)) sortOrder = CategoryEntry.COLUMN_NAME_NAME;
                cursor = queryBuilder.query(
                        mDb,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                cursor.setNotificationUri(getContext().getContentResolver(), CATEGORY_URI);
                return cursor;
            case RECORDS:
                queryBuilder.setTables(RecordEntry.TABLE_NAME);
                if (TextUtils.isEmpty(sortOrder)) sortOrder = RecordEntry.COLUMN_NAME_TIMESTAMP + " ASC";
                cursor = queryBuilder.query(
                        mDb,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                cursor.setNotificationUri(getContext().getContentResolver(), RECORD_URI);
                return cursor;
            default:
                throw new IllegalArgumentException("<<URI mismatch>> URI provided: " + uri.toString() +
                        " does not match any existing URI.");
        }
    }

    /**
     * Genomför en insättning av data på det begärda URIt.
     * @param uri typ av data som ska sättas in
     * @param contentValues data som ska sättas in
     * @return URI för den skapade datan, i from av rad ID
     * @throws IllegalArgumentException om inget URI kunde matchas
     */

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        checkLoaded();

        long rowId;

        switch (sUriMatcher.match(uri)) {
            case EXPENSES:
                rowId = mDb.insert(ExpenseEntry.TABLE_NAME, null, contentValues);
                break;
            case LOANS:
                rowId = mDb.insert(LoanEntry.TABLE_NAME, null, contentValues);
                break;
            case CATEGORIES:
                rowId = mDb.insert(CategoryEntry.TABLE_NAME, null, contentValues);
                break;
            case RECORDS:
                rowId = mDb.insert(RecordEntry.TABLE_NAME, null, contentValues);
                break;
            default:
                throw new IllegalArgumentException("<<URI mismatch>> URI provided: " + uri.toString() +
                        " does not match any existing URI.");
        }

        // rowId kommer att vara -1 om ett fel uppstått.
        if (rowId >= 0) {
            // Skapa URIt för den skapade datan (raden i databasen)
            Uri changeUri = ContentUris.withAppendedId(uri, rowId);

            // Här är det som länkar setNotificationUri till ändringar. När ny data registrerats
            // så nofifieras alla cursors på samma URI om denna ändring och kan därmed uppdatera sin
            // data.
            getContext().getContentResolver().notifyChange(uri, null);

            return changeUri;
        }
        // null vid fel, ingen ny data har skapats.
        return null;
    }

    /**
     * Gör en borttagning av data som matchar where och whereArgs
     * @param uri typ av data som ska tas bort
     * @param where där datan har id/namn/summa...
     * @param whereArgs preparedStatments används, argumenten länkas till where där '?' ersätts med
     *                  argumentdata
     * @return int, mängd rader som tagits bort
     * @throws IllegalArgumentException om inget URI kunde matchas
     */

    @Override
    public int delete(@NonNull Uri uri,
                      @Nullable String where,
                      @Nullable String[] whereArgs) {
        checkLoaded();

        int rows;

        switch (sUriMatcher.match(uri)) {
            case EXPENSES:
                rows = mDb.delete(
                        ExpenseEntry.TABLE_NAME,
                        where,
                        whereArgs
                );
                break;
            case LOANS:
                rows = mDb.delete(
                        LoanEntry.TABLE_NAME,
                        where,
                        whereArgs
                );
                break;
            case CATEGORIES:
                rows = mDb.delete(
                        CategoryEntry.TABLE_NAME,
                        where,
                        whereArgs
                );
                break;
            case RECORDS:
                rows = mDb.delete(
                        RecordEntry.TABLE_NAME,
                        where,
                        whereArgs
                );
                break;
            default:
                throw new IllegalArgumentException("<<URI mismatch>> URI provided: " + uri.toString() +
                        " does not match any existing URI.");
        }

        // Notifiera bara ändringen om någon rad blivit påverkad. Togs ingenting bort så är det
        // onödigt att be om en uppdatering.
        if (rows > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rows;
    }

    /**
     * Uppdaterar data associerat med URIt.
     * @param uri typ av data som ska uppdateras
     * @param values värden efter uppdateringen
     * @param selection uppdatera värden där id/namn/summa är...
     * @param selectionArgs preparedStatements används så dessa argument kompletterar selectionen
     *                      ovan.
     * @return antal rader som påverkats
     * @throws IllegalArgumentException om inget URI kunde matchas
     */

    @Override
    public int update(@NonNull Uri uri,
                      @Nullable ContentValues values,
                      @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        checkLoaded();

        int rows;

        switch (sUriMatcher.match(uri)) {
            case EXPENSES:
                rows = mDb.update(
                        ExpenseEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs
                );
                break;
            case LOANS:
                rows = mDb.update(
                        LoanEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs
                );
                break;
            case CATEGORIES:
                rows = mDb.update(
                        CategoryEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs
                );
                break;
            case RECORDS:
                rows = mDb.update(
                        RecordEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs
                );
                break;
            default:
                throw new IllegalArgumentException("<<URI mismatch>> URI provided: " + uri.toString() +
                        " does not match any existing URI.");
        }

        // Som tidigare, notifiera bara om en förändring om fler än 0 rader påverkats.
        if (rows > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rows;
    }

    /**
     * Returnerar MIME-typen hos data associerat till parameter-URIt.
     * @param uri att hämta information om
     * @return en sträng som representerar MIME-typen för det URI som begärts
     * @throws IllegalArgumentException om inget URI kunde matchas
     */

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {

        switch (sUriMatcher.match(uri)) {
            case EXPENSES:
                return ExpenseEntry.MIME_TYPE;
            case EXPENSES_ID:
                return ExpenseEntry.MIME_TYPE_ID;
            case LOANS:
                return LoanEntry.MIME_TYPE;
            case LOANS_ID:
                return LoanEntry.MIME_TYPE_ID;
            case CATEGORIES:
                return CategoryEntry.MIME_TYPE;
            case CATEGORIES_ID:
                return CategoryEntry.MIME_TYPE_ID;
            case RECORDS:
                return RecordEntry.MIME_TYPE;
            case RECORDS_ID:
                return RecordEntry.MIME_TYPE_ID;
            default:
                throw new IllegalArgumentException("<<URI mismatch>> URI provided: " + uri.toString() +
                        " does not match any existing URI.");
        }
    }

    /**
     * Kontrollerar om databasen har öppnats upp. Om inte, öppnar.
     * @return true om databasen öppnats
     */

    public boolean checkLoaded() {

        if (!mLoaded) {
            mDb = mDbHelper.getWritableDatabase();
            mLoaded = true;
        }

        return mLoaded;
    }
}
