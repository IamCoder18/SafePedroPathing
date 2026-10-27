package com.aaravlabs.safepedropathing.revhub.drivetrains;

import android.annotation.SuppressLint;
import com.aaravlabs.synapse.ftc.SafeDevice;
import com.aaravlabs.synapse.ftc.SafeHardwareMap;
import com.aaravlabs.safepedropathing.drivetrain.DrivePowers;
import com.aaravlabs.safepedropathing.drivetrain.Drivetrain;
import com.aaravlabs.safepedropathing.utils.Utils;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;

import java.util.HashMap;
import java.util.Map;

public class Mecanum implements Drivetrain {
    public final MecanumConfig config;
    private final CachedMotor[] motors;
    public final double[] wheelPowers = new double[4];

    private static final int FL = 0;
    private static final int FR = 1;
    private static final int BL = 2;
    private static final int BR = 3;

    private double powerScale = 1.0;
    private DrivePowers drivePowers = DrivePowers.zero();

    public Mecanum(SafeHardwareMap safeMap, MecanumConfig config) {
        this.config = config;
        double powerDeadband = config.powerThreshold.get();

        motors = new CachedMotor[]{
                new CachedMotor(safeMap.device(DcMotorEx.class, config.frontLeftName.get()), powerDeadband),
                new CachedMotor(safeMap.device(DcMotorEx.class, config.frontRightName.get()), powerDeadband),
                new CachedMotor(safeMap.device(DcMotorEx.class, config.backLeftName.get()), powerDeadband),
                new CachedMotor(safeMap.device(DcMotorEx.class, config.backRightName.get()), powerDeadband)
        };

        motors[FL].setDirection(config.frontLeftDirection.get());
        motors[FR].setDirection(config.frontRightDirection.get());
        motors[BL].setDirection(config.backLeftDirection.get());
        motors[BR].setDirection(config.backRightDirection.get());

        setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
    }

    @SuppressLint("DefaultLocale")
    public void applyDrive(DrivePowers powers) {
        drivePowers = powers;

        double[] wheelPowers = computeWheelPowersUnnormalized(powers);

        double maxPower = 1.0;
        for (double power : wheelPowers) {
            maxPower = Math.max(maxPower, Math.abs(power));
        }

        powerScale = 1.0 / maxPower;

        for (int i = 0; i < wheelPowers.length; i++) {
            this.wheelPowers[i] = wheelPowers[i] / maxPower;
            motors[i].setPower(this.wheelPowers[i]);
        }
    }

    public double[] computeWheelPowersUnnormalized(DrivePowers powers) {
        double[] wheelPowers = new double[4];
        double forward = powers.forward();
        double strafe = powers.strafe();
        double turn = powers.turn();

        double fl = forward - strafe - turn;
        double fr = forward + strafe + turn;
        double bl = forward + strafe - turn;
        double br = forward - strafe + turn;

        wheelPowers[FL] = fl;
        wheelPowers[FR] = fr;
        wheelPowers[BL] = bl;
        wheelPowers[BR] = br;

        return wheelPowers;
    }

    @Override
    public double maxScaling(DrivePowers current, DrivePowers delta) {
        double lambda = 1.0;

        double[] currentPowers = computeWheelPowersUnnormalized(current);
        double[] deltaPowers = computeWheelPowersUnnormalized(delta);

        for (int i = 0; i < 4; i++) {
            double a = currentPowers[i];
            double b = deltaPowers[i];

            if (Math.abs(b) < 1e-9) continue;

            double t1 = (1.0 - a) / b;
            double t2 = (-1.0 - a) / b;

            if (t1 >= 0.0 && t1 < lambda) lambda = t1;
            if (t2 >= 0.0 && t2 < lambda) lambda = t2;
        }

        return Utils.clamp(lambda, 0.0, 1.0);
    }

    @Override
    public void drive(DrivePowers powers, boolean manual) {
        if (manual && config.manualBrakeMode.get())
            setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        else
            setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        applyDrive(powers);
    }

    @Override
    public void stop() {
        stop(config.manualBrakeMode.get());
    }

    public void stop(boolean brake) {
        if (brake) {
            setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        } else {
            setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        }

        for (CachedMotor motor : motors) {
            motor.setPower(0);
        }
    }

    @Override
    public Map<String, Object> debug() {
        Map<String, Object> map = new HashMap<>();

        map.put("forward", drivePowers.forward());
        map.put("strafe", drivePowers.strafe());
        map.put("turn", drivePowers.turn());
        map.put("powerScale", powerScale);
        map.put("leftFrontWheelPower", wheelPowers[FL]);
        map.put("rightFrontWheelPower", wheelPowers[FR]);
        map.put("leftBackWheelPower", wheelPowers[BL]);
        map.put("rightBackWheelPower", wheelPowers[BR]);

        return map;
    }

    public void setZeroPowerBehavior(DcMotor.ZeroPowerBehavior behavior) {
        for (CachedMotor motor : motors) {
            motor.setZeroPowerBehavior(behavior);
        }
    }

    /** Returns the sum of the four motors current in Amps
     * This is not bulk cached by the motors so each motor request is a hardware read
     */
    public double currentAmps() {
        double total = 0;
        for (CachedMotor motor : motors) {
            try {
                total += motor.raw().call(m -> m.getCurrent(CurrentUnit.AMPS));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return total;
    }

    @Override
    public double interpolateVelocity(double xRadius, double yRadius, double theta) {
        return 1.0 / (Math.abs(Math.cos(theta)) / xRadius + Math.abs(Math.sin(theta)) / yRadius);
    }
}