package com.aaravlabs.safepedropathing.revhub.localizers;

import com.aaravlabs.synapse.ftc.SafeDevice;
import com.aaravlabs.synapse.ftc.SafeHardwareMap;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.IMU;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

public class RevHubIMU implements CustomIMU {
    private SafeDevice<IMU> imu;
    private final RevHubOrientationOnRobot hubOrientation;

    public RevHubIMU(RevHubOrientationOnRobot hubOrientation) {
        this.hubOrientation = hubOrientation;
    }

    /**
     * Initializes the IMU using the SafeHardwareMap and hubOrientation.
     * @param safeMap the safe hardware map
     * @param hardwareMapName the name of the hardware map
     */
    @Override
    public void initialize(SafeHardwareMap safeMap, String hardwareMapName) {
        imu = safeMap.device(IMU.class, hardwareMapName);
        imu.run(i -> i.initialize(new IMU.Parameters(hubOrientation)));
    }

    /**
     * Gets the IMU's reading for the heading of the robot in radians
     * @return the heading of the robot in radians
     */
    @Override
    public double getHeading() {
        try {
            return imu.call(i -> i.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Resets the IMU's yaw to 0.
     */
    @Override
    public void resetYaw() {
        imu.run(IMU::resetYaw);
    }
}
