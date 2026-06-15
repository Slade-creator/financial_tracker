package com.studentassoc.financialtracker.services;


import android.util.Log;

import androidx.annotation.NonNull;

import com.studentassoc.financialtracker.Model.Transaction;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ReportApiService {

    private static final String TAG = "ReportApiService";
    private static final String BASE_URL = "https://financial-tracker-backend-841p.onrender.com";

    private static final ReportApiInterface apiInterface;

    static {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiInterface = retrofit.create(ReportApiInterface.class);
    }

    public interface ReportCallback {
        void onSuccess(ReportResponse response);
        void onError(String error);
    }

    public static void generateAIReport(
            List<Transaction> transactions,
            String startDate,
            String endDate,
            String reportType,
            ReportCallback callback
    ) {
        ReportRequest request = new ReportRequest(startDate, endDate, transactions, reportType);
        enqueue(apiInterface.generateAIReport(request), callback);
    }
    public static void generateAIReport(
            List<Transaction> transactions,
            String startDate,
            String endDate,
            ReportCallback callback
    ) {
        generateAIReport(transactions, startDate, endDate, "monthly", callback);
    }

    public static void testConnection(ReportCallback callback) {
        Call<Void> call = apiInterface.healthCheck();
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError("Server returned: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onError("Cannot connect to server: " + t.getMessage() +
                        "\nMake sure backend is running at: " + BASE_URL);
            }
        });
    }

    private static void enqueue(Call<ReportResponse> call, ReportCallback callback) {
        call.enqueue(new Callback<ReportResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReportResponse> call,
                                   Response<ReportResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    String error = "Server error: " + response.code();
                    if (response.errorBody() != null) {
                        try { error += " — " + response.errorBody().string(); }
                        catch (Exception e) { Log.e(TAG, "Error reading body", e); }
                    }
                    callback.onError(error);
                }
            }
            @Override
            public void onFailure(@NonNull Call<ReportResponse> call, Throwable t) {
                Log.e(TAG, "API request failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    public static String getFullReportUrl(String reportPath) {
        return BASE_URL + reportPath;
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }

}
