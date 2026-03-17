// utils/SupabaseClient.java
package com.example.forkit.utils;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {

    // TODO: move these to BuildConfig or a secrets file — never hardcode in production
    private static final String SUPABASE_URL = "https://fbbfaymfxetiwgfkrkxw.supabase.co/rest/v1/";
    public static final String ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZiYmZheW1meGV0aXdnZmtya3h3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzMwNTYxNTksImV4cCI6MjA4ODYzMjE1OX0.tIuYcHqTrmfxxiVPG8DFSETPf6TxK6Odwsvl9Ag1ukg";

    private static Retrofit retrofit;
    private static String accessToken = ANON_KEY;

    public static void setAccessToken(String token) {
        accessToken = token;
        retrofit = null;
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> chain.proceed(
                            chain.request().newBuilder()
                                    .addHeader("apikey", ANON_KEY)
                                    .addHeader("Authorization", "Bearer " + accessToken)
                                    .addHeader("Content-Type", "application/json")
                                    .addHeader("Prefer", "return=representation")
                                    .build()
                    ))
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(SUPABASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}