package com.aaravlabs.safepedropathing.ftc.localization;

import com.aaravlabs.synapse.ftc.SafeDevice;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

/**
 * This is the Encoder class. This tracks the position of a motor of class DcMotorEx. The motor
 * must have an encoder attached. It can also get changes in position.
 *
 * <p>All hardware access is routed through the Synapse hardware thread via the {@link SafeDevice}
 * wrapper so encoder reads/writes are safe to call from the OpMode loop.
 *
 * @author Anyi Lin - 10158 Scott's Bots
 * @version 2.0, 9/7/2026
 */
public class Encoder {
    private final SafeDevice<DcMotorEx> motor;
    private double previousPosition;
    private double currentPosition;
    private double multiplier;

    public final static double FORWARD = 1, REVERSE = -1;

    /**
     * This creates a new Encoder from a SafeDevice&lt;DcMotorEx&gt;.
     *
     * @param motor the wrapped motor this will be tracking
     */
    public Encoder(SafeDevice<DcMotorEx> motor) {
        this.motor = motor;
        multiplier = FORWARD;
        reset();
    }

    /**
     * This sets the direction/multiplier of the Encoder. Setting 1 or -1 will make the Encoder track
     * forward or in reverse, respectively. Any multiple of either one will scale the Encoder's output
     * by that amount.
     *
     * @param setMultiplier the multiplier/direction to set
     */
    public void setDirection(double setMultiplier) {
        multiplier = setMultiplier;
    }

    /**
     * This resets the Encoder's position and the current and previous position in the code.
     */
    public void reset() {
        try {
            motor.call(m -> {
                m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                previousPosition = m.getCurrentPosition();
                currentPosition = m.getCurrentPosition();
                m.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This updates the Encoder's tracked current position and previous position.
     */
    public void update() {
        try {
            motor.call(m -> {
                previousPosition = currentPosition;
                currentPosition = m.getCurrentPosition();
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This returns the multiplier/direction of the Encoder.
     *
     * @return returns the multiplier
     */
    public double getMultiplier() {
        final boolean forward;
        try {
            forward = motor.call(m -> m.getDirection() == DcMotorSimple.Direction.FORWARD);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return multiplier * (forward ? 1 : -1);
    }

    /**
     * This returns the change in position from the previous position to the current position. One
     * important thing to note is that this encoder does not track velocity, only change in position.
     * This is because I am using a pose exponential method of localization, which doesn't need the
     * velocity of the encoders. Velocity of the robot is calculated in the localizer using an elapsed
     * time timer there.
     *
     * @return returns the change in position of the Encoder
     */
    public double getDeltaPosition() {
        return getMultiplier() * (currentPosition - previousPosition);
    }
}
