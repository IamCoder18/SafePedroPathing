# Safe Pedro Pathing

[![License: BSD-3-Clause](https://img.shields.io/badge/License-BSD_3--Clause-yellow.svg)](https://opensource.org/licenses/BSD-3-Clause)

A Synapse-safe fork of [Pedro Pathing 3.0.0](https://github.com/Pedro-Pathing/PedroPathing) designed to work with [Synapse](https://github.com/IamCoder18/synapse), the FTC pub/sub library with built-in hardware-thread safety.

Pedro Pathing is a path follower that revolutionizes autonomous pathing in robotics. Safe Pedro Pathing keeps the V3 API but routes every hardware call in the `revhub` module (motors, servos, encoders, IMUs, odometry computers) through Synapse's `SafeHardwareMap` / `SafeDevice<T>` wrappers so all device access happens on a single dedicated hardware thread — the only safe way to use `DcMotorEx`, servos, and bulk reads in an FTC program.

## Installation

The library is published to **GitHub Packages** at `https://maven.pkg.github.com/IamCoder18/SafePedroPathing`. GitHub Packages requires authentication even for public packages, so configure credentials first.

`~/.gradle/gradle.properties`:
```properties
githubUser=<your-github-username>
githubToken=<token-with-read:packages>
```

Then in your FTC project (`build.dependencies.gradle`):
```gradle
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/IamCoder18/SafePedroPathing")
        credentials {
            username = findProperty("githubUser") ?: System.getenv("GITHUB_USER") ?: System.getenv("GITHUB_ACTOR")
            password = findProperty("githubToken") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation 'com.aaravlabs.safepedropathing:revhub:3.0.0'
}
```

`:revhub` pulls in `:core` (the hardware-free follower) and Synapse (from Maven Central) transitively.

## What's different from upstream Pedro Pathing

- **`revhub` classes take a `SafeHardwareMap` instead of a `HardwareMap`.** Every drivetrain (`Mecanum`, `Swerve`, `CoaxialPod`), localizer (`Pinpoint`, `OTOS`, `OctoQuad`, `TwoWheel`, `ThreeWheel`, `ThreeWheelIMU`), `Encoder`, `CachedMotor`, and `CustomIMU`/`RevHubIMU` constructor now takes Synapse's `SafeHardwareMap`.
- **Devices are `SafeDevice<T>` wrappers.** Writes go through `.run(...)` (async, FIFO on the hardware thread) and reads through `.call(...)` (blocking). There is no direct `HardwareMap.get(...)` anywhere in `revhub`.
- **`core` is untouched by design.** Upstream V3 already made the `core` module hardware-free (`Follower`, paths, `Algorithm`, configs), so it needs no fork.
- **No `FollowerBuilder`.** Upstream V3 dropped it; you construct `new Follower(localizer, drivetrain, algorithm)` directly (see example below).
- **`VoltageSensor`** is still accessed via `safeMap.raw().voltageSensor.iterator().next()` in `Swerve` (no Synapse wrapper for it).
- **Package relocated** from `com.pedropathing.*` to `com.aaravlabs.safepedropathing.*` and Maven group from `com.pedropathing` to `com.aaravlabs.safepedropathing`.

## Minimal example

```java
import com.aaravlabs.synapse.ftc.SafeOpMode;
import com.aaravlabs.safepedropathing.algorithm.Foresight;
import com.aaravlabs.safepedropathing.algorithm.ForesightConfig;
import com.aaravlabs.safepedropathing.api.Paths;
import com.aaravlabs.safepedropathing.follower.Follower;
import com.aaravlabs.safepedropathing.math.Pose;
import com.aaravlabs.safepedropathing.revhub.drivetrains.Mecanum;
import com.aaravlabs.safepedropathing.revhub.drivetrains.MecanumConfig;
import com.aaravlabs.safepedropathing.revhub.localizers.PinpointConfig;
import com.aaravlabs.safepedropathing.revhub.localizers.PinpointLocalizer;

@TeleOp(name = "SafePedroDemo")
public class SafePedroDemo extends SafeOpMode {
    private Follower follower;

    @Override
    protected void onSafeInit() {
        // safeMap is provided by SafeOpMode — all device access through it is hardware-thread safe.
        MecanumConfig driveConfig = new MecanumConfig(cfg -> {
            cfg.frontLeftName.set("frontLeft");
            cfg.frontRightName.set("frontRight");
            cfg.backLeftName.set("backLeft");
            cfg.backRightName.set("backRight");
            // cfg.*Direction.set(...) ...
        });

        PinpointConfig pinpointConfig = new PinpointConfig(cfg -> {
            cfg.name.set("pinpoint");
            cfg.xPodOffset.set(-3.7);
            cfg.yPodOffset.set(-1.2);
            cfg.xPodDirection.set(GoBildaPinpointDriver.EncoderDirection.REVERSED);
            cfg.yPodDirection.set(GoBildaPinpointDriver.EncoderDirection.FORWARD);
        });

        follower = new Follower(
                new PinpointLocalizer(safeMap, pinpointConfig),
                new Mecanum(safeMap, driveConfig),
                new Foresight(new ForesightConfig(cfg -> {
                    // headingFeedback, forwardTranslational, strafeTranslational, brake, coast ...
                }))
        );
    }

    @Override
    protected void onSafeStart() {
        follower.setPose(new Pose(0, 0, 0));
        follower.follow(Paths.line(new Pose(0, 0, 0), new Pose(24, 24, Math.PI / 2)));
    }

    @Override
    protected void onSafeLoop() {
        follower.update();
    }
}
```

## Upstream

- Original Pedro Pathing: https://github.com/Pedro-Pathing/PedroPathing
- Tuning / quickstart guide: https://pedropathing.com/
- Synapse (the pub/sub library this fork depends on): https://github.com/IamCoder18/synapse

## License

BSD 3-Clause. See `LICENSE`.
