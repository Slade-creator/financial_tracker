package com.studentassoc.financialtracker.View;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.studentassoc.financialtracker.Model.FilterCriteria;
import com.studentassoc.financialtracker.R;
import com.studentassoc.financialtracker.Utils.Utils;
import com.studentassoc.financialtracker.ViewModel.TransactionViewModel;

import java.util.Calendar;
import java.util.Locale;

public class FilterDialogFragment extends DialogFragment {

    private TransactionViewModel transactionViewModel;

    private EditText etFilterStartDate, etFilterEndDate, etFilterMemberSearch;
    private AutoCompleteTextView acFilterCategory, acFilterPaymentMethod, acFilterApprovalStatus;
    private Button btnClearFilters, btnApplyFilters;

    private final String[] allCategories = {
            "Membership Fee", "T-Shirt Sales", "Other Contribution",
            "Transport", "Food", "Supplies", "Event Costs", "Administrative", "Other"
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transactionViewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);
    }

    @Nullable
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceStatus) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_filter_transactions, null);

        initializeViews(dialogView);
        setupAdapters();
        populateCurrentFilters();
        setupListeners();

        builder.setView(dialogView);
        return builder.create();
    }

    private void initializeViews(View view) {
        etFilterStartDate = view.findViewById(R.id.etFilterStartDate);
        etFilterEndDate = view.findViewById(R.id.etFilterEndDate);
        acFilterCategory = view.findViewById(R.id.acFilterCategory);
        acFilterPaymentMethod = view.findViewById(R.id.acFilterPaymentMethod);
        acFilterApprovalStatus = view.findViewById(R.id.acFilterApprovalStatus);
        etFilterMemberSearch = view.findViewById(R.id.etFilterMemberSearch);
        btnClearFilters = view.findViewById(R.id.btnClearFilters);
        btnApplyFilters = view.findViewById(R.id.btnApplyFilters);
    }

    private void setupAdapters() {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, allCategories);
        acFilterCategory.setAdapter(categoryAdapter);

        String[] paymentMethods = {"CASH", "MOBILE_MONEY"};
        ArrayAdapter<String> paymentAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, paymentMethods);
        acFilterPaymentMethod.setAdapter(paymentAdapter);

        String[] approvalStatuses = {"Approved", "Pending"};
        ArrayAdapter<String> approvalAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, approvalStatuses);
        acFilterApprovalStatus.setAdapter(approvalAdapter);
    }

    private void populateCurrentFilters() {
        FilterCriteria current = transactionViewModel.getCurrentFilterCriteria();

        if (current.getStartDate() != null) {
            etFilterStartDate.setText(formatDateForInput(current.getStartDate()));
        }
        if (current.getEndDate() != null) {
            etFilterEndDate.setText(formatDateForInput(current.getEndDate()));
        }
        if (current.getCategory() != null) {
            acFilterCategory.setText(current.getCategory(), false);
        }
        if (current.getPaymentMethod() != null) {
            acFilterPaymentMethod.setText(current.getPaymentMethod(), false);
        }
        if (current.getApprovalStatus() != null) {
            acFilterApprovalStatus.setText(
                    current.getApprovalStatus() == 1 ? "Approved" : "Pending", false);
        }
        if (current.getMemberSearchQuery() != null) {
            etFilterMemberSearch.setText(current.getMemberSearchQuery());
        }
    }

    private void setupListeners() {
        etFilterStartDate.setOnClickListener(v -> showDatePicker(etFilterStartDate));
        etFilterEndDate.setOnClickListener(v -> showDatePicker(etFilterEndDate));

        btnClearFilters.setOnClickListener(v -> {
            transactionViewModel.clearFilters();
            dismiss();
            Toast.makeText(requireContext(), "Filters cleared", Toast.LENGTH_SHORT).show();
        });

        btnApplyFilters.setOnClickListener(v -> applyFilters());
    }

    private void showDatePicker(EditText targetField) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String date = String.format(Locale.ENGLISH, "%04d-%02d-%02d",
                            year, month + 1, dayOfMonth);
                    targetField.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void applyFilters() {
        FilterCriteria newCriteria = new FilterCriteria();

        String startDateText = etFilterStartDate.getText().toString();
        if (!startDateText.isEmpty()) {
            newCriteria.setStartDate(convertToISO8601(startDateText));
        }

        String endDateText = etFilterEndDate.getText().toString();
        if (!endDateText.isEmpty()) {
            newCriteria.setEndDate(convertToISO8601(endDateText));
        }

        String category = acFilterCategory.getText().toString();
        if (!category.isEmpty()) {
            newCriteria.setCategory(category);
        }

        String paymentMethod = acFilterPaymentMethod.getText().toString();
        if (!paymentMethod.isEmpty()) {
            newCriteria.setPaymentMethod(paymentMethod);
        }

        String approvalStatus = acFilterApprovalStatus.getText().toString();
        if (!approvalStatus.isEmpty()) {
            newCriteria.setApprovalStatus("Approved".equals(approvalStatus) ? 1 : 0);
        }

        String memberSearch = etFilterMemberSearch.getText().toString();
        if (!memberSearch.isEmpty()) {
            newCriteria.setMemberSearchQuery(memberSearch);
        }

        transactionViewModel.applyFilters(newCriteria);
        dismiss();

        int filterCount = newCriteria.getActiveFilterCount();
        if (filterCount > 0) {
            Toast.makeText(requireContext(), filterCount + " filter(s) applied", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatDateForInput(String iso8601Date) {
        if (iso8601Date != null && iso8601Date.length() >= 10) {
            return iso8601Date.substring(0, 10);
        }
        return "";
    }

    private String convertToISO8601(String dateString) {
        try {
            String[] parts = dateString.split("-");
            Calendar calendar = Calendar.getInstance();
            calendar.set(Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]) - 1,
                    Integer.parseInt(parts[2]),
                    0, 0, 0);
            return Utils.toISO8601(calendar.getTime());
        } catch (Exception e) {
            return dateString + "T00:00:00Z";
        }
    }
}
