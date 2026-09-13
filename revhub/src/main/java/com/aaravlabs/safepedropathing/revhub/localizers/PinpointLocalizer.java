package com.aaravlabs.safepedropathing.revhub.localizers;

import com.aaravlabs.synapse.ftc.SafeDevice;
import com.aaravlabs.synapse.ftc.SafeHardwareMap;
import com.aaravlabs.safepedropathing.localization.Localizer;
import com.aaravlabs.safepedropathing.localization.MotionState;
import com.aaravlabs.safepedropathing.math.Pose;
import com.aaravlabs.safepedropathing.math.Velocity;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit;

public class PinpointLocalizer implements Localizer {

    public enum ResetMode {
        RECALIBRATE_IMU,
        RESET_AND_RECALIBRATE_IMU,
        NONE
    }

    private final SafeDevice<GoBildaPinpointDriver> pinpoint;
    private final DistanceUnit globalDistanceUnit;

    private MotionState motionState;
    private final ResetMode resetMode;

    public PinpointLocalizer(SafeHardwareMap safeMap, PinpointConfig config) {
        this.globalDistanceUnit = config.globalDistanceUnit.get();

        pinpoint = safeMap.device(GoBildaPinpointDriver.class, config.name.get());

        pinpoint.run(p -> {
            p.setOffsets(config.xPodOffset.get(), config.yPodOffset.get(), config.offsetUnits.get());

            if (config.ticksPerUnit.get().isPresent()) {
                p.setEncoderResolution(config.ticksPerUnit.get().getAsDouble(), config.encoderResolutionUnit.get());
            } else {
                p.setEncoderResolution(config.podType.get());
            }

            p.setEncoderDirections(
                    config.xPodDirection.get(),
                    config.yPodDirection.get()
            );
        });

        resetMode = config.resetMode.get();

        reset();
        update();
    }

    public void setPose(Pose pose) {
        pinpoint.run(p -> p.setPosition(
                new Pose2D(
                        globalDistanceUnit,
                        pose.x(),
                        pose.y(),
                        AngleUnit.RADIANS,
                        pose.heading()
                )
        ));

        if (motionState != null) {
            motionState = motionState.withPose(pose);
        } else {
            motionState = MotionState.ofVelocity(pose, Velocity.zero());
        }
    }

    @Override
    public void update() {
        try {
            pinpoint.call(p -> {
                p.update();

                Pose pose = new Pose(
                        p.getPosX(globalDistanceUnit),
                        p.getPosY(globalDistanceUnit),
                        p.getHeading(AngleUnit.RADIANS)
                );

                Velocity velocity = new Velocity(
                        p.getVelX(globalDistanceUnit),
                        p.getVelY(globalDistanceUnit),
                        p.getHeadingVelocity(UnnormalizedAngleUnit.RADIANS)
                );

                motionState = MotionState.ofVelocity(pose, velocity);
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MotionState state() {
        return motionState;
    }

    public void reset() {
        if (resetMode == ResetMode.RESET_AND_RECALIBRATE_IMU) {
            pinpoint.run(GoBildaPinpointDriver::resetPosAndIMU);
        } else if (resetMode == ResetMode.RECALIBRATE_IMU) {
            pinpoint.run(GoBildaPinpointDriver::recalibrateIMU);
        }
    }
}
