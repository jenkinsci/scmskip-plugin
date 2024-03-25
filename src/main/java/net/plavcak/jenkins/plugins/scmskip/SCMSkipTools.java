package net.plavcak.jenkins.plugins.scmskip;

import hudson.AbortException;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogSet;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.servlet.ServletException;
import jenkins.model.CauseOfInterruption;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;

public class SCMSkipTools {

    private static final Logger LOGGER = Logger.getLogger(SCMSkipTools.class.getName());

    /**
     * Deletes given build.
     * @param build build to delete
     * @throws IOException if build cannot be deleted
     */
    public static void deleteBuild(AbstractBuild<?, ?> build) throws IOException {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Deleting AbstractBuild: '" + build.getId() + "'");
        }
        build.delete();
        AbstractProject<?, ?> project = build.getProject();
        project.updateNextBuildNumber(build.getNumber());
        project.save();
    }

    /**
     * Deletes given build.
     * @param run build to delete
     * @throws IOException if build cannot be deleted
     */
    public static void deleteRun(Run<?, ?> run) throws IOException {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Deleting Run: '" + run.getId() + "'");
        }
        run.delete();
        Job<?, ?> job = run.getParent();
        job.updateNextBuildNumber(run.number);
        job.save();
    }

    public static void tagRunForDeletion(Run<?, ?> run, boolean deleteBuild) throws IOException {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Build '" + run.getDisplayName() + "' set to delete: " + deleteBuild);
        }
        run.addAction(new SCMSkipDeleteEnvironmentAction(deleteBuild));
        run.save();
    }

    public static boolean isBuildToDelete(Run<?, ?> run) {
        SCMSkipDeleteEnvironmentAction action = run.getAction(SCMSkipDeleteEnvironmentAction.class);
        return action != null && action.isDeleteBuild();
    }

    /**
     * Inspect build for matched pattern in changelog and user cause.
     * @param run current build
     * @param matcher matcher object
     * @param listener runtime listener
     * @return true if at least one entry matched and build not started by user
     */
    public static boolean inspectChangeSetAndCause(Run<?, ?> run, SCMSkipMatcher matcher, TaskListener listener) {
        if (run.getCauses().stream().anyMatch(cause -> cause instanceof Cause.UserIdCause)) {
            return false;
        }
        if (run instanceof WorkflowRun) {
            return inspectChangeSet(((WorkflowRun) run).getChangeSets(), matcher, listener.getLogger());
        } else if (run instanceof AbstractBuild) {
            return inspectChangeSet(((AbstractBuild<?, ?>) run).getChangeSets(), matcher, listener.getLogger());
        }

        return false;
    }

    private static boolean inspectChangeSet(
            List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeLogSets,
            SCMSkipMatcher matcher,
            PrintStream logger) {
        if (changeLogSets.isEmpty()) {
            logEmptyChangeLog(logger);
            return false;
        }

        ChangeLogSet<? extends ChangeLogSet.Entry> changeLogSet = changeLogSets.get(changeLogSets.size() - 1);

        if (changeLogSet.isEmptySet()) {
            logEmptyChangeLog(logger);
        }
        String notMatched = "";
        boolean allSkipped = true;

        for (ChangeLogSet.Entry entry : changeLogSet) {
            String fullMessage = getFullMessage(entry);
            if (!matcher.match(fullMessage)) {
                // if any of the changelog messages do not have the matching skip statement, then flag this
                allSkipped = false;
                notMatched = fullMessage;
                break;
            }
        }

        String commitMessage = combineChangeLogMessages(changeLogSet);

        if (!allSkipped) {
            logger.println(
                    "SCM Skip: Pattern " + matcher.getPattern().pattern() + " NOT matched on message: " + notMatched);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                        Level.FINE,
                        "SCM Skip: Pattern "
                                + matcher.getPattern().pattern()
                                + " NOT matched on message: "
                                + notMatched);
            }
        } else {
            logger.println(
                    "SCM Skip: Pattern " + matcher.getPattern().pattern() + " matched on message: " + commitMessage);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                        Level.FINE,
                        "SCM Skip: Pattern "
                                + matcher.getPattern().pattern()
                                + " matched on message: "
                                + commitMessage);
            }
        }

        return allSkipped;
    }

    /**
     * @param run run to be terminated
     * @throws IOException may throw AbortException specifically to terminate build
     * @throws ServletException when build stopping fails
     * @throws FlowInterruptedException to terminate pipeline build
     */
    public static void stopBuild(Run<?, ?> run) throws IOException, ServletException, FlowInterruptedException {
        run.setDescription("SCM Skip - build skipped");
        run.setResult(Result.ABORTED);
        run.save();

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Stopping build: '" + run.getId() + "'");
        }

        if (run instanceof WorkflowRun) {
            throw new FlowInterruptedException(Result.NOT_BUILT, true, new CauseOfInterruption() {
                @Override
                public String getShortDescription() {
                    return "Skipped because of SCM message";
                }
            });
        } else if (run instanceof AbstractBuild) {
            AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;
            build.doStop();
        } else {
            throw new AbortException("SCM Skip: Build has been skipped due to SCM Skip Plugin!");
        }
    }

    private static String combineChangeLogMessages(ChangeLogSet<?> changeLogSet) {
        return StreamSupport.stream(changeLogSet.spliterator(), false)
                .map(SCMSkipTools::getFullMessage)
                .collect(Collectors.joining(" "));
    }

    private static void logEmptyChangeLog(PrintStream logger) {
        logger.println("SCM Skip: Changelog is empty!");
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Changelog is empty!");
        }
    }

    private static String getFullMessage(ChangeLogSet.Entry entry) {
        if (Jenkins.get().getPlugin("git") != null) {
            return GitMessageExtractor.getFullMessage(entry);
        }
        return entry.getMsg();
    }
}
