// utils/SupabaseClient.java
package com.example.forkit.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {

    private static final String TAG = "SupabaseClient";

    // TODO: move these to BuildConfig or a secrets file — never hardcode in production
    private static final String SUPABASE_HOST = "https://fbbfaymfxetiwgfkrkxw.supabase.co";
    private static final String REST_BASE = SUPABASE_HOST + "/rest/v1/";
    public static final String ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZiYmZheW1meGV0aXdnZmtya3h3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzMwNTYxNTksImV4cCI6MjA4ODYzMjE1OX0.tIuYcHqTrmfxxiVPG8DFSETPf6TxK6Odwsvl9Ag1ukg";

    private static final String PREFS = "forkit_prefs";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static Retrofit retrofit;
    private static volatile String accessToken = ANON_KEY;
    private static Context appContext;

    public static void initApplicationContext(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }

    public static void setAccessToken(String token) {
        accessToken = token != null ? token : ANON_KEY;
        retrofit = null;
    }

    /**
     * Updates in-memory JWT after a refresh without rebuilding Retrofit (OkHttp will retry with new header).
     */
    private static void updateAccessTokenMemory(String token) {
        accessToken = token != null ? token : ANON_KEY;
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> chain.proceed(
                            chain.request().newBuilder()
                                    .addHeader("apikey", ANON_KEY)
                                    .addHeader("Authorization", "Bearer " + accessToken)
                                    .addHeader("Content-Type", "application/json")
                                    .addHeader("Accept", "application/json")
                                    .addHeader("Prefer", "return=representation")
                                    .build()
                    ))
                    .authenticator((route, response) -> {
                        if (response.priorResponse() != null) {
                            return null;
                        }
                        if (response.code() != 401) {
                            return null;
                        }
                        Request req = response.request();
                        if (!req.url().encodedPath().contains("/rest/v1/")) {
                            return null;
                        }
                        if (ANON_KEY.equals(accessToken)) {
                            return null;
                        }
                        if (!refreshSessionWithStoredToken()) {
                            return null;
                        }
                        return req.newBuilder()
                                .header("Authorization", "Bearer " + accessToken)
                                .build();
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(REST_BASE)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    /**
     * Exchanges refresh_token for new access_token (and possibly rotated refresh_token).
     * Called from OkHttp Authenticator on 401 JWT expired (PGRST303).
     */
    private static synchronized boolean refreshSessionWithStoredToken() {
        Context ctx = appContext;
        if (ctx == null) {
            Log.w(TAG, "refresh skipped: app context not set");
            return false;
        }
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String refresh = prefs.getString(PrefsHelper.KEY_REFRESH_TOKEN, null);
        if (refresh == null || refresh.isEmpty()) {
            Log.w(TAG, "refresh skipped: no refresh_token in prefs (user must sign in again)");
            return false;
        }

        OkHttpClient http = new OkHttpClient();
        try {
            JSONObject body = new JSONObject();
            body.put("refresh_token", refresh);
            Request request = new Request.Builder()
                    .url(SUPABASE_HOST + "/auth/v1/token?grant_type=refresh_token")
                    .post(RequestBody.create(body.toString(), JSON))
                    .addHeader("apikey", ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response resp = http.newCall(request).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                if (!resp.isSuccessful()) {
                    Log.e(TAG, "refresh failed HTTP " + resp.code() + " " + respBody);
                    return false;
                }
                JSONObject json = new JSONObject(respBody);
                String newAccess = json.getString("access_token");
                String newRefresh = json.optString("refresh_token", refresh);
                updateAccessTokenMemory(newAccess);
                SharedPreferences.Editor ed = prefs.edit();
                ed.putString("access_token", newAccess);
                if (!newRefresh.isEmpty()) {
                    ed.putString(PrefsHelper.KEY_REFRESH_TOKEN, newRefresh);
                }
                ed.apply();
                Log.i(TAG, "session refreshed");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "refresh error: " + e.getMessage(), e);
            return false;
        }
    }
}
