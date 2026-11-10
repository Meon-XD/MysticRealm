package com.mysticrealm;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class SupabaseAuth {

    public static class Session {
        public String accessToken;
        public String refreshToken;
        public String userId;
        public String username;
        public long expiresAt;
    }

    private SupabaseAuth() {
    }

    public static String normalizeUsername(String value) throws Exception {
        String name = value == null ? "" : value.trim().toLowerCase();
        if (!name.matches("[a-z0-9_]{3,16}")) {
            throw new Exception("Username 3-16 huruf, angka, atau _.");
        }
        return name;
    }

    public static String emailFor(String username) {
        return username + "@" + SupabaseConfig.EMAIL_DOMAIN;
    }

    public static Session signIn(String username, String password) throws Exception {
        String name = normalizeUsername(username);
        if (password == null || password.length() < 6) {
            throw new Exception("Password minimal 6 karakter.");
        }
        JSONObject body = new JSONObject();
        body.put("email", emailFor(name));
        body.put("password", password);
        JSONObject result = request(SupabaseConfig.authUrl("token?grant_type=password"), body, null);
        return session(result, name);
    }

    public static Session signUp(String username, String password) throws Exception {
        String name = normalizeUsername(username);
        if (password == null || password.length() < 6) {
            throw new Exception("Password minimal 6 karakter.");
        }
        JSONObject meta = new JSONObject();
        meta.put("username", name);
        JSONObject body = new JSONObject();
        body.put("email", emailFor(name));
        body.put("password", password);
        body.put("data", meta);
        JSONObject result = request(SupabaseConfig.authUrl("signup"), body, null);
        if (result.optString("access_token", "").length() == 0) {
            throw new Exception("Konfirmasi email masih aktif. Matikan di Supabase.");
        }
        return session(result, name);
    }

    public static Session refresh(Session old) throws Exception {
        if (old == null || old.refreshToken == null || old.refreshToken.length() == 0) {
            throw new Exception("Sesi login habis. Silakan login ulang.");
        }
        JSONObject body = new JSONObject();
        body.put("refresh_token", old.refreshToken);
        JSONObject result = request(SupabaseConfig.authUrl("token?grant_type=refresh_token"), body, null);
        return session(result, old.username);
    }

    public static JSONObject rpc(String name, JSONObject args, String accessToken) throws Exception {
        return request(SupabaseConfig.rpcUrl(name), args == null ? new JSONObject() : args, accessToken);
    }

    private static Session session(JSONObject result, String fallbackName) throws Exception {
        String access = result.optString("access_token", "");
        if (access.length() == 0) {
            throw new Exception("Login gagal: token tidak diterima.");
        }
        Session session = new Session();
        session.accessToken = access;
        session.refreshToken = result.optString("refresh_token", "");
        session.expiresAt = System.currentTimeMillis() + result.optLong("expires_in", 3600L) * 1000L;
        JSONObject user = result.optJSONObject("user");
        if (user != null) {
            session.userId = user.optString("id", "");
            JSONObject meta = user.optJSONObject("user_metadata");
            String name = meta == null ? "" : meta.optString("username", "");
            session.username = name.length() == 0 ? fallbackName : name;
        } else {
            session.username = fallbackName;
        }
        if (session.userId == null || session.userId.length() == 0) {
            throw new Exception("Login gagal: user tidak valid.");
        }
        return session;
    }

    private static JSONObject request(String url, JSONObject body, String accessToken) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("apikey", SupabaseConfig.ANON_KEY);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            if (accessToken != null && accessToken.length() > 0) {
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            }
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            OutputStream out = connection.getOutputStream();
            out.write(bytes);
            out.flush();
            out.close();

            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String text = read(stream);
            JSONObject json = text.length() == 0 ? new JSONObject() : new JSONObject(text);
            if (status >= 400) {
                throw new Exception(friendly(json, status));
            }
            return json;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    private static String friendly(JSONObject json, int status) {
        String msg = json.optString("error_description", "");
        if (msg.length() == 0) msg = json.optString("msg", "");
        if (msg.length() == 0) msg = json.optString("message", "");
        if (msg.length() == 0) msg = json.optString("error", "");
        String lower = msg.toLowerCase();
        if (lower.contains("invalid login credentials")) return "Username atau password salah.";
        if (lower.contains("user already registered")) return "Username sudah dipakai.";
        if (lower.contains("email not confirmed")) return "Konfirmasi email masih aktif. Matikan di Supabase.";
        if (lower.contains("password should be at least")) return "Password minimal 6 karakter.";
        if (lower.contains("rate limit")) return "Terlalu banyak percobaan. Coba lagi nanti.";
        if (lower.contains("room_not_found")) return "Room tidak ditemukan.";
        if (lower.contains("room_full")) return "Room sudah penuh.";
        if (lower.contains("room_not_waiting")) return "Room sudah dimulai.";
        if (lower.contains("not_host")) return "Hanya host yang bisa memulai.";
        if (lower.contains("auth_required")) return "Silakan login ulang.";
        if (msg.length() > 0) return msg;
        return "Permintaan gagal (" + status + ").";
    }
}
