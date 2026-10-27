/*
 * Copyright (c) 2026 Pedro Pathing
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.aaravlabs.safepedropathing.config;

public interface Modifier {
    void apply();

    void revert();
}
