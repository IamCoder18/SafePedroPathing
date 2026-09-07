package com.aaravlabs.safepedropathing.ftc.localization.localizers;

import com.aaravlabs.synapse.ftc.SafeDevice;
import com.aaravlabs.synapse.ftc.SafeHardwareMap;
import com.aaravlabs.safepedropathing.ftc.localization.constants.OTOSConstants;
import com.qualcomm.hardware.sparkfun.SparkFunOTOS;

import com.aaravlabs.safepedropathing.localization.Localizer;
import com.aaravlabs.safepedropathing.geometry.Pose;
import com.aaravlabs.safepedropathing.math.MathFunctions;
import com.aaravlabs.safepedropathing.math.Vector;

/**
 * This is the OTOSLocalizer class. This class extends the Localizer superclass and is a
 * localizer that uses the SparkFun OTOS.
 *
 * @author Anyi Lin - 10158 Scott's Bots
 * @version 2.0, 9/7/2026
 */
public class OTOSLocalizer implements Localizer {
    private Pose startPose;
    private final SafeDevice<SparkFunOTOS> otos;
    private SparkFunOTOS.Pose2D otosPose;
    private SparkFunOTOS.Pose2D otosVel;
    private SparkFunOTOS.Pose2D otosAcc;
    private double previousHeading;
    private double totalHeading;

    /**
     * This creates a new OTOSLocalizer from a SafeHardwareMap, with a starting Pose at (0,0)
     * facing 0 heading.
     *
     * @param safeMap the SafeHardwareMap
     */
    public OTOSLocalizer(SafeHardwareMap safeMap, OTOSConstants constants) {
        this(safeMap, constants, new Pose());
    }

    /**
     * This creates a new OTOSLocalizer from a SafeHardwareMap and a Pose, with the Pose
     * specifying the starting pose of the localizer.
     *
     * @param safeMap      the SafeHardwareMap
     * @param setStartPose the Pose to start from
     */

    public OTOSLocalizer(SafeHardwareMap safeMap, OTOSConstants constants, Pose setStartPose) {

        otos = safeMap.device(SparkFunOTOS.class, constants.hardwareMapName);

        otos.run(o -> {
            o.setLinearUnit(constants.linearUnit);
            o.setAngularUnit(constants.angleUnit);
            o.setOffset(constants.offset);
            o.setLinearScalar(constants.linearScalar);
            o.setAngularScalar(constants.angularScalar);
            o.calibrateImu();
            o.resetTracking();
        });

        setStartPose(setStartPose);

        otosPose = new SparkFunOTOS.Pose2D();
        otosVel = new SparkFunOTOS.Pose2D();
        otosAcc = new SparkFunOTOS.Pose2D();
        totalHeading = 0;
        previousHeading = startPose.getHeading();

        resetOTOS();
    }



    /**
     * This returns the current pose estimate.
     *
     * @return returns the current pose estimate as a Pose
     */
    @Override
    public Pose getPose() {
        Pose pose = new Pose(otosPose.x, otosPose.y, otosPose.h);

        Vector vec = pose.getAsVector();
        vec.rotateVector(startPose.getHeading());

        return startPose.plus(new Pose(vec.getXComponent(), vec.getYComponent(), pose.getHeading()));
    }

    /**
     * This returns the current velocity estimate.
     *
     * @return returns the current velocity estimate as a Pose
     */
    @Override
    public Pose getVelocity() {
        return new Pose(otosVel.x, otosVel.y, otosVel.h);
    }

    /**
     * This returns the current velocity estimate.
     *
     * @return returns the current velocity estimate as a Vector
     */
    @Override
    public Vector getVelocityVector() {
        return getVelocity().getAsVector();
    }

    /**
     * This sets the start pose. Changing the start pose should move the robot as if all its
     * previous movements were displacing it from its new start pose.
     *
     * @param setStart the new start pose
     */
    @Override
    public void setStartPose(Pose setStart) {
        startPose = setStart;
    }

    /**
     * This sets the current pose estimate. Changing this should just change the robot's current
     * pose estimate, not anything to do with the start pose.
     *
     * @param setPose the new current pose estimate
     */
    @Override
    public void setPose(Pose setPose) {
        resetOTOS();
        Pose setOTOSPose = setPose.minus(startPose);
        final SparkFunOTOS.Pose2D p = new SparkFunOTOS.Pose2D(setOTOSPose.getX(), setOTOSPose.getY(), setOTOSPose.getHeading());
        otos.run(o -> o.setPosition(p));
    }

    /**
     * This updates the total heading of the robot. The OTOS handles all other updates itself.
     */
    @Override
    public void update() {
        try {
            otos.call(o -> {
                o.getPosVelAcc(otosPose, otosVel, otosAcc);
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Thank you to GoldenElf58 of FTC Team 16657 for spotting a bug here; it was resolved by adding the turn direction.
        totalHeading += MathFunctions.getSmallestAngleDifference(otosPose.h, previousHeading) * MathFunctions.getTurnDirection(previousHeading, otosPose.h);
        previousHeading = otosPose.h;
    }

    /**
     * This resets the OTOS.
     */
    public void resetOTOS() {
        otos.run(SparkFunOTOS::resetTracking);
    }

    /**
     * This returns how far the robot has turned in radians, in a number not clamped between 0 and
     * 2 * pi radians. This is used for some tuning things and nothing actually within the following.
     *
     * @return returns how far the robot has turned in total, in radians.
     */
    @Override
    public double getTotalHeading() {
        return totalHeading;
    }

    /**
     * This returns the multiplier applied to forward movement measurement to convert from OTOS
     * ticks to inches. For the OTOS, this value is the same as the lateral multiplier.
     * This is found empirically through a tuner.
     *
     * @return returns the forward ticks to inches multiplier
     */
    @Override
    public double getForwardMultiplier() {
        try {
            return otos.call(SparkFunOTOS::getLinearScalar);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This returns the multiplier applied to lateral/strafe movement measurement to convert from
     * OTOS ticks to inches. For the OTOS, this value is the same as the forward multiplier.
     * This is found empirically through a tuner.
     *
     * @return returns the lateral ticks to inches multiplier
     */
    @Override
    public double getLateralMultiplier() {
        try {
            return otos.call(SparkFunOTOS::getLinearScalar);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This returns the multiplier applied to turning movement measurement to convert from OTOS ticks
     * to radians. This is found empirically through a tuner.
     *
     * @return returns the turning ticks to radians multiplier
     */
    @Override
    public double getTurningMultiplier() {
        try {
            return otos.call(SparkFunOTOS::getAngularScalar);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This does nothing since this localizer does not use the IMU.
     */
    @Override
    public void resetIMU() {
    }

    @Override
    public double getIMUHeading() {
        return Double.NaN;
    }

    /**
     * This returns whether if any component of robot's position is NaN.
     *
     * @return returns whether the robot's position is NaN
     */
    @Override
    public boolean isNAN() {
        return Double.isNaN(getPose().getX()) || Double.isNaN(getPose().getY()) || Double.isNaN(getPose().getHeading());
    }
}
