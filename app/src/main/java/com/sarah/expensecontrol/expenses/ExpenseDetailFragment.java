package com.sarah.expensecontrol.expenses;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.UUID;

import com.sarah.expensecontrol.Expense;
import com.sarah.expensecontrol.R;
import com.sarah.expensecontrol.expenses.categories.CategoryAdapter;
import com.sarah.expensecontrol.model.ExpenseControlContentProvider;

import static android.app.Activity.RESULT_OK;
import static com.sarah.expensecontrol.util.LoaderManagerIDs.CATEGORIES_LOADER_ID;
import static com.sarah.expensecontrol.model.ExpenseControlContract.*;

/**
 * Kanske den mest komplexa klassen pga. att det sker en hel del state förändringar. Här hanteras
 * allt vad gäller expenses, dess datum, bilder och fält.
 */

public class ExpenseDetailFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "ExpenseDetailFragment";

    static final String EXTRA_DATE = "ExpenseDetailFragment:date";  // Det extra som identifierar en
                                                                    // datumuppdatering
    /* För att kunna spara undan allt som behövs för att återställa statet vid rotation */
    private static final String RESTORE_PICTURE_ID = "ExpenseDetailFragment:picture_id";
    private static final String RESTORE_PICTURE_CHANGED = "ExpenseDetailFragment:changed_picture";
    /* För att kunna spara undan allt som behövs för att återställa statet vid rotation */

    /* Definitioner för content provider interaktion */
    private static final String[] EXPENSE_PROJECTION = {
            ExpenseEntry.COLUMN_NAME_NAME,
            ExpenseEntry.COLUMN_NAME_AMOUNT,
            ExpenseEntry.COLUMN_NAME_TIMESTAMP,
            ExpenseEntry.COLUMN_NAME_RECURRING,
            ExpenseEntry.COLUMN_NAME_CATEGORY,
            ExpenseEntry.COLUMN_NAME_PICTURE_URI
    };
    private static final String EXPENSE_SELECTION = ExpenseEntry._ID + "=?";

    private static final String[] CATEGORY_PROJECTION = {
            CategoryEntry._ID,
            CategoryEntry.COLUMN_NAME_NAME
    };
    /* Definitioner för content provider interaktion */

    /* Requestkoder för datum, bildtagning samt permissions */
    private static final int TAKE_PICTURE_REQUEST_CODE = 0;
    private static final int CHANGE_DATE_REQUEST_CODE = 1;
    private static final int FILE_PERMISSION_REQUEST_CODE = 2;

    /* Fält för expenses */
    private EditText mExpenseName;
    private EditText mExpenseAmount;

    private CheckBox mExpenseRecurring;
    private Button mExpenseTimestamp;

    private AutoCompleteTextView mExpenseCategory;
    private CategoryAdapter mCategoryAdapter;
    /* Fält för expenses */

    /* Fält som behövs i samband med bildtagning och sparandet av bilder */
    private Button mExpenseRemovePicture;
    private ImageView mExpensePicture;
    private String mNewPictureId;
    private String mOldPictureId;
    private boolean mNewPictureTaken;
    private boolean mExistingPicture;
    private boolean mDeleteExistingPicture;
    /* Fält som behövs i samband med bildtagning och sparandet av bilder */

    private Expense mExpense;
    private boolean mIsEdit;    // Om en expense just nu editeras.

    /**
     * Standard, skapar vyn för fragmentet.
     * @param inflater layoutinflater
     * @param container parent
     * @param savedInstanceState state
     * @return view för expense detailjer
     */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expense_detail, container, false);

        Log.d(TAG, "onCreateView triggered");

        mExpenseName = (EditText) view.findViewById(R.id.expense_detail_name);

        mExpenseAmount = (EditText) view.findViewById(R.id.expense_detail_amount);

        mExpenseRecurring = (CheckBox) view.findViewById(R.id.expense_detail_recurring);
        mExpenseRecurring.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    // Om recurring kryssats är det en räkning och timestamp clearas.
                    mExpense.setTimestamp(0);
                } else {
                    // Om inte, sätt timestamp till dagens datum.
                    mExpense.setTimestamp(new Date().getTime());
                }
                // Kalla på setDate för att fälten ska visas ordentligt vid förändring.
                setDate();
                mExpenseTimestamp.setEnabled(!checked); // Göm även datumväljaren om ikryssad.
            }
        });

        mExpenseTimestamp = (Button) view.findViewById(R.id.expense_detail_timestamp);
        mExpenseTimestamp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fm = getActivity().getSupportFragmentManager();

                // Starta datepickern vid det datum som just nu är valt.
                Date requestDate;
                if (mExpense.getTimestamp() == 0) {
                    requestDate = new Date();
                } else {
                    requestDate = new Date(mExpense.getTimestamp());
                }
                DatePickerFragment dialog = DatePickerFragment.newInstance(requestDate.getTime());

                dialog.setTargetFragment(ExpenseDetailFragment.this, CHANGE_DATE_REQUEST_CODE);
                dialog.show(fm, TAG);
            }
        });

        mExpenseCategory = (AutoCompleteTextView) view.findViewById(R.id.expense_detail_category);
        mCategoryAdapter = new CategoryAdapter(
                getContext(),
                new ArrayList<String>());   // Adaptern hålls tom tills alla tillgängliga kategorier
                                            // laddats klart.
        mExpenseCategory.setAdapter(mCategoryAdapter);

        final Button mExpenseTakePicture = (Button) view.findViewById(R.id.expense_detail_take_picture);
        mExpenseTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Vid vissa versioner så måste tillstånd begäras med dialog.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    checkPermissionsBeforeTakingPicture();
                } else {
                    // Om inte denna version, gå vidare direkt.
                    takePicture();
                }
            }
        });

        mExpenseRemovePicture = (Button) view.findViewById(R.id.expense_detail_remove_picture);
        mExpenseRemovePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Om en ny bild tagits, ta bort denna.
                if (mNewPictureTaken) {
                    Log.d(TAG, "New picture removed.");
                    mNewPictureTaken = false;
                    Toast.makeText(getContext(), "Removed picture", Toast.LENGTH_SHORT).show();

                    removePicture(mNewPictureId);   // Ta bort x tagna bilden.
                    if (mExistingPicture) {         // Om det fanns en bild innan, ladda då denna
                                                    // istället.
                        Log.d(TAG, "There was an old picture to load.");
                        loadPicture(mExpense.getPictureUri());
                    } else {
                        // Om det inte finns en tidigare bild, dölj borttagningsknappen.
                        mExpenseRemovePicture.setVisibility(View.INVISIBLE);
                    }
                    // Finns det ingen ny bild, men dock en existerande bild när knappen trycktes...
                } else if (mExistingPicture) {
                    // Flagga bilden för borttagning, men ta inte bort den förrän användaren bekräftar
                    // genom att spara ändringarna.
                    mDeleteExistingPicture = true;
                    Log.d(TAG, "Flagged existing picture for deleting.");
                    Toast.makeText(getContext(), "Picture will be deleted when you save", Toast.LENGTH_SHORT).show();

                    // Clear till skillnad från delete rensar bara det visuella, men bildfiler finns
                    // kvar.
                    clearPicture();
                    mExpenseRemovePicture.setVisibility(View.INVISIBLE);
                }
            }
        });

        mExpensePicture = (ImageView) view.findViewById(R.id.expense_detail_actual_picture);

        // Initiera ett expense objekt så att expense fält kan kontrolleras vid nedsparning.
        mExpense = new Expense();
        // Ladda eventuella extras som kommit med.
        loadExtras();
        // Ladda upp data i de inputfält som finns, speciellt om det är en editerad expense.
        populateFields();

        setUpCustomActionBar();     // Sätt upp den custom action bar med knappar för att gå tillbaka
                                    // ta bort en expense samt för att spara.

        // Starta kategoriloadern.
        getActivity().getSupportLoaderManager().initLoader(CATEGORIES_LOADER_ID, null, this);

        return view;
    }

    /**
     * Används för att återställa bilden vid rotation.
     * @param savedInstanceState sparat state
     */

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.d(TAG, "onActivityCreated triggered");

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(RESTORE_PICTURE_CHANGED)) {
                loadPicture(savedInstanceState.getString(RESTORE_PICTURE_ID));
            }
        }
    }

    /**
     * Sparar under bildstatus vid rotation.
     * @param savedInstanceState state att spara
     */

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(RESTORE_PICTURE_CHANGED, mNewPictureTaken);
        savedInstanceState.putString(RESTORE_PICTURE_ID, mNewPictureId);

        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Tar emot svaret från bildtagnings- och datumvalaktiviteter.
     * @param requestCode för anropet
     * @param resultCode resultat (OK/NOT OK)
     * @param intent för resultatet
     */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Allt annat än OK ignoreras.
        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == TAKE_PICTURE_REQUEST_CODE) {
            // Om newpicturetaken redan är satt när svaret kommer tillbaka så finns det redan en bild
            // taget, denna tas bort ur filsystemet.
            if (mNewPictureTaken) removePicture(mOldPictureId);

            mNewPictureTaken = true;    // Flagga för att en ny bild finns
                                        // och gör det möjligt att ta bort denna.
            mExpenseRemovePicture.setVisibility(View.VISIBLE);

            loadPicture(mNewPictureId); // Ladda upp den nya bilden.
        } else if (requestCode == CHANGE_DATE_REQUEST_CODE) {
            long newDate = intent.getLongExtra(EXTRA_DATE, 0);
            // Sätt tidsstämpeln och ladda om datumet.
            mExpense.setTimestamp(newDate);

            setDate();
        }
    }

    /**
     * Denna funktion kommer att först kontrollera existerande permissions och om något saknas
     * som behövs inför bildtagning så visas en dialog för att få användarens godkännande.
     */

    private void checkPermissionsBeforeTakingPicture() {
        if (ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    getActivity(),
                    new String[] {
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    FILE_PERMISSION_REQUEST_CODE);
        } else {
            takePicture();
        }
    }

    /**
     * Startar en bildtagningsaktivitet och hanterar så att bildstatet sparas på ett korrekt sätt.
     */

    private void takePicture() {
        // Om det redan finns en ny bild tagen, spara undan detta id. Det är inte säkert att den
        // nya bildtagningen kommer att resultera i att användaren faktiskt tar en ny bild. Om användaren
        // avbryter så måste den gamla bildens referens finnas kvar.
        if (mNewPictureTaken) mOldPictureId = mNewPictureId;

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File pictureDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // UUID används för att säkerställa filnamnets unikhet.
        mNewPictureId = UUID.randomUUID().toString();
        File pictureFile = new File(pictureDir + "/" + mNewPictureId);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(pictureFile));
        startActivityForResult(intent, TAKE_PICTURE_REQUEST_CODE);
    }

    /**
     * Spara alla nuvarande värden i den expense som laddats upp eller i en helt ny expense om ingen
     * valdes för att starta den här aktiviteten.
     * @return true, om sparningen kunde genomföras
     */

    private boolean saveChanges() {
        ContentValues values = new ContentValues();

        // Finns ingen amount, tillåt inte användaren att spara.
        if (TextUtils.isEmpty(mExpenseAmount.getText())) {
            Toast.makeText(getContext(), "Could not save, enter an amount!", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Kontrollera datumet, vid exception tillåt ej sparning.
        if (!mExpenseRecurring.isChecked()) {
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            Date date;
            try {
                date = formatter.parse(mExpenseTimestamp.getText().toString());
            } catch (ParseException e) {
                Log.d(TAG, "Badly formatted date! " + e.getMessage());
                return false;
            }
            values.put(ExpenseEntry.COLUMN_NAME_TIMESTAMP, date.getTime());
        } else {
            values.put(ExpenseEntry.COLUMN_NAME_TIMESTAMP, "");
        }

        /* Fält som ej krävs för att spara! */
        values.put(ExpenseEntry.COLUMN_NAME_NAME, mExpenseName.getText().toString());
        values.put(ExpenseEntry.COLUMN_NAME_AMOUNT, Integer.valueOf(mExpenseAmount.getText().toString()));
        values.put(ExpenseEntry.COLUMN_NAME_RECURRING, mExpenseRecurring.isChecked());
        values.put(ExpenseEntry.COLUMN_NAME_CATEGORY, mExpenseCategory.getText().toString());
        /* Fält som ej krävs för att spara! */

        // Separat hantering av bildsparning pga. komplexitet.
        values.put(ExpenseEntry.COLUMN_NAME_PICTURE_URI, resolvePictureToSave());

        // Insert eller update beroende på om en expense editeras eller om en ny skapas.
        if (mIsEdit) {
            getActivity().getContentResolver().update(
                    ExpenseControlContentProvider.EXPENSE_URI,
                    values,
                    EXPENSE_SELECTION,
                    new String[] { String.valueOf(mExpense.getId()) }
            );
        } else {
            getActivity().getContentResolver().insert(
                    ExpenseControlContentProvider.EXPENSE_URI,
                    values
            );
        }
        // Skapa till sist en kategori.
        createCategory();

        return true;
    }

    private String resolvePictureToSave() {
        Log.d(TAG, "Resolving which picture to save...");
        Log.d(TAG, "New picture taken? " + mNewPictureTaken);
        Log.d(TAG, "Existing picture? " + mExistingPicture);
        // Om en ny bild finns...
        if (mNewPictureTaken) {
            // Ta bort den gamla bilden om den finns.
            if (mExistingPicture) removePicture(mExpense.getPictureUri());

            // Viktigt för att onDestroy inte ska ta bort bilden när fragmentet stängs ned.
            mNewPictureTaken = false;

            // och returnera den nya bildens ID-nummer
            return mNewPictureId;
            // Har den nuvarande bilden flaggats för borttagning?
        } else if (mDeleteExistingPicture) {
            // Ta bort den i så fall
            removePicture(mExpense.getPictureUri());

            // Och returnera en tom sträng så att ingen referens till den borttagna bilden sparas.
            return "";
        } else {
            // Om inget annat, behåll det nuvarande värdet.
            return mExpense.getPictureUri();
        }
    }

    /**
     * Skapar en kategori med den text som matats in i kategori-fältet.
     */

    private void createCategory() {
        String categoryName = mExpenseCategory.getText().toString();
        ContentValues values = new ContentValues();

        // Om fältet är tomt, skapa ingenting och avsluta.
        if (categoryName.length() > 0) {
            values.put(CategoryEntry.COLUMN_NAME_NAME, categoryName);

            // Dubbletter hanteras längre ner. SQLite kommer inte tillåta fler än en kategori med
            // samma namn pga. UNIQUE constraint på tabellen (se ExpenseControlDbHelper).
            getActivity().getContentResolver().insert(
                    ExpenseControlContentProvider.CATEGORY_URI,
                    values
            );
        }
    }

    /**
     * Tar bort den aktiva expensen, såklart bara ett alternativ om en expense just nu editeras.
     * Se setUpCustomActionBar för hantering av editering/icke editering.
     */

    private void delete() {
        getActivity().getContentResolver().delete(
                ExpenseControlContentProvider.EXPENSE_URI,
                EXPENSE_SELECTION,
                new String[] { String.valueOf(mExpense.getId()) }
        );
        resolvePictureDeletion();
    }

    /**
     * Väljer om någon bild skall tas bort.
     */

    private void resolvePictureDeletion() {
        // Här kontrolleras vilka bilder som ska bort, eftersom att delete valts.
        if (mNewPictureTaken) {
            removePicture(mNewPictureId);
        }
        if (mExistingPicture) {
            removePicture(mExpense.getPictureUri());
        }
    }

    /**
     * Laddar alla extras som kom med vid aktivitetens start.
     */

    private void loadExtras() {
        Intent intent = getActivity().getIntent();
        mIsEdit = intent.getBooleanExtra(ExpensesFragment.EXTRA_EDIT, false);
        mExpense.setId(intent.getLongExtra(ExpensesFragment.EXTRA_EXPENSE_ID, -1));
    }

    private void populateFields() {
        // Om expensen har ett id, ladda den.
        if (mExpense.getId() != -1) loadEditedExpense(mExpense.getId());

        // Sätt alla fält.
        mExpenseName.setText(mExpense.getName());
        mExpenseAmount.setText(String.valueOf(mExpense.getAmount()));
        mExpenseRecurring.setChecked(mExpense.isRecurring());
        mExpenseCategory.setText(mExpense.getCategory());

        setDate();

        // Kontrollera bildstatet.
        if (TextUtils.isEmpty(mExpense.getPictureUri())) {
            Log.d(TAG, "This expense had no picture to load!");
            mExpenseRemovePicture.setVisibility(View.INVISIBLE);
        } else {
            Log.d(TAG, "Loading existing picture for this expense.");
            mExistingPicture = true;
            loadPicture(mExpense.getPictureUri());
        }
    }

    /**
     * Tar argumentet, hämtar den expense med samma id och sätter fälten i klassfältet mExpense.
     * @param expenseId för den expense som ska laddas
     */

    private void loadEditedExpense(long expenseId) {
        Cursor cursor = getActivity().getContentResolver().query(
                ExpenseControlContentProvider.EXPENSE_URI,
                EXPENSE_PROJECTION,
                EXPENSE_SELECTION,
                new String[] { String.valueOf(expenseId) },
                null
        );
        if (cursor.moveToFirst()) {
            mExpense.setName(cursor.getString(cursor.getColumnIndex(ExpenseEntry.COLUMN_NAME_NAME)));
            mExpense.setAmount(cursor.getInt(cursor.getColumnIndex(ExpenseEntry.COLUMN_NAME_AMOUNT)));
            mExpense.setTimestamp(cursor.getLong(cursor.getColumnIndex(ExpenseEntry.COLUMN_NAME_TIMESTAMP)));
            mExpense.setRecurring(cursor.getInt(cursor.getColumnIndex(ExpenseEntry.COLUMN_NAME_RECURRING)) == 1);
            mExpense.setCategory(cursor.getString(cursor.getColumnIndex(ExpenseEntry.COLUMN_NAME_CATEGORY)));
            mExpense.setPictureUri(cursor.getString(cursor.getColumnIndex(ExpenseEntry.COLUMN_NAME_PICTURE_URI)));
        }
        cursor.close();
    }

    /**
     * Laddar fram och visar den bild med det id som parametern har.
     * @param pictureUri för bilden som ska visas
     */

    private void loadPicture(String pictureUri) {
        Log.d(TAG, "Loading picture: " + pictureUri);
        // Trots namnet så är externalFilesDir en internlagring för applikationen som bara denna
        // applikation kommer åt.
        File pictureDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        Bitmap bitMap = BitmapFactory.decodeFile(
                new File(pictureDir + "/" + pictureUri).getAbsolutePath());

        // Bestämd bildstorlek pga. att imageviews inte kan hantera bilder större än 4096x4096,
        // vilket kan bli ett problem på vissa telefoner.
        mExpensePicture.setImageBitmap(Bitmap.createScaledBitmap(bitMap, 1024, 768, false));
    }

    /**
     * Tar endast bort det visuella med den aktiva bilden, filen rörs inte.
     */

    private void clearPicture() {
        mExpensePicture.setImageResource(android.R.color.transparent);
    }

    /**
     * Tar bort en bild helt ut filsystemet.
     * @param pictureUri för den bild som ska tas bort
     */

    private void removePicture(String pictureUri) {
        File pictureToDelete = new File(
                getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES) +
                "/" +
                pictureUri
        );
        boolean deleted = pictureToDelete.delete();
        Log.d(TAG, "Successfully deleted picture " + pictureUri + ": " + deleted);
        clearPicture();
    }

    /**
     * Sätter datumet efter formattering till ett läsbart format.
     */

    private void setDate() {
        Date date;
        if (mExpense.getTimestamp() > 0) {
            date = new Date(mExpense.getTimestamp());
        } else {
            date = new Date();
        }
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        String prettify = formatter.format(date);
        mExpenseTimestamp.setText(prettify);
    }

    /**
     * Säkerställ att även om fragmentet förstörs så ska eventuella nya bilder tas bort.
     */

    public void onDestroy() {
        if (mNewPictureTaken) removePicture(mNewPictureId);
        super.onDestroy();
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
            case CATEGORIES_LOADER_ID:
                return new CursorLoader(
                        getActivity().getApplicationContext(),
                        ExpenseControlContentProvider.CATEGORY_URI,
                        CATEGORY_PROJECTION,
                        null,
                        null,
                        null
                );
            default:
                Log.d(TAG, "No loader id was matched.");
                break;
        }
        return null;
    }

    /**
     * Kallas när loadern hämtat sin associerade data.
     * @param loader, den färdiga loadern
     * @param data, laddad data
     */

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        ArrayList<String> categories = new ArrayList<>();
        mCategoryAdapter.clear();

        for (data.moveToFirst(); !data.isAfterLast(); data.moveToNext()) {
            categories.add(data.getString(data.getColumnIndex(CategoryEntry.COLUMN_NAME_NAME)));
        }
        mCategoryAdapter.addAll(categories);
    }

    /**
     * Om loadern (bara en i detta fallet) återställs så clearas datan från kategoriadaptern för
     * att ny, OK, data ska kunna visas istället.
     * @param loader som återställts
     */

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCategoryAdapter.clear();
    }

    /**
     * Sätter upp knapparna i den custom action bar som skapats i den underliggande aktiviteten
     * NavigationLessActivity.
     */

    private void setUpCustomActionBar() {
        View view = ((AppCompatActivity) getActivity()).getSupportActionBar().getCustomView();
        ImageButton btnClose = (ImageButton) view.findViewById(R.id.custom_action_bar_close);
        btnClose.setBackgroundColor(Color.TRANSPARENT);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Stängs expensen ner så måste eventuella nya bilder tas bort.
                if (mNewPictureTaken) removePicture(mNewPictureId);
                getActivity().finish();
            }
        });
        ImageButton btnDelete = (ImageButton) view.findViewById(R.id.custom_action_bar_delete);
        btnClose.setBackgroundColor(Color.TRANSPARENT);
        btnDelete.setVisibility(mIsEdit ? View.VISIBLE : View.INVISIBLE);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Deleting expense with ID: " + mExpense.getId());
                delete();
                getActivity().finish();
            }
        });
        ImageButton btnDone = (ImageButton) view.findViewById(R.id.custom_action_bar_done);
        btnClose.setBackgroundColor(Color.TRANSPARENT);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Stänger endast om sparningen gick igenom.
                if (saveChanges()) getActivity().finish();
            }
        });
    }
}
