package org.team100.lib.commands.drivetrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.team100.lib.controller.drivetrain.HolonomicDriveControllerFactory;
import org.team100.lib.controller.drivetrain.HolonomicFieldRelativeController;
import org.team100.lib.experiments.Experiment;
import org.team100.lib.experiments.Experiments;
import org.team100.lib.logging.LoggerFactory;
import org.team100.lib.logging.TestLoggerFactory;
import org.team100.lib.logging.primitive.TestPrimitiveLogger;
import org.team100.lib.motion.drivetrain.Fixtured;
import org.team100.lib.motion.drivetrain.kinodynamics.SwerveModuleState100;
import org.team100.lib.state.Control100;
import org.team100.lib.testing.Timeless;
import org.team100.lib.timing.TimingConstraint;
import org.team100.lib.timing.TimingConstraintFactory;
import org.team100.lib.trajectory.TrajectoryMaker;
import org.team100.lib.util.Util;
import org.team100.lib.visualization.TrajectoryVisualization;

import edu.wpi.first.wpilibj.DataLogManager;

class TrajectoryListCommandTest extends Fixtured implements Timeless {
    boolean dump = false;
    private static final double kDelta = 0.001;
    private static final double kDtS = 0.02;
    private static final LoggerFactory logger = new TestLoggerFactory(new TestPrimitiveLogger());
    private static final TrajectoryVisualization viz = new TrajectoryVisualization(logger);

    List<TimingConstraint> constraints = new TimingConstraintFactory(fixture.swerveKinodynamics).allGood();
    TrajectoryMaker maker = new TrajectoryMaker(constraints);

    @BeforeEach
    void nolog() {
        DataLogManager.stop();
    }

    @Test
    void testSimple() {
        Experiments.instance.testOverride(Experiment.UseSetpointGenerator, true);
        HolonomicFieldRelativeController control = HolonomicDriveControllerFactory.get(
                logger,
                new HolonomicFieldRelativeController.Log(logger));
        TrajectoryListCommand c = new TrajectoryListCommand(
                logger,
                fixture.drive,
                control,
                x -> List.of(maker.line(x)),
                viz);
        c.initialize();
        assertEquals(0, fixture.drive.getPose().getX(), kDelta);
        c.execute();
        assertFalse(c.isFinished());
        // the trajectory takes a little over 3s
        for (double t = 0; t < 3.1; t += kDtS) {
            stepTime(kDtS);
            c.execute();
            fixture.drive.periodic(); // for updateOdometry
        }
        // at goal; wide tolerance due to test timing
        assertTrue(c.isFinished());
        assertEquals(1.031, fixture.drive.getPose().getX(), 0.05);
    }

    /**
     * See also DriveInALittleSquareTest.
     * This exists to produce useful output to graph.
     */
    @Test
    void testLowLevel() {
        HolonomicFieldRelativeController controller = HolonomicDriveControllerFactory.get(
                logger,
                new HolonomicFieldRelativeController.Log(logger));
        TrajectoryListCommand command = new TrajectoryListCommand(
                logger,
                fixture.drive,
                controller,
                x -> maker.square(x),
                viz);
        Experiments.instance.testOverride(Experiment.UseSetpointGenerator, false);
        fixture.drive.periodic();
        command.initialize();
        int counter = 0;
        do {
            counter++;
            if (counter > 1000)
                fail("counter exceeded");
            stepTime(kDtS);
            fixture.drive.periodic();
            command.execute();
            double measurement = fixture.drive.getSwerveLocal().states().frontLeft().angle().get().getRadians();
            SwerveModuleState100 goal = fixture.swerveLocal.getDesiredStates().frontLeft();
            Control100 setpoint = fixture.swerveLocal.getSetpoints()[0];
            // this output is useful to see what's happening.
            if (dump)
                Util.printf("goal %5.3f setpoint x %5.3f setpoint v %5.3f measurement %5.3f\n",
                        goal.angle().get().getRadians(),
                        setpoint.x(),
                        setpoint.v(),
                        measurement);
        } while (!command.isFinished());
    }
}
