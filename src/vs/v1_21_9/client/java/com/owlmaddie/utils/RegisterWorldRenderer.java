// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later

package com.owlmaddie.utils;

import java.util.function.Consumer;

/**
 * No-op implementation for MC 1.21.9.
 * Fabric API removed WorldRenderEvents in 1.21.9, so rendering is handled by mixin.
 * This class exists to satisfy the compile-time dependency from ClientInit.
 */
public class RegisterWorldRenderer {

    public static void register(Consumer<Object> event) {
        // No-op: Rendering is handled by WorldRenderMixin in 1.21.9
    }
}
