package com.aaravlabs.safepedropathing.revhub.localizers;

import com.aaravlabs.safepedropathing.config.ConfigVar;
import com.aaravlabs.safepedropathing.config.Configuration;
import com.aaravlabs.safepedropathing.math.Pose;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

public class OTOSConfig {
    public final ConfigVar<String> name = ConfigVar.required();
    public final ConfigVar<DistanceUnit> linearUnit = ConfigVar.of(DistanceUnit.INCH);
    public final ConfigVar<Double> linearScalar = ConfigVar.of(1.0);
    public final ConfigVar<Double> angularScalar = ConfigVar.of(1.0);
    public final ConfigVar<Pose> offset = ConfigVar.required();
    public OTOSConfig(Configuration<OTOSConfig> config) {
        config.configure(this);
    }
}
