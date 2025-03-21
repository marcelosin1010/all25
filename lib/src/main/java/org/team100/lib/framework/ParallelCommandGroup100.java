package org.team100.lib.framework;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.team100.lib.logging.Level;
import org.team100.lib.logging.LoggerFactory;
import org.team100.lib.logging.LoggerFactory.StringLogger;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

/**
 * A command composition that runs a set of commands in parallel, ending when
 * the last command ends.
 *
 * <p>
 * The rules for command compositions apply: command instances that are passed
 * to it cannot be
 * added to any other composition or scheduled individually, and the composition
 * requires all
 * subsystems its components require.
 *
 * <p>
 * This class is provided by the NewCommands VendorDep
 */
public class ParallelCommandGroup100 extends Command {
    // maps commands in this composition to whether they are still running
    // LinkedHashMap guarantees we iterate over commands in the order they were
    // added (Note that
    // changing the value associated with a command does NOT change the order)
    private final Map<Command, Boolean> m_commands = new LinkedHashMap<>();
    private boolean m_runWhenDisabled = true;
    private InterruptionBehavior m_interruptBehavior = InterruptionBehavior.kCancelIncoming;
    private final StringLogger m_log_active_commands;

    /**
     * Creates a new ParallelCommandGroup. The given commands will be executed
     * simultaneously. The
     * command composition will finish when the last command finishes. If the
     * composition is
     * interrupted, only the commands that are still running will be interrupted.
     *
     * @param commands the commands to include in this composition.
     */
    @SuppressWarnings("this-escape")
    public ParallelCommandGroup100(LoggerFactory parent, Command... commands) {
        m_log_active_commands = parent.stringLogger(Level.TRACE, "active commands");
        addCommands(commands);
    }

    /**
     * Adds the given commands to the group.
     *
     * @param commands Commands to add to the group.
     */
    public final void addCommands(Command... commands) {
        if (m_commands.containsValue(true)) {
            throw new IllegalStateException(
                    "Commands cannot be added to a composition while it's running");
        }

        CommandScheduler.getInstance().registerComposedCommands(commands);

        for (Command command : commands) {
            if (!Collections.disjoint(command.getRequirements(), getRequirements())) {
                throw new IllegalArgumentException(
                        "Multiple commands in a parallel composition cannot require the same subsystems");
            }
            m_commands.put(command, false);
            addRequirements(command.getRequirements());
            m_runWhenDisabled &= command.runsWhenDisabled();
            if (command.getInterruptionBehavior() == InterruptionBehavior.kCancelSelf) {
                m_interruptBehavior = InterruptionBehavior.kCancelSelf;
            }
        }
    }

    @Override
    public final void initialize() {
        for (Map.Entry<Command, Boolean> commandRunning : m_commands.entrySet()) {
            commandRunning.getKey().initialize();
            commandRunning.setValue(true);
        }
    }

    @Override
    public final void execute() {
        StringBuilder activeCommands = new StringBuilder();
        for (Map.Entry<Command, Boolean> commandRunning : m_commands.entrySet()) {
            if (!commandRunning.getValue()) {
                continue;
            }
            activeCommands.append(":");
            activeCommands.append(commandRunning.getKey().getName());
            commandRunning.getKey().execute();
            if (commandRunning.getKey().isFinished()) {
                commandRunning.getKey().end(false);
                commandRunning.setValue(false);
            }
        }
        m_log_active_commands.log(activeCommands::toString);
    }

    @Override
    public final void end(boolean interrupted) {
        m_log_active_commands.log(() -> "");
        if (interrupted) {
            for (Map.Entry<Command, Boolean> commandRunning : m_commands.entrySet()) {
                if (commandRunning.getValue()) {
                    commandRunning.getKey().end(true);
                }
            }
        }
    }

    @Override
    public final boolean isFinished() {
        return !m_commands.containsValue(true);
    }

    @Override
    public boolean runsWhenDisabled() {
        return m_runWhenDisabled;
    }

    @Override
    public InterruptionBehavior getInterruptionBehavior() {
        return m_interruptBehavior;
    }
}
