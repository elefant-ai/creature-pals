// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later

package com.owlmaddie.utils;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.21.5+: use getEquippedStack
 */
public class ArmorHelper {
    public static ItemStack getArmor(Player player, EquipmentSlot slot) {
        return player.getItemBySlot(slot);
    }
}
