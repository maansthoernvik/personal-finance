package com.sarah.expensecontrol.expenses;

import android.database.Cursor;
import java.text.SimpleDateFormat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.TextView;

import java.util.Date;

import static com.sarah.expensecontrol.model.ExpenseControlContract.ExpenseEntry;

/**
 * Denna klass används som custom view binder åt expenses som måste visa datum. Eftersom att dessa
 * sparats som longs i databasen så visas bara ett långt nummer när de laddas upp standardmässigt.
 */

public class ExpenseCursorAdapterViewBinder implements SimpleCursorAdapter.ViewBinder {

    // Formatet för datum som visas i listorna för utgifter..
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yy/MM/dd");

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        int timestampIndex = cursor.getColumnIndex(ExpenseEntry.COLUMN_NAME_TIMESTAMP);

        // Om ViewValue som just nu sätts är timestamp så...
        if (timestampIndex == columnIndex) {

            // Formattera datumet och visa.
            String formattedDate = "";
            Long timestamp = cursor.getLong(timestampIndex);
            if (timestamp > 0) {
                Date date = new Date(timestamp);
                formattedDate = dateFormatter.format(date);
            }
            ((TextView) view).setText(formattedDate);

            return true;
        }

        return false;
    }
}
