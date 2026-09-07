package com.aaravlabs.safepedropathing.ftc;
import com.aaravlabs.synapse.ftc.SafeHardwareMap;
import com.aaravlabs.safepedropathing.drivetrain.Drivetrain;
import com.aaravlabs.safepedropathing.follower.Follower;
import com.aaravlabs.safepedropathing.follower.FollowerConstants;
import com.aaravlabs.safepedropathing.ftc.drivetrains.*;
import com.aaravlabs.safepedropathing.ftc.localization.constants.DriveEncoderConstants;
import com.aaravlabs.safepedropathing.ftc.localization.constants.OctoQuadConstants;
import com.aaravlabs.safepedropathing.ftc.localization.constants.OTOSConstants;
import com.aaravlabs.safepedropathing.ftc.localization.constants.PinpointConstants;
import com.aaravlabs.safepedropathing.ftc.localization.constants.ThreeWheelConstants;
import com.aaravlabs.safepedropathing.ftc.localization.constants.ThreeWheelIMUConstants;
import com.aaravlabs.safepedropathing.ftc.localization.constants.TwoWheelConstants;
import com.aaravlabs.safepedropathing.ftc.localization.localizers.DriveEncoderLocalizer;
import com.aaravlabs.safepedropathing.ftc.localization.localizers.OctoQuadLocalizer;
import com.aaravlabs.safepedropathing.ftc.localization.localizers.OTOSLocalizer;
import com.aaravlabs.safepedropathing.ftc.localization.localizers.PinpointLocalizer;
import com.aaravlabs.safepedropathing.ftc.localization.localizers.ThreeWheelIMULocalizer;
import com.aaravlabs.safepedropathing.ftc.localization.localizers.ThreeWheelLocalizer;
import com.aaravlabs.safepedropathing.ftc.localization.localizers.TwoWheelLocalizer;
import com.aaravlabs.safepedropathing.localization.Localizer;
import com.aaravlabs.safepedropathing.paths.PathConstraints;

/** This is the FollowerBuilder.
 * It is used to create Followers with a specific drivetrain + localizer without having to use a full constructor
 *
 * @author Baron Henderson - 20077 The Indubitables
 */
public class FollowerBuilder {
    private final FollowerConstants constants;
    private PathConstraints constraints;
    private final SafeHardwareMap safeMap;
    private Localizer localizer;
    private Drivetrain drivetrain;

    public FollowerBuilder(FollowerConstants constants, SafeHardwareMap safeMap) {
        this.constants = constants;
        this.safeMap = safeMap;
        constraints = PathConstraints.defaultConstraints;
    }

    public FollowerBuilder setLocalizer(Localizer localizer) {
        this.localizer = localizer;
        return this;
    }

    public FollowerBuilder driveEncoderLocalizer(DriveEncoderConstants lConstants) {
        return setLocalizer(new DriveEncoderLocalizer(safeMap, lConstants));
    }

    public FollowerBuilder octoQuadLocalizer(OctoQuadConstants lConstants, OctoQuadLocalizer.InitMode initMode) {
        return setLocalizer(new OctoQuadLocalizer(safeMap, lConstants, initMode));
    }

    public FollowerBuilder OTOSLocalizer(OTOSConstants lConstants) {
        return setLocalizer(new OTOSLocalizer(safeMap, lConstants));
    }

    public FollowerBuilder pinpointLocalizer(PinpointConstants lConstants) {
        return setLocalizer(new PinpointLocalizer(safeMap, lConstants));
    }

    public FollowerBuilder threeWheelIMULocalizer(ThreeWheelIMUConstants lConstants) {
        return setLocalizer(new ThreeWheelIMULocalizer(safeMap, lConstants));
    }

    public FollowerBuilder threeWheelLocalizer(ThreeWheelConstants lConstants) {
        return setLocalizer(new ThreeWheelLocalizer(safeMap, lConstants));
    }

    public FollowerBuilder twoWheelLocalizer(TwoWheelConstants lConstants) {
        return setLocalizer(new TwoWheelLocalizer(safeMap, lConstants));
    }

    public FollowerBuilder setDrivetrain(Drivetrain drivetrain) {
        this.drivetrain = drivetrain;
        return this;
    }

    public FollowerBuilder mecanumDrivetrain(MecanumConstants mecanumConstants) {
        return setDrivetrain(new Mecanum(safeMap, mecanumConstants));
    }

    @Deprecated
    public FollowerBuilder mecanumExDrivetrain(MecanumConstants mecanumConstants) {
        return setDrivetrain(new MecanumEx(safeMap, mecanumConstants));
    }

    public FollowerBuilder swerveDrivetrain(SwerveConstants swerveConstants, SwervePod... pods) {
        return setDrivetrain(new Swerve(safeMap, swerveConstants, pods));
    }

    public FollowerBuilder pathConstraints(PathConstraints pathConstraints) {
        this.constraints = pathConstraints;
        PathConstraints.setDefaultConstraints(pathConstraints);
        return this;
    }

    public Follower build() {
        return new Follower(constants, localizer, drivetrain, constraints);
    }
}
