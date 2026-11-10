package com.mysticrealm;

import android.util.Base64;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.concurrent.LinkedBlockingQueue;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class RealtimeClient {

    public interface Listener {
        void onConnected();

        void onEvent(String event, JSONObject payload);

        void onDisconnected(String error);
    }

    private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private final String url;
    private final Listener listener;
    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private final SecureRandom random = new SecureRandom();

    private volatile boolean running;
    private volatile boolean notified;
    private volatile Socket socket;
    private volatile OutputStream output;

    public RealtimeClient(String url, Listener listener) {
        this.url = url;
        this.listener = listener;
    }

    public void connect() {
        if (running) return;
        running = true;
        notified = false;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                open();
            }
        }, "realtime-connect");
        thread.setDaemon(true);
        thread.start();
    }

    public void send(String text) {
        if (running && text != null) queue.offer(text);
    }

    public void close() {
        running = false;
        try {
            writeFrame(8, new byte[0]);
        } catch (Exception ignored) {
        }
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {
        }
        socket = null;
        output = null;
    }

    private void open() {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme() == null ? "ws" : uri.getScheme().toLowerCase();
            boolean tls = "wss".equals(scheme) || "https".equals(scheme);
            String host = uri.getHost();
            int port = uri.getPort();
            if (port < 0) port = tls ? 443 : 80;
            String path = uri.getRawPath();
            if (path == null || path.length() == 0) path = "/";
            if (uri.getRawQuery() != null) path += "?" + uri.getRawQuery();

            Socket raw = new Socket();
            raw.connect(new InetSocketAddress(host, port), 12000);
            raw.setTcpNoDelay(true);
            Socket active = raw;
            if (tls) {
                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                SSLSocket ssl = (SSLSocket) factory.createSocket(raw, host, port, true);
                ssl.startHandshake();
                if (!HttpsURLConnection.getDefaultHostnameVerifier().verify(host, ssl.getSession())) {
                    ssl.close();
                    throw new Exception("Sertifikat server tidak valid.");
                }
                active = ssl;
            }
            socket = active;
            output = active.getOutputStream();

            String key = key();
            String request = "GET " + path + " HTTP/1.1\r\n"
                    + "Host: " + host + "\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Key: " + key + "\r\n"
                    + "Sec-WebSocket-Version: 13\r\n"
                    + "User-Agent: MysticRealm\r\n\r\n";
            output.write(request.getBytes("UTF-8"));
            output.flush();

            BufferedInputStream input = new BufferedInputStream(active.getInputStream());
            String status = readLine(input);
            if (status == null || !status.contains(" 101 ")) {
                throw new Exception("Realtime ditolak: " + (status == null ? "tanpa respons" : status));
            }
            String accept = null;
            String line;
            while ((line = readLine(input)) != null && line.length() > 0) {
                int colon = line.indexOf(':');
                if (colon > 0 && "sec-websocket-accept".equalsIgnoreCase(line.substring(0, colon).trim())) {
                    accept = line.substring(colon + 1).trim();
                }
            }
            if (accept != null && !accept.equals(accept(key))) {
                throw new Exception("Realtime handshake tidak valid.");
            }

            startWriter();
            startHeartbeat();
            if (listener != null) listener.onConnected();
            readLoop(input);
        } catch (Exception e) {
            if (running) fail(shortError(e));
        }
    }

    private void readLoop(InputStream input) throws Exception {
        ByteArrayOutputStream message = null;
        int messageOpcode = 0;
        while (running) {
            Frame frame = readFrame(input);
            if (frame == null) break;
            if (frame.opcode == 8) break;
            if (frame.opcode == 9) {
                writeFrame(10, frame.payload);
                continue;
            }
            if (frame.opcode == 10) continue;
            if (frame.opcode == 1 || frame.opcode == 2) {
                message = new ByteArrayOutputStream();
                messageOpcode = frame.opcode;
                message.write(frame.payload);
            } else if (frame.opcode == 0 && message != null) {
                message.write(frame.payload);
            } else {
                continue;
            }
            if (!frame.fin) continue;
            if (messageOpcode == 1) {
                String text = new String(message.toByteArray(), "UTF-8");
                JSONObject json = new JSONObject(text);
                String topic = json.optString("topic", "");
                if ("phoenix".equals(topic)) continue;
                String event = json.optString("event", "");
                JSONObject payload = json.optJSONObject("payload");
                if (listener != null && event.length() > 0) listener.onEvent(event, payload == null ? new JSONObject() : payload);
            }
            message = null;
        }
        if (running) fail("Koneksi realtime terputus.");
    }

    private void startWriter() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (running) {
                        String text = queue.take();
                        writeFrame(1, text.getBytes("UTF-8"));
                    }
                } catch (Exception ignored) {
                }
            }
        }, "realtime-write");
        thread.setDaemon(true);
        thread.start();
    }

    private void startHeartbeat() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (running) {
                        Thread.sleep(25000L);
                        if (running) {
                            JSONObject heartbeat = new JSONObject();
                            heartbeat.put("topic", "phoenix");
                            heartbeat.put("event", "heartbeat");
                            heartbeat.put("payload", new JSONObject());
                            heartbeat.put("ref", "hb");
                            send(heartbeat.toString());
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }, "realtime-heartbeat");
        thread.setDaemon(true);
        thread.start();
    }

    private void fail(String error) {
        running = false;
        if (notified) return;
        notified = true;
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {
        }
        if (listener != null) listener.onDisconnected(error);
    }

    private Frame readFrame(InputStream in) throws Exception {
        int b0 = in.read();
        int b1 = in.read();
        if (b0 < 0 || b1 < 0) return null;
        boolean fin = (b0 & 0x80) != 0;
        int opcode = b0 & 0x0F;
        boolean masked = (b1 & 0x80) != 0;
        long length = b1 & 0x7F;
        if (length == 126) {
            length = ((long) readByte(in) << 8) | readByte(in);
        } else if (length == 127) {
            length = 0;
            for (int i = 0; i < 8; i++) length = (length << 8) | readByte(in);
        }
        if (length < 0 || length > 4L * 1024L * 1024L) {
            throw new Exception("Payload realtime terlalu besar.");
        }
        byte[] mask = null;
        if (masked) {
            mask = new byte[4];
            readFully(in, mask);
        }
        byte[] payload = new byte[(int) length];
        readFully(in, payload);
        if (mask != null) {
            for (int i = 0; i < payload.length; i++) payload[i] ^= mask[i % 4];
        }
        return new Frame(fin, opcode, payload);
    }

    private synchronized void writeFrame(int opcode, byte[] payload) throws Exception {
        OutputStream out = output;
        if (out == null) return;
        int length = payload == null ? 0 : payload.length;
        out.write(0x80 | (opcode & 0x0F));
        if (length < 126) {
            out.write(0x80 | length);
        } else if (length <= 65535) {
            out.write(0x80 | 126);
            out.write((length >>> 8) & 0xFF);
            out.write(length & 0xFF);
        } else {
            out.write(0x80 | 127);
            long len = length;
            for (int i = 7; i >= 0; i--) out.write((int) ((len >>> (i * 8)) & 0xFF));
        }
        byte[] mask = new byte[4];
        random.nextBytes(mask);
        out.write(mask);
        for (int i = 0; i < length; i++) out.write(payload[i] ^ mask[i % 4]);
        out.flush();
    }

    private static void readFully(InputStream in, byte[] data) throws Exception {
        int offset = 0;
        while (offset < data.length) {
            int count = in.read(data, offset, data.length - offset);
            if (count < 0) throw new Exception("Koneksi realtime terputus.");
            offset += count;
        }
    }

    private static int readByte(InputStream in) throws Exception {
        int value = in.read();
        if (value < 0) throw new Exception("Koneksi realtime terputus.");
        return value;
    }

    private static String readLine(InputStream in) throws Exception {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int value;
        while ((value = in.read()) >= 0) {
            if (value == '\n') break;
            if (value != '\r') line.write(value);
        }
        if (value < 0 && line.size() == 0) return null;
        return new String(line.toByteArray(), "UTF-8");
    }

    private static String key() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private static String accept(String key) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        byte[] hash = sha.digest((key + GUID).getBytes("UTF-8"));
        return Base64.encodeToString(hash, Base64.NO_WRAP);
    }

    private static String shortError(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.length() == 0) msg = e.getClass().getSimpleName();
        return msg.length() > 90 ? msg.substring(0, 90) : msg;
    }

    private static class Frame {
        final boolean fin;
        final int opcode;
        final byte[] payload;

        Frame(boolean fin, int opcode, byte[] payload) {
            this.fin = fin;
            this.opcode = opcode;
            this.payload = payload;
        }
    }
}
