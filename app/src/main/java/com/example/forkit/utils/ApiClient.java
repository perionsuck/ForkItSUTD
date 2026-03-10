package com.example.forkit.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "https://api.api-ninjas.com/";

    public static final String API_KEY = "SyRfqJfSSO2beMKmlkpwC2MvVLzJNzLUYxkYzDdj";

    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {
            Gson gson = new GsonBuilder().setLenient().create();
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    public static CaloriesNinjaApi getApi() {
        return getClient().create(CaloriesNinjaApi.class);
    }
}