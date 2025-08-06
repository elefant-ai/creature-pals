package com.owlmaddie.Player2;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import com.google.gson.JsonObject;

public class TTS {
    static class PerEntityTTS {
        public String voiceId;
        public String lastMessage;
        public PerEntityTTS(String voiceId, String lastMessage) {
            this.voiceId = voiceId;
            this.lastMessage = lastMessage;
        }
    }
    public static final ExecutorService ttsThread = Executors.newSingleThreadExecutor();
    public static ConcurrentHashMap<UUID, PerEntityTTS> entityTTSData = new ConcurrentHashMap<>();
    public static List<String> englishVoices = null;
    public static boolean enabled = true;
    private static final Random random = new Random();

    public static int disableTTS(){
        enabled = false;
        return 1;
    }
    public static int enableTTS(){
        enabled = true;
        return 1;
    }
    public static void speak(String message, UUID entityId) {
        if(!enabled){
            return;
        }
        ttsThread.submit(() -> {
            entityTTSData.computeIfAbsent(entityId, id -> {
                if (englishVoices == null) {
                    List<JsonObject> voices = Player2APIService.getVoices();
                    englishVoices = voices.stream().filter(jsonV -> {
                        String language = jsonV.get("language").getAsString();
                        return language.equals("american_english") || language.equals("british_english");
                    }).map(jsonV -> {
                        return jsonV.get("id").getAsString();
                    }).toList();
                }
                int index = random.nextInt(englishVoices.size());
                PerEntityTTS data =  new PerEntityTTS(englishVoices.get(index), "");
                return data;
            });
            PerEntityTTS data = entityTTSData.get(entityId);
            if(data.lastMessage.equals(message)){
                return;
            }
            data.lastMessage = message;
            Player2APIService.textToSpeech(message, data.voiceId);
        });
    }

}