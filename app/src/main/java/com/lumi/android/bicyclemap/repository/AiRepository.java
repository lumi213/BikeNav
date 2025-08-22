package com.lumi.android.bicyclemap.repository;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lumi.android.bicyclemap.MainViewModel;
import com.lumi.android.bicyclemap.api.dto.CourseDto;
import com.lumi.android.bicyclemap.gpt.AiDebugStore;            // ✅ 추가
import com.lumi.android.bicyclemap.gpt.ChatCompletionRequest;
import com.lumi.android.bicyclemap.gpt.ChatCompletionResponse;
import com.lumi.android.bicyclemap.gpt.GptRouteRecommendation;
import com.lumi.android.bicyclemap.gpt.OpenAiClient;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiRepository {

    public interface CallbackResult {
        void onSuccess(GptRouteRecommendation result);
        void onError(String error);
    }

    private final Gson gson = new Gson();
    private final OpenAiClient client;

    public AiRepository(Context ctx){
        client = OpenAiClient.getInstance(ctx);
    }

    public void recommendWithGpt(List<CourseDto> allCourses,
                                 MainViewModel.RouteType routeType,
                                 MainViewModel.GroupType groupType,
                                 MainViewModel.Difficulty diff,
                                 CallbackResult cb){
        Log.d("AI_RECO", "recommendWithGpt() called: courses="
                + (allCourses==null?0:allCourses.size())
                + ", routeType=" + routeType + ", groupType=" + groupType + ", diff=" + diff);

        // ✅ NPE 방지 기본값
        if (routeType == null) routeType = MainViewModel.RouteType.BIKE;
        if (groupType == null) groupType = MainViewModel.GroupType.SMALL;
        if (diff == null)      diff      = MainViewModel.Difficulty.NORMAL;

        // 1) 코스 목록을 프롬프트용으로 슬림화
        List<PromptCourse> brief = slimCourses(allCourses, routeType);

        // 2) 사용자 선택 요약
        String choice = String.format("type=%s, group=%s, difficulty=%s",
                routeType== MainViewModel.RouteType.WALK ? "walk":"bike",
                groupType== MainViewModel.GroupType.LARGE ? "group":"small",
                diff.name().toLowerCase());

        // 3) 시스템/유저 메시지 구성 (JSON 강제)
        String system = "당신은 자전거·산책 코스 추천 어시스턴트입니다. " +
                "반드시 JSON만 반환하세요. 마크다운/설명/문장 없이 아래 스키마만:\n" +
                "{ \"recommendations\": [ { \"course_id\": number, \"reason\": string, \"estimated_time\": string } ] }";

        String user = "사용자 선택: " + choice + "\n" +
                "코스 목록(간략): " + gson.toJson(brief) + "\n" +
                "규칙: course_id는 반드시 위 목록의 id만 사용, 추천 최대 3개, reason은 1~2문장, " +
                "estimated_time은 대략적인 체감 소요(예: \"약 60분\").\n" +
                "JSON만 출력하세요.";

        // ✅ 전송 직전 디버그 스냅샷 저장 + 로그
        AiDebugStore.lastModel  = pickModel();
        AiDebugStore.lastSystem = system;
        AiDebugStore.lastUser   = user;
        AiDebugStore.lastSentAt = System.currentTimeMillis();

        Log.d("AI_REQ_PROMPT",
                    "model=" + AiDebugStore.lastModel +
                            "\n--- system ---\n" + system +
                            "\n--- user ---\n" + user);

        ChatCompletionRequest body = new ChatCompletionRequest();
        body.model = AiDebugStore.lastModel; // ✅ pickModel()과 동일한 값을 사용
        body.messages.add(new ChatCompletionRequest.Message("system", system));
        body.messages.add(new ChatCompletionRequest.Message("user", user));
        body.temperature = 0.2;
        body.max_tokens = 600;

        client.api().createChatCompletion(body).enqueue(new Callback<ChatCompletionResponse>() {
            @Override
            public void onResponse(Call<ChatCompletionResponse> call, Response<ChatCompletionResponse> res) {
                AiDebugStore.lastHttpCode = res.code(); // ✅ HTTP 코드 저장

                if (!res.isSuccessful()) {
                    String err = "";
                    try { err = res.errorBody() != null ? res.errorBody().string() : ""; } catch (Exception ignore) {}
                    String rid = res.headers() != null ? res.headers().get("x-request-id") : null;
                    Log.d("AI_RES_BODY", "code=" + res.code() + " rid=" + rid + "\n" + err);

                    // 429 → 과금/쿼터/속도 문제 가능성 높음
                    if (res.code() == 429 && (err == null || err.isEmpty())) {
                        cb.onError("429 (rate limit/quota). 대시보드의 Requests/Rate limits/Quota를 확인하세요.");
                    } else {
                        cb.onError(err.isEmpty() ? ("HTTP " + res.code()) : err);
                    }
                    return;
                }

                String content = res.body().choices.get(0).message.content;
                AiDebugStore.lastResponse = (content != null) ? content : "<null>";
                Log.d("AI_RES_BODY", "code=" + res.code() + "\n" + AiDebugStore.lastResponse);

                content = sanitizeJson(content); // ✅ 만약의 경우 코드펜스 제거
                try {
                    Type t = new TypeToken<GptRouteRecommendation>(){}.getType();
                    GptRouteRecommendation parsed = gson.fromJson(content, t);
                    if (parsed==null || parsed.recommendations==null) {
                        cb.onError("JSON 파싱 실패");
                    } else {
                        cb.onSuccess(parsed);
                    }
                } catch (Exception e){
                    Log.e("AI_PARSE", "error", e);
                    cb.onError("JSON 파싱 예외: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ChatCompletionResponse> call, Throwable t) {
                AiDebugStore.lastHttpCode = -1;                  // ✅ 저장
                AiDebugStore.lastResponse = "failure: " + t.getMessage();
                Log.e("AI_RES_FAIL", t.getMessage(), t);
                cb.onError(t.getMessage());
            }
        });
    }

    /* 코스 요약 구조체 */
    static class PromptCourse {
        int id;
        String title;
        Double dist_km;
        Integer time;
        Integer diff;
        String type;
        PromptCourse(int id, String title, Double dist_km, Integer time, Integer diff, String type){
            this.id=id; this.title=title; this.dist_km=dist_km; this.time=time; this.diff=diff; this.type=type;
        }
    }

    private List<PromptCourse> slimCourses(List<CourseDto> all, MainViewModel.RouteType rt){
        if (all==null) return new ArrayList<>();
        String want = (rt== MainViewModel.RouteType.WALK) ? "walk" : "bike";
        // 우선 type 매칭 + 상위 30개만
        return all.stream()
                .filter(c -> want.equalsIgnoreCase(c.getType()))
                .limit(30)
                .map(c -> new PromptCourse(
                        c.getCourse_id(),
                        safe(c.getTitle()),
                        c.getDist_km(),
                        c.getTime(),
                        c.getDiff(),
                        c.getType()
                )).collect(Collectors.toList());
    }

    // 모델 선택: values(map_api.xml) 우선, 없으면 기본값
    private String pickModel() {
        String m = client.getModel(); // OpenAiClient가 values에서 읽어둔 값
        return (m != null && !m.trim().isEmpty()) ? m : "gpt-4.1-mini";
    }

    // GPT가 ```json … ```로 감싸서 보낼 때 대비
    private String sanitizeJson(String s){
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("```")) {
            int first = t.indexOf('{');
            int last  = t.lastIndexOf('}');
            if (first >= 0 && last >= first) return t.substring(first, last+1);
        }
        return t;
    }

    private String safe(String s){ return s==null? "": s; }
}
