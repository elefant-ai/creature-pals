// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later

package com.owlmaddie.network;

import net.minecraft.network.chat.ClickEvent;

public class ClickEventHelper {
    /** old API: construct the legacy ClickEvent */
    public static ClickEvent openUrl(String url) {
        return new ClickEvent(ClickEvent.Action.OPEN_URL, url);
    }
}
