package com.owlmaddie.chat;

import com.owlmaddie.utils.Randomizer;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

public class MessageData {
    public String userLanguage;
    public String userMessage;
    public boolean is_auto_message;
    public ServerPlayerEntity player;


    public MessageData(String userLanguage, ServerPlayerEntity player, String userMessage,
            boolean is_auto_message) {
        this.is_auto_message = is_auto_message;
        this.userLanguage = userLanguage;
        this.player = player;
        this.userMessage = userMessage;
    }

    public static MessageData genCharacterAndOrGreetingMessage(String userLanguage, ServerPlayerEntity player, Entity entity){
        // make sure greeting is first in queue:
        String randomAdjective = Randomizer.getRandomMessage(Randomizer.RandomType.ADJECTIVE);
        String randomClass = Randomizer.getRandomMessage(Randomizer.RandomType.CLASS);
        String randomAlignment = Randomizer.getRandomMessage(Randomizer.RandomType.ALIGNMENT);
        String randomSpeakingStyle = Randomizer.getRandomMessage(Randomizer.RandomType.SPEAKING_STYLE);

        // Generate random name parameters
        String randomLetter = Randomizer.RandomLetter();
        int randomSyllables = Randomizer.RandomNumber(5) + 1;

        // Build the message
        StringBuilder userMessageBuilder = new StringBuilder();
        userMessageBuilder.append("Please generate a ").append(randomAdjective).append(" character. ");
        userMessageBuilder.append("This character is a ").append(randomClass).append(" class, who is ")
                .append(randomAlignment).append(". ");
        if (entity.getCustomName() != null && !entity.getCustomName().getString().equals("N/A")) {
            userMessageBuilder.append("Their name is '").append(entity.getCustomName().getString()).append("'. ");
        } else {
            userMessageBuilder.append("Their name starts with the letter '").append(randomLetter)
                    .append("' and is ").append(randomSyllables).append(" syllables long. ");
        }
        userMessageBuilder.append("They speak in '").append(userLanguage).append("' with a ")
                .append(randomSpeakingStyle).append(" style.");
        return new MessageData(userLanguage, player, userMessageBuilder.toString(), false);
    }
}
