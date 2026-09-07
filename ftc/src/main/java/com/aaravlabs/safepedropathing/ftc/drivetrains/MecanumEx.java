package com.aaravlabs.safepedropathing.ftc.drivetrains;

import com.aaravlabs.synapse.ftc.SafeDevice;
import com.aaravlabs.synapse.ftc.SafeHardwareMap;
import com.aaravlabs.safepedropathing.drivetrain.CustomDrivetrain;
import com.aaravlabs.safepedropathing.math.Vector;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;

import java.util.Arrays;
import java.util.List;

/**
 * This is the MecanumEx class, a version of Mecanum using CustomDrivetrain
 * @author Kabir Goyal
 * @version 1.1, 9/7/2026
 */
@Deprecated
public class MecanumEx extends CustomDrivetrain {
    public MecanumConstants constants;
    private final SafeDevice<DcMotorEx> leftFront;
    private final SafeDevice<DcMotorEx> leftRear;
    private final SafeDevice<DcMotorEx> rightFront;
    private final SafeDevice<DcMotorEx> rightRear;
    private final List<SafeDevice<DcMotorEx>> motors;
    private final double[] lastMotorPowers;
    private final VoltageSensor voltageSensor;
    private double motorCachingThreshold;
    private boolean useBrakeModeInTeleOp;
    private double staticFrictionCoefficient;

    /**
     * This creates a new Mecanum, which takes in various movement vectors and outputs
     * the wheel drive powers necessary to move in the intended direction, given the true movement
     * vector for the front left mecanum wheel.
     *
     * @param safeMap          this is the SafeHardwareMap object that contains the motors and other hardware
     * @param mecanumConstants this is the MecanumConstants object that contains the names of the motors and directions etc.
     */
    public MecanumEx(SafeHardwareMap safeMap, MecanumConstants mecanumConstants) {
        constants = mecanumConstants;

        this.maxPowerScaling = mecanumConstants.maxPower;
        this.motorCachingThreshold = mecanumConstants.motorCachingThreshold;
        this.useBrakeModeInTeleOp = mecanumConstants.useBrakeModeInTeleOp;

        voltageSensor = safeMap.raw().voltageSensor.iterator().next();

        leftFront = safeMap.device(DcMotorEx.class, mecanumConstants.leftFrontMotorName);
        leftRear = safeMap.device(DcMotorEx.class, mecanumConstants.leftRearMotorName);
        rightRear = safeMap.device(DcMotorEx.class, mecanumConstants.rightRearMotorName);
        rightFront = safeMap.device(DcMotorEx.class, mecanumConstants.rightFrontMotorName);

        motors = Arrays.asList(leftFront, leftRear, rightFront, rightRear);
        lastMotorPowers = new double[] {0,0,0,0};

        for (SafeDevice<DcMotorEx> motor : motors) {
            motor.run(m -> {
                MotorConfigurationType motorConfigurationType = m.getMotorType().clone();
                motorConfigurationType.setAchieveableMaxRPMFraction(1.0);
                m.setMotorType(motorConfigurationType);
            });
        }

        setMotorsToFloat();
        breakFollowing();

        Vector copiedFrontLeftVector = mecanumConstants.frontLeftVector.normalize();
        vectors = new Vector[]{
                new Vector(copiedFrontLeftVector.getMagnitude(), copiedFrontLeftVector.getTheta()),
                new Vector(copiedFrontLeftVector.getMagnitude(), 2 * Math.PI - copiedFrontLeftVector.getTheta()),
                new Vector(copiedFrontLeftVector.getMagnitude(), 2 * Math.PI - copiedFrontLeftVector.getTheta()),
                new Vector(copiedFrontLeftVector.getMagnitude(), copiedFrontLeftVector.getTheta())};
    }

    /**
     * This method takes in forward, strafe, and rotation values and applies them to
     * the drivetrain.
     *
     * @param forward the forward power value, which would typically be
     *                -gamepad1.left_stick_y in a normal arcade drive setup
     * @param strafe the strafe power value, which would typically be
     *               -gamepad1.left_stick_x in a normal arcade drive setup
     *               because pedro treats left as positive
     * @param rotation the rotation power value, which would typically be
     *                 -gamepad1.right_stick_x in a normal arcade drive setup
     *                 because CCW is positive
     */
    public void arcadeDrive(double forward, double strafe, double rotation) {
        double[] wheelPowers = new double[4];

        wheelPowers[0] = forward + strafe - rotation; //leftFront
        wheelPowers[1] = forward - strafe - rotation; //leftRear
        wheelPowers[2] = forward - strafe + rotation; //rightFront
        wheelPowers[3] = forward + strafe + rotation; //rightRear

        double denom = 1;
        for (double power : wheelPowers) {
           denom = Math.max(denom, Math.abs(power));
        }

        for (int i = 0; i < 4; i++) {
            wheelPowers[i] = wheelPowers[i] / denom;
        }

        for (int i = 0; i < motors.size(); i++) {
            if (Math.abs(lastMotorPowers[i] - wheelPowers[i]) > motorCachingThreshold ||
                    (wheelPowers[i] == 0 && lastMotorPowers[i] != 0)) {
                final int idx = i;
                final double power = wheelPowers[i];
                lastMotorPowers[i] = wheelPowers[i];
                motors.get(idx).run(m -> m.setPower(power));
            }
        }
    }

    @Override
    public void updateConstants() {
        leftFront.run(m -> m.setDirection(constants.leftFrontMotorDirection));
        leftRear.run(m -> m.setDirection(constants.leftRearMotorDirection));
        rightFront.run(m -> m.setDirection(constants.rightFrontMotorDirection));
        rightRear.run(m -> m.setDirection(constants.rightRearMotorDirection));
        this.motorCachingThreshold = constants.motorCachingThreshold;
        this.useBrakeModeInTeleOp = constants.useBrakeModeInTeleOp;
        this.voltageCompensation = constants.useVoltageCompensation;
        this.nominalVoltage = constants.nominalVoltage;
        this.staticFrictionCoefficient = constants.staticFrictionCoefficient;
    }


    /**
     * This sets the motors to the zero power behavior of brake.
     */
    private void setMotorsToBrake() {
        for (SafeDevice<DcMotorEx> motor : motors) {
            motor.run(m -> m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE));
        }
    }

    /**
     * This sets the motors to the zero power behavior of float.
     */
    private void setMotorsToFloat() {
        for (SafeDevice<DcMotorEx> motor : motors) {
            motor.run(m -> m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT));
        }
    }

    @Override
    public void breakFollowing() {
        for (int i = 0; i < motors.size(); i++) {
            final int idx = i;
            lastMotorPowers[idx] = 0;
            motors.get(idx).run(m -> m.setPower(0));
        }
        setMotorsToFloat();
    }

    @Override
    public void startTeleopDrive() {
        if (useBrakeModeInTeleOp) {
            setMotorsToBrake();
        }
    }

    @Override
    public void startTeleopDrive(boolean brakeMode) {
        if (brakeMode) {
            setMotorsToBrake();
        } else {
            setMotorsToFloat();
        }
    }

    public void getAndRunDrivePowers(Vector correctivePower, Vector headingPower,
                                     Vector pathingPower, double robotHeading, Vector robotVelocity) {
        super.runDrive(correctivePower, headingPower, pathingPower, robotHeading,
                       robotVelocity);
    }

    @Override
    public double xVelocity() {
        return constants.xVelocity;
    }

    @Override
    public double yVelocity() {
        return constants.yVelocity;
    }

    @Override
    public void setXVelocity(double xMovement) { constants.setXVelocity(xMovement); }
    @Override
    public void setYVelocity(double yMovement) { constants.setYVelocity(yMovement); }

    public double getStaticFrictionCoefficient() {
        return staticFrictionCoefficient;
    }

    @Override
    public double getVoltage() {
        return voltageSensor.getVoltage();
    }

    private double getVoltageNormalized() {
        double voltage = getVoltage();
        return (nominalVoltage - (nominalVoltage * staticFrictionCoefficient)) / (voltage - ((nominalVoltage * nominalVoltage / voltage) * staticFrictionCoefficient));
    }

    @Override
    public String debugString() {
        return "Mecanum{" +
                " leftFront=" + leftFront +
                ", leftRear=" + leftRear +
                ", rightFront=" + rightFront +
                ", rightRear=" + rightRear +
                ", motors=" + motors +
                ", motorCachingThreshold=" + motorCachingThreshold +
                ", useBrakeModeInTeleOp=" + useBrakeModeInTeleOp +
                '}';
    }

    public List<SafeDevice<DcMotorEx>> getMotors() {
        return motors;
    }
}
