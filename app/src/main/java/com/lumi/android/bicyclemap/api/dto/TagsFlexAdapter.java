package com.lumi.android.bicyclemap.api.dto;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.*;

public class TagsFlexAdapter implements JsonDeserializer<List<String>> {

    @Override
    public List<String> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
            throws JsonParseException {

        List<String> out = new ArrayList<>();

        if (json == null || json.isJsonNull()) return out;

        // 배열인 경우: ["A","B"] 또는 [{"k":"A"}, {"x":"B"}] 모두 처리
        if (json.isJsonArray()) {
            JsonArray arr = json.getAsJsonArray();
            for (JsonElement el : arr) {
                collectStrings(el, out);
            }
            return out;
        }

        // 단일 문자열인 경우
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            out.add(json.getAsString());
            return out;
        }

        // 객체인 경우: 모든 값(value)을 문자열로 추출
        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                collectStrings(e.getValue(), out);
            }
            return out;
        }

        // 그 외 타입은 무시
        return out;
    }

    /** JsonElement에서 문자열 후보를 out에 수집 */
    private void collectStrings(JsonElement el, List<String> out) {
        if (el == null || el.isJsonNull()) return;

        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
            String v = el.getAsString();
            if (v != null && !v.isEmpty()) out.add(v);
            return;
        }

        if (el.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet()) {
                collectStrings(e.getValue(), out);
            }
            return;
        }

        if (el.isJsonArray()) {
            for (JsonElement sub : el.getAsJsonArray()) {
                collectStrings(sub, out);
            }
        }
    }
}
