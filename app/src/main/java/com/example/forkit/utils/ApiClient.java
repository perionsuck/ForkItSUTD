package com.example.forkit.utils;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "https://api.api-ninjas.com/";

    public static final String API_KEY = "SyRfqJfSSO2beMKmlkpwC2MvVLzJNzLUYxkYzDdj";

    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static CaloriesNinjaApi getApi() {
        return getClient().create(CaloriesNinjaApi.class);
    }
}