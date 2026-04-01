package com.example.forkit.utils;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiApi {

    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    Call<GeminiResponse> generateContent(@Query("key") String apiKey, @Body GeminiRequest body);

    class GeminiRequest {
        public Content[] contents;
        public GenerationConfig generationConfig;

        public GeminiRequest(Content[] contents, GenerationConfig config) {
            this.contents = contents;
            this.generationConfig = config;
        }

        public static class Content {
            public Part[] parts;
        }

        public static class Part {
            public String text;
            public InlineData inlineData;

            public Part(String text) {
                this.text = text;
            }

            public Part(InlineData inlineData) {
                this.inlineData = inlineData;
            }

            public static class InlineData {
                public String mimeType;
                public String data;
            }
        }

        public static class GenerationConfig {
            public String responseMimeType = "application/json";
        }
    }

    class GeminiResponse {
        public Candidate[] candidates;
    }

    class Candidate {
        public Content2 content;
    }

    class Content2 {
        public Part2[] parts;
    }

    class Part2 {
        public String text;
    }
}
