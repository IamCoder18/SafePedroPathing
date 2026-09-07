package com.aaravlabs.safepedropathing.ftc.localization;

import com.aaravlabs.synapse.ftc.SafeHardwareMap;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;

public interface CustomIMU {
    /**
     * Initializes the IMU using the SafeHardwareMap and hubOrientation.
     */
    void initialize(SafeHardwareMap safeMap, String hardwareMapName, RevHubOrientationOnRobot hubOrientation);

    /**
     * Gets the IMU's reading for the heading of the robot in radians
     * @return the heading of the robot in radians
     */
    double getHeading();

    /**
     * Resets the IMU's yaw to 0.
     */
    void resetYaw();
}

