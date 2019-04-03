package net.plavcak.jenkins.plugins.scmskip;

import hudson.AbortException;
import hudson.model.*;
import hudson.scm.ChangeLogSet;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.logging.Logger;

public class SCMSkipTools {

    private static final Logger LOGGER = Logger.getLogger(SCMSkipTools.class.getName());

    /**
     * Deletes given build.
     * @param build build to delete
     * @throws IOException if build cannot be deleted
     */
    public static void deleteBuild(AbstractBuild build) throws IOException {
        LOGGER.info("Deleting AbstractBuild: " + build.getDisplayName());
        build.delete();
        AbstractProject project = build.getProject();
        project.updateNextBuildNumber(build.getNumber());
        project.save();
    }

    /**
     * Deletes given build.
     * @param run build to delete
     * @throws IOException if build cannot be deleted
     */
    public static void deleteRun(Run<?,?> run) throws IOException {
        LOGGER.info("Deleting Run: " + run.getDisplayName());
        run.delete();
        //run.getParent().updateNextBuildNumber(run.getNumber());
        //run.getParent().save();
    }

    public static void setRunToDelete(Run<?,?> run, boolean deleteBuild) throws IOException {
        LOGGER.info("Build set to delete: " + run.getDisplayName());
        run.addAction(new SCMSkipDeleteEnvironmentAction(deleteBuild));
        run.save();
    }

    public static boolean isBuildToDelete(Run<?,?> run) {
        SCMSkipDeleteEnvironmentAction action = run.getAction(SCMSkipDeleteEnvironmentAction.class);
        return action != null && action.isDeleteBuild();
    }

    /**
     * Return true if build should be deleted. Value is retrieved from build variables.
     * @param build current build
     * @return flag indicating build deletion
     */
    public static boolean isBuildToDelete(AbstractBuild build) {
        boolean deleteBuild = false;

        if (build.getBuildVariables().containsKey(SCMSkipConstants.DELETE_BUILD)) {
            Object deleteBuildObj = build.getBuildVariables().get(SCMSkipConstants.DELETE_BUILD);

            try {
                deleteBuild = Boolean.parseBoolean((String) deleteBuildObj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return deleteBuild;
    }

    /**
     * Inspect build for matched pattern in changelog.
     * @param run current build
     * @param matcher matcher object
     * @param listener runtime listener
     * @return true if matched otherwise returns false
     */
    public static boolean inspectChangeSet(Run run, SCMSkipMatcher matcher, TaskListener listener) {
        if (run instanceof WorkflowRun) {
            return inspectChangeSet(((WorkflowRun) run).getChangeSets(), matcher, listener.getLogger());
        } else if(run instanceof AbstractBuild) {
            return inspectChangeSet(((AbstractBuild<?,?>) run).getChangeSets(), matcher, listener.getLogger());
        }

        return false;
    }

    private static boolean inspectChangeSet(List<ChangeLogSet<?>> changeLogSets, SCMSkipMatcher matcher, PrintStream logger) {
        if (changeLogSets.isEmpty()) {
            logger.println("SCM Skip: Changelog is empty!");
            LOGGER.info("Changelog is empty!");
        } else {
            ChangeLogSet<?> changeLogSet = changeLogSets.get(changeLogSets.size()-1);

            ChangeLogSet.Entry item = (ChangeLogSet.Entry) changeLogSet.getItems()[changeLogSet.getItems().length-1];

            if (matcher.match(item.getMsg())) {
                logger.println("SCM Skip: Pattern "
                        + matcher.getPattern().pattern()
                        + " matched on message: "
                        + item.getMsg());
                LOGGER.info("SCM Skip: Pattern "
                        + matcher.getPattern().pattern()
                        + " matched on message: "
                        + item.getMsg());
                return true;
            } else {
                logger.println("SCM Skip: Pattern "
                        + matcher.getPattern().pattern()
                        + " NOT matched on message: "
                        + item.getMsg());
                LOGGER.info("SCM Skip: Pattern "
                        + matcher.getPattern().pattern()
                        + " NOT matched on message: "
                        + item.getMsg());
            }
        }
        return false;
    }

    public static void stopBuild(Run<?, ?> run) throws AbortException, IOException, ServletException {
        run.setDescription("SCM Skip - build skipped");
        run.setResult(Result.ABORTED);
        run.save();
        LOGGER.info("Stopping build...");
        if (run instanceof WorkflowRun) {
            WorkflowRun workflowRun = (WorkflowRun)run;
            workflowRun.doStop();
        } else if (run instanceof AbstractBuild) {
            AbstractBuild build = (AbstractBuild) run;
            build.doStop();
        } else {
            throw new AbortException("SCM Skip: Build has been skipped due to SCM Skip Plugin!");
        }
    }
}
