package com.aaravlabs.safepedropathing.revhub.localizers;

import com.aaravlabs.synapse.ftc.SafeDevice;
import com.aaravlabs.synapse.ftc.SafeHardwareMap;
import com.aaravlabs.safepedropathing.localization.Localizer;
import com.aaravlabs.safepedropathing.localization.MotionState;
import com.aaravlabs.safepedropathing.math.Pose;
import com.aaravlabs.safepedropathing.math.Velocity;
import com.qualcomm.hardware.sparkfun.SparkFunOTOS;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

public class OTOSLocalizer implements Localizer {
    private final SafeDevice<SparkFunOTOS> otos;

    private MotionState motionState;

    public OTOSLocalizer(SafeHardwareMap safeMap, OTOSConfig config) {
        otos = safeMap.device(SparkFunOTOS.class, config.name.get());

        otos.run(o -> {
            o.setLinearUnit(config.linearUnit.get());
            o.setAngularUnit(AngleUnit.RADIANS);

            SparkFunOTOS.Pose2D offsetPose = new SparkFunOTOS.Pose2D(
                    config.offset.get().x(),
                    config.offset.get().y(),
                    config.offset.get().heading() + Math.PI / 2
            );
            o.setOffset(offsetPose);
            o.setLinearScalar(config.linearScalar.get());
            o.setAngularScalar(config.angularScalar.get());

            o.calibrateImu();
            o.resetTracking();
        });

        update();
    }

    public void setPose(Pose pose) {
        otos.run(o -> o.setPosition(
                new SparkFunOTOS.Pose2D(
                        pose.x(),
                        pose.y(),
                        pose.heading() + Math.PI / 2
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
        final SparkFunOTOS.Pose2D[] readings;
        try {
            readings = otos.call(o -> new SparkFunOTOS.Pose2D[]{o.getPosition(), o.getVelocity()});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        SparkFunOTOS.Pose2D pose2D = readings[0];
        SparkFunOTOS.Pose2D velocity2D = readings[1];

        Pose pose = new Pose(
                pose2D.x,
                pose2D.y,
                pose2D.h - Math.PI / 2
        );

        Velocity velocity = new Velocity(
                velocity2D.x,
                velocity2D.y,
                velocity2D.h
        );

        motionState = MotionState.ofVelocity(pose, velocity);
    }

    @Override
    public MotionState state() {
        return motionState;
    }

    public void reset() {
        otos.run(SparkFunOTOS::resetTracking);
    }
}
