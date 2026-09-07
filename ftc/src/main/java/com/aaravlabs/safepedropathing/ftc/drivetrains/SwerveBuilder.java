package com.aaravlabs.safepedropathing.ftc.drivetrains;

import com.aaravlabs.synapse.ftc.SafeHardwareMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for Swerve drivetrains.
 * @author Kabir Goyal
 */
public class SwerveBuilder {
    private final SafeHardwareMap safeMap;
    private final SwerveConstants constants;
    private final List<SwervePod> pods = new ArrayList<>();

    /**
     * @param constants Swerve Constants for your bot
     */
    public SwerveBuilder(SafeHardwareMap safeMap, SwerveConstants constants) {
        this.safeMap = safeMap;
        this.constants = constants;
    }

    /**
     * Add a configured SwervePod implementation to the builder.
     * Pods should be fully configured before building the drivetrain.
     *
     * @param pod configured swerve pod
     * @return this builder
     */
    public SwerveBuilder addPod(SwervePod pod) {
        if (pod == null) throw new IllegalArgumentException("pod cannot be null");
        pods.add(pod);
        return this;
    }

    /**
     * Build the Swerve drivetrain with the added pods.
     *
     * @return constructed swerve drivetrain
     */
    public Swerve build() {
        return new Swerve(safeMap, constants, pods.toArray(new SwervePod[0]));
    }
}
