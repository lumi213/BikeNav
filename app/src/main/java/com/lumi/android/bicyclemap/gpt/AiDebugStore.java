package com.lumi.android.bicyclemap.gpt;

public class AiDebugStore {
    public static volatile String lastModel = "";
    public static volatile String lastSystem = "";
    public static volatile String lastUser = "";
    public static volatile String lastResponse = "";
    public static volatile int lastHttpCode = -1;
    public static volatile long lastSentAt = 0L;
}
