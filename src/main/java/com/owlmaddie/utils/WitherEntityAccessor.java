// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later

package com.owlmaddie.utils;

import net.minecraft.world.damagesource.DamageSource;

/**
 * Accessor interface for WitherEntity to allow calling dropEquipment
 * externally.
 */
public interface WitherEntityAccessor {
    void callDropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops);
}
