package com.example.yugiohdeckbuilder.api;
import com.example.yugiohdeckbuilder.model.ApiResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {
    @GET("api/v7/cardinfo.php")
    Call<ApiResponse> getCardsByLanguage(
            @Query("type") String cardType,
            @Query("language") String lang
    );

    @GET("api/v7/cardinfo.php")
    Call<ApiResponse> getCardsInEnglish(@Query("type") String cardType);
}