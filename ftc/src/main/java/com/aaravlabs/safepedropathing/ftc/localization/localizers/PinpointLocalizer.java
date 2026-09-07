package com.aaravlabs.safepedropathing.ftc.localization.localizers;

import android.annotation.SuppressLint;

import com.aaravlabs.synapse.ftc.SafeDevice;
import com.aaravlabs.synapse.ftc.SafeHardwareMap;
import com.aaravlabs.safepedropathing.ftc.PoseConverter;
import com.aaravlabs.safepedropathing.ftc.localization.constants.PinpointConstants;
import com.aaravlabs.safepedropathing.geometry.PedroCoordinates;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import com.aaravlabs.safepedropathing.localization.Localizer;
import com.aaravlabs.safepedropathing.geometry.Pose;
import com.aaravlabs.safepedropathing.math.MathFunctions;
import com.aaravlabs.safepedropathing.math.Vector;
import com.aaravlabs.safepedropathing.util.NanoTimer;

import java.util.Objects;

/**
 * This is the Pinpoint class. This class extends the Localizer superclass and is a
 * localizer that uses the two wheel odometry set up with the IMU to have more accurate heading
 * readings. The diagram below, which is modified from Road Runner, shows a typical set up.
 *
 * @author Logan Nash
 * @author Havish Sripada 12808 - RevAmped Robotics
 * @author Ethan Doak - GoBilda
 * @version 2.0, 9/7/2026
 */
public class PinpointLocalizer implements Localizer {
    private final SafeDevice<GoBildaPinpointDriver> odo;
    private final PinpointConstants constants;
    private double previousHeading;
    private double totalHeading;
    private Pose startPose;
    private Pose currentVelocity;
    private Pose pinpointPose;

    /**
     * This creates a new PinpointLocalizer from a SafeHardwareMap, with a starting Pose at (0,0)
     * facing 0 heading.
     *
     * @param safeMap the SafeHardwareMap
     */
    public PinpointLocalizer(SafeHardwareMap safeMap, PinpointConstants constants){ this(safeMap, constants, new Pose());}

    /**
     * This creates a new PinpointLocalizer from a SafeHardwareMap and a Pose, with the Pose
     * specifying the starting pose of the localizer.
     *
     * @param safeMap      the SafeHardwareMap
     * @param setStartPose the Pose to start from
     */
    @SuppressLint("NewApi")
    public PinpointLocalizer(SafeHardwareMap safeMap, PinpointConstants constants, Pose setStartPose){

        this.constants = constants;
        odo = safeMap.device(GoBildaPinpointDriver.class, constants.hardwareMapName);

        odo.run(o -> {
            o.setOffsets(constants.forwardPodY, constants.strafePodX, constants.distanceUnit);

            if (constants.yawScalar.isPresent()) {
                o.setYawScalar(constants.yawScalar.getAsDouble());
            }

            if (constants.customEncoderResolution.isPresent()) {
                o.setEncoderResolution(constants.customEncoderResolution.getAsDouble(), constants.distanceUnit);
            } else {
                o.setEncoderResolution(constants.encoderResolution);
            }

            o.setEncoderDirections(constants.forwardEncoderDirection, constants.strafeEncoderDirection);
        });

        setStartPose(setStartPose);
        totalHeading = 0;
        pinpointPose = startPose;
        currentVelocity = new Pose();
        previousHeading = setStartPose.getHeading();
    }

    /**
     * This returns the current pose estimate.
     *
     * @return returns the current pose estimate as a Pose
     */
    @Override
    public Pose getPose() {
        return pinpointPose;
    }

    /**
     * This returns the current velocity estimate.
     *
     * @return returns the current velocity estimate as a Pose
     */
    @Override
    public Pose getVelocity() {
        return currentVelocity;
    }

    /**
     * This returns the current velocity estimate.
     *
     * @return returns the current velocity estimate as a Vector
     */
    @Override
    public Vector getVelocityVector() {
        return currentVelocity.getAsVector();
    }

    /**
     * This sets the start pose. This alters the start position even if it is already set, compensating as needed.
     *
     * @param setStart the new start pose
     */
    @Override
    public void setStartPose(Pose setStart) {
        if (!Objects.equals(startPose, new Pose()) && startPose != null) {
            Pose currentPose = pinpointPose.rotate(-startPose.getHeading(), false).minus(startPose);
            setPose(setStart.plus(currentPose.rotate(setStart.getHeading(), false)));
        } else {
            setPose(setStart);
        }

        this.startPose = setStart;
    }

    /**
     * This sets the current pose estimate. Changing this should just change the robot's current
     * pose estimate, not anything to do with the start pose.
     *
     * @param setPose the new current pose estimate
     */
    @Override
    public void setPose(Pose setPose) {
        final org.firstinspires.ftc.robotcore.external.navigation.Pose2D p2d = PoseConverter.poseToPose2D(setPose, PedroCoordinates.INSTANCE);
        odo.run(o -> o.setPosition(p2d));
        pinpointPose = setPose;
        previousHeading = setPose.getHeading();
    }

    /**
     * This updates the total heading of the robot. The Pinpoint handles all other updates itself.
     */
    @Override
    public void update() {
        try {
            odo.call(o -> {
                o.update();
                Pose currentPinpointPose = PoseConverter.pose2DToPose(o.getPosition(), PedroCoordinates.INSTANCE);
                // Thank you to GoldenElf58 of FTC Team 16657 for spotting a bug here; it was resolved by adding the turn direction.
                totalHeading += MathFunctions.getSmallestAngleDifference(currentPinpointPose.getHeading(), previousHeading) * MathFunctions.getTurnDirection(previousHeading, currentPinpointPose.getHeading());
                previousHeading = currentPinpointPose.getHeading();
                currentVelocity = new Pose(o.getVelX(DistanceUnit.INCH), o.getVelY(DistanceUnit.INCH), o.getHeadingVelocity(AngleUnit.RADIANS.getUnnormalized()));
                pinpointPose = currentPinpointPose;
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
     * This returns the Y encoder value as none of the odometry tuners are required for this localizer
     * @return returns the Y encoder value
     */
    @Override
    public double getForwardMultiplier() {
        try {
            return odo.call(GoBildaPinpointDriver::getEncoderY);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This returns the X encoder value as none of the odometry tuners are required for this localizer
     * @return returns the X encoder value
     */
    @Override
    public double getLateralMultiplier() {
        try {
            return odo.call(GoBildaPinpointDriver::getEncoderX);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This returns either the factory tuned yaw scalar or the yaw scalar tuned by yourself.
     * @return returns the yaw scalar
     */
    @Override
    public double getTurningMultiplier() {
        try {
            return odo.call(GoBildaPinpointDriver::getYawScalar);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This resets the IMU. Does not change heading estimation.
     */
    @Override
    public void resetIMU() {
        resetPinpoint();
    }

    @Override
    public double getIMUHeading() {
        return Double.NaN;
    }

    /**
     * This resets the pinpoint.
     */
    private void resetPinpoint() {
        odo.run(o -> o.resetPosAndIMU());

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This recalibrates the Pinpoint. It will take 0.25 seconds to recalibrate, and the robot must be still
     */
    public void recalibrate() {
        odo.run(GoBildaPinpointDriver::recalibrateIMU);
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

    /**
     * This returns the GoBildaPinpointDriver object used by this localizer, in case you want to
     * access any of its methods directly.
     *
     * @return returns the GoBildaPinpointDriver object used by this localizer
     */
    public GoBildaPinpointDriver getPinpoint() {
        try {
            return odo.call(o -> o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setX(double x) {
        odo.run(o -> o.setPosX(x, constants.distanceUnit));
    }

    @Override
    public void setY(double y) {
        odo.run(o -> o.setPosY(y, constants.distanceUnit));
    }

    @Override
    public void setHeading(double heading) {
        odo.run(o -> o.setHeading(heading, AngleUnit.RADIANS));
    }
}
