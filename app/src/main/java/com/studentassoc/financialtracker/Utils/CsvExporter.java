package com.studentassoc.financialtracker.Utils;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.studentassoc.financialtracker.Model.Transaction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class CsvExporter {

    private static final String TAG = "CsvExporter";
    private static final String CSV_HEADER = "Date,Type,Amount (K),Category,Member Name,Payment Method,Status,Notes\n";


    public static Uri exportToCSV(Context context, List<Transaction> transactions, String fileName) {
        try {
            if (fileName == null || fileName.isEmpty()) {
                fileName = generateFileName();
            }

            File exportDir = new File(context.getCacheDir(), "exports");

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Files.createDirectories(exportDir.toPath());
                } else {
                    if (!exportDir.exists() && !exportDir.mkdirs()) {
                        throw new IOException("Failed to create directory: " + exportDir.getPath());
                    }
                }
            } catch (IOException e) {
                Log.e("FileUtils", "Could not create export directory", e);
            }


            File csvFile = new File(exportDir, fileName);
            FileWriter writer = new FileWriter(csvFile);

            writer.append(CSV_HEADER);

            for (Transaction transaction : transactions) {
                writer.append(formatTransactionRow(transaction));
            }

            writer.flush();
            writer.close();

            Uri fileUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    csvFile
            );

            Log.d(TAG, "CSV export successful: " + fileName);
            return fileUri;
        } catch (IOException e) {
            Log.e(TAG, "Error exporting CSV: " + e.getMessage());
            return null;
        }
    }

    private static String formatTransactionRow(Transaction transaction) {
        StringBuilder row = new StringBuilder();

        row.append(escapeCsvValue(Utils.formatDateForDisplay(transaction.getTransactionDate())));
        row.append(",");

        row.append(transaction.getTransactionType());
        row.append(",");

        double amountKwacha = transaction.getAmount() / 100.0;
        row.append(String.format(Locale.US, "%.2f", amountKwacha));
        row.append(",");

        row.append(escapeCsvValue(transaction.getCategory()));
        row.append(",");

        String memberName = transaction.getMemberName() != null ? transaction.getMemberName() : "";
        row.append(escapeCsvValue(memberName));
        row.append(",");

        String paymentMethod = transaction.getPaymentMethod().replace("_", " ");
        row.append(escapeCsvValue(paymentMethod));
        row.append(",");

        String status = transaction.getIsApproved() == 1 ? "Approved" : "Pending";
        row.append(status);
        row.append(",");

        String notes = transaction.getNotes() != null ? transaction.getNotes() : "";
        row.append(escapeCsvValue(notes));
        row.append("\n");

        return row.toString();
    }

    private static String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }

        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            // Escape existing quotes by doubling them
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }

        return value;
    }

    private static String generateFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US);
        String timestamp = sdf.format(new Date());
        return "transactions_" + timestamp + ".csv";
    }

    public static Uri exportWithSummary(Context context, List<Transaction> transactions,
                                        int totalIncome, int totalExpenses, int balance) {
        try {
            String fileName = generateFileName();

            File exportDir = new File(context.getCacheDir(), "exports");

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Files.createDirectories(exportDir.toPath());
                } else {
                    if (!exportDir.exists() && !exportDir.mkdirs()) {
                        throw new IOException("Failed to create directory: " + exportDir.getPath());
                    }
                }
            } catch (IOException e) {
                Log.e("FileUtils", "Could not create export directory", e);
            }

            File csvFile = new File(exportDir, fileName);
            FileWriter writer = new FileWriter(csvFile);

            writer.append("Financial Summary\n");
            writer.append("Total Income,").append(Utils.toKwacha(totalIncome)).append("\n");
            writer.append("Total Expenses,").append(Utils.toKwacha(totalExpenses)).append("\n");
            writer.append("Net Balance,").append(Utils.toKwacha(balance)).append("\n");
            writer.append("Total Transactions,").append(String.valueOf(transactions.size())).append("\n");
            writer.append("\n");

            writer.append(CSV_HEADER);
            for (Transaction transaction : transactions) {
                writer.append(formatTransactionRow(transaction));
            }

            writer.flush();
            writer.close();

            Uri fileUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    csvFile
            );

            Log.d(TAG, "CSV export with summary successful: " + fileName);
            return fileUri;
        } catch (IOException e) {
            Log.e(TAG, "Error exporting CSV with summary: " + e.getMessage());
            return null;
        }
    }

    public static String getFileSize(Context context, Uri fileUri) {
        try {
            File file = new File(Objects.requireNonNull(fileUri.getPath()));
            long bytes = file.length();

            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
            return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
        } catch (Exception e) {
            return "Unknown size";
        }
    }
}
