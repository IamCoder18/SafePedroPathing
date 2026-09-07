# Safe Pedro Pathing

[![Release](https://jitpack.io/v/IamCoder18/SafePedroPathing.svg)](https://jitpack.io/#IamCoder18/SafePedroPathing)
[![License: BSD-3-Clause](https://img.shields.io/badge/License-BSD_3--Clause-yellow.svg)](https://opensource.org/licenses/BSD-3-Clause)

A Synapse-safe fork of [Pedro Pathing](https://github.com/Pedro-Pathing/PedroPathing) designed to work with [Synapse](https://github.com/IamCoder18/synapse), the FTC pub/sub library with built-in hardware-thread safety.

Pedro Pathing is a path follower that revolutionizes autonomous pathing in robotics. Safe Pedro Pathing keeps the same API but routes every hardware call (motors, servos, encoders, IMUs, sensors) through Synapse's `SafeHardwareMap` / `SafeDevice<T>` wrappers so all device access happens on a single dedicated hardware thread — the only safe way to use `DcMotorEx`, servos, and bulk reads in an FTC program.

## Installation

### JitPack (preferred)

Add JitPack as a repository and pull the latest release:

```gradle
repositories {
    mavenCentral()
    google()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation 'com.github.IamCoder18:SafePedroPathing:ftc:<release-tag>'
}
```

Replace `<release-tag>` with the latest GitHub release tag (e.g. `v3.0.0`). The `ftc` artifact is the Android library; pull `core` as well only if you need the headless path-follower math outside the FTC SDK.

Browse builds and tags at https://jitpack.io/#IamCoder18/SafePedroPathing.

### GitHub Packages (fallback)

The library is also published to GitHub Packages at `https://maven.pkg.github.com/IamCoder18/SafePedroPathing`. GitHub Packages requires authentication even for public packages, so configure credentials first:

`~/.gradle/gradle.properties`:
```properties
githubUser=<your-github-username>
githubToken=<token-with-read:packages>
```

Then in your FTC project:
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
    implementation 'com.aaravlabs.safepedropathing:ftc:<version>'
}
```

## What's different from upstream Pedro Pathing

- **All `HardwareMap` references are gone.** Every drivetrain, localizer, IMU helper, and the `FollowerBuilder` now take a `SafeHardwareMap` instead.
- **Devices are `SafeDevice<T>` wrappers.** Motor, servo, encoder, IMU, and sensor reads/writes are routed through Synapse's hardware thread via `.run()`, `.call()`, and `.callAsync()`.
- **Encoder reads are synchronous.** `Encoder.update()` / `Encoder.reset()` use `SafeDevice.call(...)` so reads are guaranteed to finish before the next line runs on the OpMode loop.
- **`VoltageSensor`** is still accessed via `safeMap.raw().voltageSensor.iterator().next()` (no Synapse wrapper for it).
- **Package relocated** from `com.pedropathing.*` to `com.aaravlabs.safepedropathing.*` and Maven group from `com.pedropathing` to `com.aaravlabs.safepedropathing`.

## Minimal example

```java
import com.aaravlabs.synapse.ftc.HardwareActions;
import com.aaravlabs.synapse.ftc.SafeHardwareMap;
import com.aaravlabs.synapse.ftc.SafeOpMode;
import com.aaravlabs.synapse.ftc.FtcOrchestrator;
import com.aaravlabs.safepedropathing.follower.FollowerConstants;
import com.aaravlabs.safepedropathing.ftc.FollowerBuilder;
import com.aaravlabs.safepedropathing.ftc.localization.constants.PinpointConstants;

@TeleOp(name = "SafePedroDemo")
public class SafePedroDemo extends SafeOpMode {
    @Override protected void onSafeInit() {
        SafeHardwareMap safeMap = new SafeHardwareMap(hardwareMap, orchestrator.hardware());
        new FollowerBuilder(FollowerConstants.load(), safeMap)
                .pinpointLocalizer(PinpointConstants.lConstants)
                .mecanumDrivetrain(MecanumConstants.mConstants)
                .build();
    }

    @Override protected void onSafeLoop() {
        // follower.update();
    }
}
```

## Upstream

- Original Pedro Pathing: https://github.com/Pedro-Pathing/PedroPathing
- Tuning / quickstart guide: https://pedropathing.com/
- Synapse (the pub/sub library this fork depends on): https://github.com/IamCoder18/synapse

## License

BSD 3-Clause. See `LICENSE`.
