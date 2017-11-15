package com.sarah.expensecontrol.expenses;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.DatePicker;

import com.sarah.expensecontrol.R;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static com.sarah.expensecontrol.expenses.ExpenseDetailFragment.EXTRA_DATE;

/**
 * Denna dialog används för att visa en kalender och låta användaren välja för vilken datum som en
 * utgift ska registreras för.
 */

public class DatePickerFragment extends DialogFragment {

    private long mDate; // Datumet i fråga.

    public static DatePickerFragment newInstance(long date) {
        Bundle args = new Bundle();
        args.putLong(EXTRA_DATE, date);

        DatePickerFragment dateFragment = new DatePickerFragment();
        dateFragment.setArguments(args);

        return dateFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mDate = getArguments().getLong(EXTRA_DATE);     // Hämta datumet som datepickern ska börja på
        Calendar cal = Calendar.getInstance();          // när den visas.
        cal.setTime(new Date(mDate));
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_date_picker, null);

        DatePicker datePicker = (DatePicker) view.findViewById(R.id.dialog_date_datePicker);
        datePicker.init(year, month, day, new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker datePicker, int year, int month, int day) {
                mDate = new GregorianCalendar(year, month, day).getTimeInMillis();
                // Uppdatera fältet varje gång ett nytt datum valts.
                getArguments().putLong(EXTRA_DATE, mDate);
            }
        });

        return new AlertDialog.Builder(getActivity())
                .setPositiveButton(
                        android.R.string.ok,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                sendResult(Activity.RESULT_OK);
                            }
                        }
                )
                .setNegativeButton(
                        android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                sendResult(Activity.RESULT_CANCELED);
                            }
                        }
                )
                .setView(view)
                .create();
    }

    private void sendResult(int resultCode) {
        if (getTargetFragment() == null) {
            return;
        }

        Intent i = new Intent();
        // Skicka med fältet med den senaste ändringen.
        i.putExtra(EXTRA_DATE, mDate);

        getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, i);
    }
}
