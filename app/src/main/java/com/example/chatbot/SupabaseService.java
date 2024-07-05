package com.example.chatbot;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SupabaseService {
    @GET("account")
    Call<List<YourDataModel>> getData();

    @GET("transactions")
    Call<List<TransactionDataModel>> getTransactionData();
}
