/*
 * Copyright (c) 2026 Pedro Pathing
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.aaravlabs.safepedropathing.algorithm;

import com.aaravlabs.safepedropathing.drivetrain.DrivePowers;
import com.aaravlabs.safepedropathing.drivetrain.Drivetrain;
import com.aaravlabs.safepedropathing.localization.MotionState;
import com.aaravlabs.safepedropathing.math.Pose;
import com.aaravlabs.safepedropathing.math.Vector2D;
import com.aaravlabs.safepedropathing.paths.PathTracker;
import java.util.Map;

public interface Algorithm {
    DrivePowers calculatePath(Drivetrain drivetrain, PathTracker pathTracker, MotionState state, double deltaTime);

    DrivePowers calculateHold(
            Drivetrain drivetrain, Pose target, MotionState state, boolean useScaling, double deltaTime);

    double completion();

    Pose closestPose();

    Vector2D closestTangent();

    Vector2D closestNormal();

    double curvature();

    double remainingDistance();

    default double parametricCompletion() {
        return completion();
    }

    boolean atParametricEnd();

    void reset();

    boolean isBusy();

    Map<String, Object> debug();
}
