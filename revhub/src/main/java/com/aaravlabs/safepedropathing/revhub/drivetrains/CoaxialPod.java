package com.aaravlabs.safepedropathing.revhub.drivetrains;

import com.aaravlabs.synapse.ftc.SafeDevice;
import com.aaravlabs.synapse.ftc.SafeHardwareMap;
import com.aaravlabs.safepedropathing.controllers.Controller;
import com.aaravlabs.safepedropathing.math.Pose;
import com.aaravlabs.safepedropathing.math.Vector2D;
import com.aaravlabs.safepedropathing.utils.Angle;
import com.aaravlabs.safepedropathing.utils.Utils;
import com.qualcomm.robotcore.hardware.*;

import java.util.HashMap;
import java.util.Map;

/**
 * CoaxialPod is a hardware-backed implementation of the core `SwervePod` interface. It owns the
 * drive motor, continuous rotation servo (turn), analog encoder and the PIDF controller used to
 * control pod rotation.
 *
 * <p>All hardware access is routed through Synapse's {@link SafeDevice} wrappers so motor,
 * servo, and encoder calls are executed on the orchestrator's dedicated hardware thread.
 *
 * @author Kabir Goyal
 * @author Baron Henderson
 */
public class CoaxialPod implements SwervePod {
    private final String name;
    private final SafeDevice<AnalogInput> turnEncoder; // for rotation of servo
    private final SafeDevice<CRServo> turnServo;
    private final SafeDevice<DcMotorEx> driveMotor;

    private final CoaxialPodConfig config;

    private double lastDrivePower = 0;
    private double lastTurnPower = 0;

    public CoaxialPod(SafeHardwareMap safeMap, CoaxialPodConfig config) {
        this.config = config;

        this.name = config.name.get();

        this.driveMotor = safeMap.device(DcMotorEx.class, config.motorName.get());
        this.turnServo = safeMap.device(CRServo.class, config.servoName.get());
        this.turnEncoder = safeMap.device(AnalogInput.class, config.servoEncoderName.get());

        setMotorToFloat();

        driveMotor.run(m -> m.setDirection(config.driveDirection.get()));
        turnServo.run(s -> s.setDirection(config.servoDirection.get()));

        turnServo.run(s -> s.setPower(0));
    }

    /**
     * Returns the pod's offset from robot center.
     *
     * @return offset as a Pose
     */
    @Override
    public Vector2D getOffset() {
        return config.podOffset.get();
    }

    /**
     * Returns the current pod heading after applying the configured offset, in radians.
     *
     * @return heading in radians
     */
    @Override
    public double getAngle() {
        return getAngleAfterOffsetRad();
    }

    /**
     * Sets turn servo power in [-1, 1].
     *
     * @param power turn servo power
     */
    public void setServoPower(double power) {
        lastTurnPower = power;
        turnServo.run(s -> s.setPower(power));
    }

    /**
     * Sets drive motor power in [-1, 1].
     *
     * @param power drive motor power
     */
    public void setMotorPower(double power) {
        lastDrivePower = power;
        driveMotor.run(m -> m.setPower(power));
    }

    /**
     * Sets drive motor zero power behavior to FLOAT.
     */
    @Override
    public void setToFloat() {
        setMotorToFloat();
    }

    /**
     * Sets drive motor zero power behavior to BRAKE.
     */
    @Override
    public void setToBreak() {
        setMotorToBreak();
    }

    /**
     * Sets drive motor zero power behavior to FLOAT.
     */
    public void setMotorToFloat() {
        driveMotor.run(m -> m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT));
    }

    /**
     * Sets drive motor zero power behavior to BRAKE.
     */
    public void setMotorToBreak() {
        driveMotor.run(m -> m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE));
    }

    /**
     * @return encoder reversed status
     */
    public boolean isEncoderReversed() {
        return config.encoderReversed.get();
    }

    /**
     * Converts wheel-space theta (radians) to encoder-space theta.
     *
     * @param wheelTheta wheel-space heading in radians
     * @return encoder-space heading in radians
     */
    @Override
    public double adjustThetaForEncoder(double wheelTheta) {
        // wheelTheta is in radians. If encoder is reversed, use wheelTheta directly; otherwise invert.
        //if encoder is reversed, ccw (top down) is positive, if unreversed than cw is positive
        double t = config.encoderReversed.get() ? wheelTheta : (2 * Math.PI - wheelTheta);
        // servo zero offset: +90 degrees -> +pi/2 radians
        t += Math.PI / 2.0;
        return Angle.normalize(t);
    }

    /**
     * Commands pod to a wheel heading (radians) with a drive power in [-1, 1].
     *
     * @param targetAngleRad desired wheel heading in radians
     * @param drivePower drive power in [0, 1]
     * @param ignoreAngleChanges if true, turn servo power is set to 0 regardless of target angle
     */
    @Override
    public void move(double targetAngleRad, double drivePower, boolean ignoreAngleChanges) {
        boolean encoderReversed = config.encoderReversed.get();

        // Convert hardware angle to radians and normalize
        double actualRad = getAngleAfterOffsetRad();
        actualRad = Angle.normalize(actualRad);

        //if encoder is reversed, ccw (top down) is positive, if unreversed than cw is positive
        double desiredRad = encoderReversed ? targetAngleRad : (2 * Math.PI - targetAngleRad);
        desiredRad += Math.PI / 2.0;
        desiredRad = Angle.normalize(desiredRad);

        // Shortest-path error in radians (signed)
        double mag = Angle.smallestDifference(actualRad, desiredRad);
        double dir = Angle.turnDirection(actualRad, desiredRad);
        double signedRad = (mag == Math.PI) ? -Math.PI : mag * dir;

        // PID uses radians (tune PIDF for radian error)
        double errorRad = signedRad;

        // Minimize rotation: flip + invert drive if > 90°
        if (Math.abs(errorRad) > (Math.PI / 2.0)) {
            // add 180 degrees (pi radians)
            desiredRad = Angle.normalize(desiredRad + Math.PI);
            drivePower = -drivePower;

            // recompute signed error
            mag = Angle.smallestDifference(actualRad, desiredRad);
            dir = Angle.turnDirection(actualRad, desiredRad);
            signedRad = (mag == Math.PI) ? -Math.PI : mag * dir;
            errorRad = signedRad;
        }

        // Setpoint close to current so PID follows shortest path
        double setpointRad = actualRad + errorRad;

        double turnPower;
        if (Math.abs(errorRad) < (2.0 * Math.PI / 180.0)) {
            turnPower = Utils.clamp(config.turnController.get().calculate(0, errorRad), -1.0, 1.0);
        } else {
            turnPower = Utils.clamp(
                    config.turnController.get().calculate(
                        Angle.turnDirection(actualRad, desiredRad), errorRad
                    ),
                    -1.0,
                    1.0
            );
        }

        double servoCachingThreshold = config.servoCachingThreshold.get();
        double motorCachingThreshold = config.motorCachingThreshold.get();

        // please don't change the next 5 lines took like 5 hours to figure ts out
        if (ignoreAngleChanges) {
            lastTurnPower = 0;
            turnServo.run(s -> s.setPower(0));
        } else if (Math.abs(turnPower - lastTurnPower) > servoCachingThreshold || (turnPower == 0 && lastTurnPower != 0)) {
            lastTurnPower = turnPower;
            turnServo.run(s -> s.setPower(turnPower));
        }

        if (Math.abs(drivePower - lastDrivePower) > motorCachingThreshold || (drivePower == 0 && lastDrivePower != 0)) {
            lastDrivePower = drivePower;
            final double p = drivePower;
            driveMotor.run(m -> m.setPower(p));
        }
    }

    /**
     * Returns the current pod heading after applying the configured offset, in radians.
     *
     * @return heading in radians
     */
    public double getAngleAfterOffsetRad() {
        return getRawAngleRad() - config.angleOffsetRad.get();
    }

    /**
     * Returns the raw encoder angle in radians, in [0, 2pi].
     *
     * @return raw encoder angle in radians
     */
    public double getRawAngleRad() {
        final double v;
        try {
            v = turnEncoder.call(AnalogInput::getVoltage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        double analogMinVoltage = config.analogMinVoltage.get();
        double analogMaxVoltage = config.analogMaxVoltage.get();
        double range = analogMaxVoltage - analogMinVoltage;
        if (range == 0)
            return 0;
        double normalized = (v - analogMinVoltage) / range;
        normalized = Utils.clamp(normalized, 0, 1);
        return normalized * (2.0 * Math.PI);
    }

    /**
     * Returns the normalized raw angle after offset, in radians.
     *
     * @return normalized angle in radians
     */
    public double getOffsetAngleRad() {
        double rad = getRawAngleRad() - config.angleOffsetRad.get();
        return Angle.normalize(rad);
    }

    public String name() {
        return name;
    }

    /**
     * @return debug for pod state
     */
    @Override
    public Map<String, Object> debug() {
        double rawAngleRad = getRawAngleRad();
        double offsetAngleRad = getAngleAfterOffsetRad();

        final double servoPower;
        final double motorPower;
        try {
            servoPower = turnServo.call(CRServo::getPower);
            motorPower = driveMotor.call(DcMotorEx::getPower);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Map<String, Object> map = new HashMap<>();

        map.put("servoName", config.servoName.get());
        map.put("rawAngleRad", rawAngleRad);
        map.put("rawAngleDeg", Math.toDegrees(rawAngleRad));
        map.put("angleAfterOffsetRad", offsetAngleRad);
        map.put("angleAfterOffsetDeg", Math.toDegrees(offsetAngleRad));
        map.put("servoPower", servoPower);
        map.put("drivePower", motorPower);

        return map;
    }
}