package com.mysticrealm;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class LanCoop {
    public static final int PORT = 45877;

    public static class Remote {
        public String id = "";
        public String name = "";
        public int classIndex;
        public int level = 1;
        public float x, y, dir, hp = 1f, maxHp = 1f;
        public boolean moving;
        public int stage = -1;
        public long lastSeen;
        public boolean self;
    }

    public volatile boolean host;
    public volatile boolean connected;
    public volatile String error;
    public volatile String localIp = "0.0.0.0";
    public volatile String remoteName = "";
    public volatile int remoteClass;
    public volatile int remoteLevel = 1;
    public volatile float remoteX, remoteY, remoteDir, remoteHp = 1f, remoteMaxHp = 1f;
    public volatile boolean remoteMoving;
    public volatile int remoteStage = -1;

    protected final SaveData save;
    private volatile ServerSocket server;
    private volatile Socket socket;
    private volatile PrintWriter out;
    private volatile Thread hostThread;
    private volatile Thread joinThread;
    private volatile Thread readThread;
    protected volatile boolean running;
    protected volatile int stageRequest = -1;
    protected volatile int localStage = -1;
    protected volatile long lastStateAt;

    public LanCoop(SaveData save) {
        this.save = save;
    }

    public boolean isHost() {
        return host;
    }

    public boolean isConnected() {
        return connected;
    }

    public List<Remote> remotes() {
        ArrayList<Remote> list = new ArrayList<>();
        if (connected) {
            Remote r = new Remote();
            r.id = "lan";
            r.name = remoteName;
            r.classIndex = remoteClass;
            r.level = remoteLevel;
            r.x = remoteX;
            r.y = remoteY;
            r.dir = remoteDir;
            r.hp = remoteHp;
            r.maxHp = remoteMaxHp;
            r.moving = remoteMoving;
            r.stage = remoteStage;
            r.lastSeen = System.currentTimeMillis();
            list.add(r);
        }
        return list;
    }

    public synchronized int takeStageRequest() {
        int s = stageRequest;
        stageRequest = -1;
        return s;
    }

    public void host() {
        if (running) return;
        host = true;
        running = true;
        connected = false;
        error = null;
        localStage = -1;
        hostThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    localIp = findLocalIp();
                    ServerSocket ss = new ServerSocket();
                    ss.setReuseAddress(true);
                    ss.bind(new InetSocketAddress(PORT));
                    server = ss;
                    Socket s = ss.accept();
                    if (!running) {
                        s.close();
                        return;
                    }
                    attach(s);
                } catch (Exception e) {
                    if (running) error = "Host gagal: " + shortError(e);
                }
            }
        }, "lan-host");
        hostThread.setDaemon(true);
        hostThread.start();
    }

    public void join(final String ip) {
        if (running) return;
        host = false;
        running = true;
        connected = false;
        error = null;
        localStage = -1;
        joinThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket s = new Socket();
                    s.connect(new InetSocketAddress(ip.trim(), PORT), 5000);
                    if (!running) {
                        s.close();
                        return;
                    }
                    attach(s);
                } catch (Exception e) {
                    if (running) error = "Gabung gagal: " + shortError(e);
                }
            }
        }, "lan-join");
        joinThread.setDaemon(true);
        joinThread.start();
    }

    private void attach(Socket s) throws Exception {
        if (!running) {
            s.close();
            return;
        }
        socket = s;
        s.setTcpNoDelay(true);
        out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
        connected = true;
        send("HELLO|" + safeName() + "|" + save.classIndex + "|" + save.level);
        readThread = new Thread(new Reader(in), "lan-read");
        readThread.setDaemon(true);
        readThread.start();
    }

    private class Reader implements Runnable {
        private final BufferedReader in;

        Reader(BufferedReader in) {
            this.in = in;
        }

        @Override
        public void run() {
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    handle(line);
                }
            } catch (Exception ignored) {
            }
            if (running) {
                connected = false;
                if (error == null) error = "Koneksi terputus.";
            }
        }
    }

    private void handle(String line) {
        String[] p = line.split("\\|");
        if (p.length == 0) return;
        if ("HELLO".equals(p[0])) {
            if (p.length > 1) remoteName = cleanName(p[1]);
            if (p.length > 2) remoteClass = Gfx.clamp(parseInt(p[2], 0), 0, 2);
            if (p.length > 3) remoteLevel = Math.max(1, parseInt(p[3], 1));
        } else if ("STAGE".equals(p[0])) {
            int s = parseInt(p.length > 1 ? p[1] : "-1", -1);
            if (s >= 0 && s < 25) {
                remoteStage = s;
                synchronized (this) {
                    stageRequest = s;
                }
            }
        } else if ("STATE".equals(p[0]) && p.length >= 9) {
            remoteX = parseFloat(p[1], remoteX);
            remoteY = parseFloat(p[2], remoteY);
            remoteDir = parseFloat(p[3], remoteDir);
            remoteHp = parseFloat(p[4], remoteHp);
            remoteMaxHp = Math.max(1f, parseFloat(p[5], remoteMaxHp));
            remoteClass = Gfx.clamp(parseInt(p[6], remoteClass), 0, 2);
            remoteMoving = "1".equals(p[7]);
            if (p.length > 8) remoteStage = parseInt(p[8], remoteStage);
        } else if ("BYE".equals(p[0])) {
            connected = false;
            error = "Partner keluar dari permainan.";
        }
    }

    public void sendStage(int stage) {
        localStage = stage;
        send("STAGE|" + stage);
    }

    public void setLocalStage(int stage) {
        localStage = stage;
    }

    public void sendState(Player player) {
        if (!connected || out == null) return;
        long now = System.currentTimeMillis();
        if (now - lastStateAt < 70) return;
        lastStateAt = now;
        send("STATE|" + f(player.x) + "|" + f(player.y) + "|" + f(player.dir) + "|"
                + f(player.hp) + "|" + f(player.maxHp) + "|" + save.classIndex + "|"
                + (player.moving ? 1 : 0) + "|" + localStage);
    }

    public void close() {
        running = false;
        connected = false;
        send("BYE");
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {
        }
        try {
            if (server != null) server.close();
        } catch (Exception ignored) {
        }
        socket = null;
        server = null;
        out = null;
        host = false;
    }

    private void send(String s) {
        PrintWriter w = out;
        if (w == null) return;
        try {
            w.println(s);
        } catch (Exception ignored) {
        }
    }

    private String safeName() {
        String n = save.playerName == null ? "Pemain" : save.playerName;
        return cleanName(n);
    }

    protected static String cleanName(String n) {
        if (n == null) return "Pemain";
        n = n.replace('|', ' ').replace('\n', ' ').replace('\r', ' ').trim();
        if (n.length() == 0) return "Pemain";
        return n.length() > 16 ? n.substring(0, 16) : n;
    }

    private static String f(float v) {
        return String.format(java.util.Locale.US, "%.2f", v);
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static float parseFloat(String s, float def) {
        try {
            return Float.parseFloat(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static String shortError(Exception e) {
        String m = e.getMessage();
        if (m == null || m.length() == 0) m = e.getClass().getSimpleName();
        return m.length() > 54 ? m.substring(0, 54) : m;
    }

    private static String findLocalIp() {
        String fallback = null;
        try {
            Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(ifs)) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && !a.isLoopbackAddress()) {
                        String ip = a.getHostAddress();
                        if (ip == null || ip.startsWith("127.")) continue;
                        if (isPrivate(ip)) return ip;
                        if (fallback == null) fallback = ip;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return fallback == null ? "0.0.0.0" : fallback;
    }

    private static boolean isPrivate(String ip) {
        if (ip.startsWith("10.") || ip.startsWith("192.168.")) return true;
        if (!ip.startsWith("172.")) return false;
        int dot = ip.indexOf('.', 4);
        if (dot < 0) return false;
        try {
            int second = Integer.parseInt(ip.substring(4, dot));
            return second >= 16 && second <= 31;
        } catch (Exception e) {
            return false;
        }
    }
}
