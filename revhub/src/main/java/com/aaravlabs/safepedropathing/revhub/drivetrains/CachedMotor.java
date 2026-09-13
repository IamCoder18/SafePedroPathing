package com.aaravlabs.safepedropathing.revhub.drivetrains;

import com.aaravlabs.synapse.ftc.SafeDevice;
import com.aaravlabs.safepedropathing.utils.Utils;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

/**
 * A motor wrapper that caches power writes, only touching the hardware when the requested power
 * differs enough from the last written power. All hardware access is routed through Synapse's
 * {@link SafeDevice} wrapper so calls are executed on the orchestrator's hardware thread.
 */
public class CachedMotor {
    private final SafeDevice<DcMotorEx> motor;

    private double power = 0;
    private final double powerThreshold;
    private DcMotor.ZeroPowerBehavior zeroPowerBehavior;
    private DcMotorSimple.Direction direction;

    public CachedMotor(SafeDevice<DcMotorEx> motor, double powerThreshold) {
        this.motor = motor;
        this.powerThreshold = powerThreshold;
    }

    public void setPower(double power) {
        if (Double.isNaN(power) || Double.isInfinite(power)) return;

        double desired = Utils.clamp(power, -1, 1);

        boolean exceedsThreshold = Math.abs(this.power - desired) >= powerThreshold;
        boolean switchesSigns = Math.signum(this.power) != Math.signum(desired);

        if (exceedsThreshold || switchesSigns) {
            this.power = desired;
            final double p = desired;
            motor.run(m -> m.setPower(p));
        }
    }

    public void setZeroPowerBehavior(DcMotor.ZeroPowerBehavior behavior) {
        if (zeroPowerBehavior != behavior) {
            zeroPowerBehavior = behavior;
            motor.run(m -> m.setZeroPowerBehavior(behavior));
        }
    }

    public void setDirection(DcMotorSimple.Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
            motor.run(m -> m.setDirection(direction));
        }
    }

    public SafeDevice<DcMotorEx> raw() {
        return motor;
    }
}
