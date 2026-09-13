package com.aaravlabs.safepedropathing.revhub.localizers;

import com.aaravlabs.synapse.ftc.SafeHardwareMap;

public interface CustomIMU {
    /**
     * Initializes the IMU using the SafeHardwareMap and the configured hub orientation.
     */
    void initialize(SafeHardwareMap safeMap, String hardwareMapName);

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
