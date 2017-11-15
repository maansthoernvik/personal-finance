package com.sarah.expensecontrol.expenses.categories;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

import com.sarah.expensecontrol.R;
import com.sarah.expensecontrol.model.ExpenseControlContentProvider;

import static com.sarah.expensecontrol.model.ExpenseControlContract.CategoryEntry;

/**
 * Denna adapter används enbart i utgiftsdetaljvyn för att visa kategoriförslag för en utgift.
 */

public class CategoryAdapter extends ArrayAdapter<String> {

    /* Definitioner för content provider interaktion */
    private static final String CATEGORY_WHERE = CategoryEntry.COLUMN_NAME_NAME + "=?";
    /* Definitioner för content provider interaktion */

    /**
     * Standard constructor.
     * @param context, applikationens context
     * @param objects, objekt att sätta in i adaptern, i detta fallet strängar
     */

    public CategoryAdapter(@NonNull Context context,
                           @NonNull ArrayList<String> objects) {
        super(context, 0, objects);
    }

    /**
     * Här väljs vad som ska visas på en specifik position i den dropdownlista som visas när en
     * kategori delvis blir ifylld och matchningar finns.
     * @param position, index för elementet
     * @param convertView, gammal, återanvändbar view
     * @param parent, föräldern
     * @return view med det som ska visas på input position
     */

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        // Används som selection argument vid borttagning av kategorin och som visad text.
        final String category = getItem(position);
        // Kolla om convertView kan återanvändas först och främst.
        if (convertView == null) {
            // Om inte, inflatea en ny view.
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.category_list_item, parent, false
            );
        }
        TextView tv = (TextView) convertView.findViewById(R.id.custom_category_array_adapter_category);
        tv.setText(category);

        ImageButton btnDelete = (ImageButton) convertView.findViewById(R.id.custom_category_array_adapter_delete);
        btnDelete.setOnClickListener(new View.OnClickListener() {

            /**
             * Tar bort en kategori från dropdownlistan.
             * @param view, den klickade kategorin
             */

            @Override
            public void onClick(View view) {
                getContext().getContentResolver().delete(
                        ExpenseControlContentProvider.CATEGORY_URI,
                        CATEGORY_WHERE,
                        // Eftersom att kategorier har en unique constraint på sina namn så ska det bara
                        // finnas en kategori med samma namn, därmed går denna bra att använda som
                        // selection argument.
                        new String[] { category }
                );
            }
        });

        return convertView;
    }
}
