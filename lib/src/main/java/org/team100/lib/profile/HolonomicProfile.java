package org.team100.lib.profile;

import org.team100.lib.framework.TimedRobot100;
import org.team100.lib.motion.drivetrain.SwerveControl;
import org.team100.lib.motion.drivetrain.SwerveModel;
import org.team100.lib.profile.Profile100.ResultWithETA;
import org.team100.lib.state.Control100;
import org.team100.lib.state.Model100;
import org.team100.lib.util.Util;

import edu.wpi.first.math.MathUtil;

/**
 * Coordinates three axes so that their profiles complete at about the same
 * time, by adjusting the maximum allowed acceleration.
 * 
 * Note that because acceleration is adjusted, but not cruise velocity, the
 * resulting paths will not be straight, for rest-to-rest profiles.
 */
public class HolonomicProfile {
    /** For testing */
    private static final boolean DEBUG = false;
    private static final double ETA_TOLERANCE = 0.02;
    private static final double kDt = TimedRobot100.LOOP_PERIOD_S;

    private final TrapezoidProfile100 px;
    private final TrapezoidProfile100 py;
    private final TrapezoidProfile100 ptheta;

    private TrapezoidProfile100 ppx;
    private TrapezoidProfile100 ppy;
    private TrapezoidProfile100 pptheta;

    public HolonomicProfile(
            double maxXYVel,
            double maxXYAccel,
            double xyTolerance,
            double maxAngularVel,
            double maxAngularAccel,
            double angularTolerance) {
        px = new TrapezoidProfile100(maxXYVel, maxXYAccel, xyTolerance);
        py = new TrapezoidProfile100(maxXYVel, maxXYAccel, xyTolerance);
        ptheta = new TrapezoidProfile100(maxAngularVel, maxAngularAccel, angularTolerance);
        // default scale is 1.
        ppx = px;
        ppy = py;
        pptheta = ptheta;
    }

    /** Reset the scale factors. */
    public void solve(SwerveModel i, SwerveModel g) {
        // first find the max ETA
        if (DEBUG) {
            Util.printf("ix %s gx %s\n", i.x(), g.x());
        }
        ResultWithETA rx = px.calculateWithETA(kDt, i.x(), g.x());
        ResultWithETA ry = py.calculateWithETA(kDt, i.y(), g.y());
        ResultWithETA rtheta = ptheta.calculateWithETA(kDt, i.theta(), g.theta());
        if (DEBUG) {
            Util.printf("rx %.3f ry %.3f rtheta %.3f\n", rx.etaS(), ry.etaS(), rtheta.etaS());
        }
        double slowETA = rx.etaS();
        slowETA = Math.max(slowETA, ry.etaS());
        slowETA = Math.max(slowETA, rtheta.etaS());

        double sx = px.solve(kDt, i.x(), g.x(), slowETA, ETA_TOLERANCE);
        double sy = py.solve(kDt, i.y(), g.y(), slowETA, ETA_TOLERANCE);
        double stheta = ptheta.solve(kDt, i.theta(), g.theta(), slowETA, ETA_TOLERANCE);

        if (DEBUG) {
            Util.printf("sx %.3f sy %.3f stheta %.3f\n", sx, sy, stheta);
        }

        ppx = px.scale(sx);
        ppy = py.scale(sy);
        pptheta = ptheta.scale(stheta);
    }

    /** Compute the control for the end of the next time step */
    public SwerveControl calculate(SwerveModel i, SwerveModel g) {
        if (i == null || g == null) {
            // this can happen on startup when the initial state hasn't yet been defined,
            // but the memo refresher is trying to update the references.
            return SwerveControl.zero();
        }
        if (DEBUG) {
            Util.printf("initial %s goal %s\n", i, g);
        }
        Control100 stateX = ppx.calculate(kDt, i.x(), g.x());
        Control100 stateY = ppy.calculate(kDt, i.y(), g.y());
        // theta is periodic; choose a setpoint angle near the goal.
        Model100 theta = new Model100(
                MathUtil.angleModulus(i.theta().x() - g.theta().x()) + g.theta().x(),
                i.theta().v());
        Control100 stateTheta = pptheta.calculate(kDt, theta, g.theta());
        return new SwerveControl(stateX, stateY, stateTheta);
    }
}
