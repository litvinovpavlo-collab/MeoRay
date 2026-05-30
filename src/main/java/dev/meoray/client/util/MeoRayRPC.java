package dev.meoray.client.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MeoRayRPC {
    private static final String CLIENT_ID = "1507094068755431524";

    private static RandomAccessFile pipe;
    private static boolean connected = false;
    private static long startTime;

    public static void start() {
        startTime = System.currentTimeMillis() / 1000L;

        Thread t = new Thread(() -> {
            try {
                if (!connect()) {
                    System.out.println("[Nova RPC] Could not find Discord pipe");
                    return;
                }
                handshake();
                connected = true;
                System.out.println("[Nova RPC] Connected to Discord, sending initial activity...");
                Thread.sleep(500);
                update("Just launched", "Idle in menu");
            } catch (Exception e) {
                System.err.println("[Nova RPC] Failed: " + e.getMessage());
                e.printStackTrace();
            }
        }, "Nova-RPC-Thread");
        t.setDaemon(true);
        t.start();
    }

    private static boolean connect() {
        for (int i = 0; i < 10; i++) {
            try {
                String path = "\\\\?\\pipe\\discord-ipc-" + i;
                pipe = new RandomAccessFile(path, "rw");
                System.out.println("[Nova RPC] Connected to pipe: discord-ipc-" + i);
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static void handshake() throws IOException {
        JsonObject obj = new JsonObject();
        obj.addProperty("v", 1);
        obj.addProperty("client_id", CLIENT_ID);
        System.out.println("[Nova RPC] Sending handshake: " + obj);
        send(0, obj.toString());
        String response = read();
        System.out.println("[Nova RPC] Handshake response: " + response);
    }

    public static void update(String details, String state) {
        if (!connected || pipe == null) {
            System.out.println("[Nova RPC] update skipped: connected=" + connected);
            return;
        }

        try {
            JsonObject activity = new JsonObject();
            activity.addProperty("details", details);
            activity.addProperty("state", state);

            JsonObject timestamps = new JsonObject();
            timestamps.addProperty("start", startTime);
            activity.add("timestamps", timestamps);

            JsonArray buttons = new JsonArray();
            JsonObject button = new JsonObject();
            button.addProperty("label", "Join Discord");
            button.addProperty("url", "https://discord.gg/TsfsrS45tG");
            buttons.add(button);
            activity.add("buttons", buttons);

            JsonObject args = new JsonObject();
            args.addProperty("pid", ProcessHandle.current().pid());
            args.add("activity", activity);

            JsonObject payload = new JsonObject();
            payload.addProperty("cmd", "SET_ACTIVITY");
            payload.add("args", args);
            payload.addProperty("nonce", UUID.randomUUID().toString());

            System.out.println("[Nova RPC] Sending activity: " + payload);
            send(1, payload.toString());
            String response = read();
            System.out.println("[Nova RPC] Activity response: " + response);
        } catch (Exception e) {
            System.err.println("[Nova RPC] Update failed: " + e.getMessage());
            e.printStackTrace();
            connected = false;
        }
    }

    private static void send(int op, String data) throws IOException {
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(8 + bytes.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(op);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
        pipe.write(buffer.array());
    }

    private static String read() throws IOException {
        byte[] header = new byte[8];
        pipe.readFully(header);
        ByteBuffer buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        buf.getInt();
        int len = buf.getInt();
        byte[] data = new byte[len];
        pipe.readFully(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    public static void stop() {
        if (pipe != null) {
            try { pipe.close(); } catch (Exception ignored) {}
        }
        connected = false;
    }

    public static boolean isConnected() {
        return connected;
    }
}
