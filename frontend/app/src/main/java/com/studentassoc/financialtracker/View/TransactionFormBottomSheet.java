package com.studentassoc.financialtracker.View;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.studentassoc.financialtracker.Model.Transaction;
import com.studentassoc.financialtracker.R;
import com.studentassoc.financialtracker.Utils.Utils;
import com.studentassoc.financialtracker.ViewModel.TransactionViewModel;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TransactionFormBottomSheet extends BottomSheetDialogFragment {
    private static final String ARG_TRANSACTION = "transaction";
    private static final String ARG_IS_EDIT_MODE = "is_edit_mode";

    private TransactionViewModel transactionViewModel;

    private TextView tvTitle;
    private MaterialButtonToggleGroup toggleGroupTransactionType;
    private EditText etAmount, etMemberName, etNotes, etTransactionDate;
    private TextInputLayout tilMemberName;
    private AutoCompleteTextView acCategory, acPaymentMethod;
    private Button btnSaveTransaction;

    private Transaction transactionBeingEdited;
    private boolean isEditMode = false;
    private String currentTransactionType = "INCOME";

    private final String[] incomeCategories = {"Membership Fee", "T-Shirt Sales", "Other Contribution"};
    private final String[] expenseCategories = {"Transport", "Food", "Supplies", "Event Costs", "Administrative", "Other"};

    public static TransactionFormBottomSheet newInstance(Transaction transaction) {
        TransactionFormBottomSheet fragment = new TransactionFormBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TRANSACTION, transaction);
        args.putBoolean(ARG_IS_EDIT_MODE, true);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transactionViewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);

        if (getArguments() != null) {
            isEditMode = getArguments().getBoolean(ARG_IS_EDIT_MODE, false);
            if (isEditMode) {
                transactionBeingEdited = (Transaction) getArguments().getSerializable(ARG_TRANSACTION);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_transaction_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupListeners();

        if (isEditMode && transactionBeingEdited != null) {
            populateFormForEdit();
        } else {
            setTodayDate();
        }
    }

    private void initializeViews(View view) {
        tvTitle = view.findViewById(R.id.tvBottomSheetTitle);
        toggleGroupTransactionType = view.findViewById(R.id.toggleGroupTransactionType);
        etAmount = view.findViewById(R.id.etAmount);
        etMemberName = view.findViewById(R.id.etMemberName);
        tilMemberName = view.findViewById(R.id.tilMemberName);
        etNotes = view.findViewById(R.id.etNotes);
        etTransactionDate = view.findViewById(R.id.etTransactionDate);
        acCategory = view.findViewById(R.id.acCategory);
        acPaymentMethod = view.findViewById(R.id.acPaymentMethod);
        btnSaveTransaction = view.findViewById(R.id.btnSaveTransaction);

        tvTitle.setText(isEditMode ? "Edit Transaction" : "Add Transaction");
        btnSaveTransaction.setText(isEditMode ? "Update Transaction" : "Save Transaction");

        // Setup payment method adapter
        ArrayAdapter<CharSequence> paymentAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.payment_methods, android.R.layout.simple_dropdown_item_1line
        );
        acPaymentMethod.setAdapter(paymentAdapter);
        acPaymentMethod.setText(paymentAdapter.getItem(0), false);

        updateCategoryAdapter(incomeCategories);

        acCategory.setThreshold(0);
        acPaymentMethod.setThreshold(0);
    }

    private void setupListeners() {
        toggleGroupTransactionType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnTypeIncome) {
                    currentTransactionType = "INCOME";
                    tilMemberName.setVisibility(View.VISIBLE);
                    updateCategoryAdapter(incomeCategories);
                } else {
                    currentTransactionType = "EXPENSE";
                    tilMemberName.setVisibility(View.GONE);
                    updateCategoryAdapter(expenseCategories);
                }
            }
        });

        etTransactionDate.setOnClickListener(v -> showDatePicker());

        btnSaveTransaction.setOnClickListener(v -> {
            if (isEditMode) {
                updateTransaction();
            } else {
                saveTransaction();
            }
        });
    }

    private void populateFormForEdit() {
        if (transactionBeingEdited == null) return;

        currentTransactionType = transactionBeingEdited.getTransactionType();

        if ("INCOME".equals(currentTransactionType)) {
            toggleGroupTransactionType.check(R.id.btnTypeIncome);
            tilMemberName.setVisibility(View.VISIBLE);
            updateCategoryAdapter(incomeCategories);
        } else {
            toggleGroupTransactionType.check(R.id.btnTypeExpense);
            tilMemberName.setVisibility(View.GONE);
            updateCategoryAdapter(expenseCategories);
        }

        // Set amount
        double kwachaAmount = transactionBeingEdited.getAmount() / 100.0;
        etAmount.setText(String.valueOf(kwachaAmount));

        // Set member name
        if (transactionBeingEdited.getMemberName() != null) {
            etMemberName.setText(transactionBeingEdited.getMemberName());
        }

        // Set category
        acCategory.setText(transactionBeingEdited.getCategory(), false);

        // Set payment method
        String paymentDisplay = transactionBeingEdited.getPaymentMethod().equals("CASH")
                ? "Cash" : "Mobile Money";
        acPaymentMethod.setText(paymentDisplay, false);

        try {
            Date transactionDate = Utils.fromISO8601(transactionBeingEdited.getTransactionDate());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(transactionDate);
            String dateStr = String.format(Locale.ENGLISH, "%04d-%02d-%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH));
            etTransactionDate.setText(dateStr);
        } catch (Exception e) {
            setTodayDate();
        }

        if (transactionBeingEdited.getNotes() != null) {
            etNotes.setText(transactionBeingEdited.getNotes());
        }
    }

    private void saveTransaction() {
        String amountStr = etAmount.getText().toString().trim();
        String memberName = etMemberName.getText().toString().trim();
        String category = acCategory.getText().toString();
        String paymentMethod = acPaymentMethod.getText().toString();
        String dateStr = etTransactionDate.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (amountStr.isEmpty()) {
            etAmount.setError("Amount is required");
            etAmount.requestFocus();
            return;
        }

        int amountNgwee = Utils.toNgwee(amountStr);
        if (!Utils.isValidAmount(amountNgwee)) {
            etAmount.setError("Amount must be greater than zero");
            etAmount.requestFocus();
            return;
        }

        if (category.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show();
            acCategory.setError("Please select a category");
            acCategory.requestFocus();
            return;
        }

        try {
            String[] dateParts = dateStr.split("-");
            Calendar calendar = Calendar.getInstance();
            calendar.set(
                    Integer.parseInt(dateParts[0]),
                    Integer.parseInt(dateParts[1]) - 1,
                    Integer.parseInt(dateParts[2]),
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    0
            );
            Date transactionDate = calendar.getTime();

            if (Utils.isDateInFuture(transactionDate)) {
                etTransactionDate.setError("Date cannot be in the future");
                etTransactionDate.requestFocus();
                return;
            }

            String timestamp = Utils.getCurrentTimestamp();
            String transactionDateISO = Utils.toISO8601(transactionDate);
            int isApproved = "INCOME".equals(currentTransactionType) ? 1 : 0;

            Transaction transaction = new Transaction(
                    Utils.generateUUID(),
                    currentTransactionType,
                    amountNgwee,
                    memberName.isEmpty() ? null : memberName,
                    category,
                    paymentMethod.equals("Cash") ? "CASH" : "MOBILE_MONEY",
                    isApproved,
                    transactionDateISO,
                    notes.isEmpty() ? null : notes,
                    timestamp,
                    timestamp
            );

            transactionViewModel.insert(transaction);
            Toast.makeText(requireContext(), "Transaction saved", Toast.LENGTH_SHORT).show();
            dismiss();
        } catch (Exception e) {
            etTransactionDate.setError("Invalid date format");
            etTransactionDate.requestFocus();
        }
    }

    private void updateTransaction() {
        if (transactionBeingEdited == null) return;

        String amountStr = etAmount.getText().toString().trim();
        String memberName = etMemberName.getText().toString().trim();
        String category = acCategory.getText().toString();
        String paymentMethod = acPaymentMethod.getText().toString();
        String dateStr = etTransactionDate.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (amountStr.isEmpty()) {
            etAmount.setError("Amount is required");
            etAmount.requestFocus();
            return;
        }

        int amountNgwee = Utils.toNgwee(amountStr);
        if (!Utils.isValidAmount(amountNgwee)) {
            etAmount.setError("Amount must be greater than zero");
            etAmount.requestFocus();
            return;
        }

        if (category.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show();
            acCategory.setError("Please select a category");
            acCategory.requestFocus();
            return;
        }

        try {
            String[] dateParts = dateStr.split("-");
            Calendar calendar = Calendar.getInstance();

            Date originalDate = Utils.fromISO8601(transactionBeingEdited.getTransactionDate());
            Calendar originalCal = Calendar.getInstance();
            originalCal.setTime(originalDate);

            calendar.set(
                    Integer.parseInt(dateParts[0]),
                    Integer.parseInt(dateParts[1]) - 1,
                    Integer.parseInt(dateParts[2]),
                    originalCal.get(Calendar.HOUR_OF_DAY),
                    originalCal.get(Calendar.MINUTE),
                    0
            );
            Date transactionDate = calendar.getTime();

            if (Utils.isDateInFuture(transactionDate)) {
                etTransactionDate.setError("Date cannot be in the future");
                etTransactionDate.requestFocus();
                return;
            }

            String transactionDateISO = Utils.toISO8601(transactionDate);
            String updatedTimestamp = Utils.getCurrentTimestamp();
            int isApproved = "INCOME".equals(currentTransactionType) ? 1 : 0;

            Transaction updatedTransaction = new Transaction(
                    transactionBeingEdited.getId(),
                    currentTransactionType,
                    amountNgwee,
                    memberName.isEmpty() ? null : memberName,
                    category,
                    paymentMethod.equals("Cash") ? "CASH" : "MOBILE_MONEY",
                    isApproved,
                    transactionDateISO,
                    notes.isEmpty() ? null : notes,
                    transactionBeingEdited.getCreatedAt(),
                    updatedTimestamp
            );

            transactionViewModel.update(updatedTransaction);
            Toast.makeText(requireContext(), "Transaction updated successfully", Toast.LENGTH_SHORT).show();
            dismiss();
        } catch (Exception e) {
            etTransactionDate.setError("Invalid date format");
            etTransactionDate.requestFocus();
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String date = String.format(Locale.ENGLISH, "%04d-%02d-%02d",
                            year, month + 1, dayOfMonth);
                    etTransactionDate.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void updateCategoryAdapter(String[] categories) {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, categories
        );
        acCategory.setAdapter(categoryAdapter);
        acCategory.setText("", false);
        acCategory.setOnClickListener(v -> {
            acCategory.showDropDown();
        });
        acCategory.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                acCategory.showDropDown();
            }
        });
    }

    private void setTodayDate() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        etTransactionDate.setText(String.format(Locale.ENGLISH, "%04d-%02d-%02d", year, month, day));
    }
}
