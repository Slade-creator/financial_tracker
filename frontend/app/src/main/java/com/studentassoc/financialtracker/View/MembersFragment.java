package com.studentassoc.financialtracker.View;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.studentassoc.financialtracker.Model.Transaction;
import com.studentassoc.financialtracker.R;
import com.studentassoc.financialtracker.Utils.Utils;
import com.studentassoc.financialtracker.ViewModel.TransactionViewModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MembersFragment extends Fragment {

    private TransactionViewModel transactionViewModel;

    private RecyclerView recyclerView;
    private EditText etSearch;
    private TextView tvCount;
    private View layoutEmpty;

    private MembersAdapter membersAdapter;
    private final List<Transaction> allMemberTransactions = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transactionViewModel = new ViewModelProvider(requireActivity())
                .get(TransactionViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_members, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceSatate) {
        super.onViewCreated(view, savedInstanceSatate);

        recyclerView = view.findViewById(R.id.recyclerViewMembers);
        etSearch = view.findViewById(R.id.etSearchMember);
        tvCount = view.findViewById(R.id.tvMemberCount);
        layoutEmpty = view.findViewById(R.id.layoutEmptyState);

        membersAdapter = new MembersAdapter(new ArrayList<>(), this::showMemberTransactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(membersAdapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMembers(s.toString().trim());
            }
        });

        transactionViewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            allMemberTransactions.clear();
            if (transactions != null) {
                for (Transaction t : transactions) {
                    if (t.getMemberName() != null && !t.getMemberName().isEmpty()) {
                        allMemberTransactions.add(t);
                    }
                }
            }
            filterMembers(etSearch.getText().toString().trim());
        });
    }

    private void filterMembers(String query) {
        Map<String, Transaction> lastestByMember = new LinkedHashMap<>();
        for (Transaction t : allMemberTransactions) {
            String name = t.getMemberName();
            if (query.isEmpty() || name.toLowerCase(Locale.ROOT)
                    .contains(query.toLowerCase(Locale.ROOT))) {
                lastestByMember.putIfAbsent(name, t);
            }
        }

        List<Transaction> rows = new ArrayList<>(lastestByMember.values());
        membersAdapter.setItems(rows);

        int count = rows.size();
        tvCount.setText(String.format(Locale.ENGLISH,
                "%d member%s", count, count == 1 ? "" : "s"));

        boolean empty = rows.isEmpty();
        layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showMemberTransactions(String memberName) {
        List<Transaction> memberTx = new ArrayList<>();

        for (Transaction t: allMemberTransactions) {
            if (memberName.equals(t.getMemberName())) memberTx.add(t);
        }

        StringBuilder sb = new StringBuilder();
        for (Transaction t: memberTx) {
            sb.append("• ")
                    .append(Utils.formatDateForDisplay(t.getTransactionDate()))
                    .append("  |  ")
                    .append(t.getPaymentMethod().replace("_", " "))
                    .append("  |  ")
                    .append(Utils.toKwacha(t.getAmount()));

            if (t.getNotes() != null && !t.getNotes().isEmpty()) {
                sb.append("\n Notes: ").append(t.getNotes());
            }

            sb.append("\n\n");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(memberName)
                .setMessage(sb.length() > 0 ? sb.toString().trim() : "No transactions found.")
                .setPositiveButton("close", null)
                .show();

    }
}
