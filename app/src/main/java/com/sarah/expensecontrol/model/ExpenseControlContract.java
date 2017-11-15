package com.sarah.expensecontrol.model;

import android.provider.BaseColumns;

/**
 * Denna klass definierar "kontraktet" för databasen. Detta ska göra det enkelt att utföra förändringar
 * i tabellerna eftersom att de kommer att eskalera ut i hela applikationen. Fälten nedan för de olika
 * tabellerna ska användas var man än ska utföra en query/insert/delete/update för att säkerställa
 * att man inte queryar mot kolumner som inte existerar. BaseColumns implementeras för att få _ID-
 * fältet gratis, detta används av de flesta adapters för att kunna korrekt referera till klickade
 * list-items.
 */

public final class ExpenseControlContract {

    private ExpenseControlContract() {}

    public static class ExpenseEntry implements BaseColumns {

        // SQL-tabell och kolumner
        public static final String TABLE_NAME = "expenses";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_AMOUNT = "amount";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_RECURRING = "recurring";
        public static final String COLUMN_NAME_CATEGORY = "category";
        public static final String COLUMN_NAME_PICTURE_URI = "picture_uri";

        // MIME typer
        public static final String MIME_TYPE = "vnd.android.cursor.dir/" +
                "vnd.com.sarah.ExpenseControlContentProvider." + TABLE_NAME;
        public static final String MIME_TYPE_ID = "vnd.android.cursor.item/" +
                "vnd.com.sarah.ExpenseControlContentProvider." + TABLE_NAME;
    }

    public static class LoanEntry implements BaseColumns {
        public static final String TABLE_NAME = "loans";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_AMOUNT = "amount";
        public static final String COLUMN_NAME_INTEREST_RATE = "interest";
        public static final String COLUMN_NAME_AMORTIZATION_AMOUNT = "amortization";

        public static final String MIME_TYPE = "vnd.android.cursor.dir/" +
                "vnd.com.sarah.ExpenseControlContentProvider." + TABLE_NAME;
        public static final String MIME_TYPE_ID = "vnd.android.cursor.item/" +
                "vnd.com.sarah.ExpenseControlContentProvider." + TABLE_NAME;
    }

    public static class CategoryEntry implements BaseColumns {
        public static final String TABLE_NAME = "categories";
        public static final String COLUMN_NAME_NAME = "name";

        public static final String MIME_TYPE = "vnd.android.cursor.dir/" +
                "vnd.com.sarah.ExpenseControlContentProvider." + TABLE_NAME;
        public static final String MIME_TYPE_ID = "vnd.android.cursor.item/" +
                "vnd.com.sarah.ExpenseControlContentProvider." + TABLE_NAME;
    }

    public abstract static class RecordEntry implements BaseColumns {
        public static final String TABLE_NAME = "records";
        public static final String COLUMN_NAME_TYPE = "type";
        public static final String COLUMN_NAME_AMOUNT = "amount";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";

        // Records har tre olika typer av inlägg, därför är dessa definierade här så att inget
        // misstag görs när de ska kallas upp.
        public static final String TYPE_PARAM_RECURRING_EXPENSE = "recurring_expense";
        public static final String TYPE_PARAM_LOAN = "loan";
        public static final String TYPE_PARAM_LOAN_PAYMENT = "loan_payment";

        public static final String MIME_TYPE = "vnd.android.cursor.dir/" +
                "vnd.com.sarah.ExpenseControlContentProvider." + TABLE_NAME;
        public static final String MIME_TYPE_ID = "vnd.android.cursor.item/" +
                "vnd.com.sarah.ExpenseControlContentProvider." + TABLE_NAME;
    }

}
