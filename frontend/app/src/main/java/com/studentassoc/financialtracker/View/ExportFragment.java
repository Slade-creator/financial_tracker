package com.studentassoc.financialtracker.View;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.studentassoc.financialtracker.R;

/**
 * ExportFragment — the nav-bar destination for the "Export" tab.
 *
 * When the user taps the Export tab the fragment opens, and the
 * ExportBottomSheet is immediately presented on top of it.
 * All export logic remains in ExportBottomSheet unchanged.
 *
 * Layout (fragment_export.xml) shows a simple placeholder so the
 * fragment never appears empty if the sheet is dismissed without
 * an export being performed.
 */
public class ExportFragment extends Fragment {

    private static final String TAG_EXPORT_SHEET = "ExportBottomSheet";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_export, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Open the export sheet as soon as the fragment is ready.
        // Guard against re-opening if already shown (e.g. after rotation).
        if (getParentFragmentManager().findFragmentByTag(TAG_EXPORT_SHEET) == null) {
            ExportBottomSheet sheet = new ExportBottomSheet();
            sheet.show(getParentFragmentManager(), TAG_EXPORT_SHEET);
        }

        // "Open Export" button on the placeholder layout lets the user
        // reopen the sheet if they dismissed it without exporting.
        View btnOpen = view.findViewById(R.id.btnOpenExport);
        if (btnOpen != null) {
            btnOpen.setOnClickListener(v -> {
                if (getParentFragmentManager().findFragmentByTag(TAG_EXPORT_SHEET) == null) {
                    new ExportBottomSheet().show(getParentFragmentManager(), TAG_EXPORT_SHEET);
                }
            });
        }
    }
}