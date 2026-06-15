package com.studentassoc.financialtracker.services;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ReportApiInterface {

    @POST("/api/generate-report")
    Call<ReportResponse> generateAIReport(@Body ReportRequest request);

    @GET("/api/health")
    Call<Void> healthCheck();
}
