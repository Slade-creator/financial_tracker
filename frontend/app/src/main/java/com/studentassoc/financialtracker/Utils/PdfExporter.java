package com.studentassoc.financialtracker.Utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.studentassoc.financialtracker.Model.Transaction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PdfExporter {

    private static final String TAG = "PdfExporter";

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;

    private static final int MARGIN_LEFT = 40;
    private static final int MARGIN_RIGHT = 40;
    private static final int MARGIN_TOP = 60;
    private static final int MARGIN_BOTTOM = 60;

    private static final int CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;

    private static final int COLOR_PRIMARY = Color.rgb(33, 150, 243);
    private static final int COLOR_INCOME = Color.rgb(76, 175, 80);
    private static final int COLOR_EXPENSE = Color.rgb(244, 67, 54);
    private static final int COLOR_TEXT = Color.rgb(33, 33, 33);
    private static final int COLOR_GRAY = Color.rgb(117, 117, 117);
    private static final int COLOR_LIGHT_GRAY = Color.rgb(238, 238, 238);

    public static Uri generateReport(Context context, List<Transaction> transactions,
                                     int totalIncome, int totalExpenses, int balance,
                                     String reportTitle) {
        try {
            PdfDocument document = new PdfDocument();

            int currentPage = 1;
            int yPosition = MARGIN_TOP;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH, PAGE_HEIGHT, currentPage).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            yPosition = drawHeader(canvas, reportTitle, yPosition);
            yPosition += 20;

            yPosition = drawSummarySection(canvas, totalIncome, totalExpenses, balance, yPosition);
            yPosition += 30;

            Map<String, Integer> categoryTotals = calculateCategoryTotals(transactions);
            yPosition = drawCategoryBreakdown(canvas, categoryTotals, yPosition);
            yPosition += 30;

            yPosition = drawTransactionTableHeader(canvas, yPosition);
            yPosition += 10;

            for (int i = 0; i < transactions.size(); i++) {
                Transaction transaction = transactions.get(i);

                if (yPosition > PAGE_HEIGHT - MARGIN_BOTTOM - 50) {
                    drawFooter(canvas, currentPage, transactions.size());
                    document.finishPage(page);

                    currentPage++;
                    pageInfo = new PdfDocument.PageInfo.Builder(
                            PAGE_WIDTH, PAGE_HEIGHT, currentPage).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();

                    yPosition = MARGIN_TOP;
                    yPosition = drawTransactionTableHeader(canvas, yPosition);
                    yPosition += 10;
                }

                yPosition = drawTransactionRow(canvas, transaction, yPosition, i % 2 == 0);
            }

            drawFooter(canvas, currentPage, transactions.size());
            document.finishPage(page);

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

            String fileName = generatePdfFileName();
            File pdfFile = new File(exportDir, fileName);

            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

            Uri fileUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    pdfFile
            );

            Log.d(TAG, "PDF export successful: " + fileName);
            return fileUri;

        } catch (IOException e) {
            Log.e(TAG, "Error generating PDF: " + e.getMessage());
            return null;
        }
    }

    private static int drawHeader(Canvas canvas, String title, int yPos) {
        Paint paint = new Paint();

        paint.setColor(COLOR_PRIMARY);
        paint.setTextSize(24);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(title, MARGIN_LEFT, yPos, paint);
        yPos += 35;

        paint.setColor(COLOR_GRAY);
        paint.setTextSize(12);
        paint.setTypeface(Typeface.DEFAULT);
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US);
        String dateStr = "Generated on " + sdf.format(new Date());
        canvas.drawText(dateStr, MARGIN_LEFT, yPos, paint);
        yPos += 20;

        paint.setColor(COLOR_PRIMARY);
        paint.setStrokeWidth(2);
        canvas.drawLine(MARGIN_LEFT, yPos, PAGE_WIDTH - MARGIN_RIGHT, yPos, paint);
        yPos += 10;

        return yPos;
    }

    private static int drawSummarySection(Canvas canvas, int totalIncome,
                                          int totalExpenses, int balance, int yPos) {
        Paint paint = new Paint();

        paint.setColor(COLOR_TEXT);
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Financial Summary", MARGIN_LEFT, yPos, paint);
        yPos += 25;

        int boxWidth = (CONTENT_WIDTH - 40) / 3;
        int boxHeight = 70;

        drawSummaryBox(canvas, MARGIN_LEFT, yPos, boxWidth, boxHeight,
                "Total Income", Utils.toKwacha(totalIncome), COLOR_INCOME);

        drawSummaryBox(canvas, MARGIN_LEFT + boxWidth + 20, yPos, boxWidth, boxHeight,
                "Total Expenses", Utils.toKwacha(totalExpenses), COLOR_EXPENSE);

        int balanceColor = balance >= 0 ? COLOR_INCOME : COLOR_EXPENSE;
        drawSummaryBox(canvas, MARGIN_LEFT + (boxWidth + 20) * 2, yPos, boxWidth, boxHeight,
                "Net Balance", Utils.toKwacha(balance), balanceColor);

        return yPos + boxHeight + 10;
    }

    private static void drawSummaryBox(Canvas canvas, int x, int y, int width, int height,
                                       String label, String value, int valueColor) {
        Paint paint = new Paint();

        // Draw box background
        paint.setColor(COLOR_LIGHT_GRAY);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(x, y, x + width, y + height, paint);

        // Draw box border
        paint.setColor(valueColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawRect(x, y, x + width, y + height, paint);

        // Draw label
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(COLOR_GRAY);
        paint.setTextSize(11);
        paint.setTypeface(Typeface.DEFAULT);
        canvas.drawText(label, x + 10, y + 25, paint);

        // Draw value
        paint.setColor(valueColor);
        paint.setTextSize(18);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(value, x + 10, y + 50, paint);
    }

    private static int drawCategoryBreakdown(Canvas canvas, Map<String, Integer> categoryTotals, int yPos) {
        Paint paint = new Paint();

        //Section title
        paint.setColor(COLOR_TEXT);
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Category Breakdown", MARGIN_LEFT, yPos, paint);
        yPos += 25;

        // Draw category bars
        paint.setTextSize(12);
        paint.setTypeface(Typeface.DEFAULT);

        int maxAmount = getMaxCategoryAmount(categoryTotals);
        int barMaxWidth = CONTENT_WIDTH - 150;

        for (Map.Entry<String, Integer> entry : categoryTotals.entrySet()) {
            String category = entry.getKey();
            int amount = entry.getValue();

            paint.setColor(COLOR_TEXT);
            canvas.drawText(category, MARGIN_LEFT, yPos, paint);

            String amountStr = Utils.toKwacha(amount);
            paint.setColor(COLOR_GRAY);
            canvas.drawText(amountStr, MARGIN_LEFT + 150, yPos, paint);

            int barWidth = maxAmount > 0 ? (amount * barMaxWidth) / maxAmount : 0;
            paint.setColor(COLOR_PRIMARY);
            canvas.drawRect(MARGIN_LEFT + 250, yPos - 12, MARGIN_LEFT + 250 + barWidth, yPos, paint);

            yPos += 20;
        }
        return yPos;
    }

    private static int drawTransactionTableHeader(Canvas canvas, int yPos) {
        Paint paint = new Paint();

        // Background
        paint.setColor(COLOR_PRIMARY);
        canvas.drawRect(MARGIN_LEFT, yPos - 15, PAGE_WIDTH - MARGIN_RIGHT, yPos + 5, paint);

        // Header text
        paint.setColor(Color.WHITE);
        paint.setTextSize(11);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawText("Date", MARGIN_LEFT + 5, yPos, paint);
        canvas.drawText("Type", MARGIN_LEFT + 90, yPos, paint);
        canvas.drawText("Category", MARGIN_LEFT + 150, yPos, paint);
        canvas.drawText("Amount", MARGIN_LEFT + 280, yPos, paint);
        canvas.drawText("Payment", MARGIN_LEFT + 380, yPos, paint);

        return yPos + 15;
    }

    private static int drawTransactionRow(Canvas canvas, Transaction transaction,
                                          int yPos, boolean alternateRow) {
        Paint paint = new Paint();

        // Alternate row background
        if (alternateRow) {
            paint.setColor(COLOR_LIGHT_GRAY);
            canvas.drawRect(MARGIN_LEFT, yPos - 12, PAGE_WIDTH - MARGIN_RIGHT, yPos + 8, paint);
        }

        paint.setTextSize(10);
        paint.setTypeface(Typeface.DEFAULT);

        // Date
        paint.setColor(COLOR_TEXT);
        String dateStr = Utils.formatDateForDisplay(transaction.getTransactionDate());
        // Shorten date format for table
        dateStr = dateStr.substring(0, Math.min(dateStr.length(), 12));
        canvas.drawText(dateStr, MARGIN_LEFT + 5, yPos, paint);

        // Type
        paint.setColor("INCOME".equals(transaction.getTransactionType()) ?
                COLOR_INCOME : COLOR_EXPENSE);
        canvas.drawText(transaction.getTransactionType(), MARGIN_LEFT + 90, yPos, paint);

        // Category
        paint.setColor(COLOR_TEXT);
        String category = transaction.getCategory();
        if (category.length() > 15) {
            category = category.substring(0, 12) + "...";
        }
        canvas.drawText(category, MARGIN_LEFT + 150, yPos, paint);

        // Amount
        paint.setColor("INCOME".equals(transaction.getTransactionType()) ?
                COLOR_INCOME : COLOR_EXPENSE);
        String amountStr = Utils.toKwacha(transaction.getAmount());
        canvas.drawText(amountStr, MARGIN_LEFT + 280, yPos, paint);

        // Payment method
        paint.setColor(COLOR_GRAY);
        String payment = transaction.getPaymentMethod().replace("_", " ");
        canvas.drawText(payment, MARGIN_LEFT + 380, yPos, paint);

        return yPos + 20;
    }

    private static void drawFooter(Canvas canvas, int pageNumber, int totalTransactions) {
        Paint paint = new Paint();
        paint.setColor(COLOR_GRAY);
        paint.setTextSize(10);

        // Page number
        String pageText = "Page " + pageNumber;
        canvas.drawText(pageText, PAGE_WIDTH / 2 - 20, PAGE_HEIGHT - 30, paint);

        // Total transactions
        String totalText = "Total Transactions: " + totalTransactions;
        canvas.drawText(totalText, MARGIN_LEFT, PAGE_HEIGHT - 30, paint);
    }

    private static Map<String, Integer> calculateCategoryTotals(List<Transaction> transactions) {
        Map<String, Integer> totals = new HashMap<>();

        for (Transaction transaction : transactions) {
            String category = transaction.getCategory();
            int amount = transaction.getAmount();

            totals.merge(category, amount, Integer::sum);
        }

        return totals;
    }

    private static int getMaxCategoryAmount(Map<String, Integer> categoryTotals) {
        int max = 0;
        for (int amount : categoryTotals.values()) {
            if (amount > max) {
                max = amount;
            }
        }
        return max;
    }

    private static String generatePdfFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US);
        String timestamp = sdf.format(new Date());
        return "financial_report_" + timestamp + ".pdf";
    }
}
