package org.team100.lib.motion.drivetrain;

import org.team100.lib.controller.drivetrain.HolonomicDriveControllerFactory;
import org.team100.lib.controller.drivetrain.HolonomicFieldRelativeController;
import org.team100.lib.geometry.GeometryUtil;
import org.team100.lib.localization.SwerveDrivePoseEstimator100;
import org.team100.lib.localization.VisionData;
import org.team100.lib.logging.LoggerFactory;
import org.team100.lib.logging.TestLoggerFactory;
import org.team100.lib.logging.primitive.TestPrimitiveLogger;
import org.team100.lib.motion.drivetrain.kinodynamics.SwerveKinodynamics;
import org.team100.lib.motion.drivetrain.kinodynamics.SwerveKinodynamicsFactory;
import org.team100.lib.motion.drivetrain.module.SwerveModuleCollection;
import org.team100.lib.sensors.Gyro;
import org.team100.lib.sensors.SimulatedGyro;
import org.team100.lib.swerve.AsymSwerveSetpointGenerator;

/**
 * A real swerve subsystem populated with simulated motors and encoders,
 * for testing.
 */
public class Fixture {
    public SwerveModuleCollection collection;
    public Gyro gyro;
    public SwerveDrivePoseEstimator100 poseEstimator;
    public SwerveKinodynamics swerveKinodynamics;
    public SwerveLocal swerveLocal;
    public SwerveDriveSubsystem drive;
    public HolonomicFieldRelativeController controller;
    public LoggerFactory logger;
    public LoggerFactory fieldLogger;

    public Fixture() {
        logger = new TestLoggerFactory(new TestPrimitiveLogger());
        fieldLogger = new TestLoggerFactory(new TestPrimitiveLogger());
        swerveKinodynamics = SwerveKinodynamicsFactory.forTest();
        collection = SwerveModuleCollection.get(logger, 10, 20, swerveKinodynamics);
        gyro = new SimulatedGyro(swerveKinodynamics, collection);
        final AsymSwerveSetpointGenerator setpointGenerator = new AsymSwerveSetpointGenerator(
                logger,
                swerveKinodynamics,
                () -> 12);
        swerveLocal = new SwerveLocal(logger, swerveKinodynamics, setpointGenerator, collection);
        poseEstimator = swerveKinodynamics.newPoseEstimator(
                logger,
                gyro,
                collection.positions(),
                GeometryUtil.kPoseZero,
                0); // initial time is zero here for testing
        VisionData v = new VisionData() {
            @Override
            public void update() {
            }
        };

        drive = new SwerveDriveSubsystem(
                fieldLogger,
                logger,
                gyro,
                poseEstimator,
                swerveLocal,
                v);

        controller = HolonomicDriveControllerFactory.get(logger,
                new HolonomicFieldRelativeController.Log(logger));
    }

    public void close() {
        // close the DIO inside the turning encoder
        collection.close();
    }

}
