package com.example.forkit.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "https://api.calorieninjas.com/";
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/";

    public static final String API_KEY = "Qhk2BNrnlmg5MgY3wE7Q/A==66vtMH5kwHDTwGjX";

    public static final String GEMINI_API_KEY = "AIzaSyDxoiZwsAPZYDC9jYJl9372yNp7puu0caY";

    private static Retrofit retrofit;
    private static Retrofit geminiRetrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {
            Gson gson = new GsonBuilder().setLenient().create();
            OkHttpClient ok = new OkHttpClient.Builder()
                    .connectTimeout(java.time.Duration.ofSeconds(12))
                    .readTimeout(java.time.Duration.ofSeconds(12))
                    .writeTimeout(java.time.Duration.ofSeconds(12))
                    .build();
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(ok)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    public static Retrofit getGeminiClient() {
        if (geminiRetrofit == null) {
            Gson gson = new GsonBuilder().setLenient().create();
            OkHttpClient ok = new OkHttpClient.Builder()
                    .connectTimeout(java.time.Duration.ofSeconds(15))
                    .readTimeout(java.time.Duration.ofSeconds(30))
                    .writeTimeout(java.time.Duration.ofSeconds(15))
                    .build();
            geminiRetrofit = new Retrofit.Builder()
                    .baseUrl(GEMINI_BASE_URL)
                    .client(ok)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return geminiRetrofit;
    }

    public static CaloriesNinjaApi getApi() { return getClient().create(CaloriesNinjaApi.class); }

    public static GeminiApi getGeminiApi() { return getGeminiClient().create(GeminiApi.class); }
}
