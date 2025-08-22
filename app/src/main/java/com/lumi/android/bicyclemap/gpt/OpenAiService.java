package com.lumi.android.bicyclemap.gpt;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface OpenAiService {
    @POST("v1/chat/completions")
    Call<ChatCompletionResponse> createChatCompletion(@Body ChatCompletionRequest body);
}
