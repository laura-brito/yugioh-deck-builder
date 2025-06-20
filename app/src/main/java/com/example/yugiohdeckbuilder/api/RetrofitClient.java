package com.example.yugiohdeckbuilder.api;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static final String BASE_URL = "https://db.ygoprodeck.com/";

    // Método singleton para garantir uma única instância do Retrofit
    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create()) // Usa Gson para converter JSON
                    .build();
        }
        return retrofit;
    }
}