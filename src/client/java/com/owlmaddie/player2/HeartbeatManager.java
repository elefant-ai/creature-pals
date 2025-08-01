package com.owlmaddie.player2;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HeartbeatManager {
    public static final ExecutorService heartbeatManager = Executors.newSingleThreadExecutor();
    public static long lastHeartbeatTime = System.nanoTime();

    public static void sendHeartbeat() {
        heartbeatManager.submit(() -> {
            Player2APIService.sendHeartbeat();
        });
    }

    // inject into ontick
    public static void injectIntoOnTick() {
        long now = System.nanoTime();
        // every 60 seconds send heartbeat
        if (now - lastHeartbeatTime > 60_000_000_000L) {
            sendHeartbeat();
            lastHeartbeatTime = now;
        }
    }
}