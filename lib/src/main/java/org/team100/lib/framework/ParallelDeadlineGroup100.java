package org.team100.lib.framework;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.team100.lib.logging.Level;
import org.team100.lib.logging.LoggerFactory;
import org.team100.lib.logging.LoggerFactory.StringLogger;

import edu.wpi.first.util.sendable.SendableBuilder;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

/**
 * A command composition that runs a set of commands in parallel, ending only
 * when a specific
 * command (the "deadline") ends, interrupting all other commands that are still
 * running at that
 * point.
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
public class ParallelDeadlineGroup100 extends Command {
    // maps commands in this composition to whether they are still running
    // LinkedHashMap guarantees we iterate over commands in the order they were
    // added (Note that
    // changing the value associated with a command does NOT change the order)
    private final Map<Command, Boolean> m_commands = new LinkedHashMap<>();
    private boolean m_runWhenDisabled = true;
    private boolean m_finished = true;
    private Command m_deadline;
    private InterruptionBehavior m_interruptBehavior = InterruptionBehavior.kCancelIncoming;
    private final StringLogger m_log_active_commands;

    /**
     * Creates a new ParallelDeadlineGroup. The given commands, including the
     * deadline, will be
     * executed simultaneously. The composition will finish when the deadline
     * finishes, interrupting
     * all other still-running commands. If the composition is interrupted, only the
     * commands still
     * running will be interrupted.
     *
     * @param deadline      the command that determines when the composition ends
     * @param otherCommands the other commands to be executed
     * @throws IllegalArgumentException if the deadline command is also in the
     *                                  otherCommands argument
     */
    @SuppressWarnings("this-escape")
    public ParallelDeadlineGroup100(LoggerFactory parent, Command deadline, Command... otherCommands) {
        m_log_active_commands = parent.stringLogger(Level.TRACE, "active commands");
        setDeadline(deadline);
        addCommands(otherCommands);
    }

    /**
     * Sets the deadline to the given command. The deadline is added to the group if
     * it is not already
     * contained.
     *
     * @param deadline the command that determines when the group ends
     * @throws IllegalArgumentException if the deadline command is already in the
     *                                  composition
     */
    public final void setDeadline(Command deadline) {
        @SuppressWarnings("PMD.CompareObjectsWithEquals")
        boolean isAlreadyDeadline = deadline == m_deadline;
        if (isAlreadyDeadline) {
            return;
        }
        if (m_commands.containsKey(deadline)) {
            throw new IllegalArgumentException(
                    "The deadline command cannot also be in the other commands!");
        }
        addCommands(deadline);
        m_deadline = deadline;
    }

    /**
     * Adds the given commands to the group.
     *
     * @param commands Commands to add to the group.
     */
    public final void addCommands(Command... commands) {
        if (!m_finished) {
            throw new IllegalStateException(
                    "Commands cannot be added to a composition while it's running");
        }

        CommandScheduler.getInstance().registerComposedCommands(commands);

        for (Command command : commands) {
            if (!Collections.disjoint(command.getRequirements(), getRequirements())) {
                throw new IllegalArgumentException(
                        "Multiple commands in a parallel group cannot require the same subsystems");
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
        m_finished = false;
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
                if (commandRunning.getKey().equals(m_deadline)) {
                    m_finished = true;
                }
            }
        }
        m_log_active_commands.log(activeCommands::toString);
    }

    @Override
    public final void end(boolean interrupted) {
        m_log_active_commands.log(() -> "");
        for (Map.Entry<Command, Boolean> commandRunning : m_commands.entrySet()) {
            if (commandRunning.getValue()) {
                commandRunning.getKey().end(true);
            }
        }
    }

    @Override
    public final boolean isFinished() {
        return m_finished;
    }

    @Override
    public boolean runsWhenDisabled() {
        return m_runWhenDisabled;
    }

    @Override
    public InterruptionBehavior getInterruptionBehavior() {
        return m_interruptBehavior;
    }

    @Override
    public void initSendable(SendableBuilder builder) {
        super.initSendable(builder);

        builder.addStringProperty("deadline", m_deadline::getName, null);
    }
}
