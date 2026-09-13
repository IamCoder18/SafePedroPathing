package com.aaravlabs.safepedropathing.revhub.localizers;

import com.aaravlabs.synapse.ftc.SafeDevice;
import com.aaravlabs.synapse.ftc.SafeHardwareMap;
import com.aaravlabs.safepedropathing.localization.Localizer;
import com.aaravlabs.safepedropathing.localization.MotionState;
import com.aaravlabs.safepedropathing.math.Pose;
import com.aaravlabs.safepedropathing.math.Velocity;
import com.qualcomm.hardware.digitalchickenlabs.OctoQuad;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import com.qualcomm.hardware.digitalchickenlabs.OctoQuad.*;

public class OctoQuadLocalizer implements Localizer {
    private final LocalizerDataBlock localizer = new LocalizerDataBlock();
    public final SafeDevice<OctoQuad> octoQuad;
    private final DistanceUnit globalDistanceUnit;

    private MotionState motionState;

    public OctoQuadLocalizer(SafeHardwareMap safeMap, OctoQuadConfig config) {
        octoQuad = safeMap.device(OctoQuad.class, config.name.get());

        globalDistanceUnit = config.globalDistanceUnit.get();

        octoQuad.run(o -> {
            o.setSingleEncoderDirection(config.xPodPort.get(), config.xPodDirection.get());
            o.setSingleEncoderDirection(config.yPodPort.get(), config.yPodDirection.get());

            double mmPerUnit = config.encoderResolutionUnit.get().toMm(1.0);
            float ticksPerMM = (float) (config.ticksPerUnit.get() / mmPerUnit);

            o.setAllLocalizerParameters(
                    config.xPodPort.get(),
                    config.yPodPort.get(),
                    ticksPerMM,
                    ticksPerMM,
                    (float) -config.offsetUnits.get().toMm(config.xPodOffset.get()),
                    (float) -config.offsetUnits.get().toMm(config.yPodOffset.get()),
                    config.headingScalar.get().floatValue(),
                    config.localizerVelocityIntervalMS.get()
            );
            o.setI2cRecoveryMode(config.i2cRecoveryMode.get());
        });

        reset();

        try {
            while (octoQuad.call(OctoQuad::getLocalizerStatus) != LocalizerStatus.RUNNING) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        update();
    }

    @Override
    public void update() {
        try {
            octoQuad.call(o -> {
                o.readLocalizerData(localizer);
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (!localizer.isDataValid()) {
            return;
        }

        Pose pose = new Pose(
                globalDistanceUnit.fromMm(localizer.posX_mm),
                globalDistanceUnit.fromMm(localizer.posY_mm),
                localizer.heading_rad
        );

        Velocity velocity = new Velocity(
                globalDistanceUnit.fromMm(localizer.velX_mmS),
                globalDistanceUnit.fromMm(localizer.velY_mmS),
                localizer.velHeading_radS
        );

        motionState = MotionState.ofVelocity(pose, velocity);
    }

    @Override
    public void setPose(Pose pose) {
        octoQuad.run(o -> o.setLocalizerPose((int) globalDistanceUnit.toMm(pose.x()), (int) globalDistanceUnit.toMm(pose.y()), (float) pose.heading()));

        if (motionState != null) {
            motionState = motionState.withPose(pose);
        } else {
            motionState = MotionState.ofVelocity(pose, Velocity.zero());
        }
    }

    @Override
    public MotionState state() {
        return motionState;
    }

    @Override
    public void reset() {
        octoQuad.run(OctoQuad::resetLocalizerAndCalibrateIMU);
    }
}
