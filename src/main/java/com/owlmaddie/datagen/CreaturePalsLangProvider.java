// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.datagen;

import com.owlmaddie.chat.Advancements;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.i18n.CCText;
import com.owlmaddie.utils.Randomizer;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

/**
 * Generates the English fallback language file.
 */
public class CreaturePalsLangProvider extends FabricLanguageProvider {
    public CreaturePalsLangProvider(FabricDataOutput output) {
        super(output, "creaturepals");
    }

    @Override
    public void generateTranslations(TranslationBuilder builder) {
        Map<String, String> en = new TreeMap<>();
        Stream.of(
                Randomizer.allErrorText(),
                Randomizer.allNoResponseText(),
                CCText.UI_TEXT.stream(),
                CCText.CONFIG_TEXT.stream(),
                EntityChatData.ERROR_MISC.stream(),
                EntityChatData.ERROR_SOLUTIONS.stream(),
                Advancements.allText()
        ).flatMap(s -> s).forEach(tr -> en.putIfAbsent(tr.key(), tr.en()));

        en.forEach(builder::add);
        LangSync.sync(en);
    }

    @Override
    public String getName() {
        return "CreaturePals Lang";
    }
}
