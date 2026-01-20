// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.inventory;

import net.minecraft.world.Container;

/**
 * Interface for entities that hold a CreaturePals inventory.
 */
public interface ChatInventory {
    /**
     * Returns the inventory associated with this entity.
     *
     * @return the {@link Container} for this entity
     */
    Container creaturepals$getInventory();
}
