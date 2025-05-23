package org.team100.lib.controller.drivetrain;

import org.team100.lib.config.Identity;
import org.team100.lib.logging.LoggerFactory;

/**
 * Known-good controller settings.
 */
public class SwerveControllerFactory {

    /** For real robots. */
    public static SwerveController byIdentity(LoggerFactory log) {
        switch (Identity.instance) {
            case COMP_BOT -> {
                return new FullStateSwerveController(log, 2.9, 3.5, 0.025, 0.01, 0.02, 0.3, 1, 1);
                // return new FullStateSwerveController(log, 0, 0, 0, 0, 0, 0, 0, 0);
            }
            case SWERVE_ONE -> {
                return new FullStateSwerveController(log, 3, 3.5, 0.05, 0, 0.01, 0.01, 1, 1);
            }
            case SWERVE_TWO -> {
                return new FullStateSwerveController(log, 4, 4, 0.25, 0.25, 0.01, 0.02, 0.01, 0.02);
            }
            default -> {
                return new FullStateSwerveController(log, 3.0, 3.5, 0, 0, 0.01, 0.01, 0.01, 0.01);
            }
        }
    }

    public static FullStateSwerveController ridiculous(LoggerFactory log) {
        return new FullStateSwerveController(log, 3, 3, 0.1, 0.1, 0.01, 0.01, 0.01, 0.01);
    }

    public static FullStateSwerveController fieldRelativeFancyPIDF(LoggerFactory log) {
        return new FullStateSwerveController(log, 2.4, 1.3, 0.1, 0.1, 0.01, 0.02, 0.01, 0.02);
    }

    public static FullStateSwerveController fieldRelativeGoodPIDF(LoggerFactory log) {
        return new FullStateSwerveController(log, 1, 1.3, 0.1, 0.1, 0.01, 0.02, 0.01, 0.02);
    }

    public static FullStateSwerveController autoFieldRelativePIDF(LoggerFactory log) {
        return new FullStateSwerveController(log, 1.5, 1.3, 0, 0, 0.1, 0.1, 0.1, 0.1);
    }

    public static FullStateSwerveController auto2025LooseTolerance(LoggerFactory log) {
        return new FullStateSwerveController(log, 7.2, 3.5, 0.055, 0.01, 0.035, 0.1, 1, 1);
    }



    ////////////////
    //
    // don't use these for real robots
    //

    public static FullStateSwerveController testFieldRelativePIDF(LoggerFactory log) {
        return new FullStateSwerveController(log, 2.4, 2.4, 0.1, 0.1, 0.01, 0.02, 0.01, 0.02);
    }   

    public static FullStateSwerveController testFieldRelativeFFOnly(LoggerFactory log) {
        return new FullStateSwerveController(log, 0, 0, 0, 0, 0.01, 0.02, 0.01, 0.02);
    }

    public static FullStateSwerveController test(LoggerFactory log) {
        return new FullStateSwerveController(log, 3.0, 3.5, 0, 0, 0.01, 0.01, 0.01, 0.01);
    }

    /** high gains used in tests. */
    public static FullStateSwerveController test2(LoggerFactory log) {
        return new FullStateSwerveController(log, 4, 4, 0.25, 0.25, 0.01, 0.02, 0.01, 0.02);
    }

    private SwerveControllerFactory() {
        // don't call this
    }

}
