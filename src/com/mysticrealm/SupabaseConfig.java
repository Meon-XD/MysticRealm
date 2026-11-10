package com.mysticrealm;

public final class SupabaseConfig {
    public static final String URL = "https://dppyctxowfuoaoxfrdfa.supabase.co";
    public static final String ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRwcHljdHhvd2Z1b2FveGZyZGZhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTQzMjAyMTgsImV4cCI6MjEwOTg5NjIxOH0.ZmLvnBVl5knTAFPCLu-utLyMpPiCzP3sOaOkIH1IGDg";
    public static final String EMAIL_DOMAIN = "dppyctxowfuoaoxfrdfa.supabase.co";

    private SupabaseConfig() {
    }

    public static String authUrl(String path) {
        return URL + "/auth/v1/" + path;
    }

    public static String rpcUrl(String name) {
        return URL + "/rest/v1/rpc/" + name;
    }

    public static String realtimeUrl() {
        return URL.replace("https://", "wss://") + "/realtime/v1/websocket?apikey=" + ANON_KEY + "&vsn=1.0.0";
    }
}
